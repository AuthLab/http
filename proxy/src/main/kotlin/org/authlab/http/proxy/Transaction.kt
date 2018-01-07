package org.authlab.http.proxy

import org.authlab.http.Request
import org.authlab.http.Response
import java.time.Instant

data class Transaction(val request: Request, val response: Response,
                       val startInstant: Instant, val stopInstant: Instant) {
    fun toHar(): Map<String, *> {
        return mapOf(
                "request" to request.toHar(),
                "response" to response.toHar(),
                "startedDateTime" to startInstant.toString())
    }
}
