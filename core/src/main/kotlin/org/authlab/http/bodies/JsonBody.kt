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

import com.google.gson.Gson
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class JsonBody private constructor(data: Any?, originalBody: RawBody?) :
        Body("application/json", null, null, originalBody) {
    private val _data = data

    constructor(data: Any) : this(data, null)

    companion object {
        private val _gson = Gson()

        fun fromRawBody(rawBody: RawBody): JsonBody {
            return JsonBody(null, rawBody)
        }
    }

    val json: String by lazy {
        originalBody?.let {
            originalBody.bytes.toString(StandardCharsets.UTF_8)
        } ?: _gson.toJson(data)
    }

    val data: Any by lazy {
        data ?: _gson.fromJson(json, Any::class.java)
    }

    inline fun <reified T> getTypedData()
            = getTypedData(T::class.java)

    fun <T> getTypedData(type: Class<T>): T {
        return _gson.fromJson(json, type)
    }

    override fun calculateSize()
            = json.length

    override fun doWrite(outputStream: OutputStream) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        writer.write(json)
        writer.flush()
    }

    override fun toString(): String {
        return json
    }
}