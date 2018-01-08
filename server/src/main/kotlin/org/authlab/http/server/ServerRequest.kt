package org.authlab.http.server

import org.authlab.http.bodies.Body
import org.authlab.http.Headers
import org.authlab.http.Host
import org.authlab.http.QueryParameters
import org.authlab.http.Request
import org.authlab.http.RequestLine

class ServerRequest internal constructor(internal val internalRequest: Request) {
    val requestLine: RequestLine
        get() = internalRequest.requestLine

    val host: Host?
        get() = internalRequest.requestLine.location.host

    val path: String
        get() = internalRequest.requestLine.location.safePath

    val query: QueryParameters
        get() = internalRequest.requestLine.location.query

    val fragment: String?
        get() = internalRequest.requestLine.location.fragment

    val headers: Headers
        get() = internalRequest.headers

    val body: Body
        get() = internalRequest.body

    val contentType: String?
        get() = internalRequest.headers
                .getHeader("Content-Type")?.getFirst()

    val contentLength: Int
        get() = internalRequest.headers
                .getHeader("Content-Length")?.getFirstAsInt() ?: 0

    fun toHar() = internalRequest.toHar()
}
