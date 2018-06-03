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
import org.authlab.http.Headers
import org.authlab.http.ParametersBuilder
import org.authlab.http.bodies.Body
import org.authlab.http.bodies.BodyReader
import org.authlab.http.bodies.DelayedBody
import org.authlab.http.bodies.FormBodyWriter
import org.authlab.http.bodies.JsonBodyReader
import org.authlab.http.bodies.JsonBodyWriter
import org.authlab.http.bodies.TextBodyReader
import org.authlab.util.loggerFor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object BodyHelpers {
    val _logger = loggerFor<BodyHelpers>()
}

fun <B : Body> convertBody(body: Body, bodyReader: BodyReader<B>): B {
    val outputStream = ByteArrayOutputStream()
    body.writer.write(outputStream)
    val headers = Headers().withHeader("Content-Length", "${outputStream.size()}")
    return bodyReader.read(ByteArrayInputStream(outputStream.toByteArray()), headers)
            .getBody()
}

fun ClientResponse<*>.asText(): String {
    return getBody(TextBodyReader()).text
}

inline fun <reified T> ClientResponse<*>.asJson(): T {
    val contentType = headers.getHeader("Content-Type")?.getFirst()

    if (!contentType.equals("application/json")) {
        BodyHelpers._logger.warn("Response content-type was not 'application/json', but '$contentType'; " +
                "attempting to read json body anyways")
    }

    return getBody(JsonBodyReader()).getTypedValue()
}

fun Client.getText(path: String = "/", init: RequestBuilder.() -> Unit = {}): String {
    return get(TextBodyReader(), path, init).getBody().text
}

inline fun <reified T> Client.getJson(path: String = "/", noinline init: RequestBuilder.() -> Unit = {}): T {
    return get(JsonBodyReader(), path, init).getBody().getTypedValue()
}

fun Client.postJson(data: Any, path: String = "/", init: RequestBuilder.() -> Unit = {}): ClientResponse<DelayedBody> {
    return post(JsonBodyWriter(data), path, init)
}

fun Client.postForm(path: String = "/", requestInit: RequestBuilder.() -> Unit = {},
                    parametersInit: ParametersBuilder.() -> Unit): ClientResponse<DelayedBody> {
    val formParameters = FormParametersBuilder(parametersInit).build()
    return post(FormBodyWriter(formParameters), path, requestInit)
}

fun Client.postForm(path: String = "/", parameters: Map<String, String>,
                    requestInit: RequestBuilder.() -> Unit = {}): ClientResponse<DelayedBody> {
    val formParametersBuilder = FormParametersBuilder()
    parameters.forEach { key, value -> formParametersBuilder.parameter { key to value } }
    return post(FormBodyWriter(formParametersBuilder.build()), path, requestInit)
}
