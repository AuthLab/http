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

package org.authlab.http.client

import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldNotBe
import io.kotlintest.specs.StringSpec
import org.authlab.http.bodies.JsonBodyReader
import org.authlab.http.bodies.SerializedJsonBody
import org.authlab.http.bodies.StringBodyWriter
import org.authlab.http.echo.EchoServerBuilder
import org.authlab.util.randomPort

class ClientIntegrationSpec : StringSpec() {
    private val _serverPort = randomPort()
    private val _encryptedServerPort = randomPort()

    @Suppress("unused")
    private val _echoServer = autoClose(EchoServerBuilder {
        listen {
            host = "localhost"
            port = _serverPort
        }
    }.build().also { it.start() })

    @Suppress("unused")
    private val _encryptedEchoServer = autoClose(EchoServerBuilder {
        listen {
            host = "localhost"
            port = _encryptedServerPort
            secure = true
        }
    }.build().also { it.start() })

    override val oneInstancePerTest = false

    init {
        "it should be possible to make a simple GET request" {
            val json = buildClient("http://localhost:$_serverPort")
                    .use { client ->
                        client.request().getJson<Map<String, Any>>()
                    }

            json["method"] shouldBe "GET"
            json["url"] shouldBe "http://localhost:$_serverPort/"
            json["httpVersion"] shouldBe "HTTP/1.1"
            json["bodySize"] shouldBe 0.0

            val headers = getHeaders(json)

            headers.find { it["name"] == "Host" }!!["value"] shouldBe "localhost:$_serverPort"
        }

        "it should be possible to make a simple GET request with a delayed body processor" {
            buildClient("http://localhost:$_serverPort")
                    .use { client ->
                        val response = client.request().get()

                        response.statusCode shouldBe 200

                        val json = getJson(response)

                        json["method"] shouldBe "GET"
                        json["url"] shouldBe "http://localhost:$_serverPort/"
                        json["httpVersion"] shouldBe "HTTP/1.1"
                        json["bodySize"] shouldBe 0.0

                        val headers = getHeaders(json)

                        headers.find { it["name"] == "Host" }!!["value"] shouldBe "localhost:$_serverPort"
                    }
        }

        "it should be possible to make a simple GET request with a path" {
            buildClient("http://localhost:$_serverPort")
                    .use { client ->
                        val response = client.request().get(path = "/some/place/nice")

                        response.statusCode shouldBe 200

                        val json = getJson(response)

                        json["method"] shouldBe "GET"
                        json["url"] shouldBe "http://localhost:$_serverPort/some/place/nice"
                        json["httpVersion"] shouldBe "HTTP/1.1"
                        json["bodySize"] shouldBe 0.0
                    }
        }

        "it should be possible to make a simple GET request with query parameters" {
            buildClient("http://localhost:$_serverPort")
                    .use { client ->
                        val response = client.request {
                            query { "foo" to "bar" }
                            query("13", "37")
                            query("42")
                        }.get()

                        response.statusCode shouldBe 200

                        val json = getJson(response)

                        json["method"] shouldBe "GET"
                        json["url"] shouldBe "http://localhost:$_serverPort/?foo=bar&13=37&42"
                        json["httpVersion"] shouldBe "HTTP/1.1"
                        json["bodySize"] shouldBe 0.0

                        val query = getQuery(json)

                        query.find { it["name"] == "foo" }!!["value"] shouldBe "bar"
                        query.find { it["name"] == "13" }!!["value"] shouldBe "37"
                        query.find { it["name"] == "42" }!!["value"] shouldBe ""
                    }
        }

        "it should be possible to make a simple GET request with headers" {
            buildClient("http://localhost:$_serverPort")
                    .use { client ->
                        val response = client.request {
                            header { "foo" to "bar" }
                            header("13", "37")
                            header("foo", "also_bar")
                        }.get()

                        response.statusCode shouldBe 200

                        val json = getJson(response)

                        json["method"] shouldBe "GET"
                        json["url"] shouldBe "http://localhost:$_serverPort/"
                        json["httpVersion"] shouldBe "HTTP/1.1"
                        json["bodySize"] shouldBe 0.0

                        val headers = getHeaders(json)

                        headers.find { it["name"] == "Host" }!!["value"] shouldBe "localhost:$_serverPort"
                        headers.find { it["name"] == "13" }!!["value"] shouldBe "37"
                        headers.filter { it["name"] == "foo" }[0]["value"] shouldBe "bar"
                        headers.filter { it["name"] == "foo" }[1]["value"] shouldBe "also_bar"
                    }
        }

        "it should be possible to make a simple POST request" {
            buildClient("http://localhost:$_serverPort")
                    .use { client ->
                        val response = client.request().post(StringBodyWriter("hello"))

                        response.statusCode shouldBe 200

                        val json = getJson(response)

                        json["method"] shouldBe "POST"
                        json["url"] shouldBe "http://localhost:$_serverPort/"
                        json["httpVersion"] shouldBe "HTTP/1.1"
                        json["bodySize"] shouldBe 5.0

                        val headers = getHeaders(json)

                        headers.find { it["name"] == "Content-Type" }!!["value"] shouldBe "text/plain"
                        headers.find { it["name"] == "Content-Length" }!!["value"] shouldBe "5"

                        val postData = getPostData(json)

                        postData["size"] shouldBe 5.0
                        postData["mimeType"] shouldBe "text/plain"
                        postData["text"] shouldBe "hello"
                    }
        }

        "it should be possible to make a simple json POST request" {
            buildClient("http://localhost:$_serverPort")
                    .use { client ->
                        val response = client.request().postJson(mapOf("foo" to "bar"))

                        response.statusCode shouldBe 200

                        val json = getJson(response)

                        json["method"] shouldBe "POST"
                        json["url"] shouldBe "http://localhost:$_serverPort/"
                        json["httpVersion"] shouldBe "HTTP/1.1"

                        val postData = getPostData(json)

                        postData["size"] shouldBe 13.0
                        postData["mimeType"] shouldBe "application/json"
                        postData["text"] shouldBe "{\"foo\":\"bar\"}"
                    }
        }

        "it should be possible to make a simple form POST request" {
            buildClient("http://localhost:$_serverPort")
                    .use { client ->
                        val response = client.request().postForm {
                            parameter { "name" to "Foo" }
                            parameter { "surname" to "Bar" }
                        }

                        response.statusCode shouldBe 200

                        val json = getJson(response)

                        json["method"] shouldBe "POST"
                        json["url"] shouldBe "http://localhost:$_serverPort/"
                        json["httpVersion"] shouldBe "HTTP/1.1"

                        val postData = getPostData(json)

                        postData["size"] shouldBe 20.0
                        postData["mimeType"] shouldBe "application/x-www-form-urlencoded"

                        val postParams = (postData["params"] as List<*>).filterIsInstance<Map<String, String>>()

                        postParams.size shouldBe 2
                        postParams.find { it["name"] == "name" }!!["value"] shouldBe "Foo"
                        postParams.find { it["name"] == "surname" }!!["value"] shouldBe "Bar"
                    }
        }

        "it should be possible to make a simple proxied GET request" {
            buildClient("http://localhost:$_serverPort") {
                proxy = "localhost:8080"
            }.use { client ->
                val response = client.request().get()

                response.statusCode shouldBe 200

                response.headers.getHeader("Content-Type")!!.getFirst() shouldBe "application/json"

                val json = getJson(response)

                json["url"] shouldBe "http://localhost:$_serverPort/"
                json["httpVersion"] shouldBe "HTTP/1.1"
                json["bodySize"] shouldBe 0.0

                val headers = getHeaders(json)
                headers.find { it["name"] == "Host" }!!["value"] shouldBe "localhost:$_serverPort"
                headers.find { it["name"] == "Forwarded" }!!["value"]!! shouldBe
                        "by=/127.0.0.1:8080; for=${client.socket.localSocketAddress}"
                headers.find { it["name"] == "Proxy-Token" }!!["value"] shouldNotBe null
            }
        }.config(enabled = false)

        "it should be possible to make an encrypted GET request through a proxy tunnel" {
            buildClient("https://localhost:$_encryptedServerPort") {
                proxy = "localhost:8080"
            }.use { client ->
                val response = client.request().get()

                response.statusCode shouldBe 200

                response.headers.getHeader("Content-Type")!!.getFirst() shouldBe "application/json"

                val json = getJson(response)

                json["url"] shouldBe "http://localhost:$_encryptedServerPort/"
                json["httpVersion"] shouldBe "HTTP/1.1"
                json["bodySize"] shouldBe 0.0

                val headers = getHeaders(json)
                headers.find { it["name"] == "Host" }!!["value"] shouldBe "localhost:$_serverPort"
                headers.find { it["name"] == "Forwarded" }!!["value"]!! shouldBe
                        "by=/127.0.0.1:8080; for=${client.socket.localSocketAddress}"
                headers.find { it["name"] == "Proxy-Token" }!!["value"] shouldNotBe null
            }
        }.config(enabled = false)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getPostData(json: Map<String, Any>)
            = json["postData"] as Map<String, Any>

    private fun getQuery(json: Map<String, Any>)
            = (json["queryString"] as List<*>).filterIsInstance<Map<String, String>>()

    private fun getHeaders(json: Map<String, Any>)
            = (json["headers"] as List<*>).filterIsInstance<Map<String, String>>()

    private fun getJson(response: ClientResponse<*>): Map<String, String> {
        response.headers.getHeader("Content-Type")!!.getFirst() shouldBe "application/json"

        val jsonBody = response.getBody(JsonBodyReader()) as SerializedJsonBody
        println(jsonBody.string)

        return jsonBody.getTypedValue()
    }
}
