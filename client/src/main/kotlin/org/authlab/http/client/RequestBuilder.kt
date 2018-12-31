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

import org.authlab.http.Header
import org.authlab.http.Headers
import org.authlab.http.authentication.BasicAuthenticationResponse
import org.authlab.http.authentication.Credential
import org.authlab.http.bodies.BodyWriter

@ClientMarker
interface RequestBuilder {
    var method: String
    var path: String
    var bodyWriter: BodyWriter
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

    fun header(header: Header): RequestBuilder

    fun headers(headers: Headers): RequestBuilder

    fun headers(init: () -> Headers) {
        headers(init())
    }

    fun basicAuthorization(credential: Credential) {
        header(BasicAuthenticationResponse(credential).toRequestHeader())
    }

    fun build(): ClientRequest
}
