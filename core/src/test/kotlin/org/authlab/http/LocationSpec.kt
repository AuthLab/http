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

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.forAll
import io.kotlintest.tables.headers
import io.kotlintest.tables.row
import io.kotlintest.tables.table

class LocationSpec : StringSpec() {
    init {
        "Location strings can be deserialized" {
            val data = table(
                    headers("input",                                                                 "scheme",          "host",        "port",       "username",            "password",            "path",                       "query",                        "fragment"),
                    row("example.com",                                                               null as String?,   "example.com", null as Int?, null as String?,       null as String?,       null as String?,              null as String?,                null as String?),
                    row("example.com:123",                                                           null as String?,   "example.com", 123 as Int?,  null as String?,       null as String?,       null as String?,              null as String?,                null as String?),
                    row("username@example.com:123",                                                  null as String?,   "example.com", 123 as Int?,  "username" as String?, null as String?,       null as String?,              null as String?,                null as String?),
                    row("username:password@example.com:123",                                         null as String?,   "example.com", 123 as Int?,  "username" as String?, "password" as String?, null as String?,              null as String?,                null as String?),
                    row("http://username:password@example.com:123",                                  "http" as String?, "example.com", 123 as Int?,  "username" as String?, "password" as String?, null as String?,              null as String?,                null as String?),
                    row("http://username:password@example.com:123/",                                 "http" as String?, "example.com", 123 as Int?,  "username" as String?, "password" as String?, "/" as String?,               null as String?,                null as String?),
                    row("http://username:password@example.com:123/this/is/a/path",                   "http" as String?, "example.com", 123 as Int?,  "username" as String?, "password" as String?, "/this/is/a/path" as String?, null as String?,                null as String?),
                    row("http://username:password@example.com:123/this/is/a/path#!#$'()*+,/:;=?@[]", "http" as String?, "example.com", 123 as Int?,  "username" as String?, "password" as String?, "/this/is/a/path" as String?, null as String?,                "!#\$'()*+,/:;=?@[]" as String?),
                    row("http://example.com:123",                                                    "http" as String?, "example.com", 123 as Int?,  null as String?,       null as String?,       null as String?,              null as String?,                null as String?),
                    row("http://example.com:123/",                                                   "http" as String?, "example.com", 123 as Int?,  null as String?,       null as String?,       "/" as String?,               null as String?,                null as String?),
                    row("http://example.com:123/this/is/a/path",                                     "http" as String?, "example.com", 123 as Int?,  null as String?,       null as String?,       "/this/is/a/path" as String?, null as String?,                null as String?),
                    row("http://example.com:123/this/is/a/path?a&b=1&c=foo&c=bar",                   "http" as String?, "example.com", 123 as Int?,  null as String?,       null as String?,       "/this/is/a/path" as String?, "a&b=1&c=foo&c=bar" as String?, null as String?),
                    row("http://example.com:123/this/is/a/path#",                                    "http" as String?, "example.com", 123 as Int?,  null as String?,       null as String?,       "/this/is/a/path" as String?, null as String?,                "" as String?),
                    row("http://example.com:123/this/is/a/path#!#$'()*+,/:;=?@[]",                   "http" as String?, "example.com", 123 as Int?,  null as String?,       null as String?,       "/this/is/a/path" as String?, null as String?,                "!#\$'()*+,/:;=?@[]" as String?)
            )

            forAll(data) { input: String, scheme: String?, host: String, port: Int?, username: String?, password: String?, path: String?, query: String?, fragment: String? ->
                val location = Location.fromString(input)

                location.scheme shouldBe scheme
                location.authority?.hostname shouldBe host
                location.host?.hostname shouldBe host
                location.authority?.port shouldBe port
                location.authority?.authentication?.username shouldBe username
                location.authority?.authentication?.password shouldBe password
                location.path shouldBe path
                location.fragment shouldBe fragment

                location.query shouldBe QueryParameters.fromString(query)
            }
        }
    }
}