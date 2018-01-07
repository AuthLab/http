package org.authlab.http

abstract class Parameters<T : Parameters<T>>(val parameters: Map<String, Parameter>) {
    protected abstract fun newInstance(parameters: Map<String, Parameter>): T

    fun withParameter(name: String, value: String?): T {
        return withParameter(if (value != null) {
            Parameter(name, value)
        } else {
            Parameter(name)
        })
    }

    fun withParameter(parameter: Parameter): T {
        val mutableParameters = parameters.toMutableMap()

        mutableParameters.merge(parameter.name, parameter) { originalParameter, incomingParameter ->
            originalParameter.withValues(incomingParameter.values)
        }

        return newInstance(mutableParameters)
    }

    fun withoutParameters(name: String): T {
        val mutableHeaders = parameters.toMutableMap()

        mutableHeaders.remove(name.toUpperCase())

        return newInstance(mutableHeaders)
    }

    fun getParameter(name: String, ignoreCase: Boolean = false): Parameter? {
        return if (ignoreCase) {
            parameters.filter { name.equals(it.key, ignoreCase = true) }
                    .flatMap { parameter -> parameter.value.values }
                    .takeIf { values -> !values.isEmpty() }
                    ?.let { values -> Parameter(name, values) }
        } else {
            parameters[name.toUpperCase()]
        }
    }

    fun toHar(): List<*> {
        return parameters.flatMap { (_, parameter) ->
            parameter.toHar()
        }
    }
}

class Parameter(val name: String, val values: List<String> = emptyList()) {
    constructor(name: String, value: String) : this(name, listOf(value))

    fun withValue(value: String): Parameter {
        val newValues = values.toMutableList()
        newValues.add(value)
        return Parameter(name, newValues)
    }

    fun withValues(values: List<String>): Parameter {
        val newValues = this.values.toMutableList()
        newValues.addAll(values)
        return Parameter(name, newValues)
    }

    fun first() = values.first()

    fun firstOrNull() = values.firstOrNull()

    fun toHar(): List<*> {
        return if (values.isEmpty()) {
            listOf(mapOf("name" to name, "value" to ""))
        } else {
            values.map { value ->
                mapOf("name" to name, "value" to value)
            }
        }
    }
}
