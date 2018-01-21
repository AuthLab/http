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

package org.authlab.http

import org.authlab.http.bodies.Body
import org.authlab.http.bodies.BodyReader
import org.authlab.http.bodies.BodyWriter
import org.authlab.http.bodies.EmptyBody
import org.authlab.http.bodies.FormBody
import org.authlab.http.bodies.FormBodyReader
import org.authlab.http.bodies.StringBodyReader
import org.authlab.http.bodies.emptyBody
import org.authlab.util.loggerFor
import org.authlab.io.readLine
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.nio.charset.StandardCharsets
import java.util.Base64

class Request(val requestLine: RequestLine, val headers: Headers = Headers(), val body: Body = emptyBody()) {
    companion object {
        private val _logger = loggerFor<Request>()

        fun fromInputStream(inputStream: InputStream, bodyReader: BodyReader<*>, beforeBody: (Request) -> Unit = {}): Request {
            val request = fromInputStreamWithoutBody(inputStream)

            beforeBody(request)

            val body = bodyReader.read(inputStream, request.headers).getBody()

            return request.withBody(body)
        }

        fun fromInputStreamWithoutBody(inputStream: InputStream): Request {
            _logger.debug("Reading request from input stream")

            val pushbackInputStream = inputStream as? PushbackInputStream ?: PushbackInputStream(inputStream)

            var line: String?
            var request: Request? = null

            do {
                line = pushbackInputStream.readLine()

                if (line != null && !line.isEmpty()) {
                    _logger.trace("Line read: $line")

                    request = if (request == null) {
                        Request(RequestLine.fromString(line))
                    } else {
                        request.withHeader(Header.fromString(line))
                    }
                } else if (line == null) {
                    _logger.trace("No line read")
                } else {
                    _logger.trace("Empty line read")
                }

            } while(line != null && !line.isEmpty())

            return request ?: throw IllegalStateException("Request could not be read from input stream")
        }
    }

    val host: Host?
        get() {
            val hostHeader = headers.getHeader("Host")

            return if (hostHeader != null) {
                Host.fromString(hostHeader.getFirst())
            } else {
                requestLine.location.host
            }
        }

    fun withRequestLine(requestLine: RequestLine): Request {
        return Request(requestLine, headers, body)
    }

    fun withHeaders(headers: Headers): Request {
        return Request(requestLine, headers, body)
    }

    fun withHeader(header: Header): Request {
        return Request(requestLine, headers.withHeader(header), body)
    }

    fun withHeader(name: String, value: String): Request {
        return Request(requestLine, headers.withHeader(name, value), body)
    }

    fun withoutHeaders(name: String): Request {
        return Request(requestLine, headers.withoutHeaders(name), body)
    }

    fun withBody(body: Body): Request {
        return Request(requestLine, headers, body)
    }

    fun write(outputStream: OutputStream) {
        write(outputStream, body.writer)
    }

    fun write(outputStream: OutputStream, bodyWriter: BodyWriter) {
        _logger.debug("Writing  request to output stream")
        _logger.trace("Request: $this")

        val writer = outputStream.writer()

        toLines().forEach { line ->
            writer.write(line)
            writer.write("\r\n")
        }
        writer.write("\r\n")
        writer.flush()

        bodyWriter.write(outputStream)

        _logger.info("Request written to output stream: {}", requestLine)
    }

    fun toLines(): List<String> {
        val lines = mutableListOf<String>()
        lines.addAll(requestLine.toLines())
        lines.addAll(headers.toLines())
        return lines
    }

    override fun toString(): String {
        val sb = StringBuilder()
        toLines().forEach { line -> sb.appendln(line) }
        return sb.toString()
    }

    fun toHar(): Map<String, *> {
        val har = mutableMapOf(
                "method" to requestLine.method,
                "url" to requestLine.location
                        .withHost(host ?: throw IllegalStateException("Host required for HAR"))
                        .withoutFragment()
                        .toString(),
                "httpVersion" to requestLine.version,
                "cookies" to Cookies.fromRequestHeaders(headers).toHar(),
                "headers" to headers.toHar(),
                "queryString" to requestLine.location.query.toHar(),
                "headersSize" to -1)

        if (body is EmptyBody) {
            har["bodySize"] = 0
        } else {
            val outputStream = ByteArrayOutputStream()
            body.writer.write(outputStream)
            val bodyBytes = outputStream.toByteArray()

            har["bodySize"] = bodyBytes.size

            if (bodyBytes.isNotEmpty()) {
                val postDataHar = mutableMapOf<String, Any>()

                val contentType = headers.getHeader("Content-Type")?.getFirst() ?: "application/octet-stream"

                postDataHar["mimeType"] = contentType

                if (contentType.equals("application/octet-stream", ignoreCase = true)) {
                    postDataHar["encoding"] = "base64"
                    postDataHar["text"] = Base64.getEncoder().encodeToString(bodyBytes)
                } else {
                    postDataHar["text"] = bodyBytes.toString(StandardCharsets.UTF_8)
                }

                postDataHar["params"] = if (contentType.equals("application/x-www-form-urlencoded", ignoreCase = true)) {
                    val formBody = body as? FormBody ?: FormBodyReader()
                            .read(ByteArrayInputStream(bodyBytes), headers)
                            .getBody()
                    formBody.parameters.toHar()
                } else {
                    listOf<Any>()
                }

                har["postData"] = postDataHar
            }
        }

        return har
    }
}
