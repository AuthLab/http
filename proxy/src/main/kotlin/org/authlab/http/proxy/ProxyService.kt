package org.authlab.http.proxy

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.MarkerManager
import org.authlab.util.loggerFor
import org.authlab.util.markerFor
import org.authlab.logging.SimpleMapMessage
import org.authlab.http.proxy.Proxy
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLServerSocketFactory

class ProxyService(inetAddress: InetAddress, port: Int, backlog: Int, encrypted: Boolean = false,
                   threadPoolSize: Int = 10, val inspectTunnels: Boolean) : Runnable {
    companion object {
        private val AUDIT_MARKER = markerFor("AUDIT")
        private val HAR_MARKER = MarkerManager.getMarker("HAR")
        private val _logger = loggerFor<ProxyService>()
        private val _harLogger = LogManager.getLogger(ProxyService::class.java)
    }

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
        _logger.info("Starting proxy")

        fun onTransaction(transaction: Transaction) {
            val har = transaction.toHar()

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

        while(true) {
            val incomingSocket = _socket.accept()
            try {
                _threadPool.execute(Proxy(incomingSocket,
                        inspectTunnels,
                        ::onTransaction,
                        ::onClose,
                        ::onException
                ))
            } catch (e: Exception) {
                _logger.warn("Failed to start proxy thread", e)
            }

            logStatus()
        }
    }

    fun close() {
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