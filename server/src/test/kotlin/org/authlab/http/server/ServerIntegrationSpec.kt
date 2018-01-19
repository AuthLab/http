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

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import org.authlab.crypto.setupDefaultSslContext
import org.authlab.http.bodies.DelayedBodyReader
import org.authlab.http.bodies.FormBodyReader
import org.authlab.http.bodies.StringBodyReader
import org.authlab.http.bodies.StringBodyWriter
import org.authlab.http.client.buildClient
import org.authlab.http.client.postForm
import org.authlab.util.randomPort

class ServerIntegrationSpec : StringSpec() {
    override val oneInstancePerTest = false

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
                    client.request().get()
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
                    body { StringBodyWriter("bar") }
                }
            }.also { it.start() }

            server.use {
                buildClient("localhost:$serverPort")
                        .use { client ->
                            val response = client.request().get()
                            response.responseLine.statusCode shouldBe 404
                        }

                buildClient("localhost:$serverPort")
                        .use { client ->
                            val response = client.request().get("/foo")
                            response.responseLine.statusCode shouldBe 200
                            response.getBody(StringBodyReader()).string shouldBe "bar"
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
                    body { StringBodyWriter("/do/*/mi") }
                }

                handle("/do/*") {
                    status { 200 to "OK" }
                    body { StringBodyWriter("/do/*") }
                }
            }.also { it.start() }

            server.use {
                buildClient("localhost:$serverPort")
                        .use { client ->
                            val badResponse = client.request().get()
                            badResponse.responseLine.statusCode shouldBe 404
                        }

                buildClient("localhost:$serverPort")
                        .use { client ->
                            val okResponse = client.request().get("/do/re")
                            okResponse.responseLine.statusCode shouldBe 200
                            okResponse.getBody(StringBodyReader()).string shouldBe "/do/*"
                        }

                buildClient("localhost:$serverPort")
                        .use { client ->
                            val okResponse = client.request().get("/do/re/mi")
                            okResponse.responseLine.statusCode shouldBe 200
                            okResponse.getBody(StringBodyReader()).string shouldBe "/do/*/mi"
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
                    body { StringBodyWriter(body.string.toUpperCase()) }
                }

                handle("/form", FormBodyReader()) { request ->
                    val body = request.getBody()

                    status { 200 to "OK" }
                    body { StringBodyWriter(body.parameters.toString()) }
                }

                handle("/delayed", DelayedBodyReader()) { request ->
                    val body = request.getBody(StringBodyReader())

                    status { 200 to "OK" }
                    body { StringBodyWriter(body.string.toUpperCase()) }
                }
            }.also { it.start() }

            server.use {
                buildClient("localhost:$serverPort")
                        .use { client ->
                            val response = client.request().post(StringBodyWriter("lorem ipsum ..."), "/text")

                            response.responseLine.statusCode shouldBe 200
                            response.getBody(StringBodyReader()).string shouldBe "LOREM IPSUM ..."
                        }

                buildClient("localhost:$serverPort")
                        .use { client ->
                            val response = client.request().postForm("/form") {
                                parameter { "foo" to "bar" }
                                parameter { "13" to "37" }
                            }

                            response.responseLine.statusCode shouldBe 200
                            response.getBody(StringBodyReader()).string shouldBe "foo=bar&13=37"
                        }

                buildClient("localhost:$serverPort")
                        .use { client ->
                            val response = client.request().post(StringBodyWriter("lorem ipsum ..."), "/delayed")

                            response.responseLine.statusCode shouldBe 200
                            response.getBody(StringBodyReader()).string shouldBe "LOREM IPSUM ..."
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
                    body { StringBodyWriter("bar") }
                }
            }.also { it.start() }

            server.use {
                buildClient("https://localhost:$serverPort")
                        .use { client ->
                            val response = client.request().get("/foo")

                            response.responseLine.statusCode shouldBe 200
                            response.getBody(StringBodyReader()).string shouldBe "bar"
                        }


            }
        }
    }
}
