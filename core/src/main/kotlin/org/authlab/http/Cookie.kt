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
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class Cookie(val name: String,
             val value: String,
             val path: String? = null,
             val domain: String? = null,
             val expires: Instant? = null,
             val httpOnly: Boolean? = null,
             val secure: Boolean? = null,
             val sameSite: SameSite? = null) {
    companion object {
        fun fromString(input: String): Cookie {
            val splitInput = input.split(";")

            val nameValuePair = splitInput.first().split("=", limit=2)
            val name = nameValuePair.first().trim()
            val value = if (nameValuePair.size > 1) nameValuePair[1] else ""

            val directives = mutableMapOf<String, String>()

            for (directiveString in splitInput.subList(1, splitInput.size)) {
                val directivePair = directiveString.split("=", limit = 2)
                val directiveName = directivePair.first().trim()
                if (directiveName.isNotEmpty()) {
                    val directiveValue = if (directivePair.size > 1) directivePair[1] else ""
                    directives[directiveName.toUpperCase()] = directiveValue
                }
            }

            return Cookie(name, value,
                    directives["PATH"],
                    directives["DOMAIN"],
                    directives["EXPIRES"]?.let { Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(it)) },
                    directives["HTTPONLY"]?.let { it.isEmpty() || it.toBoolean() },
                    directives["SECURE"]?.let { it.isEmpty() || it.toBoolean() },
                    directives["SAMESITE"]?.let { if (it.isEmpty()) SameSite.LAX else SameSite.fromString(it) })
        }
    }

    val safePath: String
        get() = path ?: "/"

    val expiresString: String?
            = expires?.let {
        DateTimeFormatter.RFC_1123_DATE_TIME
                .format(ZonedDateTime.ofInstant(it, ZoneOffset.UTC))
    }

    fun toResponseHeader(): Header
            = Header("Set-Cookie", toString(true))

    fun toRequestHeader(): Header
            = Header("Cookie", toString(false))

    fun toString(includeDirectives: Boolean): String {
        return StringBuilder().also { sb ->
            sb.append(name).append('=').append(value)
            if (includeDirectives) {
                path?.let { sb.append("; Path=").append(it) }
                domain?.let { sb.append("; Domain=").append(it) }
                expiresString?.let { sb.append("; Expires=").append(it) }
                httpOnly?.let { sb.append("; HttpOnly") }
                secure?.let { sb.append("; Secure") }
                sameSite?.let { sb.append("; SameSite=").append(it) }
            }
        }.toString()
    }

    override fun toString()
            = toString(true)

    fun toHar(): Map<String, *> {
        val har: MutableMap<String, Any> = mutableMapOf("name" to name, "value" to value)

        path?.also { har["path"] = it }
        domain?.also { har["domain"] = it }
        expires?.also { har["expires"] = it.toString() }
        httpOnly?.also { har["httpOnly"] = it }
        secure?.also { har["secure"] = it }
        sameSite?.also { har["SameSite"] = it }

        return har
    }
}

/**
 * https://tools.ietf.org/html/draft-ietf-httpbis-rfc6265bis-02#section-5.3.7
 */
enum class SameSite(val mode: String) {
    LAX("Lax"), STRICT("Strict");

    override fun toString() = mode

    companion object {
        fun fromString(mode: String): SameSite? {
            return values().first { value ->
                value.mode.equals(mode, ignoreCase = true)
            }
        }
    }
}
