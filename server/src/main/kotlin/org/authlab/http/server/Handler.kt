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
