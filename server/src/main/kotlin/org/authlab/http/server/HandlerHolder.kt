/*
 * MIT License
 *
 * Copyright (c) 2019 Johan Fylling
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

package org.authlab.http.server

import org.authlab.http.bodies.Body
import org.authlab.http.bodies.BodyReader

class HandlerHolder<B : Body>(entryPoint: String,
                              val handler: Handler<B>,
                              val bodyReader: BodyReader<B>) : EntryPoint(entryPoint)

@ServerMarker
class HandlerHolderBuilder<B: Body> private constructor() {
    var entryPoint: String = "/*"
    var handler: Handler<B>? = null
    var bodyReader: BodyReader<B>? = null
    var handlerBuilder: HandlerBuilder<B>? = null

    constructor(init: HandlerHolderBuilder<B>.() -> Unit) : this() {
        init()
    }

    fun build(): HandlerHolder<B> {
        val handler = this.handler ?: handlerBuilder?.build()
            ?: throw IllegalStateException("Handler or HandlerBuilder not defined for HandlerHolder")

        val bodyReader = this.bodyReader ?: throw IllegalStateException("BodyReader not defined for HandlerHolder")

        return HandlerHolder(entryPoint, handler, bodyReader)
    }
}