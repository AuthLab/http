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

import org.authlab.http.FormParametersBuilder
import org.authlab.http.ParametersBuilder
import org.authlab.http.bodies.FormBodyWriter
import org.authlab.http.bodies.JsonBodyReader
import org.authlab.http.bodies.JsonBodyWriter
import org.authlab.http.bodies.SerializedJsonBody
import org.authlab.util.loggerFor

object BodyHelpers {
    val _logger = loggerFor<BodyHelpers>()
}

inline fun <reified T> ClientResponse<*>.asJson(): T {
    val contentType = headers.getHeader("Content-Type")?.getFirst()

    if (!contentType.equals("application/json")) {
        BodyHelpers._logger.warn("Response content-type was not 'application/json', but '$contentType'; " +
                "attempting to read json body anyways")
    }

    return (getBody(JsonBodyReader()) as SerializedJsonBody).getTypedValue()
}

inline fun <reified T> RequestBuilder.getJson(path: String = "/"): T {
    return (get(JsonBodyReader(), path).getBody() as SerializedJsonBody).getTypedValue()
}

fun RequestBuilder.postJson(data: Any, path: String = "/"): ClientResponse<*> {
    return post(JsonBodyWriter(data), path)
}

fun RequestBuilder.postForm(path: String = "/", init: ParametersBuilder.() -> Unit): ClientResponse<*> {
    val formParameters = FormParametersBuilder(init).build()
    return post(FormBodyWriter(formParameters), path)
}
