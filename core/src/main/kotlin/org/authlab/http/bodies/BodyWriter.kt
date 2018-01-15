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
import org.authlab.util.loggerFor
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer

interface BodyWriter {
    val contentLength: Int?

    val contenteType: String?

    val contenteEncoding: String?

    val transferEncoding: String?

    fun write(outputStream: OutputStream)
}

abstract class AbstractBodyWriter : BodyWriter {
    companion object {
        private val _logger = loggerFor<BodyWriter>()
    }

    override val transferEncoding: String?
        get() = if (chunked) {
            "chunked"
        } else {
            null
        }

    private val chunked: Boolean
        get() = contentLength.let { it == null || it < 0 }

    fun getHeaders(): Headers {
        val headers = Headers()

        return if (chunked) {
            headers.withHeader("Transfer-Encoding", "chunked")
        } else {
            headers.withHeader("Content-Length", "$contentLength")
        }
    }

    override fun write(outputStream: OutputStream) {
        val chunkSize = contentLength ?: 1024

        val buffer = ByteBuffer.allocateDirect(chunkSize)

        var hasMoreData = true
        var bytesWritten = 0

        while (hasMoreData) {
            buffer.clear()

            while (hasMoreData && buffer.hasRemaining()) {
                hasMoreData = onWriteChunk(buffer)
            }

            buffer.flip()

            if (chunked) {
                writeChunkHeader(buffer.remaining(), outputStream)
            }

            while (buffer.hasRemaining()) {
                outputStream.write(buffer.get().toInt())
                bytesWritten++
            }

            if (chunked) {
                outputStream.write("\r\n".toByteArray())
            }

            if (!chunked && hasMoreData && bytesWritten >= contentLength!!) {
                _logger.warn("Content length reached, but writer reports more data to write; aborting write")
                hasMoreData = false
            }
        }

        if (!chunked && bytesWritten < contentLength!!) {
            throw IOException("Could not write as many bytes as reported content length")
        }
    }

    private fun writeChunkHeader(size: Int, outputStream: OutputStream) {
        val chunkHeader = size.toString(16)

        _logger.trace("Writing chunk header: $chunkHeader")

        outputStream.write(chunkHeader.toByteArray())
        outputStream.write("\r\n".toByteArray())
    }

    abstract fun onWriteChunk(buffer: ByteBuffer): Boolean
}
