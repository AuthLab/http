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

import java.time.Instant

class Cookies private constructor (private val cookies: Map<String, Cookie> = mapOf()) :
        Map<String, Cookie> by cookies {
    constructor(cookie: Cookie) : this(listOf(cookie))
    constructor(cookies: List<Cookie> = emptyList()) : this(cookies.map { it.name to it }.toMap())

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

    fun withCookie(cookie: Cookie): Cookies {
        val mutableCookies = cookies.toMutableMap()

        mutableCookies[cookie.name] = cookie

        return Cookies(mutableCookies)
    }

    fun withCookieIf(cookie: Cookie, predicate: (Cookie) -> Boolean): Cookies {
        val mutableCookies = cookies.toMutableMap()

        val existingCookie = cookies[cookie.name]

        if (existingCookie == null || predicate(existingCookie)) {
            mutableCookies[cookie.name] = cookie
        } else {
            return this
        }

        return Cookies(mutableCookies)
    }

    fun withCookies(cookies: Cookies): Cookies {
        val mutableCookies = this.cookies.toMutableMap()

        mutableCookies.putAll(cookies)

        return Cookies(mutableCookies)
    }

    fun withoutExpired(now: Instant): Cookies {
        return Cookies(this.cookies.filterValues {
            it.expires == null || it.expires.isAfter(now)
        })
    }

    fun toResponseHeaders(): Headers {
        return Headers(cookies.values.map {
            it.toResponseHeader()
        })
    }

    fun toRequestHeaders(): Headers {
        // TODO: Only return one header containing all cookies; RFC-6265 section-5.4
        return Headers(cookies.values.map {
            it.toRequestHeader()
        })
    }

    fun toHar(): List<*> {
        return cookies.map { it.value.toHar() }
    }
}
