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

package org.authlab.http.authentication

import org.authlab.http.Header
import org.authlab.util.CaseInsensitiveMap
import org.authlab.util.caseInsensitiveMapOf

open class AuthenticationChallenge(val scheme: String, val parameters: CaseInsensitiveMap<String> = caseInsensitiveMapOf()) {
    companion object {
        fun fromString(input: String) {
            TODO("Not Implemented")
        }
    }

    fun withAttribute(key: String, value: String): AuthenticationChallenge {
        val mutableParameters = parameters.toMutableMap()
        mutableParameters[key] = value
        return AuthenticationChallenge(scheme, mutableParameters)
    }

    fun withRealm(realm: String): AuthenticationChallenge {
        return withAttribute("realm", realm)
    }

    fun toResponseHeader(): Header
            = Header("WWW-Authenticate", toString())

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(scheme)

        if (parameters.isNotEmpty()) {
            sb.append(" ")
            parameters.asSequence()
                    .map { "${it.key}=${escape(it.value)}" }
                    .joinTo(sb, ", ")
        }
        return sb.toString()
    }

    private fun escape(text: String): String {
        if (text.contains("\\s".toRegex())) {
            val sb = StringBuilder()
            sb.append('"').append(text.replace("\"", "\\\"")).append('"')
            return sb.toString()
        }

        return text
    }
}