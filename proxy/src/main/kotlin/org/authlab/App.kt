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

@file:JvmName("App")

package org.authlab

import org.authlab.crypto.createSslContext
import org.authlab.http.proxy.ProxyService
import java.net.InetAddress

fun main(args: Array<String>) {
    var inetAddress = InetAddress.getByName("0.0.0.0")
    var port = 8080
    var backlog = 50
    var threadPool = 100
    var inspectTunnels = false

    var action: ((String) -> Unit)? = null

    try {
        for (option in args) {
            if (action != null) {
                action(option)
                action = null
                continue
            }

            if (option == "-h" || option == "--help") {
                printUsage()
                return
            } else if (option == "-b" || option == "--backlog") {
                action = { value ->
                    try {
                        backlog = Integer.parseInt(value)
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Invalid backlog; must be a valid integer")
                    }
                }
            } else if (option == "-i" || option == "--interface") {
                action = { value ->
                    try {
                        inetAddress = InetAddress.getByName(value)
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Invalid interface; must be an IP address or a host name")
                    }
                }
            } else if (option == "-I" || option == "--inspect-tunnels") {
                inspectTunnels = true
            } else if (option == "-p" || option == "--port") {
                action = { value ->
                    try {
                        port = Integer.parseInt(value)
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Invalid port; must be a valid integer")
                    }
                }
            } else if (option == "-t" || option == "--thread-pool") {
                action = { value ->
                    try {
                        threadPool = Integer.parseInt(value)
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Invalid thread-pool; must be a valid integer")
                    }
                }
            } else {
                throw IllegalArgumentException("Unknown option '$option'")
            }
        }
    } catch (e: IllegalArgumentException) {
        println(e.message)
        printUsage()
        return
    }

    val sslContext = createSslContext(storeGeneratedCertificates = true)

    Thread(ProxyService(inetAddress, port, backlog, sslContext, false, threadPool, inspectTunnels)).start()
}

internal fun printUsage() {
    println("""
        |proxy [options...]
        |Options:
        |  -b, --backlog          Maximum queue length for incoming connections.
        |  -h, --help             Prints this help message.
        |  -i, --interface        The interface (or address) to listen on.
        |  -I, --inspect-tunnels  Inspect tunnels created through the CONNECT method.
        |  -t, --thread-pool      The size of the thread pool for managing active connections.
        |  -p, --port             The port to listen on.
    """.trimMargin())
}
