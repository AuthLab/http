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

import org.authlab.util.toUrlDecodedString
import org.authlab.util.toUrlEncodedString

class Authority(hostname: String, port: Int? = null,
                val authentication: Authentication? = null) : Host(hostname, port) {
    companion object {
        fun fromString(input: String): Authority {
            // First split authority from host and port ..
            val authSplitIndex = input.lastIndexOf("@")

            val authentication: Authentication?
            val hostAndPort: String

            if (authSplitIndex == -1) {
                authentication = null
                hostAndPort = input
            } else {
                if (input.length <= authSplitIndex + 1) {
                    throw IllegalArgumentException("No hostname in authority string")
                }

                authentication = Authentication.fromString(input.substring(0, authSplitIndex))
                hostAndPort = input.substring(authSplitIndex + 1)
            }

            val host = Host.fromString(hostAndPort)

            return Authority(host.hostname, host.port, authentication)
        }

        fun fromHost(host: Host): Authority {
            return Authority(host.hostname, host.port)
        }
    }

    fun withAuthentication(authentication: Authentication) = Authority(hostname, port, authentication)

    fun withoutAuthentication() = Authority(hostname, port, null)

    fun asEncodedString(): String {
        val sb = StringBuilder()

        if (authentication != null) {
            sb.append(authentication.asEncodedString()).append("@")
        }

        sb.append(hostname)

        if (port != null) {
            sb.append(":").append(port)
        }

        return sb.toString()
    }

    override fun toString(): String {
        val sb = StringBuilder()

        if (authentication != null) {
            sb.append(authentication).append("@")
        }

        sb.append(hostname)

        if (port != null) {
            sb.append(":").append(port)
        }

        return sb.toString()
    }

    data class Authentication(val username: String, val password: String? = null) {
        companion object {
            fun fromString(input: String): Authentication {
                // Split password from username
                val components = input.split(":", limit = 2)

                return when {
                    components.isEmpty() -> throw IllegalArgumentException("No username specified in authority authentication component")
                    components.size == 1 -> Authentication(components[0].toUrlDecodedString())
                    components.size == 2 -> Authentication(components[0].toUrlDecodedString(), components[1].toUrlDecodedString())
                    else -> throw IllegalArgumentException("Invalid authority authentication component format")
                }
            }
        }

        fun asEncodedString() = if (password.isNullOrEmpty()) {
            username.toUrlEncodedString()
        } else {
            "${username.toUrlEncodedString()}:${password!!.toUrlEncodedString()}"
        }

        override fun toString() = if (password.isNullOrEmpty()) {
            username
        } else {
            "$username:****"
        }
    }
}

