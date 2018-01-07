package org.authlab.http.client

import org.authlab.util.loggerFor
import org.authlab.http.Body
import org.authlab.http.EmptyBody
import org.authlab.http.Header
import org.authlab.http.Headers
import org.authlab.http.Host
import org.authlab.http.Location
import org.authlab.http.QueryParameters
import org.authlab.http.Request
import org.authlab.http.RequestLine
import org.authlab.http.Response
import org.authlab.http.emptyBody
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
                                     private var path: String = "/",
                                     private var query: QueryParameters = QueryParameters(),
                                     private var headers: Headers = Headers()) : RequestBuilder {

        constructor(client: Client, init: RequestBuilder.() -> Unit) : this(client) {
            init()
        }

        override fun path(path: String): RequestBuilder {
            this.path = path
            return this
        }

        override fun query(param: Pair<String, String>)
                = query(param.first, param.second)

        override fun query(name: String, value: String?): RequestBuilder {
            query = query.withParameter(name, value)
            return this
        }

        override fun contentType(contentType: String): RequestBuilder {
            headers = headers.withoutHeaders("Content-Type")
                    .withHeader("Content-Type", contentType)
            return this
        }

        override fun accept(accept: String): RequestBuilder {
            headers = headers.withoutHeaders("Accept")
                    .withHeader("Accept", accept)
            return this
        }

        override fun header(header: Pair<String, String>)
                = header(header.first, header.second)

        override fun header(name: String, value: String): RequestBuilder {
            this.headers = headers.withHeader(name, value)
            return this
        }

        override fun get(): Response {
            return execute("GET", emptyBody())
        }

        override fun post(body: Body): Response {
            return execute("POST", body)
        }

        override fun put(body: Body): Response {
            return execute("PUT", body)
        }

        override fun delete(): Response {
            return execute("DELETE", emptyBody())
        }

        override fun patch(body: Body): Response {
            return execute("PATCH", body)
        }

        override fun execute(method: String, body: Body): Response {
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
                    RequestLine(method, Location(client.host, path, query)),
                    headers, body))
        }
    }
}

fun buildClient(init: ClientBuilder.() -> Unit) = ClientBuilder(init).build()
fun buildClient(host: String, init: ClientBuilder.() -> Unit = {}) = ClientBuilder(host, init).build()

class ClientBuilder() {
    private var _host = Host("localhost")
    private var _proxy: Host? = null
    private var _socketFactory: SocketFactory? = null

    constructor(init: ClientBuilder.() -> Unit) : this() {
        init()
    }

    constructor(host: String, init: ClientBuilder.() -> Unit = {}) : this(init) {
        host { host }
    }

    fun host(init: () -> String) {
        _host = Host.fromString(init())
    }

    fun hostname(init: () -> String) {
        _host = _host.withHostname(init())
    }

    fun port(init: () -> Int) {
        _host = _host.withPort(init())
    }

    fun scheme(init: () -> String) {
        _host = _host.withScheme(init())
    }

    fun proxy(init: () -> String) {
        _proxy = Host.fromString(init())
    }

    fun socketFactory(init: () -> SocketFactory) {
        _socketFactory = init()
    }

    fun build(): Client {
        val socketFactory = when {
            _socketFactory != null -> _socketFactory!!
            _proxy != null -> SocketFactory.getDefault()
            _host.scheme == "https" -> SSLSocketFactory.getDefault()
            else -> SocketFactory.getDefault()
        }

        val socketProvider: () -> Socket = {
            (_proxy ?: _host).run { socketFactory.createSocket(hostname, port) }
        }

        return Client(_host, socketProvider, _proxy)
    }
}
