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

package org.authlab.http.server.authorization

import org.authlab.http.authentication.BasicAuthenticationChallenge
import org.authlab.http.authentication.BasicAuthenticationResponse
import org.authlab.http.authentication.Credential
import org.authlab.http.bodies.TextBodyWriter
import org.authlab.http.server.Filter
import org.authlab.http.server.FilterBuilder
import org.authlab.http.server.FilterException
import org.authlab.http.server.MutableContext
import org.authlab.http.server.ServerBuilder
import org.authlab.http.server.ServerRequest
import org.authlab.http.server.ServerResponseBuilder
import org.authlab.util.loggerFor

private val logger = loggerFor<BasicAuthorizationFilter>()

class BasicAuthorizationFilter(val realm: String, val credentials: Map<String, Credential>) : Filter {
    override fun onRequest(request: ServerRequest<*>, context: MutableContext) {
        val authenticationResponse = BasicAuthenticationResponse.fromRequestHeaders(request.headers)
                .firstOrNull()

        if (authenticationResponse == null) {
            logger.debug("No Basic authorization header on request")
            throw abort()
        }

        val incomingCredential = authenticationResponse.credential

        val credential = credentials[incomingCredential.subject.subject]

        if (credential == null) {
            logger.debug("Unknown subject '{}'", incomingCredential.subject)
            throw abort()
        }

        if (credential.password != incomingCredential.password) {
            logger.debug("Invalid password for subject '{}'", incomingCredential.subject)
            throw abort()
        }

        logger.debug("Subject '{}' authenticated", incomingCredential.subject)

        context.set("subject", authenticationResponse.credential.subject)

    }

    private fun abort(): FilterException {
        return FilterException(ServerResponseBuilder {
            status(401 to "Unauthorized")
            header(BasicAuthenticationChallenge.Builder(realm)
                    .build()
                    .toResponseHeader())
            body(TextBodyWriter("Unauthorized"))
        }.build())
    }
}

class BasicAuthorizationFilterBuilder private constructor() : FilterBuilder {
    private val credentials = mutableMapOf<String, Credential>()

    var realm: String = "default"

    constructor(init: BasicAuthorizationFilterBuilder.() -> Unit) : this() {
        init()
    }

    fun credential(username: String, password: String) {
        credential(Credential.of(username, password))
    }

    fun credential(credential: Credential) {
        credentials[credential.subject.subject] = credential
    }

    override fun build(): Filter {
        return BasicAuthorizationFilter(realm, credentials.toMap())
    }
}

fun ServerBuilder.basicAuthorization(entryPoint: String, init: BasicAuthorizationFilterBuilder.() -> Unit) {
    filter(entryPoint, BasicAuthorizationFilterBuilder(init))
}
