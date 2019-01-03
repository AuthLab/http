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

package org.authlab.http

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class QueryParameters(parameters: Map<String, Parameter> = mapOf()) : Parameters<QueryParameters>(parameters) {
    companion object {
        fun fromString(input: String?): QueryParameters {
            return QueryParameters(Parameter.fromString(input, "&") {
                URLDecoder.decode(it, StandardCharsets.UTF_8.name())
            })
        }
    }

    override fun newInstance(parameters: Map<String, Parameter>)
            = QueryParameters(parameters)

    override fun toString()
            = toString("&") { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) }
            .let { if (it.isEmpty()) { it } else { "?$it" } }
}
