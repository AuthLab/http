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

import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

open class StringBody protected constructor(val data: String, contentType: String = "text/plain",
                      originalBody: RawBody?): Body(contentType, null, originalBody) {
    constructor(data: String, contentType: String = "text/plain") : this(data, contentType, null)

    companion object {
        fun fromRawBody(rawBody: RawBody): StringBody {
            val rawString = rawBody.bytes.toString(StandardCharsets.UTF_8)

            return StringBody(rawString, originalBody = rawBody)
        }
    }

    override fun calculateSize()
            =  data.length

    override fun doWrite(outputStream: OutputStream) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        writer.write(data)
        writer.flush()
    }
}