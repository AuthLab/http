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

package org.authlab.http.client

import org.authlab.http.bodies.Body
import org.authlab.http.bodies.BodyReader
import org.authlab.http.bodies.BodyWriter
import org.authlab.http.bodies.DelayedBodyReader
import org.authlab.http.bodies.EmptyBodyWriter

interface RequestBuilder {
    var path: String
    var contentType: String?
    var accept: String?

    fun query(name: String, value: String? = null): RequestBuilder

    fun query(param: Pair<String, String>): RequestBuilder

    fun query(init: () -> Pair<String, String>) {
        query(init())
    }

    fun header(name: String, value: String): RequestBuilder

    fun header(header: Pair<String, String>): RequestBuilder

    fun header(init: () -> Pair<String, String>) {
        header(init())
    }

    fun get(path: String? = null)
            = get(DelayedBodyReader(), path)

    fun <B : Body> get(bodyReader: BodyReader<B>, path: String? = null)
            = execute("GET", EmptyBodyWriter(), bodyReader, path)

    fun post(bodyWriter: BodyWriter, path: String? = null)
            = post(bodyWriter, DelayedBodyReader(), path)

    fun <B : Body> post(bodyWriter: BodyWriter, bodyReader: BodyReader<B>, path: String? = null)
            = execute("POST", bodyWriter, bodyReader, path)

    fun put(bodyWriter: BodyWriter, path: String? = null)
            = put(bodyWriter, DelayedBodyReader(), path)

    fun <B : Body> put(bodyWriter: BodyWriter, bodyReader: BodyReader<B>, path: String? = null)
            = execute("PUT", bodyWriter, bodyReader, path)

    fun delete(path: String? = null)
            = delete(DelayedBodyReader(), path)

    fun <B : Body> delete(bodyReader: BodyReader<B>, path: String? = null)
            = execute("DELETE", EmptyBodyWriter(), bodyReader, path)

    fun patch(bodyWriter: BodyWriter, path: String? = null)
            = patch(bodyWriter, DelayedBodyReader(), path)

    fun <B : Body> patch(bodyWriter: BodyWriter, bodyReader: BodyReader<B>, path: String? = null)
            = execute("PATCH", bodyWriter, bodyReader, path)

    fun <B : Body> execute(method: String, bodyWriter: BodyWriter, bodyReader: BodyReader<B>, path: String? = null): ClientResponse<B>
}
