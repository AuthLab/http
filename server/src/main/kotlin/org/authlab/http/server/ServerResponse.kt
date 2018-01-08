package org.authlab.http.server

import org.authlab.http.bodies.Body
import org.authlab.http.bodies.EmptyBody
import org.authlab.http.Headers
import org.authlab.http.Response
import org.authlab.http.ResponseLine

class ServerResponse internal constructor(internal val internalResponse: Response) {
    val responseLine: ResponseLine
        get() = internalResponse.responseLine

    val headers: Headers
        get() = internalResponse.headers

    val body: Body
        get() = internalResponse.body

    val statusCode: Int
        get() = internalResponse.responseLine.statusCode

    val contentType: String?
        get() = internalResponse.headers
                .getHeader("Content-Type")?.getFirst()

    val contentLength: Int
        get() = internalResponse.headers
                .getHeader("Content-Length")?.getFirstAsInt() ?: 0

    fun toHar() = internalResponse.toHar()
}

class ServerResponseBuilder() {
    private var _statusLine: ResponseLine? = null
    private var _headers = Headers()
    private var _body: Body = EmptyBody()

    constructor(init: ServerResponseBuilder.() -> Unit) : this() {
        init()
    }

    fun status(init: () -> Pair<Int, String>) {
        _statusLine = init().run { ResponseLine(first, second) }
    }

    fun header(init: () -> Pair<String, String>) {
        val header = init()
        _headers = _headers.withHeader(header.first, header.second)
    }

    fun body(init: () -> Body) {
        _body = init()
    }

    fun build(): ServerResponse {
        val statusLine = _statusLine ?: throw IllegalStateException("No statusLine on server response")
        val body = _body

        if (body !is EmptyBody) {
            _headers = _headers.withoutHeaders("Content-Length")
                    .withHeader("Content-Length", body.size.toString())

            if(!_headers.hasHeader("Content-Type")) {
                _headers = _headers.withHeader("Content-Type", body.contentType)
            }
        }

        val internalResponse = Response(statusLine, _headers, _body)

        return ServerResponse(internalResponse)
    }
}
