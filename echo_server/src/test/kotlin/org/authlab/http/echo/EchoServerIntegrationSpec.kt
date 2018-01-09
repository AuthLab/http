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

package org.authlab.http.echo

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import org.authlab.http.client.ClientBuilder
import org.authlab.http.client.asJson

class EchoServerIntegrationSpec : StringSpec() {
    private val _port = 9090

    @Suppress("unused")
    private val _echoServer = autoClose(EchoServerBuilder {
        listen {
            host = "localhost"
            port = _port
        }
    }.build().also { it.start() })

    private var _client = autoClose(ClientBuilder("http://localhost:$_port").build())

    override val oneInstancePerTest = false

    init {
        "A simple GET request is properly echoed as JSON" {
            val response = _client.request {
                path = "/foo/bar"
                query { "1337" to "42" }
                header { "Custom-Header" to "Not a footer" }
            }.get()

            response.responseLine.statusCode shouldBe 200

            response.headers.getHeader("Content-Type")!!.getFirst() shouldBe "application/json"

            val json = response.asJson<Map<String, Any>>()


            println(json)

            json["url"] shouldBe "http://localhost:$_port/foo/bar?1337=42"
            json["httpVersion"] shouldBe "HTTP/1.1"
            json["bodySize"] shouldBe 0.0

            val query = (json["queryString"] as List<*>).filterIsInstance<Map<String, String>>()
            query.find { it["name"] == "1337" }!!["value"] shouldBe "42"

            val headers = (json["headers"] as List<*>).filterIsInstance<Map<String, String>>()
            headers.find { it["name"] == "Host" }!!["value"] shouldBe "localhost:$_port"
            headers.find { it["name"] == "Custom-Header" }!!["value"] shouldBe "Not a footer"
        }
    }
}
