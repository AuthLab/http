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

import org.authlab.http.FormParameters
import org.authlab.http.FormParametersBuilder
import org.authlab.http.ParametersBuilder
import org.authlab.http.Response
import org.authlab.http.bodies.Body
import org.authlab.http.bodies.FormBody
import org.authlab.http.bodies.JsonBody
import org.authlab.http.bodies.StringBody
import org.authlab.util.loggerFor

object BodyHelpers {
    val _logger = loggerFor<BodyHelpers>()
}

fun RequestBuilder.verifiedGet(path: String): Response {
    val response = get(path)

    val status = response.responseLine.statusCode

    if (status != 200) {
        BodyHelpers._logger.info("Response status was $status")
    }

    return response
}

fun RequestBuilder.verifiedPost(body: Body, path: String): Response {
    val response = post(body, path)

    val status = response.responseLine.statusCode

    if (status != 200) {
        BodyHelpers._logger.info("Response status was $status")
    }

    return response
}

inline fun <reified T> Response.asJson(): T {
    val contentType = headers.getHeader("Content-Type")?.getFirst()

    if (!contentType.equals("application/json")) {
        BodyHelpers._logger.warn("Response content-type was not 'application/json', but '$contentType'; " +
                "attempting to read json body anyways")
    }

    return (body as JsonBody).getTypedData()
}

inline fun <reified T> RequestBuilder.getJson(path: String = "/"): T {
    accept = "application/json"

    return verifiedGet(path).asJson()
}

fun RequestBuilder.postJson(data: Any, path: String = "/"): Response {
    return verifiedPost(JsonBody(data), path)
}

fun RequestBuilder.postForm(form: Map<String, String?>, path: String = "/"): Response {
    var formParameters = FormParameters()

    form.forEach{ (name, value) ->
        formParameters = formParameters.withParameter(name, value)
    }

    return verifiedPost(FormBody(formParameters), path)
}

fun RequestBuilder.postForm(path: String = "/", init: ParametersBuilder.() -> Unit): Response {
    val formParameters = FormParametersBuilder(init).build()

    return verifiedPost(FormBody(formParameters), path)
}

fun RequestBuilder.postText(text: String, path: String = "/"): Response {
    return verifiedPost(StringBody(text, "text/plain"), path)
}

fun RequestBuilder.postHtml(text: String, path: String = "/"): Response {
    return verifiedPost(StringBody(text, "text/html"), path)
}
