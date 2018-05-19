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
import io.kotlintest.specs.StringSpec
import org.authlab.http.bodies.TextBodyWriter

class ServerResponseBuilderSpec : StringSpec() {
    init {
        "The builder can produce the expected response" {
            val serverResponse = ServerResponseBuilder {
                status(200 to "OK")
                header("foo" to "bar")
                body { TextBodyWriter("lorem ipsum ...") }
            }.build()

            serverResponse.statusCode shouldBe 200
            serverResponse.contentType shouldBe "text/plain"
            serverResponse.contentLength shouldBe 15

            serverResponse.internalResponse.run {
                headers.getHeader("Content-Type")?.values?.size shouldBe 1
                headers.getHeader("Content-Length")?.values?.size shouldBe 1
            }
        }
    }
}
