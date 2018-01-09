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

package org.authlab.http.server

import java.util.regex.Pattern

typealias OnRequest = (ServerRequest) -> ServerResponseBuilder

class Handler(val entryPoint: String, val onRequest: OnRequest) {
    val entryPointPattern: Pattern by lazy {
        Pattern.compile(entryPoint.split("*").joinToString(".*") { Regex.escape(it) })
    }
}

class HandlerBuilder private constructor() {
    private var _entryPoint: String? = null
    private var _onRequest: OnRequest? = null

    constructor(init: HandlerBuilder.() -> Unit) : this() {
        init()
    }

    fun entryPoint(init: () -> String) {
        _entryPoint = init()
    }

    fun onRequest(init: ServerResponseBuilder.(ServerRequest) -> Unit) {
        _onRequest = { request ->
            val serverResponseBuilder = ServerResponseBuilder()
            serverResponseBuilder.init(request)
            serverResponseBuilder
        }
    }

    fun build(): Handler {
        val entryPoint = _entryPoint ?: throw IllegalStateException("entryPoint not defined on Handler")
        val onRequest = _onRequest ?: throw IllegalStateException("onRequest not defined on Handler")

        return Handler(entryPoint, onRequest)
    }
}
