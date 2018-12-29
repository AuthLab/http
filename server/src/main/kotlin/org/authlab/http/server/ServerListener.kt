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

import org.authlab.util.loggerFor
import java.io.Closeable
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import javax.net.ssl.SSLContext

class ServerListener(val inetAddress: InetAddress, val port: Int,
                     val secure: Boolean, val backlog: Int,
                     val sslContext: SSLContext = SSLContext.getDefault(),
                     var onAccept: (Socket, ServerListener) -> Unit = { _, _ ->}) : Runnable, Closeable {
    companion object {
        private val _logger = loggerFor<ServerListener>()
    }

    private var _socket: ServerSocket? = null
    private var _running = false
    private var _initialized = false

    val initialized: Boolean
        get() = _initialized

    fun setup () {
        _logger.info("Creating server socket on ${inetAddress.hostAddress}:$port (backlog=$backlog, secure=$secure)")

        _socket = if (secure) {
            sslContext.serverSocketFactory.createServerSocket(port, backlog, inetAddress)
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

        _logger.info("Accepting incoming connections on ${inetAddress.hostAddress}:$port")

        while (_running && !socket.isClosed) {
            try {
                onAccept(socket.accept(), this)
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

@ServerMarker
class ServerListenerBuilder() {
    constructor(init: ServerListenerBuilder.() -> Unit) : this() {
        init()
    }

    var host: String = "0.0.0.0"
    var port: Int = 8080
    var secure: Boolean = false
    var backlog: Int = 50
    var sslContext: SSLContext = SSLContext.getDefault()
//    var blocking: Boolean = true

    fun build(): ServerListener {
        return ServerListener(InetAddress.getByName(host), port, secure, backlog, sslContext)
    }
}