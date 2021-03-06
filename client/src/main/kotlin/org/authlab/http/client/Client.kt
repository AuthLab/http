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

package org.authlab.http.client

import org.authlab.http.Authority
import org.authlab.http.Endpoint
import org.authlab.http.Header
import org.authlab.http.Headers
import org.authlab.http.Location
import org.authlab.http.QueryParameters
import org.authlab.http.Request
import org.authlab.http.RequestLine
import org.authlab.http.Response
import org.authlab.http.bodies.Body
import org.authlab.http.bodies.BodyReader
import org.authlab.http.bodies.BodyWriter
import org.authlab.http.bodies.ByteBodyReader
import org.authlab.http.bodies.DelayedBody
import org.authlab.http.bodies.DelayedBodyReader
import org.authlab.http.bodies.EmptyBodyWriter
import org.authlab.util.loggerFor
import java.io.Closeable
import java.io.IOException
import java.net.Socket
import java.net.SocketTimeoutException
import java.time.Instant
import javax.net.SocketFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

class Client(val location: Location,
             val keepAlive: Boolean,
             val reconnect: Boolean,
             private val socketProvider: () -> Socket,
             private val sslContext: SSLContext,
             val cookieManager: CookieManager?,
             private val authenticationManager: AuthenticationManager?,
             val proxy: Endpoint? = null) : Closeable {
    companion object {
        private val _logger = loggerFor<Client>()
    }

    private var _socket: Socket? = null

    val socket: Socket
        get() = _socket ?: connect()

    val closed: Boolean
        get() = _socket?.isClosed ?: false

    val connected: Boolean
        get() = _socket?.isConnected ?: false

    private fun connect(): Socket {
        val socket = socketProvider()
        _logger.debug("Socket created: {}", socket)

        var encryptedSocket: Socket? = null

        if (proxy != null && location.scheme == "https") {
            _logger.debug("Sending CONNECT requestBuilder to proxy")

            val host = location.host ?: throw IllegalStateException("Host information missing in location '$location'")

            Request(RequestLine("CONNECT", Location(authority = Authority.fromHost(host))))
                    .write(socket.outputStream)

            val connectResponse = Response.fromInputStream(socket.inputStream, ByteBodyReader())

            if (connectResponse.responseLine.statusCode == 200) {
                _logger.debug("Proxy tunnel established")
            } else {
                throw IOException("Proxy connection refused")
            }

            encryptedSocket = sslContext.socketFactory
                    .createSocket(socket, null, socket.port, true) as SSLSocket
            encryptedSocket.useClientMode = true
        }

        _socket = encryptedSocket ?: socket

        return _socket!!
    }

    fun requestBuilder(): RequestBuilder {
        return RequestBuilderImpl(this)
    }

    fun requestBuilder(init: RequestBuilder.() -> Unit): RequestBuilder {
        return RequestBuilderImpl(this, init)
    }

    fun get(path: String? = null, init: RequestBuilder.() -> Unit = {}): ClientResponse<DelayedBody> {
        return get(DelayedBodyReader(), path, init)
    }

    fun <B: Body> get(bodyReader: BodyReader<B>, path: String? = null, init: RequestBuilder.() -> Unit = {}): ClientResponse<B> {
        return request(bodyReader) {
            init()
            method = "GET"
            if (path != null) {
                this.path = path
            }
        }
    }

    fun post(bodyWriter: BodyWriter, path: String? = null, init: RequestBuilder.() -> Unit = {}): ClientResponse<DelayedBody> {
        return post(bodyWriter, DelayedBodyReader(), path, init)
    }

    fun <B: Body> post(bodyWriter: BodyWriter, bodyReader: BodyReader<B>, path: String? = null, init: RequestBuilder.() -> Unit = {}): ClientResponse<B> {
        return request(bodyReader) {
            init()
            this.bodyWriter = bodyWriter
            method = "POST"
            if (path != null) {
                this.path = path
            }
        }
    }

    fun put(bodyWriter: BodyWriter, path: String? = null, init: RequestBuilder.() -> Unit = {}): ClientResponse<DelayedBody> {
        return put(bodyWriter, DelayedBodyReader(), path, init)
    }

    fun <B: Body> put(bodyWriter: BodyWriter, bodyReader: BodyReader<B>, path: String? = null, init: RequestBuilder.() -> Unit = {}): ClientResponse<B> {
        return request(bodyReader) {
            init()
            this.bodyWriter = bodyWriter
            method = "PUT"
            if (path != null) {
                this.path = path
            }
        }
    }

    fun patch(bodyWriter: BodyWriter, path: String? = null, init: RequestBuilder.() -> Unit = {}): ClientResponse<DelayedBody> {
        return patch(bodyWriter, DelayedBodyReader(), path, init)
    }

    fun <B: Body> patch(bodyWriter: BodyWriter, bodyReader: BodyReader<B>, path: String? = null, init: RequestBuilder.() -> Unit = {}): ClientResponse<B> {
        return request(bodyReader) {
            init()
            this.bodyWriter = bodyWriter
            method = "PATCH"
            if (path != null) {
                this.path = path
            }
        }
    }

    fun delete(path: String? = null, init: RequestBuilder.() -> Unit = {}): ClientResponse<DelayedBody> {
        return get(DelayedBodyReader(), path, init)
    }

    fun <B: Body> delete(bodyReader: BodyReader<B>, path: String? = null, init: RequestBuilder.() -> Unit = {}): ClientResponse<B> {
        return request(bodyReader) {
            init()
            method = "DELETE"
            if (path != null) {
                this.path = path
            }
        }
    }

    fun <B: Body> request(bodyReader: BodyReader<B>, init: RequestBuilder.() -> Unit): ClientResponse<B> {
        val requestBuilder = requestBuilder(init)

        if (keepAlive) {
            requestBuilder.header("Connection", "keep-alive")
        }

        if (cookieManager != null) {
            requestBuilder.headers(cookieManager.toRequestHeaders(location, Instant.now()))
        }

        if (authenticationManager != null) {
            _logger.debug("Applying authentication to request")
            authenticationManager.authenticate(requestBuilder)
        }

        return request(requestBuilder.build(), bodyReader)
    }

    fun <B: Body> request(clientRequest: ClientRequest, bodyReader: BodyReader<B>): ClientResponse<B> {
        val response = execute(clientRequest.internalRequest, clientRequest.bodyWriter)

        cookieManager?.addFromResponseHeaders(response.headers)

        val body = bodyReader.read(socket.inputStream, response.headers)
                .getBody()

        return ClientResponse(response.withBody(body), body)
    }

    private fun execute(request: Request, bodyWriter: BodyWriter): Response {
        if (connected && !checkConnection()) {
            if (reconnect) {
                _logger.info("Socket not connected; reconnecting")
                connect()
            } else {
                _logger.info("Socket not connected; not reconnecting")
                throw IOException("Connection closed by peer")
            }
        }

        _logger.debug("Sending request: {}", request.requestLine)
        _logger.trace("Request: {}", request)

        request.write(socket.outputStream, bodyWriter)

        _logger.debug("Waiting for response")
        var response = Response.fromInputStreamWithoutBody(socket.inputStream)

        if (response.responseLine.statusCode == 100) {
            // Read response body fully, even though we expect none
            ByteBodyReader().read(socket.inputStream, response.headers)

            _logger.debug("Received 100 Continue; dropping response and preparing for next")
            response = Response.fromInputStreamWithoutBody(socket.inputStream)
        }

        _logger.debug("Response received: {}", response.responseLine)
        _logger.trace("Response: {}", response)

        return response
    }

    private fun checkConnection(): Boolean {
        val soTimeout = socket.soTimeout
        try {
            // Set timeout low; timeout-exception is expected if connected and no junk in stream
            socket.soTimeout = 100
            if (socket.getInputStream().read() < 0) {
                _logger.info("Socket not connected")
                connect()
            } else {
                _logger.warn("Unexpected inbound data")
            }
        } catch (ignore: SocketTimeoutException) {
            _logger.trace("Socket connected")
            return true
        } finally {
            socket.soTimeout = soTimeout
        }
        return false
    }

    override fun close() {
        socket.close()
    }

    private class RequestBuilderImpl(private val client: Client,
                                     override var method: String = "GET",
                                     override var path: String = "/",
                                     override var bodyWriter: BodyWriter = EmptyBodyWriter(),
                                     private var query: QueryParameters = QueryParameters(),
                                     private var headers: Headers = Headers()) : RequestBuilder {
        constructor(client: Client, init: RequestBuilder.() -> Unit) : this(client) {
            init()
        }

        override var contentType: String?
            get() = headers.getHeader("Content-Type")?.getFirst()
            set(contentType) {
                headers = headers.withoutHeaders("Content-Type")

                if (contentType != null) {
                    headers = headers.withHeader("Content-Type", contentType)
                }
            }

        override var accept: String?
            get() = headers.getHeader("Accept")?.getFirst()
            set(contentType) {
                headers = headers.withoutHeaders("Accept")

                if (contentType != null) {
                    headers = headers.withHeader("Accept", contentType)
                }
            }

        override fun query(name: String, value: String?): RequestBuilder {
            query = query.withParameter(name, value)
            return this
        }

        override fun header(header: Header): RequestBuilder {
            this.headers = headers.withHeader(header)
            return this
        }

        override fun headers(headers: Headers): RequestBuilder {
            this.headers = headers.withHeaders(headers)
            return this
        }

        override fun build(): ClientRequest {
            val location = client.location.withSuffixedPath(this.path).withQuery(this.query)

            val host = location.host ?: throw IllegalStateException("Host information missing in location '${client.location}'")

            headers = headers.withHeader("Host", host.toString())

            if (bodyWriter !is EmptyBodyWriter) {
                bodyWriter.contentLength?.also {
                    headers = headers.withHeader("Content-Length", it.toString())
                }

                bodyWriter.contentType?.also {
                    headers = headers.withHeader("Content-Type", it)
                }

                bodyWriter.contentEncoding?.also {
                    headers = headers.withHeader("Content-Encoding", it)
                }

                bodyWriter.transferEncoding?.also {
                    headers = headers.withHeader("Transfer-Encoding", it)
                }
            }

            val request = Request(RequestLine(method, location),
                    headers)

            return ClientRequest(request, bodyWriter)
        }
    }
}

