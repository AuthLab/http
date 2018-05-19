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

import io.kotlintest.specs.StringSpec
import org.authlab.http.client.ClientBuilder
import org.authlab.http.client.asText
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
            val response = _client.request().get()

            println(response.asText())
        }
    }
}