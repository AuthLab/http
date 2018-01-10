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

            if (!body.streaming && !_headers.hasHeader("Content-Length")) {
                _headers = _headers.withHeader("Content-Length", body.size.toString())
            }

            if (!_headers.hasHeader("Content-Type")) {
                _headers = _headers.withHeader("Content-Type", body.contentType)
            }

            if (body.contentEncoding != null && !_headers.hasHeader("Content-Encoding")) {
                _headers = _headers.withHeader("Content-Encoding", body.contentEncoding!!)
            }

            if (body.transferEncoding != null && !_headers.hasHeader("Transfer-Encoding")) {
                _headers = _headers.withHeader("Transfer-Encoding", body.transferEncoding!!)
            }
        }

        val internalResponse = Response(statusLine, _headers, _body)

        return ServerResponse(internalResponse)
    }
}
