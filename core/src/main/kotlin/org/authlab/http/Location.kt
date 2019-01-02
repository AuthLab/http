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

/**
 *                     hierarchical part
 *         ┌───────────────────┴─────────────────────┐
 *                     authority               path
 *         ┌───────────────┴───────────────┐┌───┴────┐
 *   abc://username:password@example.com:123/path/data?key=value#fragid1
 *   └┬┘   └───────┬───────┘ └────┬────┘ └┬┘           └───┬───┘ └──┬──┘
 * scheme  user information     host     port            query   fragment
 *
 * Of the ASCII character set, the characters ':', '/', '?', '#', '[', ']' and '@' are reserved for use as delimiters of the generic URI components and must be percent-encoded.
 * ':' and '@' may appear unencoded within the path, query, and fragment; and '?' and '/' may appear unencoded as data within the query or fragment.
 */
data class Location(val scheme: String? = null, val authority: Authority? = null,
                    val pathComponents: List<String> = emptyList(), val query: QueryParameters = QueryParameters(),
                    val fragment: String? = null, val asterisk: Boolean = false) {
    companion object {
        fun fromString(input: String): Location {
            var remainder = input.trim()

            if (remainder == "*") {
                return Location(asterisk = true)
            }

            // 1. Split on fragment (may contain any character)
            val fragmentSplitIndex = remainder.indexOf("#")
            val fragment: String?

            if (fragmentSplitIndex == -1) {
                fragment = null
            } else {
                fragment = remainder.substring(fragmentSplitIndex).removePrefix("#")
                remainder = remainder.substring(0, fragmentSplitIndex)
            }

            // 2. Split on scheme
            val schemeSplitIndex = remainder.indexOf("://")
            val scheme: String?

            if (schemeSplitIndex == -1) {
                scheme = null
            } else {
                scheme = remainder.substring(0, schemeSplitIndex)
                remainder = remainder.substring(schemeSplitIndex).removePrefix("://")
            }

            // 3. Split on query
            val querySplitIndex = remainder.indexOf("?")
            val query: String?

            if (querySplitIndex == -1) {
                query = null
            } else {
                query = remainder.substring(querySplitIndex).removePrefix("?")

                remainder = remainder.substring(0, querySplitIndex)
            }

            // 4. Split on path
            val pathSplitIndex = remainder.indexOf("/")
            val path: List<String>

            if (pathSplitIndex == -1) {
                path = emptyList()
            } else {
                path = stringToPathComponents(remainder.substring(pathSplitIndex))
                remainder = remainder.substring(0, pathSplitIndex)
            }

            val authority = Authority.fromString(remainder)

            return Location(scheme, authority, path, QueryParameters.fromString(query), fragment)
        }

        private fun stringToPathComponents(path: String?): List<String> {
            if (path == null) {
                return emptyList()
            }

            return path.split("/").toList()
        }
    }

    val host: Host?
        get() {
            return authority?.withoutAuthentication()
        }

    val endpoint: Endpoint
        get() {
            return if (authority == null) {
                throw IllegalStateException("Authority component required to create endpoint")
            } else if (authority.port != null) {
                Endpoint(authority.hostname, authority.port)
            } else {
                when (scheme) {
                    "http" -> Endpoint(authority.hostname, 80)
                    "https" -> Endpoint(authority.hostname, 443)
                    else -> throw IllegalStateException("No scheme to infer endpoint port from")
                }
            }
        }

    val normalizedPathComponents: List<String>
        get() = pathComponents.filter { it.isNotEmpty() }

    val path: String?
        get() = when {
            pathComponents.isNotEmpty() -> pathComponents.joinToString("/")
            else -> null
        }

    val normalizedPath: String
        get() = when {
            normalizedPathComponents.isNotEmpty() -> normalizedPathComponents.joinToString("/", "/")
            else -> "/"
        }

    val safePath: String
        get() = path ?: "/"

    fun withScheme(scheme: String) = Location(scheme, authority, pathComponents, query, fragment, asterisk)

    fun withoutScheme() = Location(null, authority, pathComponents, query, fragment, asterisk)

    fun withAuthority(authority: Authority) = Location(scheme, authority, pathComponents, query, fragment, asterisk)

    fun withoutAuthority() = Location(scheme, null, pathComponents, query, fragment, asterisk)

    fun withHost(host: Host) = Location(scheme, Authority(host.hostname, host.port), pathComponents, query, fragment, asterisk)

    fun withPath(path: String) = Location(scheme, authority, stringToPathComponents(path), query, fragment, asterisk)

    fun withSuffixedPath(path: String) = Location(scheme, authority, pathComponents + stringToPathComponents(path), query, fragment, asterisk)

    fun withoutPath() = Location(scheme, authority, emptyList(), query, fragment, asterisk)

    fun withQuery(query: QueryParameters) = Location(scheme, authority, pathComponents, query, fragment, asterisk)

    fun withFragment(fragment: String) = Location(scheme, authority, pathComponents, query, fragment, asterisk)

    fun withoutFragment() = Location(scheme, authority, pathComponents, query, null, asterisk)

    override fun toString(): String {
        if (asterisk) {
            return "*"
        }

        val sb = StringBuilder()

        if (scheme != null) {
            sb.append(scheme).append("://")
        }

        if (authority != null) {
            sb.append(authority.toString())
        }

        if (path != null) {
            sb.append(path)
        } else if (scheme == null && authority == null) {
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
