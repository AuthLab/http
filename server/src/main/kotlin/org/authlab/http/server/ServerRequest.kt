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
