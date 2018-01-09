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

package org.authlab.http.bodies

import org.authlab.http.Headers
import org.authlab.util.loggerFor
import java.io.InputStream
import java.io.OutputStream

abstract class Body protected constructor(val contentType: String, val contentEncoding: String?,
                                          protected val backingBody: Body? = null) {
    constructor(contentType: String, contentEncoding: String?) : this(contentType, contentEncoding, null)

    companion object {
        private val _logger = loggerFor<Body>()

        fun empty(): Body
                = EmptyBody()

        fun fromString(string: String): Body
                = StringBody(string)

        fun fromBytes(bytes: ByteArray): Body
                = RawBody(bytes)

        fun fromInputStream(inputStream: InputStream, headers: Headers, beforeBody: () -> Unit = {}): Body {
            val contentLengthHeader = headers.getHeader("Content-Length")
            val chunked = headers.getHeader("Transfer-Encoding")?.getFirst() == "chunked"

            return if (contentLengthHeader != null || chunked) {
                beforeBody()

                _logger.debug("Attempting to read body")

                fromInputStream(inputStream,
                        contentLengthHeader?.getFirstAsInt() ?: 0,
                        chunked,
                        headers.getHeader("Content-Type")?.getFirst(),
                        headers.getHeader("Content-Encoding")?.getFirst())
            } else {
                EmptyBody()
            }
        }

        fun fromInputStream(inputStream: InputStream, length: Int = 0, chunked: Boolean = false,
                            contentType: String? = null, contentEncoding: String? = null): Body {
            var rawBody = RawBody.fromInputStream(inputStream, length, chunked)

            if (contentType != null) {
                rawBody = rawBody.withContentType(contentType)
            }

            if (contentEncoding != null) {
                rawBody = rawBody.withContentEncoding(contentEncoding)
                return rawBody
            }

            return when (contentType?.split(";")?.first()) {
                "application/x-www-form-urlencoded" -> FormBody.fromRawBody(rawBody)
                "text/plain" -> StringBody.fromRawBody(rawBody)
                "text/html" -> StringBody.fromRawBody(rawBody)
                "application/json" -> JsonBody.fromRawBody(rawBody)
                else -> rawBody
            }
        }
    }

    val size: Int
        get() = backingBody?.size ?: calculateSize()

    protected abstract fun calculateSize(): Int

    fun write(outputStream: OutputStream) {
        if (backingBody != null) {
            backingBody.write(outputStream)
        } else {
            doWrite(outputStream)
        }
    }

    protected abstract fun doWrite(outputStream: OutputStream)
}
