package org.authlab.http

data class Location(val host: Host? = null, val path: String? = null, val query: QueryParameters = QueryParameters(),
                    val fragment: String? = null, val asterisk: Boolean = false) {
    companion object {
        fun fromString(input: String): Location {
            var remainder = input.trim()

            if (remainder == "*") {
                return Location(asterisk = true)
            }

            var scheme: String? = null
            val schemeAndRemainder = remainder.split("://", limit = 2)
            if (schemeAndRemainder.size > 1) {
                scheme = schemeAndRemainder[0]
                remainder = schemeAndRemainder[1]
            }

            val pathStart = remainder.indexOf("/")

            var host: Host? = null
            var fragment: String? = null
            var query: String? = null
            var path: String? = null

            if (pathStart != -1) {
                val authority = remainder.substring(0, pathStart).takeIf { it.isNotBlank() }
                if (authority != null) {
                    host = Host.fromString(authority)
                }

                remainder = remainder.substring(pathStart)

                val remainderAndFragment = remainder.split("#", limit = 2)
                if (remainderAndFragment.size > 1) {
                    remainder = remainderAndFragment[0]
                    fragment = remainderAndFragment[1]
                }

                val remainderAndQuery = remainder.split("?", limit = 2)
                if (remainderAndQuery.size > 1) {
                    remainder = remainderAndQuery[0]
                    query = remainderAndQuery[1]
                }

                path = remainder
            } else {
                host = Host.fromString(remainder)
            }

            if (scheme != null) {
                host = host?.withScheme(scheme)
            }

            return Location(host, path, QueryParameters.fromString(query), fragment)
        }
    }

    val safePath
        get() = path ?: "/"

    fun withHost(host: Host) = Location(host, path, query, fragment, asterisk)

    fun withoutHost() = Location(null, path, query, fragment, asterisk)

    fun withPath(path: String) = Location(host, path, query, fragment, asterisk)

    fun withoutPath() = Location(host, null, query, fragment, asterisk)

    fun withQuery(query: QueryParameters) = Location(host, path, query, fragment, asterisk)

    fun withFragment(fragment: String) = Location(host, path, query, fragment, asterisk)

    fun withoutFragment() = Location(host, path, query, null, asterisk)

    override fun toString(): String {
        if (asterisk) {
            return "*"
        }

        val sb = StringBuilder()

        if (host != null) {
            sb.append(host.toString())
        }

        if (path != null) {
            sb.append(path)
        } else if (host == null) {
            sb.append(safePath)
        }

        sb.append(query.toString())

        if (fragment != null) {
            sb.append("#")
                    .append(fragment)
        }

        return sb.toString()
    }
}
