package org.authlab.http

class Host(val hostname: String, private val _port: Int? = null, val scheme: String = "http") {
    companion object {
        fun fromString(input: String): Host {
            var remainder = input

            val scheme = readScheme(remainder) {
                remainder = it
            } ?: "http"

            val hostAndPort = remainder.split(":")

            return when {
                hostAndPort.isEmpty() -> throw IllegalArgumentException("No hostname component in Host string")
                hostAndPort.size == 1 -> Host(hostAndPort[0], null, scheme)
                hostAndPort.size == 2 -> Host(hostAndPort[0], hostAndPort[1].toInt(), scheme)
                else -> throw IllegalArgumentException("Invalid Host format")
            }
        }

        fun readScheme(input: String, callback: (remainder: String) -> Unit): String? {
            val index = input.indexOf("://")

            return if (index != -1) {
                val scheme = input.substring(0, index)
                callback(input.removePrefix("$scheme://"))
                scheme.trim()
            } else {
                callback(input)
                null
            }
        }
    }

    val port: Int
        get() = when {
            _port != null -> _port
            scheme.equals("http", ignoreCase = true) -> 80
            else -> 443
        }

    val hostnameAndPort: String
        get() = "$hostname:$port"

    fun withHostname(hostname: String) = Host(hostname, _port, scheme)

    fun withPort(port: Int) = Host(hostname, port, scheme)

    fun withScheme(scheme: String) = Host(hostname, _port, scheme)

    override fun toString(): String {
        val sb = StringBuilder()

        sb.append(scheme)
                .append("://")
                .append(hostname)

        if (_port != null) {
            sb.append(":")
                    .append(_port)
        }

        return sb.toString()
    }
}
