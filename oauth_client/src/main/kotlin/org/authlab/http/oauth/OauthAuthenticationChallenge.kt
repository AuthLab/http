/*
 * MIT License
 *
 * Copyright (c) 2019 Johan Fylling
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

package org.authlab.http.oauth

import org.authlab.http.authentication.BearerAuthenticationChallenge
import org.authlab.util.CaseInsensitiveMap
import org.authlab.util.caseInsensitiveMapOf
import org.authlab.util.mutableCaseInsensitiveMapOf

class OauthAuthenticationChallenge(parameters: CaseInsensitiveMap<String> = caseInsensitiveMapOf()) : BearerAuthenticationChallenge(parameters) {
    class Builder private constructor() {
        private val parameters = mutableCaseInsensitiveMapOf<String>()
        private val scope = mutableSetOf<String>()

        constructor(init: Builder.() ->Unit) :this() {
            init()
        }

        fun parameter(key: String, value: String) {
            parameters[key] = value
        }

        fun scope(scope: String) {
            this.scope.addAll(scope.split("\\s+"))
        }

        fun scope(scope: Set<String>) {
            scope.forEach { scope(it) }
        }

        fun build() : OauthAuthenticationChallenge {
            if (scope.isNotEmpty()) {
                parameters["scope"] = scope.joinToString(" ")
            }
            return OauthAuthenticationChallenge(parameters)
        }
    }
}