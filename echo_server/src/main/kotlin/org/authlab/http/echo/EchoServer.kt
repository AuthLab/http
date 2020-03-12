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

package org.authlab.http.echo

import org.authlab.http.bodies.JsonBodyWriter
import org.authlab.http.bodies.TextBodyReader
import org.authlab.http.server.ServerBuilder
import org.authlab.http.server.ServerMarker
import org.authlab.http.server.default
import org.authlab.util.loggerFor

private val logger = loggerFor("Echo-Server")

@ServerMarker
class EchoServerBuilder private constructor() : ServerBuilder() {
    init {
        logger.info("initializing default settings")

        default(TextBodyReader()) { request ->
            val har = request.toHar()
            logger.debug("Request: {}", har)

            status { 200 to "OK" }
            body { JsonBodyWriter(har, pretty = true) }
        }
    }

    constructor(init: EchoServerBuilder.() -> Unit) : this() {
        logger.info("initializing additional settings")
        init()
    }
}