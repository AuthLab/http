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

import org.authlab.http.authentication.BearerAuthenticationResponse
import org.authlab.http.bodies.TextBodyWriter
import org.authlab.http.oauth.OauthAuthenticationChallenge
import org.authlab.http.oauth.client.IntrospectionClient
import org.authlab.http.server.Filter
import org.authlab.http.server.FilterBuilder
import org.authlab.http.server.FilterException
import org.authlab.http.server.MutableContext
import org.authlab.http.server.ServerBuilder
import org.authlab.http.server.ServerRequest
import org.authlab.http.server.ServerResponseBuilder
import org.authlab.http.server.get
import org.authlab.util.loggerFor
import java.time.Instant

// TODO: Move to oauth_server module

private val logger = loggerFor<OauthAuthorizationFilter>()

class OauthAuthorizationFilter(val scopes: Set<String> = emptySet(),
                               val audience: String?,
                               val introspectionClient: IntrospectionClient?) : Filter {
    override fun onRequest(request: ServerRequest<*>, context: MutableContext) {
        val authenticationResponse = BearerAuthenticationResponse.fromRequestHeaders(request.headers)
                .firstOrNull()

        if (authenticationResponse == null) {
            logger.info("No Basic authorization header on request")
            throw abort()
        }

        val accessToken = authenticationResponse.value

        logger.trace("Incoming access token: '{}'", accessToken)

        context.set("access_token", accessToken)

        val currentIntrospectionClient: IntrospectionClient? = introspectionClient
                ?: context.get("oauth_filter_introspection_client")

        if (currentIntrospectionClient != null) {
            logger.trace("Introspecting token: '{}'", accessToken)

            val introspectionResponse = try {
                currentIntrospectionClient.introspectToken(accessToken)
            } catch (e: Exception) {
                logger.info("Token introspection request failed: {}", e.message)
                throw abort()
            }

            if (!introspectionResponse.active) {
                logger.info("Access token is not active")
                throw abort()
            }

            val now = Instant.now()

            introspectionResponse.tokenType?.let { tokenType ->
                if (!"Bearer".equals(tokenType, ignoreCase = true)) {
                    logger.info("Access token is not a Bearer token ({})",
                            tokenType)
                    throw abort()
                }
            }

            introspectionResponse.issuedAt?.let { issuedAt ->
                if (now.isBefore(issuedAt)) {
                    logger.info("Access token must not be used before it was issued {} (now is {})",
                            issuedAt, now)
                    throw abort()
                }
            }

            introspectionResponse.notBefore?.let { notBefore ->
                if (now.isBefore(notBefore)) {
                    logger.info("Access token must not be used before {} (now is {})",
                            notBefore, now)
                    throw abort()
                }
            }

            introspectionResponse.expiration?.let { expiration ->
                if (now.isAfter(expiration)) {
                    logger.info("Access token expires at {} (now is {})",
                            expiration, now)
                    throw abort()
                }
            }

            if (audience != null && introspectionResponse.audience != audience) {
                logger.info("Access token audience '{}' wasn't the expected '{}'",
                        introspectionResponse.audience, audience)
                throw abort()
            }

            if (!introspectionResponse.scopes.containsAll(scopes)) {
                logger.info("Access token scope '{}' doesn't satisfy scope '{}'",
                        introspectionResponse.scope, scopes)
                throw abort()
            }

            val subject = introspectionResponse.subject

            if (subject != null) {
                logger.info("Subject {} authenticated through Bearer access token")
                context.set("subject", subject)
            }
        }
    }

    private fun abort(): FilterException {
        return FilterException(ServerResponseBuilder {
            status(401 to "Unauthorized")
            header(OauthAuthenticationChallenge.Builder {
                scope(scopes)
            }.build()
                    .toResponseHeader())
            body(TextBodyWriter("Unauthorized"))
        }.build())
    }
}

class OauthAuthorizationFilterBuilder private constructor() : FilterBuilder {
    private val scopes = mutableSetOf<String>()
    private var introspectionClientBuilder: IntrospectionClient.Builder? = null

    val audience: String? = null

    constructor(init: OauthAuthorizationFilterBuilder.() -> Unit) : this() {
        init()
    }

    fun scope(scope: String) {
        this.scopes.addAll(scope.split("\\s+"))
    }

    fun scope(scope: Set<String>) {
        scope.forEach { scope(it) }
    }

    fun introspection(location: String, init: IntrospectionClient.Builder.() -> Unit = {}) {
        introspectionClientBuilder = IntrospectionClient.Builder(location, init)
    }

    override fun build(): Filter {
        val introspectionClient = introspectionClientBuilder?.build()
        return OauthAuthorizationFilter(scopes, audience, introspectionClient)
    }
}

fun ServerBuilder.oauthAuthorization(entryPoint: String, init: OauthAuthorizationFilterBuilder.() -> Unit) {
    filter(entryPoint, OauthAuthorizationFilterBuilder(init))
}

class OauthAuthorizationContextBuilder private constructor() {
    private var introspectionClientBuilder: IntrospectionClient.Builder? = null

    constructor(init: OauthAuthorizationContextBuilder.() -> Unit) : this() {
        init()
    }

    fun introspection(location: String, init: IntrospectionClient.Builder.() -> Unit = {}) {
        introspectionClientBuilder = IntrospectionClient.Builder(location, init)
    }

    fun apply(serverBuilder: ServerBuilder) {
        introspectionClientBuilder?.build()?.also {
            serverBuilder.context("oauth_filter_introspection_client", it)
        }
    }
}

fun ServerBuilder.oauthAuthorizationContext(init: OauthAuthorizationContextBuilder.() -> Unit) {
    OauthAuthorizationContextBuilder(init).apply(this)
}
