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

import org.authlab.http.bodies.TextBodyWriter
import org.authlab.http.server.ServerBuilder
import org.authlab.http.server.ServerResponseBuilder
import java.util.UUID

fun main(args: Array<String>) {
    ServerBuilder {
        listen {
            port=8083
        }
        filter {
            onRequest { request, context ->
                context.data["transaction_id"] = UUID.randomUUID()
                context.data["session_id"] = request.cookies["session"]
                        ?.value ?: UUID.randomUUID()
                null
            }
        }
        filter {
            entryPoint = "/reject"
            onRequest { _, _ ->
                ServerResponseBuilder {
                    status { 400 to "Bad Request" }
                    body { TextBodyWriter("rejected") }
                }.build()
            }
        }
        transform {
            onResponse { response, context ->
                ServerResponseBuilder(response) {
                    context.data["transaction_id"]?.also {
                        header { "Transaction" to "$it" }
                    }
                    context.data["session_id"]?.also {
                        header { "Set-Cookie" to "session=$it" }
                    }
                }.build()
            }
        }
        default { request ->
            status { 200 to "OK" }
            body {
                val sb = StringBuilder()
                sb.append("hello")
                request.context.data.forEach { key, value ->
                    sb.append('\n').append(key).append('=').append(value)
                }
                TextBodyWriter(sb.toString())
            }
        }
        handle("/foo") { _ ->
            status { 200 to "OK" }
            body { TextBodyWriter("hello foo") }
        }
    }.build().start()
}