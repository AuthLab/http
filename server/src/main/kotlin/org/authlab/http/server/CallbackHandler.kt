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
import org.authlab.http.bodies.EmptyBody
import org.authlab.http.bodies.EmptyBodyReader

typealias HandlerCallback<B> = (request: ServerRequest<B>) -> ServerResponse

class CallbackHandler<B : Body>(private val bodyReader: BodyReader<B>,
                                private val callback: HandlerCallback<B>) : Handler<B> {
    override fun getBodyReader(): BodyReader<B> {
        return bodyReader
    }

    override fun onRequest(request: ServerRequest<B>): ServerResponse {
        return callback(request)
    }
}

//fun <B : Body> ServerBuilder.handle(entryPoint: String,
//                                    bodyReader: BodyReader<B>,
//                                    onRequest: ServerResponseBuilder.(ServerRequest<B>) -> Unit) {
//    handle(entryPoint, bodyReader, CallbackHandler { request ->
//        ServerResponseBuilder {
//            onRequest(request)
//        }.build()
//    })
//}

//fun ServerBuilder.handle(entryPoint: String,
//                         onRequest: ServerResponseBuilder.(ServerRequest<DelayedBody>) -> Unit) {
//    handle(entryPoint, CallbackHandler { request ->
//        ServerResponseBuilder {
//            onRequest(request)
//        }.build()
//    })
//}

//fun <B : Body> ServerBuilder.default(bodyReader: BodyReader<B>,
//                                     onRequest: ServerResponseBuilder.(ServerRequest<B>) -> Unit) {
//    default(bodyReader, CallbackHandler { request ->
//        ServerResponseBuilder {
//            onRequest(request)
//        }.build()
//    })
//}

//fun ServerBuilder.default(onRequest: ServerResponseBuilder.(ServerRequest<DelayedBody>) -> Unit) {
//    default(CallbackHandler { request ->
//        ServerResponseBuilder {
//            onRequest(request)
//        }.build()
//    })
//}

//fun <B : Body> EntryPointBuilder.handle(bodyReader: BodyReader<B>, onRequest: HandlerCallback<B>) {
//    handle(bodyReader, CallbackHandler(onRequest))
//}

//fun EntryPointBuilder.handle(onRequest: HandlerCallback<EmptyBody>) {
//    handle(EmptyBodyReader, CallbackHandler(onRequest))
//}

fun <B : Body> EntryPointBuilder.handle(bodyReader: BodyReader<B>,
                                        onRequest: ServerResponseBuilder.(ServerRequest<B>) -> Unit) {
    handle(CallbackHandler(bodyReader) { request ->
        ServerResponseBuilder {
            onRequest(request)
        }.build()
    })
}

fun EntryPointBuilder.handle(onRequest: ServerResponseBuilder.(ServerRequest<EmptyBody>) -> Unit) {
    handle(EmptyBodyReader, onRequest)
}
