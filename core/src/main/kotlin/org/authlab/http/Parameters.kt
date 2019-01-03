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

abstract class Parameters<T : Parameters<T>>(protected val parameters: Map<String, Parameter>) {
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
            parameters[name]
        }
    }

    fun toHar(): List<*> {
        return parameters.flatMap { (_, parameter) ->
            parameter.toHar()
        }
    }

    override fun equals(other: Any?)
            = other is Parameters<*> && parameters == other.parameters

    override fun hashCode()
            = parameters.hashCode()

    fun toString(delimiter: String, encode: (String) -> String = { it }): String {
        return parameters.values.joinToString(delimiter) { it.toString(delimiter, encode) }
    }
}

class Parameter(val name: String, val values: List<String> = emptyList()) {
    constructor(name: String, value: String?) : this(name, if (value != null) listOf(value) else emptyList())

    companion object {
        fun fromString(input: String?, delimiter: String, decode: (String) -> String = { it }): Map<String, Parameter> {
            val parameters = mutableMapOf<String, Parameter>()

            input?.split(delimiter)?.forEach { parameterString ->
                val keyValuePair = parameterString.split("=", limit = 2)
                        .map { decode(it) }

                val (key, value) = when {
                    keyValuePair.size == 1 -> keyValuePair[0] to null
                    keyValuePair.size == 2 -> keyValuePair[0] to keyValuePair[1]
                    else -> null to null
                }

                if (key != null) {
                    parameters[key] = Parameter(key, value)
                }
            }

            return parameters
        }
    }

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

    fun first(): String
            = values.first()

    fun firstOrNull(): String?
            = values.firstOrNull()

    fun firstOrDefault(default: String): String?
            = firstOrNull() ?: default

    fun last(): String
            = values.last()

    fun lastOrNull(): String?
            = values.lastOrNull()

    fun toHar(): List<*> {
        return if (values.isEmpty()) {
            listOf(mapOf("name" to name, "value" to ""))
        } else {
            values.map { value ->
                mapOf("name" to name, "value" to value)
            }
        }
    }

    override fun equals(other: Any?) =
        other is Parameter && name == other.name && values == other.values

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + values.hashCode()
        return result
    }

    fun toString(delimiter: String, encode: (String) -> String = { it }): String {
        val encodedName = encode(name)

        return if (values.isEmpty()) {
            encodedName
        } else {
            values.joinToString(delimiter) { "$encodedName=${encode(it)}" }
        }
    }
}

open class ParametersBuilder() {
    protected val _parameters = mutableListOf<Pair<String, String?>>()

    constructor(init: ParametersBuilder.() -> Unit) : this() {
        init()
    }

    fun parameter(init: () -> Pair<String, String?>) {
        parameter(init())
    }

    fun parameter(name: String, value: String?) {
        parameter(name to value)
    }

    fun parameter(pair: Pair<String, String?>) {
        _parameters.add(pair)
    }
}

