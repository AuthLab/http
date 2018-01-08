package org.authlab.http

import org.authlab.http.bodies.Body
import org.authlab.http.bodies.EmptyBody
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

open class Response(val responseLine: ResponseLine, val headers: Headers = Headers(), val body: Body = emptyBody()) {
    companion object {
        private val _logger = loggerFor<Response>()

        fun fromInputStream(inputStream: InputStream): Response {
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

            response ?: throw IllegalStateException("Response could not be read from input stream")

            if (line != null) {
                response = response.withBody(Body.fromInputStream(pushbackInputStream, response.headers))
            }

            _logger.info("Response read from input stream: {}", response.responseLine)

            return response
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

    fun write(outputStream: OutputStream) {
        _logger.debug("Writing response to output stream")
        _logger.trace("Response: $this")

        val writer = outputStream.writer()

        toLines().forEach { line ->
            writer.write(line)
            writer.write("\r\n")
        }
        writer.write("\r\n")
        writer.flush()

        body.write(outputStream)

        _logger.info("Response written to output stream: {}", responseLine)
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
        if (body !is EmptyBody) {
            sb.appendln().append("Body size: ").append(body.size)
        }
        return sb.toString()
    }

    fun toHar(): Map<String, *> {
        val har = mutableMapOf(
                "status" to responseLine.statusCode,
                "statusText" to responseLine.reason,
                "httpVersion" to responseLine.version,
                "cookies" to Cookies.fromResponseHeaders(headers).toHar(),
                "headers" to headers.toHar(),
                "headersSize" to -1,
                "bodySize" to body.size)

        headers.getHeader("Location")?.also {
            har.put("redirectUrl", it.getFirst())
        }

        val contentHar = mutableMapOf<String, Any>("size" to body.size)
        headers.getHeader("Content-Type")?.also {
            contentHar.put("mimeType", it.getFirst())
        }

        when (body) {
            is RawBody -> {
                contentHar.put("encoding", "base64")
                contentHar.put("text", Base64.getEncoder().encodeToString(body.bytes))
            }
            is StringBody -> contentHar.put("text", body.data)
            is JsonBody -> contentHar.put("text", body.json)
        }

        har.put("content", contentHar)

        return har
    }
}