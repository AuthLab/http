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

package org.authlab.http.bodies

import org.authlab.http.Headers
import org.authlab.http.Response
import org.authlab.io.readLine
import org.authlab.util.loggerFor
import java.io.InputStream
import java.io.PushbackInputStream
import java.nio.ByteBuffer

interface BodyReader<out B : Body> {
    fun read(inputStream: InputStream, headers: Headers): BodyReader<B>

    fun getBody(): B
}

abstract class AbstractBodyReader<out B : Body> : BodyReader<B> {
    companion object {
        private val _logger = loggerFor<BodyReader<Body>>()
    }

    override fun read(inputStream: InputStream, headers: Headers): BodyReader<B> {
        _logger.debug("Reading body from input stream")

        val pushbackInputStream = inputStream as? PushbackInputStream ?: PushbackInputStream(inputStream)

        val chunked = headers["Transfer-Encoding"]?.getFirst().equals("chunked", ignoreCase = true)

        val length = headers["Content-Length"]?.getFirstAsInt() ?: let {
            if (!chunked) {
                _logger.debug("No Content-Length but also no chunked Transfer-Encoding; assuming zero length")
            }
            0
        }

        onReadStart(if (chunked) {
            null
        } else {
            length
        })

        var bytesRead = 0

        do {
            val chunkSize = if (chunked) {
                readChunkHeader(pushbackInputStream)
            } else {
                length
            }

            val buffer = ByteBuffer.allocateDirect(chunkSize)

            while (buffer.hasRemaining()) {
                val byte = pushbackInputStream.read()
                if (byte == -1) {
                    break
                }
                buffer.put(byte.toByte())
                bytesRead++
            }

            buffer.flip()

            while (buffer.hasRemaining()) {
                onReadChunk(buffer)
            }

            if (chunked) {
                // Chunk is terminated by new-line
                pushbackInputStream.readLine()
            }
        } while (chunked && chunkSize > 0)

        _logger.debug("Body read from input stream")

        return this
    }

    private fun readChunkHeader(inputStream: PushbackInputStream): Int {
        _logger.trace("Reading chunk header")

        val chunkHeader = inputStream.readLine() ?: throw IllegalStateException("No chunk header")

        _logger.debug("Chunk header: $chunkHeader")

        return Integer.parseInt(chunkHeader.split(";", limit = 2).first().removePrefix("-"), 16)
    }

    abstract fun onReadStart(contentLength: Int?)

    abstract fun onReadChunk(buffer: ByteBuffer)
}
