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

package org.authlab.http.hello

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.authlab.http.client.ClientBuilder
import org.authlab.http.client.CookieManager
import org.authlab.http.client.asText
import org.authlab.http.client.buildClient
import org.authlab.util.randomPort

class HelloServerIntegrationSpec : StringSpec() {
    private val _port = randomPort()

    @Suppress("Unused")
    private val _helloServer = autoClose(HelloServerBuilder {
        listen {
            host = "localhost"
            port = _port
        }
    }.build().also { it.start() })

    private var _client = autoClose(ClientBuilder("http://localhost:$_port").build())

    override fun isInstancePerTest() = false

    init {
        "A GET request produces the expected hello response" {
            val response = _client.get()

            println(response.asText())
        }

        "A sequence of GET requests produces the expected hello responses" {
            buildClient("http://localhost:$_port") {
                keepAlive = true
                cookieManager = CookieManager()
//                proxy = "localhost:8080"
            }.use { client ->
                val response1 = client.get()

                response1.statusCode shouldBe 200

                println(response1.asText())

                val response2 = client.get()

                response2.statusCode shouldBe 200

                println(response2.asText())
            }
        }
    }
}