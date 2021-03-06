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
import com.google.gson.GsonBuilder

interface JsonBody : Body

class ObjectJsonBody<out T>(val data: T) : JsonBody {
    override val writer: BodyWriter
        get() = JsonBodyWriter(data)
}

class StringJsonBody(json: String) : TextBody(json), JsonBody {
    companion object {
        private val _gson = Gson()
    }

    inline fun <reified T> getTypedValue()
            = getTypedValue(T::class.java)

    fun <T> getTypedValue(type: Class<T>): T
            = _gson.fromJson(text, type)
}

class JsonBodyReader : AbstractTextBodyReader<StringJsonBody>() {
    override fun getBody() = StringJsonBody(getTextValue())
}

class JsonBodyWriter(val data: Any?, pretty: Boolean = false) :
        TextBodyWriter(getSerializer(pretty).toJson(data), "application/json") {
    companion object {
        private val _gson = Gson()
        private val _prettyGson = GsonBuilder().setPrettyPrinting().create()

        private fun getSerializer(pretty: Boolean) = if (pretty) {
            _prettyGson
        } else {
            _gson
        }
    }
}
