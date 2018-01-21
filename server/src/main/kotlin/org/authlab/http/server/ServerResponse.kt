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
    private var _bodyWriter: BodyWriter = EmptyBodyWriter()

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

    fun body(init: () -> BodyWriter) {
        _bodyWriter = init()
    }

    fun build(): ServerResponse {
        val statusLine = _statusLine ?: throw IllegalStateException("No statusLine on server response")
        val bodyWriter = _bodyWriter

        if (bodyWriter !is EmptyBodyWriter) {
            bodyWriter.contentLength?.also {
                _headers = _headers.withHeader("Content-Length", it.toString())
            }

            bodyWriter.contenteType?.also {
                _headers = _headers.withHeader("Content-Type", it)
            }

            bodyWriter.contenteEncoding?.also {
                _headers = _headers.withHeader("Content-Encoding", it)
            }

            bodyWriter.transferEncoding?.also {
                _headers = _headers.withHeader("Transfer-Encoding", it)
            }
        }

        val internalResponse = Response(statusLine, _headers)

        return ServerResponse(internalResponse, bodyWriter)
    }
}
