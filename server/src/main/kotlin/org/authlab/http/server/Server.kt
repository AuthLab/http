/*
 * MIT License
 *
 * Copyright (c) 2018 Johan Fylling
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.authlab.http.server

import org.authlab.http.Header
import org.authlab.http.Request
import org.authlab.http.Response
import org.authlab.http.ResponseLine
import org.authlab.http.bodies.Body
import org.authlab.http.bodies.BodyReader
import org.authlab.http.bodies.BodyWriter
import org.authlab.http.bodies.ByteBodyReader
import org.authlab.http.bodies.EmptyBody
import org.authlab.http.bodies.EmptyBodyWriter
import org.authlab.http.bodies.TextBody
import org.authlab.http.bodies.TextBodyReader
import org.authlab.util.loggerFor
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class Server(private val listeners: List<ServerListener>,
             private val handlers: List<Handler<*>> = emptyList(),
             private val filters: List<Filter> = emptyList(),
             private val transformers: List<Transformer> = emptyList(),
             private val initializers: List<Initializer> = emptyList(),
             private val finalizers: List<Finalizer> = emptyList(),
             private val rootContext: Context,
             private val upgradeInsecureRequestsTo: String?,
             private val threadPool: ThreadPoolExecutor) : Closeable {
    companion object {
        private val _logger = loggerFor<Server>()
    }

    private var _running = false

    val initialized: Boolean
        get() = listeners.all { it.initialized }

    fun start() {
        _logger.info("Starting server")

        _running = true

        listeners.forEach { listener ->
            listener.onAccept = ::onConnect
            listener.setup()
            threadPool.execute(listener)
        }
    }

    private fun onConnect(socket: Socket, listener: ServerListener) {
        _logger.debug("Incoming connection")

        val protocol = if (listener.secure) "https" else "http"

        threadPool.execute {
            _logger.debug("Handling connection")

            val inputStream = PushbackInputStream(socket.inputStream)
            val outputStream = socket.outputStream

            try {
                do {
                    if (!waitForInput(inputStream)) break

                    val request = Request.fromInputStreamWithoutBody(inputStream)

                    val context = MutableContext.mutableCopyOf(rootContext)

                    val noBodyRequest = ServerRequest(request, context, EmptyBody(), protocol)

                    initializers.filter { it.pathPattern.matcher(request.requestLine.location.safePath).matches() }
                            .forEach { initializer ->
                                _logger.trace("Initializing transaction with {}", initializer)

                                initializer.onRequest(noBodyRequest, context)
                            }

                    _logger.info("Incoming request: {}", request.requestLine)

                    val serverRequest: ServerRequest<*>
                    var serverResponse: ServerResponse

                    var keepAlive = false

                    if (!listener.secure && noBodyRequest.upgradeInsecureRequests && upgradeInsecureRequestsTo != null) {
                        _logger.info("Upgrading insecure request")

                        serverRequest = ServerRequest(request, context,
                                ByteBodyReader().read(inputStream, request.headers).getBody(),
                                protocol)

                        serverResponse = ServerResponse(Response(ResponseLine(307, "Moved Temporarily"))
                                .withHeader(Header("Location", upgradeInsecureRequestsTo))
                                .withHeader(Header("Vary", "Upgrade-Insecure-Requests")),
                                EmptyBodyWriter())
                    } else {
                        // Find handler by path
                        val handler = handlers.firstOrNull {
                            it.pathPattern.matcher(request.requestLine.location.safePath).matches()
                        }

                        if (handler != null) {
                            val requestResponsePair = handle(handler, request, context, inputStream, listener.secure)
                            serverRequest = requestResponsePair.first
                            serverResponse = requestResponsePair.second
                        } else {
                            serverRequest = ServerRequest(request, context,
                                    ByteBodyReader().read(inputStream, request.headers).getBody(),
                                    protocol)
                            serverResponse = ServerResponse(Response(ResponseLine(404, "Not Found")), EmptyBodyWriter())
                        }

                        keepAlive = serverRequest.keepAlive

                        transformers.filter { it.pathPattern.matcher(request.requestLine.location.safePath).matches() }
                                .forEach { transformer ->
                                    _logger.trace("Transforming response with {}", transformer)

                                    serverResponse = transformer.onResponse(serverRequest, serverResponse, context)
                                }
                    }

                    writeResponse(serverResponse.internalResponse, outputStream, serverResponse.bodyWriter)

                    finalizers.filter { it.pathPattern.matcher(request.requestLine.location.safePath).matches() }
                            .forEach { finalizer ->
                                _logger.trace("Finalizing transaction with {}", finalizer)

                                finalizer.onResponse(serverRequest, serverResponse, context)
                            }

                    if (keepAlive) {
                        _logger.debug("Connection keep-alive requested")
                    }
                } while (!socket.isClosed && keepAlive)
            } catch (e: Exception) {
                _logger.warn("Error processing request", e)

                writeResponse(Response(ResponseLine(500, "Server Error")), outputStream)

                // TODO: Also call finalizers here?
            } finally {
                _logger.debug("Closing socket")
                socket.close()
            }
        }
    }

    private fun waitForInput(inputStream: PushbackInputStream): Boolean {
        try {
            _logger.debug("Waiting for incoming request")

            val byte = inputStream.read()

            if (byte == -1) {
                _logger.debug("Connection closed by other side")
                return false
            }

            inputStream.unread(byte)
        } catch (e: IOException) {
            _logger.trace("Exception waiting for request", e)
            return false
        }

        return true
    }

    private fun writeResponse(response: Response, outputStream: OutputStream, bodyWriter: BodyWriter? = null) {
        _logger.info("Outgoing response: {}", response.responseLine)

        try {
            if (bodyWriter != null) {
                response.write(outputStream, bodyWriter)
            } else {
                response.write(outputStream)
            }
        } catch (e: IOException) {
            _logger.warn("Failed to send response", e)
        }
    }

    private fun <B : Body> handle(handler: Handler<B>, request: Request, context: MutableContext, inputStream: InputStream, secure: Boolean):
            Pair<ServerRequest<B>, ServerResponse> {
        val body = handler.bodyReader.read(inputStream, request.headers).getBody()
        val serverRequest = ServerRequest(request, context, body, if (secure) "https" else "http")

        filters.filter { it.pathPattern.matcher(request.requestLine.location.safePath).matches() }
                .forEach { filter ->
                    _logger.trace("Filtering request by {}", filter)

                    val filterResponse = filter.onRequest(serverRequest, context)
                    if (filterResponse != null) {
                        _logger.info("Response created by filter {}", filter)
                        return serverRequest to filterResponse
                    }
                }

        _logger.trace("Handling request by {}", handler)

        return serverRequest to handler.onRequest(serverRequest).build()
    }

    override fun close() {
        _logger.debug("Closing server")

        _running = false

        listeners.forEach { it.close() }

        threadPool.shutdown() // Disable new tasks from being submitted

        try {
            // Wait a while for existing tasks to terminate
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow() // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    _logger.error("Pool did not terminate")
                }
            }
        } catch (e: InterruptedException) {
            // (Re-)Cancel if current thread also interrupted
            threadPool.shutdownNow()
            // Preserve interrupt status
            Thread.currentThread().interrupt()
        }

        _logger.info("Server closed")
    }
}

fun buildServer(init: ServerBuilder.() -> Unit) = ServerBuilder(init).build()

@DslMarker
annotation class ServerMarker

@ServerMarker
open class ServerBuilder constructor() {
    var threadPoolSize: Int = 100
    var upgradeInsecureRequestsTo: String? = null
    var threadPool: ThreadPoolExecutor? = null

    private val _contextData = mutableMapOf<String, Any>()
    private val _listenerBuilders = mutableListOf<ServerListenerBuilder>()
    private val _filterBuilders = mutableListOf<FilterBuilder>()
    private val _transformerBuilders = mutableListOf<TransformerBuilder>()
    private val _initalizerBuilders = mutableListOf<InitializerBuilder>()
    private val _finalizerBuilders = mutableListOf<FinalizerBuilder>()
    private val _handlerBuilders = mutableListOf<HandlerBuilder<*, *>>()
    private var _defaultHandlerBuilder: HandlerBuilder<*, *>? = null

    constructor(init: ServerBuilder.() -> Unit) : this() {
        init()
    }

    fun context(init: () -> Pair<String, Any>) {
        val pair = init()
        _contextData[pair.first] = pair.second
    }

    fun listen(init: ServerListenerBuilder.() -> Unit) {
        _listenerBuilders.add(ServerListenerBuilder(init))
    }

    fun filter(init: FilterBuilder.() -> Unit) {
        _filterBuilders.add(FilterBuilder(init))
    }

    fun transform(init: TransformerBuilder.() -> Unit) {
        _transformerBuilders.add(TransformerBuilder(init))
    }

    fun initialize(init: InitializerBuilder.() -> Unit) {
        _initalizerBuilders.add(InitializerBuilder(init))
    }

    fun finally(init: FinalizerBuilder.() -> Unit) {
        _finalizerBuilders.add(FinalizerBuilder(init))
    }

    fun <R : BodyReader<B>, B : Body> handleCallback(entryPoint: String, bodyReader: R, handle: (ServerRequest<B>) -> ServerResponseBuilder) {
        _handlerBuilders.add(HandlerBuilder(bodyReader) {
            this.entryPoint = entryPoint
            onRequest(handle)
        })
    }

    fun handleCallback(entryPoint: String, handle: (ServerRequest<TextBody>) -> ServerResponseBuilder) {
        handleCallback(entryPoint, TextBodyReader(), handle)
    }

    fun <R : BodyReader<B>, B : Body> handle(bodyReader: R, init: HandlerBuilder<R, B>.() -> Unit) {
        _handlerBuilders.add(HandlerBuilder(bodyReader, init))
    }

    fun <R : BodyReader<B>, B : Body> handle(entryPoint: String, bodyReader: R,
                                             init: ServerResponseBuilder.(ServerRequest<B>) -> Unit) {
        handle(bodyReader) {
            this.entryPoint = entryPoint
            onRequest(init)
        }
    }

    fun handle(entryPoint: String, init: ServerResponseBuilder.(ServerRequest<TextBody>) -> Unit) {
        handle(entryPoint, TextBodyReader(), init)
    }

    fun <R : BodyReader<B>, B : Body> default(bodyReader: R, init: ServerResponseBuilder.(ServerRequest<B>) -> Unit) {
        _defaultHandlerBuilder = HandlerBuilder(bodyReader) {
            entryPoint = "*"
            onRequest(init)
        }
    }

    fun default(init: ServerResponseBuilder.(ServerRequest<TextBody>) -> Unit) {
        default(TextBodyReader(), init)
    }

    fun build(): Server {
        val listeners = _listenerBuilders.map(ServerListenerBuilder::build)
                .toMutableList()

        if (listeners.isEmpty()) {
            listeners.add(ServerListenerBuilder().build()) // Construct a default listener
        }

        val filters = _filterBuilders.map(FilterBuilder::build)

        val transformers = _transformerBuilders.map(TransformerBuilder::build)

        val initializers = _initalizerBuilders.map(InitializerBuilder::build)

        val finalizers = _finalizerBuilders.map(FinalizerBuilder::build)

        val handlers = _handlerBuilders.map(HandlerBuilder<*, *>::build)
                .toMutableList()

        _defaultHandlerBuilder?.apply { handlers.add(build()) }

        val threadPool = this.threadPool ?: Executors.newFixedThreadPool(threadPoolSize) as ThreadPoolExecutor

        return Server(listeners, handlers, filters, transformers, initializers, finalizers,
                MutableContext(_contextData), upgradeInsecureRequestsTo, threadPool)
    }
}
