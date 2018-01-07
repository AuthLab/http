package org.authlab.http

open class RequestLine(val method: String, val location: Location, val version: String = "HTTP/1.1") {
    companion object Factory {
        fun fromString(input: String): RequestLine {
            val inputParts = input.split(" ", limit = 3)

            if (inputParts.size != 3) {
                throw IllegalArgumentException("Request line input string must have three parts; got '$inputParts'")
            }

            val method = inputParts[0]

            val location = Location.fromString(inputParts[1])

            val version = inputParts[2]

            return DeserializedRequestLine(input, method, location, version)
        }
    }

    fun withOnlyUriPath(): RequestLine = CopiedRequestLine(method, location.withoutHost(), version)

    open fun toLines() = listOf(toString())

    override fun toString() = "$method $location $version"
}

private class CopiedRequestLine(method: String, location: Location, version: String):
        RequestLine(method, location, version)

private class DeserializedRequestLine(val originalInput: String, method: String, location: Location, version: String):
        RequestLine(method, location, version) {
    override fun toLines() = listOf(originalInput)
}
