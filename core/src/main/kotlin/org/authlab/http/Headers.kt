package org.authlab.http

class Headers(val headers: Map<String, Header> = emptyMap()) {
    fun withHeader(name: String, value: String): Headers {
        return withHeader(Header(name, value))
    }

    fun withHeader(header: Header): Headers {
        val mutableHeaders = headers.toMutableMap()

        mutableHeaders.merge(header.name.toUpperCase(), header) { originalHeader, incomingHeader ->
            originalHeader.withValues(incomingHeader.values)
        }

        return Headers(mutableHeaders)
    }

    fun withoutHeaders(name: String): Headers {
        val mutableHeaders = headers.toMutableMap()

        mutableHeaders.remove(name.toUpperCase())

        return Headers(mutableHeaders)
    }

    fun getHeader(name: String): Header?
            = headers[name.toUpperCase()]

    fun hasHeader(name: String): Boolean
        = getHeader(name) != null

    fun toLines() = headers.values.flatMap { header -> header.toLines() }

    fun toHar(): List<*> {
        return headers.flatMap { (_, header) ->
            header.toHar()
        }
    }
}