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

import org.authlab.crypto.CertificateGeneratingKeyManager
import org.authlab.http.Header
import org.authlab.http.Request
import org.authlab.http.Response
import org.authlab.http.ResponseLine
import org.authlab.http.bodies.ByteBodyReader
import org.authlab.util.loggerFor
import org.slf4j.MDC
import java.net.Socket
import java.time.Instant
import java.util.UUID
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

typealias OnTransaction = (Transaction) -> Unit
typealias OnClose = (incomingSocket: Socket, outgoingSocket: Socket?) -> Unit
typealias OnException = (Exception) -> Unit

open class Proxy(private val incomingSocket: Socket,
                 private val sslContext: SSLContext = SSLContext.getDefault(),
                 private val inspectTunnels: Boolean = true,
                 private val scheme: String = "http",
                 private val onTransaction: OnTransaction? = null,
                 private val onClose: OnClose? = null,
                 private val onException: OnException? = null) : Runnable {
    private val _proxyToken = UUID.randomUUID()

    val proxyToken: String
            = _proxyToken.toString()

    companion object {
        private val _logger = loggerFor<Proxy>()
    }

    override fun run() {
        MDC.put("proxyToken", proxyToken)

        _logger.info("Accepting incoming connection from ${incomingSocket.inetAddress}")

        var outgoingSocket: Socket? = null

        try {
            val transactionStartInstant = Instant.now()

            val incomingRequest = Request.fromInputStream(incomingSocket.inputStream, ByteBodyReader()) { request ->
                if (request.headers.getHeader("Content-Length")?.getFirstAsInt() ?: 0 > 0) {
                    _logger.debug("Sending 100 Continue")
                    val continueResponse = Response(ResponseLine(100, "Continue"))
                    continueResponse.write(incomingSocket.outputStream)
                } else {
                    _logger.debug("Not sending 100 Continue")
                }
            }

            _logger.debug("Received request: $incomingRequest")

            if ("CONNECT".equals(incomingRequest.requestLine.method, true)) {
                _logger.debug("Sending 200 OK")
                val okResponse = Response(ResponseLine(200, "OK"))
                okResponse.write(incomingSocket.outputStream)

                val remoteHost = incomingRequest.requestLine.location.host!!

                if (!inspectTunnels) {
                    _logger.info("Establishing opaque tunnel")

                    outgoingSocket = createRemoteSocket(incomingRequest)

                    val tunnel = SimpleTunnel(incomingSocket, outgoingSocket)
                    tunnel.run()
                } else if (remoteHost.port == 80) {
                    _logger.info("Establishing unencrypted tunnel")

                    outgoingSocket = createRemoteSocket(incomingRequest)

                    val tunneledProxy = createTunnel(incomingSocket,
                            outgoingSocket,
                            scheme,
                            onTransaction)
                    tunneledProxy.run()
                } else {
                    if (remoteHost.port != 443) {
                        _logger.debug("Unknown remote port ${remoteHost.port}; assuming SSL/TLS")
                    }
                    _logger.info("Establishing encrypted tunnel")

                    outgoingSocket = sslContext.socketFactory.createSocket(remoteHost.hostname, remoteHost.port)

                    val encryptedIncomingSocket = sslContext.socketFactory
                            .createSocket(incomingSocket, null, incomingSocket.port, true) as SSLSocket
                    encryptedIncomingSocket.useClientMode = false

                    CertificateGeneratingKeyManager.mapSocketAndHostname(encryptedIncomingSocket, remoteHost.hostname)

                    val tunneledProxy = createTunnel(encryptedIncomingSocket,
                            outgoingSocket,
                            "https",
                            onTransaction)
                    tunneledProxy.run()
                }
            } else {
                val outgoingRequest = createRequestToProxy(incomingRequest)
                _logger.debug("Request to proxy: {}", outgoingRequest)

                outgoingSocket = createRemoteSocket(incomingRequest)

                _logger.info("Proxying request '${outgoingRequest.requestLine}' to ${outgoingSocket.inetAddress}")
                outgoingRequest.write(outgoingSocket.outputStream)

                var incomingResponse = Response.fromInputStream(outgoingSocket.inputStream, ByteBodyReader())

                _logger.debug("Received response: {}", incomingResponse)

                if (incomingResponse.responseLine.statusCode == 100) {
                    _logger.debug("Received 100 Continue; dropping response and preparing for next")
                    incomingResponse = Response.fromInputStream(outgoingSocket.inputStream, ByteBodyReader())
                }

                val outgoingResponse = createResponseToProxy(incomingResponse)
                _logger.debug("Response to proxy: {}", outgoingResponse)

                _logger.info("Proxying response '${outgoingResponse.responseLine}' to ${incomingSocket.inetAddress}")

                outgoingResponse.write(incomingSocket.outputStream)

                val transactionStopInstant = Instant.now()

                onTransaction?.let {
                    _logger.trace("Calling on-transaction callback")
                    it(Transaction(outgoingRequest, incomingResponse,
                            transactionStartInstant, transactionStopInstant, scheme))
                }
            }
        } catch (e: Exception) {
            onException?.let {
                _logger.trace("Calling on-exception callback")
                it(e)
            }
        } finally {
            onClose?.let {
                _logger.trace("Calling on-close callback")
                it(incomingSocket, outgoingSocket)
            }

            doClose(incomingSocket, outgoingSocket)
        }
    }

    protected open fun createRemoteSocket(incomingRequest: Request): Socket
            = Socket(incomingRequest.host!!.hostname, incomingRequest.host!!.port)

    protected open fun doClose(incomingSocket: Socket?, outgoingSocket: Socket?) {
        outgoingSocket?.also {
            _logger.info("Closing outgoing connection with ${it.inetAddress}")
            it.close()
        }
    }

    protected open fun createRequestToProxy(incomingRequest: Request): Request {
        var headers = incomingRequest.headers
                .withoutHeaders("Proxy-Authorization")
                .withoutHeaders("Proxy-Connection")
                .withoutHeaders("Content-Length")
                .withoutHeaders("Transfer-Encoding")
                .withHeader(Header("Proxy-Token", proxyToken))
                .withHeader(Header("Forwarded",
                        "by=${incomingSocket.localSocketAddress}; " +
                                "for=${incomingSocket.remoteSocketAddress}"))

        incomingRequest.body.writer.contentLength?.also {
            headers = headers.withHeader("Content-length", "$it")
        }

        incomingRequest.body.writer.transferEncoding?.also {
            headers = headers.withHeader("Transfer-Encoding", it)
        }

        return incomingRequest.withRequestLine(incomingRequest.requestLine.withOnlyUriPath())
                .withHeaders(headers)
    }

    protected open fun createResponseToProxy(incomingResponse: Response): Response {
        var headers = incomingResponse.headers
                .withoutHeaders("Content-Length")
                .withoutHeaders("Transfer-Encoding")

        incomingResponse.body.writer.contentLength?.also {
            headers = headers.withHeader("Content-length", "$it")
        }

        incomingResponse.body.writer.transferEncoding?.also {
            headers = headers.withHeader("Transfer-Encoding", it)
        }

        return incomingResponse.withHeaders(headers)
    }

    private fun createTunnel(incomingSocket: Socket, outgoingSocket: Socket, scheme: String, onTransaction: OnTransaction?): Tunnel {
        return ProxyTunnel(incomingSocket, outgoingSocket, sslContext, scheme, onTransaction, onException)
    }
}

