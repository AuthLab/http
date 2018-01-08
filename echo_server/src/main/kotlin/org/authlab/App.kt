package org.authlab

import org.authlab.http.echo.EchoServerBuilder

fun main(args: Array<String>) {
    EchoServerBuilder().build().start()
}
