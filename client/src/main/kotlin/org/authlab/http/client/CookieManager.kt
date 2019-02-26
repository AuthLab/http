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

import org.authlab.http.Cookie
import org.authlab.http.Cookies
import org.authlab.http.Headers
import org.authlab.http.Location
import java.time.Instant

class CookieManager(cookies: Cookies = Cookies()) {
    private val cookiesByPath: MutableMap<String, Cookies> = mutableMapOf()

    init {
        addCookies(cookies)
    }

    val cookies: List<Cookie>
        get() = cookiesByPath.flatMap { it.value.values }

    fun addCookies(cookies: Cookies) {
        cookies.forEach { _, cookie ->
            addCookie(cookie)
        }
    }

    fun addCookie(cookie: Cookie) {
        cookiesByPath.compute(cookie.safePath) { _, cookies ->
            cookies?.withCookie(cookie) ?: Cookies(cookie)
        }
    }

    fun removeExpiredCookies(now: Instant = Instant.now()) {
        cookiesByPath.replaceAll { _, cookies ->
            cookies.withoutExpired(now)
        }
    }

    fun addFromResponseHeaders(headers: Headers) {
        addCookies(Cookies.fromResponseHeaders(headers))
    }

    fun toRequestHeaders(location: Location): Headers {
        return toRequestHeaders(location) { true }
    }

    fun toRequestHeaders(location: Location, now: Instant): Headers {
        return toRequestHeaders(location) { cookie -> cookie.expires?.isAfter(now) ?: true }
    }

    fun toRequestHeaders(location: Location, predicate: (Cookie) -> Boolean): Headers {
        var requestCookies = Cookies()

        cookiesByPath.filter { location.safePath.startsWith(it.key) }
                .map { it.value }
                .flatMap { cookies -> cookies.values }
                .forEach { cookie ->
                    requestCookies = requestCookies.withCookieIf(cookie) { existingCookie ->
                        // Apply the passed predicate last, to avoid false positives where the predicate
                        // allows a cookie that is later rejected because of a pre-existing cookie having
                        // a more specific path.
                        cookie.safePath.length >= existingCookie.safePath.length && predicate(cookie)
                    }
                }

        return requestCookies.toRequestHeaders()
    }
}