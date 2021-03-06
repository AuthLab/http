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

package org.authlab.http.proxy

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.MarkerManager
import org.authlab.logging.SimpleMapMessage
import org.authlab.util.loggerFor
import org.authlab.util.markerFor
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

class ProxyService(inetAddress: InetAddress, port: Int, backlog: Int, val sslContext: SSLContext = SSLContext.getDefault(), encrypted: Boolean = false,
                   threadPoolSize: Int = 10, val inspectTunnels: Boolean) : Runnable {
    companion object {
        private val AUDIT_MARKER = markerFor("AUDIT")
        private val HAR_MARKER = MarkerManager.getMarker("HAR")
        private val _logger = loggerFor<ProxyService>()
        private val _harLogger = LogManager.getLogger(ProxyService::class.java)
    }

    private val _socket: ServerSocket
    private val _threadPool: ThreadPoolExecutor
    private var _running = false

    init {
        _logger.info("Creating server socket on ${inetAddress.hostAddress}:$port (backlog=$backlog)")

        _socket = if (encrypted) {
            sslContext.serverSocketFactory.createServerSocket(port, backlog, inetAddress)
        } else {
            ServerSocket(port, backlog, inetAddress)
        }


        _logger.info("Creating thread pool (size=$threadPoolSize)")
        _threadPool = Executors.newFixedThreadPool(threadPoolSize) as ThreadPoolExecutor
    }

    override fun run() {
        _logger.info("Starting proxy")

        fun onTransaction(transaction: Transaction) {
            val har = transaction.toHar()

            _logger.trace("Logging audit and har")

            _logger.info(AUDIT_MARKER, "$har")

            _harLogger.info(HAR_MARKER, SimpleMapMessage(har))
        }

        fun onClose(incomingSocket: Socket, @Suppress("UNUSED_PARAMETER") outgoingSocket: Socket?) {
            _logger.info("Closing incoming connection with ${incomingSocket.inetAddress}")
            incomingSocket.close()

            logStatus()
        }

        fun onException(e: Exception) {
            _logger.warn("Unexpected exception on proxy thread", e)
        }

        _running = true

        try {
            while (_running) {
                val incomingSocket = _socket.accept()

                try {
                    _threadPool.execute(Proxy(incomingSocket,
                            sslContext,
                            inspectTunnels,
                            "http",
                            ::onTransaction,
                            ::onClose,
                            ::onException
                    ))
                } catch (e: Exception) {
                    _logger.warn("Failed to start proxy thread", e)
                }

                logStatus()
            }
        } catch (e: Exception) {
            if (_running) {
                // we haven't been properly closed
                _logger.warn("Proxy listener unexpectedly shut down", e)
            }
        }

        _logger.info("Proxy has shut down")
    }

    fun close() {
        _running = false

        try {
            _socket.close()
        } catch (e: Exception) {
            System.err.println("Failed to close server listener: ${e.message}")
        }

        _threadPool.shutdown() // Disable new tasks from being submitted

        try {
            // Wait a while for existing tasks to terminate
            if (!_threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                _threadPool.shutdownNow() // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!_threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate")
                }
            }
        } catch (e: InterruptedException) {
            // (Re-)Cancel if current thread also interrupted
            _threadPool.shutdownNow()
            // Preserve interrupt status
            Thread.currentThread().interrupt()
        }
    }

    private fun logStatus() {
        _logger.info("{} active threads of {} in pool; {} served",
                _threadPool.activeCount,
                _threadPool.maximumPoolSize,
                _threadPool.completedTaskCount)
    }
}