class ProxyTunnel(incomingSocket: Socket,
                  private val outgoingSocket: Socket,
                  sslContext: SSLContext,
                  scheme: String,
                  onTransaction: OnTransaction? = null,
                  onException: OnException? = null) :
        Proxy(incomingSocket, sslContext, false, scheme, onTransaction, null, onException), Tunnel {
    companion object {
        private val _logger = loggerFor<ProxyTunnel>()
    }

    override fun createRemoteSocket(incomingRequest: Request)
            = outgoingSocket

    override fun createRequestToProxy(incomingRequest: Request): Request {
        var headers = incomingRequest.headers
                .withHeader(Header("Proxy-Token", proxyToken))
                .withoutHeaders("Content-Length")
                .withoutHeaders("Transfer-Encoding")

        incomingRequest.body.writer.contentLength?.also {
            headers = headers.withHeader("Content-length", "$it")
        }

        incomingRequest.body.writer.transferEncoding?.also {
            headers = headers.withHeader("Transfer-Encoding", it)
        }

        return incomingRequest.withRequestLine(incomingRequest.requestLine.withOnlyUriPath())
                .withHeaders(headers)
    }

    override fun createResponseToProxy(incomingResponse: Response): Response {
        var headers = incomingResponse.headers
                .withoutHeaders("Content-Length")
                .withoutHeaders("Transfer-Encoding")

        incomingResponse.body.writer.contentLength?.also {
            headers = headers.withHeader("Content-length", "$it")
        }

        incomingResponse.body.writer.transferEncoding?.also {
            headers = headers.withHeader("Transfer-Encoding", it)
        }

        return incomingResponse.withHeaders(headers)
    }

    override fun doClose(incomingSocket: Socket?, outgoingSocket: Socket?) {
        _logger.info("Closing tunnel: ${incomingSocket?.inetAddress ?: "?"} -> ${outgoingSocket?.inetAddress ?: "?"}")
    }
}