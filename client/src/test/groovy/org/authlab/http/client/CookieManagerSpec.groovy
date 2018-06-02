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
import org.authlab.http.Header
import org.authlab.http.Headers
import org.authlab.http.Location
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant

class CookieManagerSpec extends Specification {
    def "A cookie-manager can be populated from HTTP response headers"() {
        given: "some Set-Cookie headers"
        def headers = new Headers(new Header("Set-Cookie", ["foo=42", "bar=1337"]))

        and: "a cookie-manager"
        def cookieManager = new CookieManager()

        when: "given the headers"
        cookieManager.addFromResponseHeaders(headers)

        then: "the cookie-manager will contain the expected cookies"
        cookieManager.cookies.size() == 2
        cookieManager.cookies.find { it.name == "foo" }.value == "42"
        cookieManager.cookies.find { it.name == "bar" }.value == "1337"
    }

    @Unroll
    def "A cookie-manager can create HTTP request headers that respect a given location (#location)"(List<Cookie> cookies, Location location, List<Header> expectedHeaders) {
        given: "a cookie-manager"
        def cookieManager = new CookieManager()

        and: "a set ot cookies"
        cookies.forEach { cookie -> cookieManager.addCookie(cookie) }

        when: "the cookie-manager is requested to generate request headers for a given location"
        def headers = cookieManager.toRequestHeaders(location)
        def cookieHeaders = headers["Cookie"]

        then: "they are as expected"
        cookieHeaders?.values?.size() ?: 0 == expectedHeaders.size()

        expectedHeaders.collect { it.values }.flatten().forEach {
            assert cookieHeaders.values.contains(it)
        }

        where:
        cookies                                          | location                                           | expectedHeaders
        [createCookie("foo", "bar")]                     | Location.@Companion.fromString("/")                | [new Header("Cookie", "foo=bar")]
        [createCookie("foo", "bar")]                     | Location.@Companion.fromString("/some/place")      | [new Header("Cookie", "foo=bar")]
        [createCookie("foo", "bar", "/")]                | Location.@Companion.fromString("/some/place")      | [new Header("Cookie", "foo=bar")]
        [createCookie("foo", "bar", "/some")]            | Location.@Companion.fromString("/some/place")      | [new Header("Cookie", "foo=bar")]
        [createCookie("foo", "bar", "/some/place")]      | Location.@Companion.fromString("/some/place")      | [new Header("Cookie", "foo=bar")]
        [createCookie("foo", "bar", "/some/place/else")] | Location.@Companion.fromString("/some/place")      | []
        [createCookie("foo", "bar", "/other/place")]     | Location.@Companion.fromString("/some/place")      | []
        [createCookie("foo", "wrong", "/some"),
         createCookie("foo", "bar", "/some/place")]      | Location.@Companion.fromString("/some/place/else") | [new Header("Cookie", "foo=bar")]
        [createCookie("foo", "bar", "/some/place"),
         createCookie("foo", "wrong", "/some")]          | Location.@Companion.fromString("/some/place/else") | [new Header("Cookie", "foo=bar")]
        [createCookie("foo", "bar", "/some"),
         createCookie("foo", "wrong", "/some/place")]    | Location.@Companion.fromString("/some")            | [new Header("Cookie", "foo=bar")]
        [createCookie("foo", "wrong", "/some/place"),
         createCookie("foo", "bar", "/some")]            | Location.@Companion.fromString("/some")            | [new Header("Cookie", "foo=bar")]
    }

    @Unroll
    def "A cookie-manager can create HTTP request headers that respect cookie expiration"(List<Cookie> cookies, Instant now, List<Header> expectedHeaders) {
        given: "a cookie-manager"
        def cookieManager = new CookieManager()

        and: "a set ot cookies"
        cookies.forEach { cookie -> cookieManager.addCookie(cookie) }

        when: "the cookie-manager is requested to generate request headers for a given now"
        def headers = cookieManager.toRequestHeaders(Location.@Companion.fromString("/") , now)
        def cookieHeaders = headers["Cookie"]

        then: "they are as expected"
        cookieHeaders?.values?.size() ?: 0 == expectedHeaders.size()

        expectedHeaders.collect { it.values }.flatten().forEach {
            assert cookieHeaders.values.contains(it)
        }

        where:
        cookies                                                   | now                         | expectedHeaders
        [createCookie("foo", "bar")]                              | Instant.ofEpochSecond(1000) | [new Header("Cookie", "foo=bar")]
        [createCookie("foo", "bar", Instant.ofEpochSecond(900))]  | Instant.ofEpochSecond(1000) | []
        [createCookie("foo", "bar", Instant.ofEpochSecond(1100))] | Instant.ofEpochSecond(1000) | [new Header("Cookie", "foo=bar")]
    }

    private static def createCookie(String name, String value) {
        return new Cookie(name, value, null, null, null, null, null)
    }

    private static def createCookie(String name, String value, String path) {
        return new Cookie(name, value, path, null, null, null, null)
    }

    private static def createCookie(String name, String value, Instant expires) {
        return new Cookie(name, value, null, null, expires, null, null)
    }
}
