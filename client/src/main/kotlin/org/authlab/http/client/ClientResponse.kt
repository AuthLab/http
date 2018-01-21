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

import org.authlab.http.Headers
import org.authlab.http.Response
import org.authlab.http.ResponseLine
import org.authlab.http.bodies.Body
import org.authlab.http.bodies.BodyReader
import org.authlab.http.bodies.DelayedBody

class ClientResponse<out B : Body> internal constructor(private val internalResponse: Response,
                                                        private val body: B) {
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

    fun toHar()
            = internalResponse.toHar()

    fun getBody(): B
            = body

    inline fun <reified B : Body> getBody(bodyReader: BodyReader<B>): B {
        val body = getBody()

        return when (body) {
            is B -> body
            is DelayedBody -> body.read(bodyReader)
            else -> convertBody(body, bodyReader)
        }
    }
}