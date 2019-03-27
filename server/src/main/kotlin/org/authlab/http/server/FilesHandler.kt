/*
 * MIT License
 *
 * Copyright (c) 2019 Johan Fylling
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

import org.authlab.http.bodies.EmptyBody
import org.authlab.http.bodies.EmptyBodyReader
import org.authlab.http.bodies.EmptyBodyWriter
import org.authlab.http.bodies.StreamBodyWriter
import org.authlab.util.loggerFor
import java.net.URI
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Paths

private val logger = loggerFor<FilesHandler>()

class FilesHandler(private val fileRoot: String) : Handler<EmptyBody> {
    override fun onRequest(request: ServerRequest<EmptyBody>): ServerResponse {
        val path = request.requestLine.location.path

        val filePath = URI.create(path).normalize().toString().let {
            if (it == "/") {
                "/index.html"
            } else {
                it
            }
        }

        val completeFilePath = URI.create("$fileRoot/$filePath").normalize().toString()

        val inputStream = try {
            logger.debug("Looking for file '{}'",
                    completeFilePath)

            Files.newInputStream(Paths.get(completeFilePath))
        } catch (e: NoSuchFileException) {
            logger.debug("File '{}' not found, looking for resource instead",
                    completeFilePath)

            ClassLoader.getSystemResourceAsStream(completeFilePath)
        }

        val response = if (inputStream != null) {
            ServerResponseBuilder {
                status(200 to "OK")
                this.allowContentHeaderOverrides = true
                header("Content-Type" to "text/html; charset=utf-8")
                body(StreamBodyWriter(inputStream))
            }
        } else {
            ServerResponseBuilder {
                status(404 to "Not Found")
                body(EmptyBodyWriter())
            }
        }

        return response.build()
    }
}

class FilesHandlerBuilder private constructor() : HandlerBuilder<EmptyBody> {
    var fileRoot = "www"

    constructor(init: FilesHandlerBuilder.() -> Unit) : this() {
        init()
    }

    override fun build(): Handler<EmptyBody> {
        return FilesHandler(fileRoot)
    }
}

fun ServerBuilder.files(entryPoint: String, fileRoot: String = "www") {
    handle(entryPoint, EmptyBodyReader(), FilesHandlerBuilder {
        this.fileRoot = fileRoot
    })
}

fun ServerBuilder.defaultFiles(fileRoot: String = "www") {
    default(EmptyBodyReader(), FilesHandlerBuilder {
        this.fileRoot = fileRoot
    })
}
