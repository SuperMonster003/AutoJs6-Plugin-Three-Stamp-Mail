package io.github.supermonster003.autojs6.plugin.angus.mail.oauth

import android.content.Context
import android.util.Log
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.OAuthLink
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MailJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthProviderId
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthTokens
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.TokenClient
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStore
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStores
import io.github.supermonster003.autojs6.plugin.angus.mail.store.SavedAccount
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The secret a session may use for a saved account (roadmap P9): a password or a pasted token as
 * stored, and for a browser sign-in ([SecretKind.OAUTH2]) the access token alone, refreshed first
 * when it expires within [OAuthTokens.REFRESH_MARGIN_MS]. A refreshed record is written back
 * (tokens and the `oauth` expiry); a refused refresh marks the record `needsReauth` and fails with
 * `AUTH_FAILED`, so scripts, the settings page and the watches all see the same state. Refreshes
 * are serialized so two sessions of one account do not race the provider.
 */
class AccountSecrets(
    private val store: AccountStore,
    private val clients: OAuthClients,
    private val tokenClient: TokenClient,
    private val clock: () -> Long = System::currentTimeMillis,
    /** Where the outcome lines go (provider, outcome and seconds only); logcat in the application, nowhere in a JVM test. */
    private val log: (priority: Int, message: String) -> Unit = { _, _ -> },
) {
    private val refreshLock = Any()

    /** Like [AccountStore.withSecret], with an OAuth 2.0 record yielding a usable access token; the characters are wiped afterwards. */
    fun <T> withUsableSecret(alias: String, action: (SavedAccount, CharArray) -> T): T {
        val saved = store.get(alias) ?: throw MailException(MailErrorCode.ACCOUNT_NOT_FOUND, "no saved account named '$alias'", retryable = false)
        if (saved.secretKind != SecretKind.OAUTH2) return store.withSecret(alias, action)
        val tokens = usableTokens(saved)
        val chars = tokens.accessToken.toCharArray()
        try {
            return action(saved, chars)
        } finally {
            chars.fill('\u0000')
        }
    }

    /** The stored tokens of an OAuth 2.0 record, refreshed when stale. */
    fun usableTokens(saved: SavedAccount): OAuthTokens {
        require(saved.secretKind == SecretKind.OAUTH2) { "not an OAuth 2.0 record" }
        synchronized(refreshLock) {
            val stored = store.withSecret(saved.alias) { _, chars -> OAuthTokens.parse(String(chars)) }
            if (!stored.expiresWithin(clock())) return stored
            return refresh(saved, stored)
        }
    }

    /** Stores a fresh grant on an existing record (a re-authorization): the tokens and the `oauth` link. */
    fun storeGrant(alias: String, provider: OAuthProviderId, tokens: OAuthTokens) {
        val saved = store.get(alias) ?: throw MailException(MailErrorCode.ACCOUNT_NOT_FOUND, "no saved account named '$alias'", retryable = false)
        val link = OAuthLink(provider.id, authorizedAt = clock(), expiresAt = tokens.expiresAt, needsReauth = false)
        store.put(saved.alias, withLink(saved.accountJson, link), SecretKind.OAUTH2, tokens.toJson().toCharArray())
        val secondsLeft = (tokens.expiresAt - clock()) / 1000
        log(Log.INFO, "provider=${provider.id} sign-in stored, expires in $secondsLeft s")
    }

    private fun refresh(saved: SavedAccount, stored: OAuthTokens): OAuthTokens {
        val account = MailAccountOptions.parse(saved.accountJson, SecretKind.OAUTH2)
        val link = account.oauth ?: throw MailException.invalidArgument("the account has no 'oauth' link")
        val providerId = link.providerId
        val provider = clients.provider(providerId)
        val clientId = clients.clientId(providerId)
        val refreshed = try {
            tokenClient.refresh(provider, clientId, stored)
        } catch (e: MailException) {
            if (TokenClient.needsReauthorization(e) && !link.needsReauth) {
                runCatching { store.put(saved.alias, withLink(saved.accountJson, link.copy(needsReauth = true)), SecretKind.OAUTH2, stored.toJson().toCharArray()) }
            }
            log(Log.WARN, "provider=${providerId.id} refresh failed: ${e.code} (${e.details ?: "-"})")
            throw e
        }
        store.put(saved.alias, withLink(saved.accountJson, link.copy(expiresAt = refreshed.expiresAt, needsReauth = false)), SecretKind.OAUTH2, refreshed.toJson().toCharArray())
        val secondsLeft = (refreshed.expiresAt - clock()) / 1000
        log(Log.INFO, "provider=${providerId.id} refresh ok, expires in $secondsLeft s")
        return refreshed
    }

    companion object {
        private const val TAG = "MailOAuth"

        @Volatile
        private var instance: AccountSecrets? = null

        fun of(context: Context): AccountSecrets = instance ?: synchronized(this) {
            instance ?: AccountSecrets(AccountStores.of(context), OAuthClients.of(context), TokenClient(HttpsFormPoster())) { priority, message -> Log.println(priority, TAG, message) }.also { instance = it }
        }

        /** The account document with its `oauth` object replaced. */
        fun withLink(accountJson: String, link: OAuthLink): String {
            val root = MailJson.format.parseToJsonElement(accountJson) as? JsonObject ?: throw MailException.invalidArgument("account must be a JSON object")
            val updated = buildJsonObject {
                root.forEach { (key, value) -> if (key != MailAccountOptions.Fields.OAUTH) put(key, value) }
                put(MailAccountOptions.Fields.OAUTH, linkJson(link))
            }
            return updated.toString()
        }

        fun linkJson(link: OAuthLink): JsonObject = buildJsonObject {
            put(OAuthLink.FIELD_PROVIDER, JsonPrimitive(link.provider))
            put(OAuthLink.FIELD_AUTHORIZED_AT, link.authorizedAt)
            put(OAuthLink.FIELD_EXPIRES_AT, link.expiresAt)
            put(OAuthLink.FIELD_NEEDS_REAUTH, link.needsReauth)
        }
    }
}
