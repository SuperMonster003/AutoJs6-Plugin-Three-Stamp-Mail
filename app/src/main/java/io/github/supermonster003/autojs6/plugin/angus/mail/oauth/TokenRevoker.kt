package io.github.supermonster003.autojs6.plugin.angus.mail.oauth

import android.content.Context
import android.util.Log
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthTokens
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.TokenClient
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStores
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * "Revoke sign-in" of the accounts page (roadmap P9): the record keeps its document but its
 * tokens are replaced by an empty, `needsReauth` marker at once (so nothing can use them any
 * more even if the provider is unreachable), and the provider is asked to revoke the refresh
 * token on a background thread when it offers a revocation endpoint (Google does; Microsoft
 * personal accounts revoke through the account's privacy page, which the dialog says). The
 * provider-side request is returned as a [Future] of whether the provider accepted it (null when
 * nothing was posted: no token, no client id), so that a device test can wait for its outcome
 * before the instrumentation ends the process; the accounts page does not wait.
 */
object TokenRevoker {

    private const val TAG = "MailOAuth"
    private val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "mail-oauth-revoke").apply { isDaemon = true } }

    fun revoke(context: Context, alias: String): Future<Boolean>? {
        val store = AccountStores.of(context.applicationContext)
        val saved = store.get(alias) ?: throw MailException(MailErrorCode.ACCOUNT_NOT_FOUND, "no saved account named '$alias'", retryable = false)
        if (saved.secretKind != SecretKind.OAUTH2) throw MailException.invalidArgument("'$alias' has no browser sign-in to revoke")
        val link = MailAccountOptions.parse(saved.accountJson, saved.secretKind).oauth ?: throw MailException.invalidArgument("'$alias' has no 'oauth' link")
        val tokens = store.withSecret(alias) { _, chars -> runCatching { OAuthTokens.parse(String(chars)) }.getOrNull() }
        // the local half first: the stored tokens become a revoked marker (an access token the providers never issued)
        // and the account's `oauth` link says so too (`needsReauth`, no expiry left), which is what `mail.accounts.list()` reports
        store.put(saved.alias, AccountSecrets.withLink(saved.accountJson, link.copy(expiresAt = 0, needsReauth = true)), SecretKind.OAUTH2, REVOKED_TOKENS.toJson().toCharArray())
        Log.i(TAG, "provider=${link.provider} sign-in revoked locally")
        val clients = OAuthClients.of(context)
        val refreshToken = tokens?.refreshToken ?: tokens?.accessToken ?: return null
        val clientId = clients.clientIdOrNull(link.providerId) ?: return null
        val provider = clients.provider(link.providerId)
        return executor.submit<Boolean> {
            val done = TokenClient(HttpsFormPoster()).revoke(provider, clientId, refreshToken)
            Log.i(TAG, "provider=${link.provider} provider-side revocation ${if (done) "accepted" else "not done"}")
            done
        }
    }

    /** What a revoked record holds: no refresh token and an access token that expired at the epoch. */
    val REVOKED_TOKENS = OAuthTokens(accessToken = "revoked", refreshToken = null, expiresAt = 0)
}
