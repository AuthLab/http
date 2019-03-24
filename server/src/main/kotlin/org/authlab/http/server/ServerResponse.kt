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

import org.authlab.http.Cookie
import org.authlab.http.Cookies
import org.authlab.http.Header
import org.authlab.http.Headers
import org.authlab.http.Response
import org.authlab.http.ResponseLine
import org.authlab.http.bodies.BodyWriter
import org.authlab.http.bodies.EmptyBodyWriter

class ServerResponse internal constructor(internal val internalResponse: Response, val bodyWriter: BodyWriter) {
    val responseLine: ResponseLine
        get() = internalResponse.responseLine

    val headers: Headers
        get() = internalResponse.headers

    val cookies: Cookies
        get() = Cookies.fromResponseHeaders(headers)

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

@ServerMarker
class ServerResponseBuilder() {
    private var _statusLine: ResponseLine = ResponseLine(200, "OK")
    private var _headers: Headers = Headers()
    private var _bodyWriter: BodyWriter = EmptyBodyWriter()

    var allowContentHeaderOverrides = false

    constructor(init: ServerResponseBuilder.() -> Unit) : this() {
        init()
    }

    constructor(other: ServerResponse, init: ServerResponseBuilder.() -> Unit) : this() {
        _statusLine = other.responseLine
        _headers = other.headers
        _bodyWriter = other.bodyWriter

        init()
    }

    fun status(init: () -> Pair<Int, String>) {
        status(init())
    }

    fun status(status: Pair<Int, String>) {
        _statusLine = ResponseLine(status.first, status.second)
    }

    fun header(header: Pair<String, String>) {
        header(Header(header.first, header.second))
    }

    fun header(header: Header) {
        _headers = _headers.withHeader(header)
    }

    fun header(init: () -> Header) {
        _headers = _headers.withHeader(init())
    }

    fun cookie(init: () -> Cookie) {
        cookie(init())
    }

    fun cookie(cookie: Pair<String, String>) {
        cookie(Cookie(cookie.first, cookie.second))
    }

    fun cookie(cookie: Cookie) {
        _headers = _headers.withHeader(cookie.toResponseHeader())
    }

    fun body(init: () -> BodyWriter) {
        body(init())
    }

    fun body(bodyWriter: BodyWriter) {
        _bodyWriter = bodyWriter
    }

    fun build(): ServerResponse {
        val statusLine = _statusLine
        val bodyWriter = _bodyWriter

        if (!allowContentHeaderOverrides) {
            _headers = _headers.withoutHeaders("Content-Length")
                    .withoutHeaders("Content-Type")
                    .withoutHeaders("Content-Encoding")
                    .withoutHeaders("Transfer-Encoding")
        }

        if (bodyWriter !is EmptyBodyWriter) {
            bodyWriter.contentLength?.also {
                if (!_headers.contains("Content-Length")) {
                    _headers = _headers.withHeader("Content-Length", it.toString())
                } else {
                    // TODO: Log that header was overridden by API user
                }
            }

            bodyWriter.contentType?.also {
                if (!_headers.contains("Content-Type")) {
                    _headers = _headers.withHeader("Content-Type", it)
                }
            }

            bodyWriter.contentEncoding?.also {
                if (!_headers.contains("Content-Encoding")) {
                    _headers = _headers.withHeader("Content-Encoding", it)
                }
            }

            bodyWriter.transferEncoding?.also {
                if (!_headers.contains("Transfer-Encoding")) {
                    _headers = _headers.withHeader("Transfer-Encoding", it)
                }
            }
        }

        val internalResponse = Response(statusLine, _headers)

        return ServerResponse(internalResponse, bodyWriter)
    }
}
