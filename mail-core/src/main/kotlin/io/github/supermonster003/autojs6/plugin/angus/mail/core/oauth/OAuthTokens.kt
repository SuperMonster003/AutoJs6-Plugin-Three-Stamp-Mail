package io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MailJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.Base64

/**
 * The secret of an account that signed in through the browser (roadmap P9): the access token the
 * mail protocols present through XOAUTH2, the refresh token that renews it and the instant the
 * access token stops working. Stored encrypted by the account store as one JSON document and
 * never handed to the host or a script; the session receives the access token alone.
 */
@Serializable
data class OAuthTokens(
    val accessToken: String,
    val refreshToken: String? = null,
    /** UTC milliseconds after which the access token is not to be used. */
    val expiresAt: Long,
    /** The scopes the provider reported as granted, space separated, when it did. */
    val scope: String? = null,
) {
    init {
        require(accessToken.isNotBlank()) { "accessToken must not be blank" }
        require(expiresAt >= 0) { "expiresAt must not be negative" }
    }

    /** True when the access token expires within [marginMs] of [now] (or already did). */
    fun expiresWithin(now: Long, marginMs: Long = REFRESH_MARGIN_MS): Boolean = expiresAt - now <= marginMs

    fun toJson(): String = MailJson.format.encodeToString(this)

    companion object {
        /** Refresh when less than this remains: long enough for a session to open and authenticate. */
        const val REFRESH_MARGIN_MS = 5 * 60_000L

        /** Assumed when the provider does not say how long its access token lives. */
        const val DEFAULT_LIFETIME_MS = 3_600_000L

        fun parse(json: String): OAuthTokens = try {
            MailJson.format.decodeFromString(serializer(), json)
        } catch (e: Exception) {
            throw MailException.invalidArgument("the stored OAuth 2.0 tokens are not readable", e.javaClass.simpleName)
        }
    }
}

/**
 * The identity claims of an OpenID id token, read without verifying its signature: the token
 * arrives over TLS directly from the provider's token endpoint in the same answer as the access
 * token, so it is trusted to the same extent, and only the address is taken from it (to prefill
 * the account form). Nothing is decided on it.
 */
object IdTokenClaims {

    fun emailOf(idToken: String?): String? {
        val payload = idToken?.split('.')?.getOrNull(1) ?: return null
        val json = try {
            String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            return null
        }
        val claims = try {
            MailJson.format.parseToJsonElement(json) as? JsonObject
        } catch (e: Exception) {
            null
        } ?: return null
        return listOf("email", "preferred_username", "upn")
            .firstNotNullOfOrNull { key -> (claims[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { '@' in it } }
    }
}
