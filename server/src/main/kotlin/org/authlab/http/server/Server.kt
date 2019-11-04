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
import org.authlab.http.bodies.DelayedBody
import org.authlab.http.bodies.DelayedBodyReader
import org.authlab.http.bodies.EmptyBody
import org.authlab.http.bodies.EmptyBodyWriter
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
             private val handlerHolders: List<HandlerHolder<*>> = emptyList(),
             private val filterHolders: List<FilterHolder> = emptyList(),
             private val transformerHolders: List<TransformerHolder> = emptyList(),
             private val initializerHolders: List<InitializerHolder> = emptyList(),
             private val finalizerHolders: List<FinalizerHolder> = emptyList(),
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

                    initializerHolders.filter { it.pathPattern.matcher(request.requestLine.location.safePath).matches() }
                            .forEach { holder ->
                                _logger.trace("Initializing transaction with {}", holder.initializer)

                                holder.initializer.onRequest(noBodyRequest, context)
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
                        val handlerHolder = handlerHolders.firstOrNull {
                            it.pathPattern.matcher(request.requestLine.location.safePath).matches()
                        }

                        if (handlerHolder != null) {
                            val requestResponsePair = handle(handlerHolder, request, context, inputStream, listener.secure)
                            serverRequest = requestResponsePair.first
                            serverResponse = requestResponsePair.second
                        } else {
                            serverRequest = ServerRequest(request, context,
                                    ByteBodyReader().read(inputStream, request.headers).getBody(),
                                    protocol)
                            serverResponse = ServerResponse(Response(ResponseLine(404, "Not Found")), EmptyBodyWriter())
                        }

                        keepAlive = serverRequest.keepAlive

                        transformerHolders.filter { it.pathPattern.matcher(request.requestLine.location.safePath).matches() }
                                .forEach { transformerHolder ->
                                    val transformer = transformerHolder.transformer
                                    _logger.trace("Transforming response with {}", transformer)

                                    serverResponse = transformer.onResponse(serverRequest, serverResponse, context)
                                }
                    }

                    writeResponse(serverResponse.internalResponse, outputStream, serverResponse.bodyWriter)

                    finalizerHolders.filter { it.pathPattern.matcher(request.requestLine.location.safePath).matches() }
                            .forEach { holder ->
                                _logger.trace("Finalizing transaction with {}", holder.finalizer)

                                holder.finalizer.onResponse(serverRequest, serverResponse, context)
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
            _logger.debug("Exception waiting for request", e)
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

    private fun <B : Body> handle(handlerHolder: HandlerHolder<B>, request: Request, context: MutableContext, inputStream: InputStream, secure: Boolean):
            Pair<ServerRequest<B>, ServerResponse> {
        val body = handlerHolder.bodyReader.read(inputStream, request.headers).getBody()
        val serverRequest = ServerRequest(request, context, body, if (secure) "https" else "http")

        filterHolders.filter { it.pathPattern.matcher(request.requestLine.location.safePath).matches() }
                .forEach { filterHolder ->
                    val filter = filterHolder.filter
                    _logger.trace("Filtering request with {} @ {}", filter, filterHolder.path)

                    val filterResult = filter.onRequest(serverRequest, context)

                    if (filterResult is AbortFilterResult) {
                        _logger.info("Response created by filter {} @ {}", filter, filterHolder.path)
                        return serverRequest to filterResult.response
                    }
                }

        _logger.trace("Handling request with {} @ {}", handlerHolder.handler, handlerHolder.path)

        return serverRequest to handlerHolder.handler.onRequest(serverRequest)
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
    private val _filterBuilders = mutableListOf<FilterHolderBuilder>()
    private val _transformerBuilders = mutableListOf<TransformerHolderBuilder>()
    private val _initalizerBuilders = mutableListOf<InitializerHolderBuilder>()
    private val _finalizerBuilders = mutableListOf<FinalizerHolderBuilder>()
    private val _handlerBuilders = mutableListOf<HandlerHolderBuilder<*>>()
    private var _defaultHandlerBuilder: HandlerHolderBuilder<*>? = null

    constructor(init: ServerBuilder.() -> Unit) : this() {
        init()
    }

    fun context(init: () -> Pair<String, Any>) {
        val pair = init()
        context(pair.first, pair.second)
    }

    fun context(key: String, value: Any): ServerBuilder {
        _contextData[key] = value
        return this
    }

    fun listen(init: ServerListenerBuilder.() -> Unit) {
        _listenerBuilders.add(ServerListenerBuilder(init))
    }

    fun filter(entryPoint: String, filterBuilder: FilterBuilder) {
        _filterBuilders.add(FilterHolderBuilder {
            this.entryPoint = entryPoint
            this.filterBuilder = filterBuilder
        })
    }

    fun filter(entryPoint: String, filter: Filter) {
        _filterBuilders.add(FilterHolderBuilder {
            this.entryPoint = entryPoint
            this.filter = filter
        })
    }

    fun transform(entryPoint: String, transformerBuilder: TransformerBuilder) {
        _transformerBuilders.add(TransformerHolderBuilder {
            this.entryPoint = entryPoint
            this.transformerBuilder = transformerBuilder
        })
    }

    fun transform(entryPoint: String, transformer: Transformer) {
        _transformerBuilders.add(TransformerHolderBuilder {
            this.entryPoint = entryPoint
            this.transformer = transformer
        })
    }

    fun initialize(entryPoint: String, initializerBuilder: InitializerBuilder) {
        _initalizerBuilders.add(InitializerHolderBuilder {
            this.entryPoint = entryPoint
            this.initializerBuilder = initializerBuilder
        })
    }

    fun initialize(entryPoint: String, initializer: Initializer) {
        _initalizerBuilders.add(InitializerHolderBuilder {
            this.entryPoint = entryPoint
            this.initializer = initializer
        })
    }

    fun initialize(initializerBuilder: InitializerBuilder) {
        initialize("*", initializerBuilder)
    }

    fun initialize(initializer: Initializer) {
        initialize("*", initializer)
    }

    fun finalize(entryPoint: String, finalizerBuilder: FinalizerBuilder) {
        _finalizerBuilders.add(FinalizerHolderBuilder {
            this.entryPoint = entryPoint
            this.finalizerBuilder = finalizerBuilder
        })
    }

    fun finalize(entryPoint: String, finalizer: Finalizer) {
        _finalizerBuilders.add(FinalizerHolderBuilder {
            this.entryPoint = entryPoint
            this.finalizer = finalizer
        })
    }

    fun finalize(finanlizerBuilder: FinalizerBuilder) {
        finalize("*", finanlizerBuilder)
    }

    fun finalize(finalizer: Finalizer) {
        finalize("*", finalizer)
    }

    fun <B : Body> handle(entryPoint: String, bodyReader: BodyReader<B>, handlerBuilder: HandlerBuilder<B>) {
        _handlerBuilders.add(HandlerHolderBuilder<B> {
            this.entryPoint = entryPoint
            this.handlerBuilder = handlerBuilder
            this.bodyReader = bodyReader
        })
    }

    fun <B : Body> handle(entryPoint: String, bodyReader: BodyReader<B>, handler: Handler<B>) {
        _handlerBuilders.add(HandlerHolderBuilder<B> {
            this.entryPoint = entryPoint
            this.handler = handler
            this.bodyReader = bodyReader
        })
    }

    fun handle(entryPoint: String, handlerBuilder: HandlerBuilder<DelayedBody>) {
        handle(entryPoint, DelayedBodyReader(), handlerBuilder)
    }

    fun handle(entryPoint: String, handler: Handler<DelayedBody>) {
        handle(entryPoint, DelayedBodyReader(), handler)
    }

    fun <B : Body> default(bodyReader: BodyReader<B>, handlerBuilder: HandlerBuilder<B>) {
        _defaultHandlerBuilder = HandlerHolderBuilder<B> {
            this.entryPoint = "*"
            this.handlerBuilder = handlerBuilder
            this.bodyReader = bodyReader
        }
    }

    fun <B : Body> default(bodyReader: BodyReader<B>, handler: Handler<B>) {
        _defaultHandlerBuilder = HandlerHolderBuilder<B> {
            this.entryPoint = "*"
            this.handler = handler
            this.bodyReader = bodyReader
        }
    }

    fun default(handlerBuilder: HandlerBuilder<DelayedBody>) {
        default(DelayedBodyReader(), handlerBuilder)
    }

    fun default(handler: Handler<DelayedBody>) {
        default(DelayedBodyReader(), handler)
    }

    fun build(): Server {
        val listeners = _listenerBuilders.map(ServerListenerBuilder::build)
                .toMutableList()

        if (listeners.isEmpty()) {
            listeners.add(ServerListenerBuilder().build()) // Construct a default listener
        }

        val filters = _filterBuilders.map(FilterHolderBuilder::build)

        val transformers = _transformerBuilders.map(TransformerHolderBuilder::build)

        val initializers = _initalizerBuilders.map(InitializerHolderBuilder::build)

        val finalizers = _finalizerBuilders.map(FinalizerHolderBuilder::build)

        val handlers = _handlerBuilders.map(HandlerHolderBuilder<*>::build)
                .toMutableList()

        _defaultHandlerBuilder?.apply { handlers.add(build()) }

        val threadPool = this.threadPool ?: Executors.newFixedThreadPool(threadPoolSize) as ThreadPoolExecutor

        return Server(listeners, handlers, filters, transformers, initializers, finalizers,
                MutableContext(_contextData), upgradeInsecureRequestsTo, threadPool)
    }
}
