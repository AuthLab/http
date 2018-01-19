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

import org.authlab.http.Request
import org.authlab.http.Response
import org.authlab.http.ResponseLine
import org.authlab.http.bodies.Body
import org.authlab.http.bodies.BodyReader
import org.authlab.http.bodies.StringBody
import org.authlab.http.bodies.StringBodyReader
import org.authlab.util.loggerFor
import java.io.Closeable
import java.io.InputStream
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class Server(private val listeners: List<ServerListener>,
             private val handlers: List<Handler<*>> = emptyList(),
             threadPoolSize: Int = 50) : Closeable {
    companion object {
        private val _logger = loggerFor<Server>()
    }

    private var _running = false
    private val _threadPool: ThreadPoolExecutor

    init {
        _logger.info("Creating thread pool (size=$threadPoolSize)")
        _threadPool = Executors.newFixedThreadPool(threadPoolSize) as ThreadPoolExecutor
    }

    val initialized: Boolean
        get() = listeners.all { it.initialized }

    fun start() {
        _logger.info("Starting server")

        _running = true

        listeners.forEach { listener ->
            listener.onAccept = ::onConnect
            listener.setup()
            _threadPool.execute(listener)
        }
    }

    private fun onConnect(socket: Socket) {
        _logger.trace("Handling connection on other thread")

        _threadPool.execute({
            _logger.trace("Handling connection")

            try {
                val request = Request.fromInputStreamWithoutBody(socket.inputStream)

                val handler = handlers.firstOrNull {
                    it.entryPointPattern.matcher(request.requestLine.location.safePath).matches()
                }

                if (handler != null) {
                    val serverResponse = handle(handler, request, socket.inputStream)

                    serverResponse.internalResponse
                            .write(socket.outputStream, serverResponse.bodyWriter)
                } else {
                    Response(ResponseLine(404, "Not Found"))
                            .write(socket.outputStream)
                }
            } catch (e: Exception) {
                _logger.warn("Error processing request", e)

                Response(ResponseLine(500, "Server Error"))
                        .write(socket.outputStream)
            } finally {
                _logger.debug("Closing socket")
                socket.close()
            }
        })
    }

    private fun <B : Body> handle(handler: Handler<B>, request: Request, inputStream: InputStream): ServerResponse {
        val body = handler.bodyReader.read(inputStream, request.headers).getBody()
        val serverRequest = ServerRequest(request, body)
        return handler.onRequest(serverRequest).build()
    }

    override fun close() {
        _logger.debug("Closing server")

        _running = false

        listeners.forEach { it.close() }

        _threadPool.shutdown() // Disable new tasks from being submitted

        try {
            // Wait a while for existing tasks to terminate
            if (!_threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                _threadPool.shutdownNow() // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!_threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    _logger.error("Pool did not terminate")
                }
            }
        } catch (e: InterruptedException) {
            // (Re-)Cancel if current thread also interrupted
            _threadPool.shutdownNow()
            // Preserve interrupt status
            Thread.currentThread().interrupt()
        }

        _logger.info("Server closed")
    }
}

fun buildServer(init: ServerBuilder.() -> Unit) = ServerBuilder(init).build()

class ServerBuilder constructor() {
    var threadPoolSize: Int = 100

    private val _listenerBuilders = mutableListOf<ServerListenerBuilder>()
    private val _handlerBuilders = mutableListOf<HandlerBuilder<*, *>>()
    private var _defaultHandlerBuilder: HandlerBuilder<*, *>? = null

    constructor(init: ServerBuilder.() -> Unit) : this() {
        init()
    }

    fun listen(init: ServerListenerBuilder.() -> Unit) {
        _listenerBuilders.add(ServerListenerBuilder(init))
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

    fun handle(entryPoint: String, init: ServerResponseBuilder.(ServerRequest<StringBody>) -> Unit) {
        handle(entryPoint, StringBodyReader(), init)
    }

    fun <R : BodyReader<B>, B : Body> default(bodyReader: R, init: ServerResponseBuilder.(ServerRequest<B>) -> Unit) {
        _defaultHandlerBuilder = HandlerBuilder(bodyReader) {
            entryPoint = "*"
            onRequest(init)
        }
    }

    fun default(init: ServerResponseBuilder.(ServerRequest<StringBody>) -> Unit) {
        default(StringBodyReader(), init)
    }

    fun build(): Server {
        val listeners = _listenerBuilders.map(ServerListenerBuilder::build)
                .toMutableList()

        if (listeners.isEmpty()) {
            listeners.add(ServerListenerBuilder().build()) // Construct a default listener
        }

        val handlers = _handlerBuilders.map(HandlerBuilder<*, *>::build)
                .toMutableList()

        _defaultHandlerBuilder?.apply { handlers.add(build()) }

        return Server(listeners, handlers, threadPoolSize)
    }
}
