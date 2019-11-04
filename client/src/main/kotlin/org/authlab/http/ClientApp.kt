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

@file:JvmName("ClientApp")

package org.authlab.http

import com.google.gson.GsonBuilder
import org.authlab.crypto.TrustAllTrustManager
import org.authlab.http.bodies.TextBodyReader
import org.authlab.http.client.ClientBuilder
import java.io.File
import java.lang.Exception
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        return
    }

    val location = Location.fromString(args.last())
    var keystorePath: String? = null
    var keystorePassword: String? = null
    var verbose = false

    var keyManager: KeyManager? = null
    var trustManager: TrustManager? = null

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
            } else if (option == "-k" || option == "--keystore") {
                action = { value ->
                    keystorePath = value
                }
            } else if (option == "-P" || option == "--keystore-password") {
                action = { value ->
                    keystorePassword = value
                }
            } else if (option == "-t" || option == "--trust-all") {
                trustManager = TrustAllTrustManager()
            } else if (option == "-v" || option == "--verbose") {
                verbose = true
            } else {
                throw IllegalArgumentException("Unknown option '$option'")
            }
        }
    } catch (e: Throwable) {
        println(e.message)
        printUsage()
        return
    }


    try {
        if (!keystorePath.isNullOrEmpty()) {
            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(File(keystorePath!!).inputStream(), keystorePassword?.toCharArray())

            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keyStore, keystorePassword?.toCharArray() ?: charArrayOf())
            keyManager = keyManagerFactory.keyManagers.firstOrNull()
        }

        if (verbose) {
            println("Sending GET request to host ${location.host}${location.path?.let { " with path $it" } ?: ""}")
        }

        val response = ClientBuilder(location) {
            sslContext = SSLContext.getInstance("TLS").also { context ->
                context.init(keyManager?.run { arrayOf(this) }, trustManager?.run { arrayOf(this) }, SecureRandom())
            }
        }.build().get(TextBodyReader(), location.path)

        if (verbose) {
            println(GsonBuilder().setPrettyPrinting().create().toJson(response.toHar()))
        } else {
            val body = response.getBody(TextBodyReader())
            println(body.text)
        }
    } catch (e: Exception) {
        println("ERROR: " + e.message)
        exitProcess(1)
    }
}

internal fun printUsage() {
    println("""
        |proxy [options...] <url>
        |Options:
        |  -h, --help              Prints this help message.
        |  -k, --keystore          The keystore to use for TLS
        |  -P, --keystore-password The keystore password
        |  -t, --trust-all         Trust all server certificates
        |  -v, --verbose           Verbose printout
    """.trimMargin())
}