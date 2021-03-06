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

package org.authlab.http.hello

import org.authlab.http.Cookie
import org.authlab.http.SameSite
import org.authlab.http.authentication.Subject
import org.authlab.http.bodies.EmptyBodyReader
import org.authlab.http.bodies.TextBodyWriter
import org.authlab.http.server.ServerBuilder
import org.authlab.http.server.ServerMarker
import org.authlab.http.server.abort
import org.authlab.http.server.default
import org.authlab.http.server.filter
import org.authlab.http.server.finalize
import org.authlab.http.server.get
import org.authlab.http.server.initialize
import org.authlab.http.server.transform
import org.authlab.util.loggerFor
import org.slf4j.MDC
import java.time.Duration
import java.time.Instant
import java.util.UUID

private val logger = loggerFor("Hello-Server")

@ServerMarker
class HelloServerBuilder private constructor() : ServerBuilder() {
    init {
        logger.info("initializing default settings")

        context { "session_manager" to SessionManager() }

        // Add transaction- and session ID to context
        initialize { request, context ->
            context.data["transaction_id"] = UUID.randomUUID()

            val sessionManager = context.get<SessionManager>("session_manager")!!
            val session = request.cookies["session"]
                    ?.let { cookie ->
                        sessionManager.getSession(cookie.value)?.takeIf { it.expires.isAfter(Instant.now()) }
                    } ?: sessionManager.createSession(Duration.ofMinutes(1L))

            context.data["session"] = session
        }

        // Update MDC
        initialize { _, context ->
            MDC.clear()
            context.data["transaction_id"]?.also { MDC.put("transaction", it.toString()) }
            context.get<Session>("session")?.also { MDC.put("session", it.id.toString()) }
        }

        // Clear MDC
        finalize  { _, _, _ ->
            MDC.clear()
        }

        filter("/reject") { _, _ ->
            abort {
                status(400 to "Bad Request")
                body(TextBodyWriter("rejected"))
            }
        }

        // Set session cookie
        transform("/*") { request, _, context ->
            context.data["transaction_id"]?.also {
                header("Transaction" to "$it")
            }
            context.get<Session>("session")?.also {
                if (request.cookies["session"]?.value != it.id.toString()) {
                    cookie(Cookie("session", it.id.toString(), path = request.path,
                            httpOnly = true, sameSite = SameSite.STRICT))
                }
            }
        }

        default(EmptyBodyReader()) { request ->
            logger.info("Saying hello")

            val sb = StringBuilder()
            sb.append("hello")

            request.context.get<Subject>("subject")?.also {
                sb.append(" ").append(it.displayName)
            }

            request.context.data["session"]?.also {
                sb.append('\n').append("session").append('=').append(it)
            }

            request.context.data["transaction_id"]?.also {
                sb.append('\n').append("transaction").append('=').append(it)
            }

            body(TextBodyWriter(sb.toString()))
        }
    }

    constructor(init: HelloServerBuilder.() -> Unit) : this() {
        logger.info("initializing additional settings")
        init()
    }
}