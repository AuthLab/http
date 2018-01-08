package org.authlab.http

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class FormParameters(parameters: Map<String, Parameter> = mapOf()) : Parameters<FormParameters>(parameters) {
    companion object {
        fun fromString(input: String?): FormParameters {
            if (input == null) {
                return FormParameters()
            }

            var parameters = FormParameters()

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
            = FormParameters(parameters)

    override fun toString() = parameters.map { (name, parameter) ->
        if (parameter.values.isEmpty()) {
            "${parameter.name}="
        } else {
            parameter.values.joinToString("&") {
                URLEncoder.encode(name, StandardCharsets.UTF_8.name()) + "=" +
                        URLEncoder.encode(it, StandardCharsets.UTF_8.name())
            }
        }
    }.joinToString("&")
}

class FormParametersBuilder() : ParametersBuilder() {
    constructor(init: ParametersBuilder.() -> Unit) : this() {
        init()
    }

    fun build(): FormParameters {
        var formParameters = FormParameters()

        _parameters.forEach {
            formParameters = formParameters.withParameter(it.first, it.second)
        }

        return formParameters
    }
}