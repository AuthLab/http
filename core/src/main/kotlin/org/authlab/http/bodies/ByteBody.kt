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

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.min

class ByteBody(val bytes: ByteArray) : Body {
    override val writer: BodyWriter
        get() = ByteBodyWriter(bytes)
}

abstract class AbstractByteBodyReader<out B : Body> : AbstractBodyReader<B>() {
    private var _buffer: ByteArrayOutputStream? = null

    override fun onReadStart(contentLength: Int?) {
        _buffer = ByteArrayOutputStream(contentLength ?: 1024)
    }

    override fun onReadChunk(buffer: ByteBuffer) {
        val internalBuffer = _buffer ?: throw IllegalStateException("Internal buffer not initialized")

        while (buffer.hasRemaining()) {
            internalBuffer.write(buffer.get().toInt())
        }
    }

    protected fun getByteArrayValue(): ByteArray {
        val internalBuffer = _buffer ?: throw IllegalStateException("Internal buffer not initialized")

        return internalBuffer.toByteArray()
    }
}

class ByteBodyReader : AbstractByteBodyReader<ByteBody>() {
    override fun getBody()
            = ByteBody(getByteArrayValue())
}

open class ByteBodyWriter(val bytes: ByteArray,
                          override val contentType: String = "application/octet-stream",
                          override val contentEncoding: String? = null) : AbstractBodyWriter() {
    private var _position = 0

    override val contentLength: Int?
        get() = bytes.size

    override fun onWriteChunk(buffer: ByteBuffer): Boolean {
        val bytesToWrite = min(buffer.remaining(), bytes.size)
        buffer.put(bytes, _position, bytesToWrite)
        _position += bytesToWrite
        return _position < bytes.size
    }
}