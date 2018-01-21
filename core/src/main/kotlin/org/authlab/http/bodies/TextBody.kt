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

import java.nio.charset.StandardCharsets

open class TextBody(val string: String) : Body {
    override val writer: BodyWriter
        get() = TextBodyWriter(string)
}

abstract class AbstractTextBodyReader<out B : Body> : AbstractByteBodyReader<B>() {
    protected fun getTextValue() = String(getByteArrayValue(), StandardCharsets.UTF_8)
}

open class TextBodyReader : AbstractTextBodyReader<TextBody>() {
    override fun getBody()
            = TextBody(getTextValue())
}

open class TextBodyWriter(string: String, contentType: String = "text/plain", contentEncoding: String? = null) :
        ByteBodyWriter(string.toByteArray(StandardCharsets.UTF_8), contentType, contentEncoding)