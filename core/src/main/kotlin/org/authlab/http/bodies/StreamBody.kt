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

import java.io.InputStream
import java.nio.ByteBuffer

class StreamBody(inputStream: InputStream) : Body<InputStream>(inputStream) {
    override val writer: BodyWriter
        get() = StreamBodyWriter(data)

}

class StreamBodyWriter(val inputStream: InputStream,
                       override val contenteType: String = "application/octet-stream",
                       override val contenteEncoding: String? = null) : BodyWriter() {
    override val contentLength: Int?
        get() = null

    override fun onWriteChunk(buffer: ByteBuffer): Boolean {
        var byte = 0

        while (byte >= 0 && buffer.hasRemaining()) {
            byte = inputStream.read()

            if (byte >= 0) {
                buffer.put(byte.toByte())
            }
        }

        return byte > 0
    }
}

//class StreamBody(val inputStream: InputStream, contentType: String = "application/octet-stream") :
//        Body(contentType, null, "chunked") {
//    companion object {
//        val _logger = loggerFor<StringBody>()
//    }
//
//    override fun isStreaming(): Boolean
//            = true
//
//    override fun calculateSize(): Int
//            = 0
//
//    override fun doWrite(outputStream: OutputStream) {
//        val buffer = ByteArray(1024)
//
//        inputStream.use { inputStream ->
//            var bytesRead: Int
//
//            do {
//                bytesRead = inputStream.read(buffer)
//
//                if (bytesRead > 0) {
//                    writeChunkHeader(bytesRead, outputStream)
//
//                    outputStream.write(buffer, 0, bytesRead)
//                    outputStream.write("\r\n".toByteArray())
//                }
//            } while (bytesRead > 0)
//
//            writeChunkHeader(0, outputStream)
//        }
//    }
//
//    private fun writeChunkHeader(size: Int, outputStream: OutputStream) {
//        val chunkHeader = size.toString(16)
//
//        _logger.trace("Writing chunk header: $chunkHeader")
//
//        outputStream.write(chunkHeader.toByteArray())
//        outputStream.write("\r\n".toByteArray())
//    }
//}