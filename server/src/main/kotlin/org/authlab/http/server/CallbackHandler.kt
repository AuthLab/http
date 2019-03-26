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
import org.authlab.http.bodies.DelayedBody

typealias HandlerCallback<B> = (request: ServerRequest<B>) -> ServerResponseBuilder

class CallbackHandler<B : Body>(private val callback: HandlerCallback<B>) : Handler<B> {
    override fun onRequest(request: ServerRequest<B>): ServerResponseBuilder {
        return callback(request)
    }
}

@ServerMarker
class CallbackHandlerBuilder<B : Body> private constructor() : HandlerBuilder<B> {
    var callback: HandlerCallback<B>? = null

    constructor(init: CallbackHandlerBuilder<B>.() -> Unit) : this() {
        init()
    }

    fun onRequest(callback: HandlerCallback<B>) {
        this.callback = callback
    }

    fun onRequestWithResponseBuilder(init: ServerResponseBuilder.(ServerRequest<B>) -> Unit) {
        onRequest { request ->
            val builder = ServerResponseBuilder()
            builder.init(request)
            return@onRequest builder
        }
    }

    override fun build(): Handler<B> {
        val callback = this.callback ?: throw IllegalStateException("Callback not defined for CallbackHandlerBuilder")

        return CallbackHandler(callback)
    }
}

fun <B : Body> ServerBuilder.handle(entryPoint: String,
                                    bodyReader: BodyReader<B>,
                                    init: ServerResponseBuilder.(ServerRequest<B>) -> Unit) {
    handle(entryPoint, bodyReader, CallbackHandlerBuilder {
        onRequestWithResponseBuilder(init)
    })
}

fun ServerBuilder.handle(entryPoint: String,
                         init: ServerResponseBuilder.(ServerRequest<DelayedBody>) -> Unit) {
    handle(entryPoint, CallbackHandlerBuilder {
        onRequestWithResponseBuilder(init)
    })
}

fun <B : Body> ServerBuilder.default(bodyReader: BodyReader<B>,
                                     init: ServerResponseBuilder.(ServerRequest<B>) -> Unit) {
    default(bodyReader, CallbackHandlerBuilder {
        onRequestWithResponseBuilder(init)
    })
}

fun ServerBuilder.default(init: ServerResponseBuilder.(ServerRequest<DelayedBody>) -> Unit) {
    default(CallbackHandlerBuilder {
        onRequestWithResponseBuilder(init)
    })
}
