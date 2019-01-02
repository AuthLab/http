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

package org.authlab.http.oauth.client

import org.authlab.http.client.Client
import org.authlab.http.client.ClientBuilder
import org.authlab.http.client.asJson
import org.authlab.http.client.postForm
import org.authlab.util.loggerFor

private val logger = loggerFor<IntrospectionClient>()

class IntrospectionClient(private val httpClient: Client) {
    fun introspectToken(token: String, tokenTypeHint: String? = null): IntrospectionResponse {
        logger.trace("Introspecting token: {}", token)

        val attributes = httpClient.postForm({
            parameter("token", token)

            if (tokenTypeHint != null) {
                parameter("token_type_hint", tokenTypeHint)
            }
        }) {
            accept = "application/json"
        }.asJson<Map<String, Any>>()

        logger.trace("Introspection response: {}", attributes)

        return IntrospectionResponse(attributes)
    }

    class Builder(var location: String) {
        private var httpInit: ClientBuilder.() -> Unit = {}

        constructor(location: String, init: Builder.() -> Unit) : this(location) {
            init()
        }

        fun http(init: ClientBuilder.() -> Unit) {
            httpInit = init
        }

        fun build(): IntrospectionClient {
            val httpClientBuilder = ClientBuilder(location, httpInit)
            return IntrospectionClient(httpClientBuilder.build())
        }
    }
}

