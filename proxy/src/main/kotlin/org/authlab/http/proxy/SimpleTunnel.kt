package org.authlab.http.proxy

import org.authlab.util.loggerFor
import java.io.IOException
import java.net.Socket
import java.net.SocketException

/**
 * Note: Extremely CPU intensive.
 */
class SimpleTunnel(val clientSocket: Socket, val serverSocket: Socket, val inactivityTimeout: Long = 15_000L) : Tunnel {
    private var _lastActive: Long = 0L

    companion object {
        private val _logger = loggerFor<SimpleTunnel>()
    }

    override fun run() {
        _logger.info("Starting tunnel between {} and {}",
                clientSocket.remoteSocketAddress, serverSocket.remoteSocketAddress)

        _lastActive = System.currentTimeMillis()

        try {
            while (!clientSocket.isClosed && !serverSocket.isClosed) {
                tunnel(clientSocket, serverSocket)
                tunnel(serverSocket, clientSocket)

                if (System.currentTimeMillis() - _lastActive > inactivityTimeout) {
                    _logger.info("Tunnel has been inactive for more than {}ms",
                            inactivityTimeout)
                    break
                }
            }
        } catch (e: SocketException) {
            _logger.info("Connection closed unexpectedly ({} -> {}); {}",
                    clientSocket.remoteSocketAddress, serverSocket.remoteSocketAddress, e.message)
        } finally {
            _logger.info("Closing tunnel between {} and {}",
                    clientSocket.remoteSocketAddress, serverSocket.remoteSocketAddress)
        }
    }

    private fun tunnel(from: Socket, to: Socket) {
        if (!from.isClosed && !to.isClosed) {
            if (from.inputStream.available() > 0) {
                val value = try {
                    from.inputStream.read()
                } catch (e: IOException) {
                    _logger.debug("Failed to read from ${from.remoteSocketAddress}")
                    throw e
                }

                if (value > -1) {
                    _lastActive = System.currentTimeMillis()

                    _logger.trace("{} -> {}: {} ({})",
                            from.remoteSocketAddress, to.remoteSocketAddress, value.toChar(), value)
                    try {
                        to.outputStream.write(value)
                    } catch (e: IOException) {
                        _logger.debug("Failed to write to ${to.remoteSocketAddress}")
                        throw e
                    }
                }
            }
        }
    }
}