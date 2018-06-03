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
import org.authlab.http.bodies.StreamBodyWriter
import org.authlab.http.bodies.TextBody
import org.authlab.http.client.buildClient
import org.authlab.http.client.getText
import org.authlab.util.randomPort
import java.io.File
import java.io.FileNotFoundException
import java.net.URI

class FileServerIntegrationSpec : StringSpec() {
    override fun isInstancePerTest() = false

    init {
        "A server can serve file resources" {
            val serverPort = randomPort()

            val server = buildServer {
                listen {
                    host = "localhost"
                    port = serverPort
                }

                handleCallback("/*", ::serveFile)
            }.also { it.start() }

            val response = server.use {
                buildClient("localhost:$serverPort")
                        .use { client ->
                            client.getText()
                        }
            }

            response shouldBe """
                |<!DOCTYPE html>
                |<html>
                |    <head>
                |        <title>Hello World!</title>
                |    </head>
                |    <body>
                |        <h1>Hello</h1>
                |        <p>world</p>
                |    </body>
                |</html>
            """.trimMargin()
        }
    }

    private fun serveFile(request: ServerRequest<TextBody>): ServerResponseBuilder {
        val path = request.requestLine.location.path

        val fileRoot = "www"

        val filePath = URI.create(path).normalize().toString().let {
            if (it == "/") {
                "/index.html"
            } else {
                it
            }
        }

        val completeFilePath = "$fileRoot/$filePath"

        println(completeFilePath)

        val fileInputStream = try {
            File(completeFilePath).inputStream()
        } catch (e: FileNotFoundException) {
            ClassLoader.getSystemResourceAsStream(completeFilePath)
        }

        return ServerResponseBuilder {
            status(200 to "OK")
            header("Content-Type" to "text/html")
            body(StreamBodyWriter(fileInputStream))
        }
    }
}