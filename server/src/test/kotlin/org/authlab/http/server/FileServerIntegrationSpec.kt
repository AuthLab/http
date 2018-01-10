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

import io.kotlintest.specs.StringSpec
import org.authlab.http.bodies.StreamBody
import org.authlab.http.bodies.StringBody
import org.authlab.http.client.buildClient
import org.authlab.util.randomPort
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.net.URI
import java.util.stream.Collectors

class FileServerIntegrationSpec : StringSpec() {
    override val oneInstancePerTest = false

    init {
        "A server can serve file resources" {
            val serverPort = randomPort()

            val server = buildServer {
                listen {
                    host = "localhost"
                    port = serverPort
                }

                handle("/*") { request ->
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

                    status { 200 to "OK" }
                    header { "Content-Type" to "text/html" }
                    body { StreamBody(fileInputStream) }
                }
            }.also { it.start() }

            val response = server.use {
                buildClient("localhost:$serverPort")
                        .use { client ->
                            client.request().get()
                        }
            }

            println(response.toHar())
        }
    }
}