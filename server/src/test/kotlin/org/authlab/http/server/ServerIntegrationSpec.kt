package org.authlab.http.server

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import org.authlab.crypto.setupDefaultSslContext
import org.authlab.http.StringBody
import org.authlab.http.client.buildClient
import org.authlab.util.randomPort

class ServerIntegrationSpec : StringSpec() {
    init {
        setupDefaultSslContext()

        "A server with no registered endpoints will return 404" {
            val port = randomPort()

            val server = buildServer {
                host { "localhost" }
                port { port }
            }.also { Thread(it).start() }

            val response = server.use {
                buildClient {
                    hostname { "localhost" }
                    port { port }
                }.use { client ->
                    client.request {
                        path { "/" }
                    }.get()
                }
            }

            response.responseLine.statusCode shouldBe 404
        }

        "A server can have a registered endpoint" {
            val port = randomPort()

            val server = buildServer {
                host { "localhost" }
                port { port }

                handle("/foo") {
                    status { 200 to "OK" }
                    body { StringBody("bar") }
                }
            }.also { Thread(it).start() }

            server.use {
                val badResponse = buildClient("localhost:$port")
                        .use { client ->
                            client.request {
                                path { "/" }
                            }.get()
                }

                badResponse.responseLine.statusCode shouldBe 404

                val okResponse = buildClient("localhost:$port")
                        .use { client ->
                            client.request {
                                path { "/foo" }
                            }.get()
                        }

                okResponse.responseLine.statusCode shouldBe 200
                (okResponse.body as StringBody).data shouldBe "bar"
            }
        }

        "A server can have a registered endpoint with wildcards in the path" {
            val port = randomPort()

            val server = buildServer {
                host { "localhost" }
                port { port }

                handle("/do/*/mi") {
                    status { 200 to "OK" }
                    body { StringBody("/do/*/mi") }
                }

                handle("/do/*") {
                    status { 200 to "OK" }
                    body { StringBody("/do/*") }
                }
            }.also { Thread(it).start() }

            server.use {
                val badResponse = buildClient("localhost:$port")
                        .use { client ->
                            client.request {
                                path { "/" }
                            }.get()
                        }

                badResponse.responseLine.statusCode shouldBe 404

                var okResponse = buildClient("localhost:$port")
                        .use { client ->
                            client.request {
                                path { "/do/re" }
                            }.get()
                        }

                okResponse.responseLine.statusCode shouldBe 200
                (okResponse.body as StringBody).data shouldBe "/do/*"

                okResponse = buildClient("localhost:$port")
                        .use { client ->
                            client.request {
                                path { "/do/re/mi" }
                            }.get()
                        }

                okResponse.responseLine.statusCode shouldBe 200
                (okResponse.body as StringBody).data shouldBe "/do/*/mi"
            }
        }

        "A server can have a registered endpoint that handles POST requests" {
            val port = randomPort()

            val server = buildServer {
                host { "localhost" }
                port { port }

                handle("/foo") { request ->
                    val body = request.body as StringBody

                    status { 200 to "OK" }
                    body { StringBody(body.data.toUpperCase()) }
                }
            }.also { Thread(it).start() }

            val response = server.use {
                buildClient("localhost:$port")
                        .use { client ->
                            client.request {
                                path { "/foo" }
                            }.post(StringBody("lorem ipsum ..."))
                        }
            }

            response.responseLine.statusCode shouldBe 200
            (response.body as StringBody).data shouldBe "LOREM IPSUM ..."
        }

        "A server can be encrypted" {
            val port = randomPort()

            val server = buildServer {
                host { "localhost" }
                port { port }
                encrypted { true }

                handle("/foo") {
                    status { 200 to "OK" }
                    body { StringBody("bar") }
                }
            }.also { Thread(it).start() }

            server.use {
                val response = buildClient("https://localhost:$port")
                        .use { client ->
                            client.request {
                                path { "/foo" }
                            }.get()
                        }

                response.responseLine.statusCode shouldBe 200
                (response.body as StringBody).data shouldBe "bar"
            }
        }
    }
}
