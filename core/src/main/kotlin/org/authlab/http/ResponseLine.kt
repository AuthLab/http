package org.authlab.http

open class ResponseLine(val statusCode: Int, val reason: String, val version: String = "HTTP/1.1") {
    companion object Factory {
        fun fromString(input: String): ResponseLine {
            val inputParts = input.split(" ", limit = 3)

            if (inputParts.size != 3) {
                throw IllegalArgumentException("Request line input string must have three parts")
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