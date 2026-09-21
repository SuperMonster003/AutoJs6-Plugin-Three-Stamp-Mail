package io.github.supermonster003.autojs6.plugin.angus.mail.oauth

import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthProviderId
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthTokens
import java.security.SecureRandom
import java.util.Base64

/** The outcome of a browser sign-in on its way from [OAuthSignInActivity] to the account editor. */
class PendingGrant(val provider: OAuthProviderId, val tokens: OAuthTokens, val email: String?, val issuedAt: Long)

/**
 * Hands a fresh grant from the sign-in screen to the account editor inside this process without
 * putting tokens into an Intent (Intents are visible to `dumpsys` and the recents). The editor
 * takes the grant once by its id; anything older than [TTL_MS] is dropped.
 */
object PendingGrants {

    private const val TTL_MS = 10 * 60_000L
    private val grants = HashMap<String, PendingGrant>()
    private val random = SecureRandom()

    @Synchronized
    fun put(grant: PendingGrant, now: Long = System.currentTimeMillis()): String {
        expire(now)
        val id = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(12).also(random::nextBytes))
        grants[id] = grant
        return id
    }

    @Synchronized
    fun take(id: String?, now: Long = System.currentTimeMillis()): PendingGrant? {
        expire(now)
        return id?.let(grants::remove)
    }

    @Synchronized
    fun clear() {
        grants.clear()
    }

    private fun expire(now: Long) {
        grants.entries.removeAll { now - it.value.issuedAt > TTL_MS }
    }
}
