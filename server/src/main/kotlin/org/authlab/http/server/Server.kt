package org.authlab.http.server

import org.authlab.util.loggerFor
import org.authlab.http.Request
import org.authlab.http.Response
import org.authlab.http.ResponseLine
import java.io.Closeable
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.net.ssl.SSLServerSocketFactory

class Server(inetAddress: InetAddress, port: Int, backlog: Int,
             encrypted: Boolean = false, threadPoolSize: Int = 50,
             val handlers: List<Handler> = emptyList()) : Runnable, Closeable {
    companion object {
        private val _logger = loggerFor<Server>()
    }

    private var _running = false
    private val _socket: ServerSocket
    private val _threadPool: ThreadPoolExecutor

    init {
        _logger.info("Creating server socket on ${inetAddress.hostAddress}:$port (backlog=$backlog)")

        _socket = if (encrypted) {
            SSLServerSocketFactory.getDefault()
                    .createServerSocket(port, backlog, inetAddress)
        } else {
            ServerSocket(port, backlog, inetAddress)
        }


        _logger.info("Creating thread pool (size=$threadPoolSize)")
        _threadPool = Executors.newFixedThreadPool(threadPoolSize) as ThreadPoolExecutor
    }

    override fun run() {
        _logger.info("Starting server")

        _running = true

        while (_running) {
            val incomingSocket = _socket.accept()

            try {
                val request = Request.fromInputStream(incomingSocket.inputStream)

                var response: Response? = null

                for (handler in handlers) {
                    if (handler.entryPointPattern.matcher(request.requestLine.location.safePath).matches()) {
                        response = handler.onRequest(ServerRequest(request))
                                .build().internalResponse
                        break
                    }
                }

                if (response == null) {
                    response = Response(ResponseLine(404, "Not Found"))
                }

                response.write(incomingSocket.outputStream)
            } catch (e: Exception) {
                _logger.warn("Error processing request", e)

                Response(ResponseLine(500, "Server Error"))
                        .write(incomingSocket.outputStream)
            } finally {
                incomingSocket.close()
            }
        }
    }

    override fun close() {
        _logger.debug("Closing server")

        _running = false

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
    private var _inetAddress: InetAddress = InetAddress.getByName("0.0.0.0")
    private var _port: Int = 8080
    private var _encrypted: Boolean = false
    private var _backlog: Int = 50
    private var _threadPoolSize: Int = 100
    private val _handlerBuilders = mutableListOf<HandlerBuilder>()
    private var _defaultHandlerBuilder: HandlerBuilder? = null

    constructor(init: ServerBuilder.() -> Unit) : this() {
        init()
    }

    fun inetAddress(init: () -> InetAddress) {
        _inetAddress = init()
    }

    fun host(init: () -> String) {
        _inetAddress = InetAddress.getByName(init())
    }

    fun port(init: () -> Int) {
        _port = init()
    }

    fun encrypted(init: () -> Boolean) {
        _encrypted = init()
    }

    fun backlog(init: () -> Int) {
        _backlog = init()
    }

    fun threadPoolSize(init: () -> Int) {
        _threadPoolSize = init()
    }

    fun handle(init: HandlerBuilder.() -> Unit) {
        _handlerBuilders.add(HandlerBuilder(init))
    }

    fun handle(entryPoint: String, init: ServerResponseBuilder.(ServerRequest) -> Unit) {
        val handler = HandlerBuilder {
            entryPoint { entryPoint }
            onRequest(init)
        }
        _handlerBuilders.add(handler)
    }

    fun default(init: ServerResponseBuilder.(ServerRequest) -> Unit) {
        _defaultHandlerBuilder = HandlerBuilder {
            entryPoint { "*" }
            onRequest(init)
        }
    }

    fun build(): Server {
        val handlers = _handlerBuilders.map(HandlerBuilder::build)
                .toMutableList()

        _defaultHandlerBuilder?.apply { handlers.add(build()) }

        return Server(_inetAddress, _port, _backlog, _encrypted,
                _threadPoolSize, handlers)
    }
}
