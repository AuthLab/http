package org.authlab.http

class Cookie(val name: String, val value: String,
             val path: String?, val domain: String?,
             val expires: String?, val httpOnly: Boolean?,
             val secure: Boolean?) {
    companion object {
        fun fromString(input: String): Cookie {
            val splitInput = input.split(";")

            val nameValuePair = splitInput.first().split("=", limit=2)
            val name = nameValuePair.first().trim()
            val value = if (nameValuePair.size > 1) nameValuePair[1] else ""

            val directives = mutableMapOf<String, String>()

            for (directiveString in splitInput.subList(1, splitInput.size)) {
                val directivePair = directiveString.split("=", limit = 2)
                val directiveName = directivePair.first().trim()
                if (directiveName.isNotEmpty()) {
                    val directiveValue = if (directivePair.size > 1) directivePair[1] else ""
                    directives.put(directiveName.toUpperCase(), directiveValue)
                }
            }

            return Cookie(name, value,
                    directives["PATH"],
                    directives["DOMAIN"],
                    directives["EXPIRES"],
                    directives["HTTPONLY"]?.let { it.isEmpty() || it.toBoolean() },
                    directives["SECURE"]?.let { it.isEmpty() || it.toBoolean() })
        }
    }

    fun toHar(): Map<String, *> {
        val har: MutableMap<String, Any> = mutableMapOf("name" to name, "value" to value)

        path?.also { har.put("path", it) }
        domain?.also { har.put("domain", it) }
        expires?.also { har.put("expires", it.toString()) }
        httpOnly?.also { har.put("httpOnly", it) }
        secure?.also { har.put("secure", it) }

        return har
    }
}
