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

import org.authlab.http.Headers
import org.authlab.util.CaseInsensitiveMap
import org.authlab.util.caseInsensitiveMapOf
import org.authlab.util.mutableCaseInsensitiveMapOf
import java.nio.charset.StandardCharsets
import java.util.Base64

class BasicAuthenticationChallenge private constructor(parameters: CaseInsensitiveMap<String> = caseInsensitiveMapOf()) :
        AuthenticationChallenge("Basic", parameters) {
    class Builder(val realm: String) {
        private val parameters = mutableCaseInsensitiveMapOf<String>()

        fun parameter(key: String, value: String) {
            parameters[key] = value
        }

        fun build() : BasicAuthenticationChallenge {
            parameters["realm"] = realm
            return BasicAuthenticationChallenge(parameters)
        }
    }
}

class BasicAuthenticationResponse(val credential: Credential) : AuthenticationResponse("Basic") {
    companion object {
        fun fromAuthenticationResponse(authenticationResponse: AuthenticationResponse): BasicAuthenticationResponse {
            return BasicAuthenticationResponse(encodedValueToCredential(authenticationResponse.value))
        }

        fun fromRequestHeaders(headers: Headers): List<BasicAuthenticationResponse> {
            return GenericAuthenticationResponse.fromRequestHeaders(headers).asSequence()
                    .filter { "Basic".equals(it.scheme, ignoreCase = true) }
                    .map { BasicAuthenticationResponse.fromAuthenticationResponse(it) }
                    .toList()
        }

        fun encodedValueToCredential(encoded: String): Credential {
            val usernameAndPassword = String(Base64.getDecoder().decode(encoded), StandardCharsets.ISO_8859_1)
                    .split(":", limit = 2)
            return Credential.of(usernameAndPassword.first(), usernameAndPassword.last())
        }

        fun credentialToEncodedValue(credential: Credential): String {
            return Base64.getEncoder()
                    .encodeToString("${credential.subject.subject}:${credential.password.plaintext}"
                            .toByteArray(StandardCharsets.ISO_8859_1))
        }
    }

    override val value: String
        get() = credentialToEncodedValue(credential)
}
