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
import org.authlab.http.bodies.emptyBody
import org.authlab.io.readLine
import org.authlab.util.loggerFor
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.nio.charset.StandardCharsets
import java.util.Base64

open class Response(val responseLine: ResponseLine, val headers: Headers = Headers(), val body: Body = emptyBody()) {
    companion object {
        private val _logger = loggerFor<Response>()

        @Throws(IOException::class)
        fun fromInputStream(inputStream: InputStream, bodyReader: BodyReader<*>): Response {
            val response = fromInputStreamWithoutBody(inputStream)

            val body = bodyReader.read(inputStream, response.headers).getBody()

            return response.withBody(body)
        }

        @Throws(IOException::class)
        fun fromInputStreamWithoutBody(inputStream: InputStream): Response {
            _logger.debug("Reading response from input stream")

            val pushbackInputStream = PushbackInputStream(inputStream)

            var line: String?
            var response: Response? = null

            do {
                line = pushbackInputStream.readLine()

                if (line != null) {
                    if (!line.isEmpty()) {
                        _logger.trace("Line read: $line")

                        response = if (response == null) {
                            Response(ResponseLine.fromString(line))
                        } else {
                            response.withHeader(Header.fromString(line))
                        }
                    } else {
                        _logger.trace("Empty line read")
                    }
                } else {
                    _logger.trace("End of stream")
                }
            } while(line != null && !line.isEmpty())

            return response ?: throw IllegalStateException("Response could not be read from input stream")
        }
    }

    fun withResponseLine(responseLine: ResponseLine): Response {
        return Response(responseLine, headers, body)
    }

    fun withHeaders(headers: Headers): Response {
        return Response(responseLine, headers, body)
    }

    fun withHeader(header: Header): Response {
        return Response(responseLine, headers.withHeader(header), body)
    }

    fun withBody(body: Body): Response {
        return Response(responseLine, headers, body)
    }

    @Throws(IOException::class)
    fun write(outputStream: OutputStream) {
        write(outputStream, body.writer)
    }

    @Throws(IOException::class)
    fun write(outputStream: OutputStream, bodyWriter: BodyWriter) {
        _logger.debug("Writing response to output stream: {}", responseLine)
        _logger.trace("Response: $this")

        val writer = outputStream.writer()

        toLines().forEach { line ->
            writer.write(line)
            writer.write("\r\n")
        }
        writer.write("\r\n")
        writer.flush()

        bodyWriter.write(outputStream)

        _logger.debug("Response written to output stream")
    }

    fun toLines(): List<String> {
        val lines = mutableListOf<String>()
        lines.addAll(responseLine.toLines())
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
                "status" to responseLine.statusCode,
                "statusText" to responseLine.reason,
                "httpVersion" to responseLine.version,
                "cookies" to Cookies.fromResponseHeaders(headers).toHar(),
                "headers" to headers.toHar(),
                "headersSize" to -1)

        headers.getHeader("Location")?.also {
            har.put("redirectUrl", it.getFirst())
        }

        val contentHar = mutableMapOf<String, Any>()

        headers.getHeader("Content-Type")?.also {
            contentHar.put("mimeType", it.getFirst())
        }

        if (body is EmptyBody) {
            har["bodySize"] = 0
        } else {
            val outputStream = ByteArrayOutputStream()
            body.writer.write(outputStream)
            val bodyBytes = outputStream.toByteArray()

            har["bodySize"] = bodyBytes.size

            if (bodyBytes.isNotEmpty()) {
                contentHar["size"] = bodyBytes.size

                val contentType = headers.getHeader("Content-Type")?.getFirst() ?: "application/octet-stream"

                contentHar["mimeType"] = contentType

                if (contentType.equals("application/octet-stream", ignoreCase = true)) {
                    contentHar["encoding"] = "base64"
                    contentHar["text"] = Base64.getEncoder().encodeToString(bodyBytes)
                } else {
                    contentHar["text"] = bodyBytes.toString(StandardCharsets.UTF_8)
                }

                har["content"] = contentHar
            }
        }

        return har
    }
}