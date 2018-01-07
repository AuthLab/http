package org.authlab.http.echo

import org.authlab.http.JsonBody
import org.authlab.http.server.Server
import org.authlab.http.server.ServerBuilder
import java.net.InetAddress

class EchoServerBuilder() {
    private val _serverBuilder = ServerBuilder()

    constructor(init: EchoServerBuilder.() -> Unit) : this() {
        init()
    }

    fun inetAddress(init: () -> InetAddress) {
        _serverBuilder.inetAddress(init)
    }

    fun host(init: () -> String) {
        _serverBuilder.host(init)
    }

    fun port(init: () -> Int) {
        _serverBuilder.port(init)
    }

    fun encrypted(init: () -> Boolean) {
        _serverBuilder.encrypted(init)
    }

    fun backlog(init: () -> Int) {
        _serverBuilder.backlog(init)
    }

    fun threadPoolSize(init: () -> Int) {
        _serverBuilder.threadPoolSize(init)
    }

    fun build(): Server {
        _serverBuilder.default { request ->
            status { 200 to "OK" }
            body { JsonBody(request.toHar()) }
        }

        return _serverBuilder.build()
    }
}