fun buildClient(host: String, init: ClientBuilder.() -> Unit = {}) = ClientBuilder(host, init).build()

@DslMarker
annotation class ClientMarker

@ClientMarker
class ClientBuilder(private val location: Location) {
    var keepAlive: Boolean = false
    var reconnect: Boolean = true
    var cookieManager: CookieManager? = null
    var proxy: String? = null
    var socketFactory: SocketFactory? = null
    var sslContext: SSLContext = SSLContext.getDefault()
    var authenticationManager: AuthenticationManager? = null

    constructor(location: Location, init: ClientBuilder.() -> Unit = {}) : this(location) {
        init()
    }

    constructor(host: String, init: ClientBuilder.() -> Unit = {}) : this(Location.fromString(host)) {
        init()
    }

    fun build(): Client {
        val proxy = this.proxy?.let { Location.fromString(it).endpoint }

        val socketFactory = when {
            this.socketFactory != null -> this.socketFactory!!
            proxy != null -> SocketFactory.getDefault()
            location.scheme == "https" -> sslContext.socketFactory
            else -> SocketFactory.getDefault()
        }

        val socketProvider: () -> Socket = {
            (proxy ?: location.endpoint).run {
                socketFactory.createSocket(hostname, port)
            }
        }

        return Client(location, keepAlive, reconnect, socketProvider, sslContext,
                cookieManager, authenticationManager, proxy)
    }
}
