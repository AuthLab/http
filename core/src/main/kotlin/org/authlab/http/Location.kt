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

data class Location(val host: Host? = null, val path: String? = null, val query: QueryParameters = QueryParameters(),
                    val fragment: String? = null, val asterisk: Boolean = false) {
    companion object {
        fun fromString(input: String): Location {
            var remainder = input.trim()

            if (remainder == "*") {
                return Location(asterisk = true)
            }

            var scheme: String? = null
            val schemeAndRemainder = remainder.split("://", limit = 2)
            if (schemeAndRemainder.size > 1) {
                scheme = schemeAndRemainder[0]
                remainder = schemeAndRemainder[1]
            }

            val pathStart = remainder.indexOf("/")

            var host: Host? = null
            var fragment: String? = null
            var query: String? = null
            var path: String? = null

            if (pathStart != -1) {
                val authority = remainder.substring(0, pathStart).takeIf { it.isNotBlank() }
                if (authority != null) {
                    host = Host.fromString(authority)
                }

                remainder = remainder.substring(pathStart)

                val remainderAndFragment = remainder.split("#", limit = 2)
                if (remainderAndFragment.size > 1) {
                    remainder = remainderAndFragment[0]
                    fragment = remainderAndFragment[1]
                }

                val remainderAndQuery = remainder.split("?", limit = 2)
                if (remainderAndQuery.size > 1) {
                    remainder = remainderAndQuery[0]
                    query = remainderAndQuery[1]
                }

                path = remainder
            } else {
                host = Host.fromString(remainder)
            }

            if (scheme != null) {
                host = host?.withScheme(scheme)
            }

            return Location(host, path, QueryParameters.fromString(query), fragment)
        }
    }

    val safePath
        get() = path ?: "/"

    fun withHost(host: Host) = Location(host, path, query, fragment, asterisk)

    fun withoutHost() = Location(null, path, query, fragment, asterisk)

    fun withPath(path: String) = Location(host, path, query, fragment, asterisk)

    fun withoutPath() = Location(host, null, query, fragment, asterisk)

    fun withQuery(query: QueryParameters) = Location(host, path, query, fragment, asterisk)

    fun withFragment(fragment: String) = Location(host, path, query, fragment, asterisk)

    fun withoutFragment() = Location(host, path, query, null, asterisk)

    override fun toString(): String {
        if (asterisk) {
            return "*"
        }

        val sb = StringBuilder()

        if (host != null) {
            sb.append(host.toString())
        }

        if (path != null) {
            sb.append(path)
        } else if (host == null) {
            sb.append(safePath)
        }

        sb.append(query.toString())

        if (fragment != null) {
            sb.append("#")
                    .append(fragment)
        }

        return sb.toString()
    }
}
