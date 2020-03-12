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

@file:JvmName("ServerApp")

package org.authlab.http

import org.authlab.crypto.SimpleKeyManager
import org.authlab.crypto.TrustAllTrustManager
import org.authlab.http.server.ServerBuilder
import org.authlab.http.server.files
import java.io.File
import java.net.InetAddress
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.X509KeyManager

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        return
    }

    val fileRoot = args.last()
    var inetAddress = InetAddress.getByName("0.0.0.0")
    var port = 8084
    var backlog = 50
    var threadPool = 100
    var keystorePath: String? = null
    var keystorePassword: String? = null
    var requireClientCertificate = false

    var action: ((String) -> Unit)? = null

    try {
        for (option in args.dropLast(1)) {
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
            } else if (option == "-k" || option == "--keystore") {
                action = { value ->
                    keystorePath = value
                }
            } else if (option == "-P" || option == "--keystore-password") {
                action = { value ->
                    keystorePassword = value
                }
            } else if (option == "-r" || option == "--require-client-certificate") {
                requireClientCertificate = true
            } else {
                throw IllegalArgumentException("Unknown option '$option'")
            }
        }
    } catch (e: Throwable) {
        println(e.message)
        printUsage()
        return
    }

    var sslContext: SSLContext? = null

    if (!keystorePath.isNullOrEmpty()) {
        sslContext = SSLContext.getInstance("TLS")

        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(File(keystorePath!!).inputStream(), keystorePassword?.toCharArray())

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, keystorePassword?.toCharArray() ?: charArrayOf())

        sslContext.init(keyManagerFactory.keyManagers.asSequence()
                .mapNotNull { it as? X509KeyManager }
                .map { SimpleKeyManager(it) }
                .toList().toTypedArray(),
                arrayOf(TrustAllTrustManager()), SecureRandom())
    }

    ServerBuilder {
        this.threadPoolSize = threadPool

        listen {
            this.host = inetAddress.hostAddress
            this.port = port
            this.backlog = backlog

            if (sslContext != null) {
                this.secure = true
                this.sslContext = sslContext
                this.requireClientCertificate = requireClientCertificate
            }
        }

        files("/*", fileRoot)
    }.build().start()
}

internal fun printUsage() {
    println("""
        |server [options...] <folder|file>
        |Options:
        |  -b, --backlog                    Maximum queue length for incoming connections.
        |  -h, --help                       Prints this help message.
        |  -i, --interface                  The interface (or address) to listen on.
        |  -t, --thread-pool                The size of the thread pool for managing active connections.
        |  -p, --port                       The port to listen on.
        |  -k, --keystore                   The keystore to use for TLS
        |  -P, --keystore-password          The keystore password
        |  -r, --require-client-certificate Require connecting clients to provide a certificate
    """.trimMargin())
}