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

class Host(val hostname: String, private val _port: Int? = null, val scheme: String = "http") {
    companion object {
        fun fromString(input: String): Host {
            var remainder = input

            val scheme = readScheme(remainder) {
                remainder = it
            } ?: "http"

            val hostAndPort = remainder.split(":")

            return when {
                hostAndPort.isEmpty() -> throw IllegalArgumentException("No hostname component in Host string")
                hostAndPort.size == 1 -> Host(hostAndPort[0], null, scheme)
                hostAndPort.size == 2 -> Host(hostAndPort[0], hostAndPort[1].toInt(), scheme)
                else -> throw IllegalArgumentException("Invalid Host format")
            }
        }

        fun readScheme(input: String, callback: (remainder: String) -> Unit): String? {
            val index = input.indexOf("://")

            return if (index != -1) {
                val scheme = input.substring(0, index)
                callback(input.removePrefix("$scheme://"))
                scheme.trim()
            } else {
                callback(input)
                null
            }
        }
    }

    val port: Int
        get() = when {
            _port != null -> _port
            scheme.equals("http", ignoreCase = true) -> 80
            else -> 443
        }

    val hostnameAndPort: String
        get() = "$hostname:$port"

    fun withHostname(hostname: String) = Host(hostname, _port, scheme)

    fun withPort(port: Int) = Host(hostname, port, scheme)

    fun withScheme(scheme: String) = Host(hostname, _port, scheme)

    override fun toString(): String {
        val sb = StringBuilder()

        sb.append(scheme)
                .append("://")
                .append(hostname)

        if (_port != null) {
            sb.append(":")
                    .append(_port)
        }

        return sb.toString()
    }
}
