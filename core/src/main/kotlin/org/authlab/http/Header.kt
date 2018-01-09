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