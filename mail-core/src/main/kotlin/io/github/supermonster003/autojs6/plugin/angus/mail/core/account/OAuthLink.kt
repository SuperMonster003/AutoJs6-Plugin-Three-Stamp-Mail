package io.github.supermonster003.autojs6.plugin.angus.mail.core.account

import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthProviderId
import kotlinx.serialization.Serializable

/**
 * The non-secret side of a browser sign-in (roadmap P9), kept in the account document under
 * `oauth`: which provider issued the tokens, when the user signed in, when the current access
 * token expires and whether a refresh failed so that only a new sign-in helps. The tokens
 * themselves are the record's encrypted secret ([SecretKind.OAUTH2]).
 */
@Serializable
data class OAuthLink(
    val provider: String,
    /** UTC milliseconds of the sign-in. */
    val authorizedAt: Long = 0,
    /** UTC milliseconds after which the stored access token is stale; the session refreshes it first. */
    val expiresAt: Long = 0,
    /** Set when a refresh was refused; cleared by the next successful sign-in. */
    val needsReauth: Boolean = false,
) {
    val providerId: OAuthProviderId get() = requireNotNull(OAuthProviderId.fromId(provider)) { "unknown OAuth provider '$provider'" }

    init {
        require(OAuthProviderId.fromId(provider) != null) { "unknown OAuth provider '$provider'" }
        require(authorizedAt >= 0 && expiresAt >= 0) { "timestamps must not be negative" }
    }

    companion object {
        const val FIELD_PROVIDER = "provider"
        const val FIELD_AUTHORIZED_AT = "authorizedAt"
        const val FIELD_EXPIRES_AT = "expiresAt"
        const val FIELD_NEEDS_REAUTH = "needsReauth"
        val FIELDS: Set<String> = setOf(FIELD_PROVIDER, FIELD_AUTHORIZED_AT, FIELD_EXPIRES_AT, FIELD_NEEDS_REAUTH)
    }
}
