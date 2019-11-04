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

open class ResponseLine(val statusCode: Int, val reason: String, val version: String = "HTTP/1.1") {
    companion object Factory {
        fun fromString(input: String): ResponseLine {
            val inputParts = input.split(" ", limit = 3)

            if (inputParts.size != 3) {
                throw IllegalArgumentException("Response line input string must have three parts")
            }

            return DeserializedResponseLine(input, Integer.parseInt(inputParts[1]), inputParts[2], inputParts[0])
        }
    }

    open fun toLines() = listOf("$version $statusCode $reason")

    override fun toString() = "$version $statusCode $reason"
}

private class DeserializedResponseLine(val originalInput: String, statusCode: Int, reason: String, version: String):
        ResponseLine(statusCode, reason, version) {
    override fun toLines() = listOf(originalInput)
}