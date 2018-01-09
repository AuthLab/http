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
import org.authlab.http.bodies.EmptyBody
import org.authlab.http.bodies.FormBody
import org.authlab.http.bodies.JsonBody
import org.authlab.http.bodies.RawBody
import org.authlab.http.bodies.StringBody
import org.authlab.http.bodies.emptyBody
import org.authlab.util.loggerFor
import org.authlab.io.readLine
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.util.Base64

class Request(val requestLine: RequestLine, val headers: Headers = Headers(), val body: Body = emptyBody()) {
    companion object {
        private val _logger = loggerFor<Request>()

        fun fromInputStream(inputStream: InputStream, beforeBody: (Request) -> Unit = {}): Request {
            _logger.debug("Reading request from input stream")

            val pushbackInputStream = PushbackInputStream(inputStream)

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

            request ?: throw IllegalStateException("Request could not be read from input stream")

            if (line != null) {
                request = request.withBody(Body.fromInputStream(pushbackInputStream, request.headers,
                        { beforeBody(request!!) }))
            }

            _logger.info("Request read from input stream: {}", request.requestLine)

            return request
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
        _logger.debug("Writing  request to output stream")
        _logger.trace("Request: $this")

        val writer = outputStream.writer()

        toLines().forEach { line ->
            writer.write(line)
            writer.write("\r\n")
        }
        writer.write("\r\n")
        writer.flush()

        body.write(outputStream)

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
        when (body) {
            is FormBody -> sb.appendln(body.toString())
            is JsonBody -> sb.appendln(body.toString())
            !is EmptyBody -> sb.appendln().append("Body size: ").append(body.size)
        }
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
                "headersSize" to -1,
                "bodySize" to body.size)

        if (body.size > 0) {
            val postDataHar = mutableMapOf<String, Any>("size" to body.size)
            headers.getHeader("Content-Type")?.also {
                postDataHar.put("mimeType", it.getFirst())
            }

            when (body) {
                is RawBody -> {
                    postDataHar.put("encoding", "base64")
                    postDataHar.put("text", Base64.getEncoder().encodeToString(body.bytes))
                    postDataHar.put("params", mapOf<String, Any>())
                }
                is StringBody -> {
                    postDataHar.put("text", body.data)
                    postDataHar.put("params", mapOf<String, Any>())
                }
                is JsonBody -> {
                    postDataHar.put("text", body.json)
                    postDataHar.put("params", mapOf<String, Any>())
                }
                is FormBody -> {
                    postDataHar.put("text", body.toString())
                    postDataHar.put("params", body.parameters.toHar())
                }

            // TODO: include form parameters, if present
            }

            har.put("postData", postDataHar)

            // TODO: include form parameters, if present
        }

        return har
    }
}
