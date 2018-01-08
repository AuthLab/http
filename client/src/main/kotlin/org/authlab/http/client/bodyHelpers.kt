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
