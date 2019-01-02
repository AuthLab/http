package org.authlab.http.oauth.client

import org.authlab.http.authentication.Subject
import java.time.Instant

class IntrospectionResponse(val attributes: Map<String, Any>) {
    val active: Boolean
        get() = attributes["active"] as? Boolean ?: false

    val scope: String?
        get() = attributes["scope"] as? String

    val scopes: Set<String>
        get() = scope?.split("\\s".toRegex())?.toSet() ?: emptySet()

    val clientId: String?
        get() = attributes["client_id"] as? String

    val username: String?
        get() = attributes["username"] as? String

    val tokenType: String?
        get() = attributes["token_type"] as? String

    val expiration: Instant?
        get() = (attributes["exp"] as? Number)?.toLong()?.let { Instant.ofEpochSecond(it) }

    val issuedAt: Instant?
        get() = (attributes["iat"] as? Number)?.toLong()?.let { Instant.ofEpochSecond(it) }

    val notBefore: Instant?
        get() = (attributes["nbf"] as? Number)?.toLong()?.let { Instant.ofEpochSecond(it) }

    val subject: Subject?
        get() = (attributes["sub"] as? String)?.let { Subject(it, username) }

    val audience: String?
        get() = attributes["aud"] as? String

    val issuer: String?
        get() = attributes["iss"] as? String

    val jwtId: String?
        get() = attributes["jti"] as? String
}