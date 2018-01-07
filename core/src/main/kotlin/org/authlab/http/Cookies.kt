package org.authlab.http

class Cookies(val cookies: List<Cookie> = listOf()) {
    companion object {
        fun fromRequestHeaders(headers: Headers): Cookies {
            return Cookies(headers.getHeader("Cookie")?.values
                    ?.flatMap { cookieString ->
                        cookieString.split(";")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .map { Cookie.fromString(it) }
                    } ?: listOf())
        }

        fun fromResponseHeaders(headers: Headers): Cookies {
            return Cookies(headers.getHeader("Set-Cookie")?.values
                    ?.filter { it.isNotEmpty() }
                    ?.map { Cookie.fromString(it) } ?: listOf())
        }
    }

    fun toHar(): List<*> {
        return cookies.map { it.toHar() }
    }
}
