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

import org.authlab.util.loggerFor
import org.authlab.http.bodies.Body
import org.authlab.http.bodies.EmptyBody
import org.authlab.http.Header
import org.authlab.http.Headers
import org.authlab.http.Host
import org.authlab.http.Location
import org.authlab.http.QueryParameters
import org.authlab.http.Request
import org.authlab.http.RequestLine
import org.authlab.http.Response
import org.authlab.http.bodies.emptyBody
import java.io.Closeable
import java.io.IOException
import java.net.Socket
import javax.net.SocketFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class Client(val host: Host, private val socketProvider: () -> Socket, val proxy: Host? = null) : Closeable {
    companion object {
        private val _logger = loggerFor<Client>()
    }

    val socket: Socket by lazy {
        connect()
    }

    val closed: Boolean
        get() = socket.isClosed

    private var _connected = false

    val connected: Boolean
        get() = _connected

    private fun connect(): Socket {
        val socket = socketProvider()
        var encryptedSocket: Socket? = null

        if (proxy != null && host.scheme == "https") {
            _logger.debug("Sending CONNECT request to proxy")
            Request(RequestLine("CONNECT", Location(host))).write(socket.outputStream)

            val connectResponse = Response.fromInputStream(socket.inputStream)

            if (connectResponse.responseLine.statusCode == 200) {
                _logger.debug("Proxy tunnel established")
            } else {
                throw IOException("Proxy connection refused")
            }

            val sslContext = SSLContext.getDefault()
            encryptedSocket = sslContext.socketFactory
                    .createSocket(socket, null, socket.port, true) as SSLSocket
            encryptedSocket.useClientMode = true
        }

        _connected = true

        return encryptedSocket ?: socket
    }

    fun request(): RequestBuilder {
        return RequestBuilderImpl(this)
    }

    fun request(init: RequestBuilder.() -> Unit): RequestBuilder {
        return RequestBuilderImpl(this, init)
    }

    fun execute(request: Request): Response {
        _logger.debug("Sending request: {}", request.requestLine)
        _logger.trace("Request: {}", request)

        request.write(socket.outputStream)

        _logger.debug("Waiting for response")
        var response = Response.fromInputStream(socket.inputStream)

        if (response.responseLine.statusCode == 100) {
            _logger.debug("Received 100 Continue; dropping response and preparing for next")
            response = Response.fromInputStream(socket.inputStream)
        }

        _logger.debug("Response received: {}", response.responseLine)
        _logger.trace("Response: {}", response)
        return response
    }

    override fun close() {
        socket.close()
    }

    private class RequestBuilderImpl(private val client: Client,
                                     override var path: String = "/",
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

        override fun query(param: Pair<String, String>)
                = query(param.first, param.second)

        override fun query(name: String, value: String?): RequestBuilder {
            query = query.withParameter(name, value)
            return this
        }

        override fun header(header: Pair<String, String>)
                = header(header.first, header.second)

        override fun header(name: String, value: String): RequestBuilder {
            this.headers = headers.withHeader(name, value)
            return this
        }

        override fun get(path: String?): Response {
            return execute("GET", emptyBody(), path)
        }

        override fun post(body: Body, path: String?): Response {
            return execute("POST", body, path)
        }

        override fun put(body: Body, path: String?): Response {
            return execute("PUT", body, path)
        }

        override fun delete(path: String?): Response {
            return execute("DELETE", emptyBody(), path)
        }

        override fun patch(body: Body, path: String?): Response {
            return execute("PATCH", body, path)
        }

        override fun execute(method: String, body: Body, path: String?): Response {
            if (path != null) {
                this.path = path
            }

            headers = headers.withHeader("Host", client.host.hostnameAndPort)

            if (body !is EmptyBody) {
                val contentLength = body.size
                _logger.debug("Applying Content-Length header: $contentLength")
                headers = headers.withHeader(Header("Content-Length", "$contentLength"))

                if (!headers.hasHeader("Content-Type")) {
                    headers = headers.withHeader("Content-Type", body.contentType)
                }
            }

            return client.execute(Request(
                    RequestLine(method, Location(client.host, this.path, query)),
                    headers, body))
        }
    }
}

fun buildClient(host: String, init: ClientBuilder.() -> Unit = {}) = ClientBuilder(host, init).build()

class ClientBuilder(private val host: String) {
    var proxy: String? = null
    var socketFactory: SocketFactory? = null

    constructor(host: String, init: ClientBuilder.() -> Unit = {}) : this(host) {
        init()
    }

    fun build(): Client {
        val host = Host.fromString(this.host)
        val proxy = this.proxy?.let { Host.fromString(it) }

        val socketFactory = when {
            this.socketFactory != null -> this.socketFactory!!
            proxy != null -> SocketFactory.getDefault()
            host.scheme == "https" -> SSLSocketFactory.getDefault()
            else -> SocketFactory.getDefault()
        }

        val socketProvider: () -> Socket = {
            (proxy ?: host).run { socketFactory.createSocket(hostname, port) }
        }

        return Client(host, socketProvider, proxy)
    }
}
