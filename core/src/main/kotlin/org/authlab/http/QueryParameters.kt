package org.authlab.http

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class QueryParameters(parameters: Map<String, Parameter> = mapOf()) : Parameters<QueryParameters>(parameters) {
    companion object {
        fun fromString(input: String?): QueryParameters {
            if (input == null) {
                return QueryParameters()
            }

            var parameters = QueryParameters()

            input.split("&").forEach {
                val keyValuePair = it.split("=", limit = 2)
                        .map { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }

                val (key, value) = when {
                    keyValuePair.size == 1 -> keyValuePair[0] to null
                    keyValuePair.size == 2 -> keyValuePair[0] to keyValuePair[1]
                    else -> null to null
                }

                if (key != null) {
                    parameters = if (value != null) {
                        parameters.withParameter(key, value)
                    } else {
                        parameters.withParameter(Parameter(key))
                    }
                }
            }

            return parameters
        }
    }

    override fun newInstance(parameters: Map<String, Parameter>)
            = QueryParameters(parameters)

    override fun toString() = parameters.map { (name, parameter) ->
        if (parameter.values.isEmpty()) {
            parameter.name
        } else {
            parameter.values.joinToString("&") {
                URLEncoder.encode(name, StandardCharsets.UTF_8.name()) + "=" +
                        URLEncoder.encode(it, StandardCharsets.UTF_8.name())
            }
        }
    }.joinToString("&")
            .let { if (it.isEmpty()) { it } else { "?" + it } }
}
