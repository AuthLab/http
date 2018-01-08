package org.authlab.http.echo

import org.authlab.http.bodies.JsonBody
import org.authlab.http.server.Server
import org.authlab.http.server.ServerBuilder
import org.authlab.http.server.ServerListenerBuilder

class EchoServerBuilder() {
    private val _serverBuilder = ServerBuilder()

    constructor(init: EchoServerBuilder.() -> Unit) : this() {
        init()
    }

    fun listen(init: ServerListenerBuilder.() -> Unit) {
        _serverBuilder.listen(init)
    }

    var threadPoolSize: Int
        get() = _serverBuilder.threadPoolSize
        set(size) {
            _serverBuilder.threadPoolSize = size
        }

    fun build(): Server {
        _serverBuilder.default { request ->
            status { 200 to "OK" }
            body { JsonBody(request.toHar()) }
        }

        return _serverBuilder.build()
    }
}