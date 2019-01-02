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

package org.authlab.http.server

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec
import org.authlab.crypto.setupDefaultSslContext
import org.authlab.http.authentication.Credential
import org.authlab.http.bodies.DelayedBodyReader
import org.authlab.http.bodies.FormBodyReader
import org.authlab.http.bodies.TextBodyReader
import org.authlab.http.bodies.TextBodyWriter
import org.authlab.http.client.basicAuthentication
import org.authlab.http.client.buildClient
import org.authlab.http.client.postForm
import org.authlab.http.server.authorization.basicAuthorization
import org.authlab.util.randomPort

class ServerIntegrationSpec : StringSpec() {
    override fun isInstancePerTest() = false

    init {
        setupDefaultSslContext()

        "A server with no registered endpoints will return 404" {
            val serverPort = randomPort()

            val server = buildServer {
                listen {
                    host = "localhost"
                    port = serverPort
                }
            }.also { it.start() }

            val response = server.use {
                buildClient("localhost:$serverPort").use { client ->
                    client.get()
                }
            }

            response.responseLine.statusCode shouldBe 404
        }

        "A server can have a registered endpoint" {
            val serverPort = randomPort()

            val server = buildServer {
                listen {
                    host = "localhost"
                    port = serverPort
                }

                handle("/foo") {
                    status { 200 to "OK" }
                    body { TextBodyWriter("bar") }
                }
            }.also { it.start() }

            server.use {
                buildClient("localhost:$serverPort")
                        .use { client ->
                            val response = client.get()
                            response.responseLine.statusCode shouldBe 404
                        }

                buildClient("localhost:$serverPort")
                        .use { client ->
                            val response = client.get("/foo")
                            response.responseLine.statusCode shouldBe 200
                            response.getBody(TextBodyReader()).text shouldBe "bar"
                        }
            }
        }

        "A server can have a registered endpoint with wildcards in the path" {
            val serverPort = randomPort()

            val server = buildServer {
                listen {
                    host = "localhost"
                    port = serverPort
                }

                handle("/do/*/mi") {
                    status { 200 to "OK" }
                    body { TextBodyWriter("/do/*/mi") }
                }

                handle("/do/*") {
                    status { 200 to "OK" }
                    body { TextBodyWriter("/do/*") }
                }
            }.also { it.start() }

            server.use {
                buildClient("localhost:$serverPort")
                        .use { client ->
                            val badResponse = client.get()
                            badResponse.responseLine.statusCode shouldBe 404
                        }

                buildClient("localhost:$serverPort")
                        .use { client ->
                            val okResponse = client.get("/do/re")
                            okResponse.responseLine.statusCode shouldBe 200
                            okResponse.getBody(TextBodyReader()).text shouldBe "/do/*"
                        }

                buildClient("localhost:$serverPort")
                        .use { client ->
                            val okResponse = client.get("/do/re/mi")
                            okResponse.responseLine.statusCode shouldBe 200
                            okResponse.getBody(TextBodyReader()).text shouldBe "/do/*/mi"
                        }
            }
        }

        "A server can have a registered endpoint that handles POST requests" {
            val serverPort = randomPort()

            val server = buildServer {
                listen {
                    host = "localhost"
                    port = serverPort
                }

                handle("/text") { request ->
                    val body = request.getBody()

                    status { 200 to "OK" }
                    body { TextBodyWriter(body.text.toUpperCase()) }
                }

                handle("/form", FormBodyReader()) { request ->
                    val body = request.getBody()

                    status { 200 to "OK" }
                    body { TextBodyWriter(body["foo"].toString()) }
                }

                handle("/delayed", DelayedBodyReader()) { request ->
                    val body = request.getBody(TextBodyReader())

                    status { 200 to "OK" }
                    body { TextBodyWriter(body.text.toUpperCase()) }
                }
            }.also { it.start() }

            server.use {
                buildClient("localhost:$serverPort")
                        .use { client ->
                            val response = client.post(TextBodyWriter("lorem ipsum ..."), "/text")

                            response.responseLine.statusCode shouldBe 200
                            response.getBody(TextBodyReader()).text shouldBe "LOREM IPSUM ..."
                        }

                buildClient("localhost:$serverPort")
                        .use { client ->
                            val response = client.postForm({
                                parameter { "foo" to "bar" }
                                parameter { "13" to "37" }
                            }, "/form")

                            response.responseLine.statusCode shouldBe 200
                            response.getBody(TextBodyReader()).text shouldBe "bar"
                        }

                buildClient("localhost:$serverPort")
                        .use { client ->
                            val response = client.post(TextBodyWriter("lorem ipsum ..."), "/delayed")

                            response.responseLine.statusCode shouldBe 200
                            response.getBody(TextBodyReader()).text shouldBe "LOREM IPSUM ..."
                        }
            }
        }

        "A server can enforce the Basic authentication scheme" {
            val serverPort = randomPort()

            val server = buildServer {
                listen {
                    host = "localhost"
                    port = serverPort
                }

                basicAuthorization("/foo") {
                    credential("john", "doe")
                }

                handle("*") {
                    status { 200 to "OK" }
                    body { TextBodyWriter("bar") }
                }
            }.also { it.start() }

            server.use {
                buildClient("localhost:$serverPort")
                        .use { client ->
                            val response = client.get("/")
                            response.responseLine.statusCode shouldBe 200
                            response.getBody(TextBodyReader()).text shouldBe "bar"
                        }

                buildClient("localhost:$serverPort")
                        .use { client ->
                            val response = client.get("/foo")
                            response.responseLine.statusCode shouldBe 401
                            response.getBody(TextBodyReader()).text shouldNotBe "bar"
                        }

                buildClient("localhost:$serverPort")
                        .use { client ->
                            val response = client.get("/foo") {
                                basicAuthentication(Credential.of("foo", "bar"))
                            }
                            response.responseLine.statusCode shouldBe 401
                            response.getBody(TextBodyReader()).text shouldNotBe "bar"
                        }

                buildClient("localhost:$serverPort")
                        .use { client ->
                            val response = client.get("/foo") {
                                basicAuthentication(Credential.of("john", "doe"))
                            }
                            response.responseLine.statusCode shouldBe 200
                            response.getBody(TextBodyReader()).text shouldBe "bar"
                        }
            }
        }

        "A server can be encrypted" {
            val serverPort = randomPort()

            val server = buildServer {
                listen {
                    host = "localhost"
                    port = serverPort
                    secure = true
                }

                handle("/foo") {
                    status { 200 to "OK" }
                    body { TextBodyWriter("bar") }
                }
            }.also { it.start() }

            server.use {
                buildClient("https://localhost:$serverPort")
                        .use { client ->
                            val response = client.get("/foo")

                            response.responseLine.statusCode shouldBe 200
                            response.getBody(TextBodyReader()).text shouldBe "bar"
                        }
            }
        }
    }
}
