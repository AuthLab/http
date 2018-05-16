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

class Cookies(private val cookies: Map<String, Cookie> = mapOf()) :
        Map<String, Cookie> by cookies {
    companion object {
        fun fromRequestHeaders(headers: Headers): Cookies {
            return Cookies(headers.getHeader("Cookie")?.values
                    ?.flatMap { cookieString ->
                        cookieString.split(";")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .map { Cookie.fromString(it) }
                    }
                    ?.map { it.name to it }
                    ?.toMap() ?: mapOf())
        }

        fun fromResponseHeaders(headers: Headers): Cookies {
            return Cookies(headers.getHeader("Set-Cookie")?.values
                    ?.filter { it.isNotEmpty() }
                    ?.map { Cookie.fromString(it) }
                    ?.map { it.name to it }
                    ?.toMap() ?: mapOf())
        }
    }

    fun toHar(): List<*> {
        return cookies.map { it.value.toHar() }
    }
}
