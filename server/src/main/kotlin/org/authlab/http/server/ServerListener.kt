package org.authlab.http.server

import org.authlab.util.loggerFor
import java.io.Closeable
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import javax.net.ssl.SSLServerSocketFactory

class ServerListener(val inetAddress: InetAddress, val port: Int,
                     val secure: Boolean, val backlog: Int,
                     var onAccept: (Socket) -> Unit = {}) : Runnable, Closeable {
    companion object {
        private val _logger = loggerFor<ServerListener>()
    }

    private var _socket: ServerSocket? = null
    private var _running = false
    private var _initialized = false

    val initialized: Boolean
        get() = _initialized

    fun setup () {
        _logger.info("Creating server socket on ${inetAddress.hostAddress}:$port (backlog=$backlog)")

        _socket = if (secure) {
            SSLServerSocketFactory.getDefault()
                    .createServerSocket(port, backlog, inetAddress)
        } else {
            ServerSocket(port, backlog, inetAddress)
        }

        _initialized = true
    }

    override fun run() {
        if (!_initialized) {
            setup()
        }

        _running = true

        val socket: ServerSocket = _socket!!

        while (_running && !socket.isClosed) {
            try {
                onAccept(socket.accept())
            } catch (e: Exception) {
                if (_running || !socket.isClosed) {
                    _logger.warn("Error processing connection", e)
                }
            }
        }

        _logger.info("Server listener closed")
    }

    override fun close() {
        _logger.debug("Closing server listener")
        _running = false
        _socket?.close()
    }
}

class ServerListenerBuilder() {
    constructor(init: ServerListenerBuilder.() -> Unit) : this() {
        init()
    }

    var host: String = "0.0.0.0"
    var port: Int = 8080
    var secure: Boolean = false
    var backlog: Int = 50

    fun build(): ServerListener {
        return ServerListener(InetAddress.getByName(host), port, secure, backlog)
    }
}