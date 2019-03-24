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

package org.authlab.http

import org.authlab.util.CaseInsensitiveMap
import org.authlab.util.toCaseInsensitiveMap
import org.authlab.util.toMutableCaseInsensitiveMap

class Headers private constructor (private val headers: CaseInsensitiveMap<Header>) :
        Map<String, Header> by headers {
    constructor(header: Header) : this(listOf(header))

    constructor(headers: List<Header> = emptyList()) :
            this(headers.map { it.name.toUpperCase() to it }.toCaseInsensitiveMap())

    fun withHeader(name: String, value: String): Headers {
        return withHeader(Header(name, value))
    }

    fun withHeader(header: Header): Headers {
        val mutableHeaders = headers.toMutableCaseInsensitiveMap()

        mutableHeaders.merge(header.name, header) { originalHeader, incomingHeader ->
            originalHeader.withValues(incomingHeader.values)
        }

        return Headers(mutableHeaders)
    }

    fun withHeaders(headers: Headers): Headers {
        var newHeaders = this

        headers.headers.values.forEach { header ->
            newHeaders = newHeaders.withHeader(header)
        }

        return newHeaders
    }

    fun withoutHeaders(name: String): Headers {
        val mutableHeaders = headers.toMutableCaseInsensitiveMap()

        mutableHeaders.remove(name)

        return Headers(mutableHeaders)
    }

    override operator fun get(key: String): Header?
            = getHeader(key)

    fun getHeader(name: String): Header?
            = headers[name]

    fun hasHeader(name: String): Boolean
        = getHeader(name) != null

    fun withReplacedHeader(header: Header): Headers
            = withoutHeaders(header.name).withHeader(header)

    fun withReplacedHeaders(headers: Headers): Headers {
        val mutableHeaders = this.headers.toMutableCaseInsensitiveMap()

        mutableHeaders.putAll(headers.headers)

        return Headers(mutableHeaders)
    }

    fun contains(name: String): Boolean
            = headers.containsKey(name.toUpperCase())

    fun toLines() = headers.values.flatMap { header -> header.toLines() }

    fun toHar(): List<*> {
        return headers.flatMap { (_, header) ->
            header.toHar()
        }
    }
}