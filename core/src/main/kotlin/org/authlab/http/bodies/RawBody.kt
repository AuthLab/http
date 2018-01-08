package org.authlab.http.bodies

import org.authlab.io.readLine
import org.authlab.util.loggerFor
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.util.Arrays

class RawBody(val bytes: ByteArray, contentType: String = "application/octet-stream",
              contentEncoding: String? = null) : Body(contentType, contentEncoding) {
    companion object {
        private val _logger = loggerFor<RawBody>()

        fun fromInputStream(inputStream: InputStream, length: Int = 0, chunked: Boolean = false): RawBody {
            val pushbackInputStream = inputStream as? PushbackInputStream ?: PushbackInputStream(inputStream)

            val initialBufferSize = if (!chunked) {
                _logger.debug("Reading body with expected length: $length")
                length
            } else {
                _logger.debug("Reading body of unknown length (chunked)")
                1024
            }

            _logger.trace("initializing buffer size to $initialBufferSize")
            val buffer = ByteArrayOutputStream(initialBufferSize)

            do {
                val bytesToRead = if (chunked) {
                    readChunkHeader(pushbackInputStream)
                } else {
                    length
                }

                if (bytesToRead > 0) {
                    readStreamToBuffer(pushbackInputStream, buffer, bytesToRead, buffer.size())
                }

                if (chunked) {
                    // Chunk is terminated by new-line
                    pushbackInputStream.readLine()
                }
            } while (chunked && bytesToRead > 0)

            val body = RawBody(buffer.toByteArray())

            _logger.debug("Body read with size ${body.bytes.size}")
            _logger.trace("Body: $body")

            return body
        }

        private fun readChunkHeader(inputStream: PushbackInputStream): Int {
            _logger.trace("Reading chunk header")

            val chunkHeader = inputStream.readLine() ?: throw IllegalStateException("No chunk header")

            _logger.debug("Chunk header: $chunkHeader")

            return Integer.parseInt(chunkHeader.split(";", limit = 2).first().removePrefix("-"), 16)
        }

        private fun readStreamToBuffer(source: InputStream, target: ByteArrayOutputStream,
                                       length: Int, readOffset: Int = 0) {
            val totalBytesToRead = readOffset + length

            var value: Int
            do {
                value = source.read()
                if (value >= 0) {
                    target.write(value)
                    _logger.trace("Byte read: '${value.toChar()}' " +
                            "(${Integer.toHexString(value)} - ${Integer.toBinaryString(value)}); " +
                            "${target.size()} of $totalBytesToRead bytes read")
                }
            } while (value >= 0 && (target.size() < totalBytesToRead))

            if (target.size() < length) {
                _logger.warn("Only ${target.size()} of $totalBytesToRead bytes read")
            }
        }
    }

    fun withContentType(contentType: String) = RawBody(bytes, contentType, contentEncoding)

    fun withContentEncoding(contentEncoding: String) = RawBody(bytes, contentType, contentEncoding)

    override fun calculateSize()
            = bytes.size

    override fun doWrite(outputStream: OutputStream) {
        outputStream.write(bytes)
        outputStream.flush()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawBody

        if (!Arrays.equals(bytes, other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(bytes)
    }
}