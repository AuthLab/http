package org.authlab.http.server

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import org.authlab.crypto.setupDefaultSslContext
import org.authlab.http.bodies.StringBody
import org.authlab.http.client.buildClient
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
                    body { StringBody("bar") }
                }
            }.also { it.start() }

            server.use {
                val badResponse = buildClient("localhost:$serverPort")
                        .use { client ->
                            client.request().get()
                }

                badResponse.responseLine.statusCode shouldBe 404

                val okResponse = buildClient("localhost:$serverPort")
                        .use { client ->
                            client.request().get("/foo")
                        }

                okResponse.responseLine.statusCode shouldBe 200
                (okResponse.body as StringBody).data shouldBe "bar"
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
                    body { StringBody("/do/*/mi") }
                }

                handle("/do/*") {
                    status { 200 to "OK" }
                    body { StringBody("/do/*") }
                }
            }.also { it.start() }

            server.use {
                val badResponse = buildClient("localhost:$serverPort")
                        .use { client ->
                            client.request().get()
                        }

                badResponse.responseLine.statusCode shouldBe 404

                var okResponse = buildClient("localhost:$serverPort")
                        .use { client ->
                            client.request().get("/do/re")
                        }

                okResponse.responseLine.statusCode shouldBe 200
                (okResponse.body as StringBody).data shouldBe "/do/*"

                okResponse = buildClient("localhost:$serverPort")
                        .use { client ->
                            client.request().get("/do/re/mi")
                        }

                okResponse.responseLine.statusCode shouldBe 200
                (okResponse.body as StringBody).data shouldBe "/do/*/mi"
            }
        }

        "A server can have a registered endpoint that handles POST requests" {
            val serverPort = randomPort()

            val server = buildServer {
                listen {
                    host = "localhost"
                    port = serverPort
                }

                handle("/foo") { request ->
                    val body = request.body as StringBody

                    status { 200 to "OK" }
                    body { StringBody(body.data.toUpperCase()) }
                }
            }.also { it.start() }

            val response = server.use {
                buildClient("localhost:$serverPort")
                        .use { client ->
                            client.request().post(StringBody("lorem ipsum ..."), "/foo")
                        }
            }

            response.responseLine.statusCode shouldBe 200
            (response.body as StringBody).data shouldBe "LOREM IPSUM ..."
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
                    body { StringBody("bar") }
                }
            }.also { it.start() }

            server.use {
                val response = buildClient("https://localhost:$serverPort")
                        .use { client ->
                            client.request().get("/foo")
                        }

                response.responseLine.statusCode shouldBe 200
                (response.body as StringBody).data shouldBe "bar"
            }
        }
    }
}
