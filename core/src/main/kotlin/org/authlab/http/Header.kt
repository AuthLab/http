package org.authlab.http

data class Header(val name: String, val values: List<String> = emptyList()) {
    companion object Factory {
        fun fromString(input: String): Header {
            val inputParts = input.split(":", limit = 2)

            if (inputParts.size != 2) {
                throw IllegalArgumentException("Request header input string must have three parts")
            }

            return Header(inputParts[0].trim(), listOf(inputParts[1].trim()))
        }
    }

    constructor(name: String, value: String) : this(name, listOf(value))

    fun withValue(value: String): Header {
        val newValues = values.toMutableList()
        newValues.add(value)
        return Header(name, newValues)
    }

    fun withValues(values: List<String>): Header {
        val newValues = this.values.toMutableList()
        newValues.addAll(values)
        return Header(name, newValues)
    }

    fun getFirst() = values.first()

    fun getFirstAsInt() = Integer.parseInt(values.first())

    fun toLines() = values.map { value -> "$name: $value" }

    fun toHar(): List<*> {
        return values.map { value ->
            mapOf("name" to name, "value" to value)
        }
    }
}