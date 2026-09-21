package io.github.supermonster003.autojs6.plugin.angus.mail

import android.app.Activity
import android.content.Context
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.AuthMethod
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.OAuthLink
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthProviderId
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthProviders
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthTokens
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.ProviderPresets
import io.github.supermonster003.autojs6.plugin.angus.mail.oauth.AccountSecrets
import io.github.supermonster003.autojs6.plugin.angus.mail.oauth.OAuthClients
import io.github.supermonster003.autojs6.plugin.angus.mail.oauth.TokenRevoker
import io.github.supermonster003.autojs6.plugin.angus.mail.settings.AccountForm
import io.github.supermonster003.autojs6.plugin.angus.mail.settings.AccountFormPolicy
import io.github.supermonster003.autojs6.plugin.angus.mail.settings.AccountsActivity
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStore
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A real browser sign-in's record on a device without typing a password on the device (roadmap
 * P9, `.python/run_oauth_device.py`): the tokens the maintainer obtained on the PC with the same
 * client registration (`.python/outlook_oauth_login.py`) arrive as instrumentation arguments
 * (`mailAlias`, `mailAddress`, `mailProvider` = outlook / office365 / gmail, `oauthProvider`,
 * `oauthAccessTokenB64`, `oauthRefreshTokenB64`, `oauthExpiresAt` in Unix seconds) and are stored
 * exactly as [io.github.supermonster003.autojs6.plugin.angus.mail.oauth.OAuthSignInActivity]
 * stores a grant; the record then goes through what every alias session does: the access token is
 * renewed at the provider when it is stale, a revoked record fails with `AUTH_FAILED` and the
 * accounts page says "sign in again", and a new grant on the same record restores it. Runs
 * through `am instrument` so the record survives for the host's `mail.connect(alias)`;
 * [removesTheAccount] cleans up. Logs carry the alias, the provider and durations only.
 */
@RunWith(AndroidJUnit4::class)
class RealAccountOAuthDeviceTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val strings: Context
        get() = AppConfiguration.wrap(context)

    private val arguments
        get() = InstrumentationRegistry.getArguments()

    private val store: AccountStore
        get() = AccountStores.of(context)

    @Test
    fun seedsASignedInAccount() {
        val alias = requireAlias()
        val seed = requireSeed()
        assumeTrue("this build carries no client id for ${seed.provider.id}", OAuthClients.of(context).isConfigured(seed.provider))
        runCatching { store.remove(alias) }
        val started = System.currentTimeMillis()
        val link = OAuthLink(seed.provider.id, authorizedAt = started, expiresAt = seed.tokens.expiresAt, needsReauth = false)
        val saved = store.put(alias, AccountFormPolicy.toAccountJson(seed.form, link), SecretKind.OAUTH2, seed.tokens.toJson().toCharArray())
        assertEquals(SecretKind.OAUTH2, saved.secretKind)
        assertFalse("no token enters the account document", saved.accountJson.contains(seed.tokens.refreshToken!!))

        // what a session does first: a stale access token is renewed at the provider
        val stale = seed.tokens.expiresWithin(System.currentTimeMillis())
        val usable = AccountSecrets.of(context).usableTokens(saved)
        assertFalse("the session gets a token with time left", usable.expiresWithin(System.currentTimeMillis()))
        if (stale) assertNotEquals("a stale token is replaced", seed.tokens.accessToken, usable.accessToken)
        val after = MailAccountOptions.parse(store.get(alias)!!.accountJson, SecretKind.OAUTH2).oauth!!
        assertEquals(usable.expiresAt, after.expiresAt)
        assertFalse(after.needsReauth)
        assertSummary(alias, needsReauth = false, provider = seed.provider)
        Log.i(TAG, "seeded alias=$alias provider=${seed.provider.id} staleAtSeed=$stale renewed=${usable.accessToken != seed.tokens.accessToken} expiresIn=${(usable.expiresAt - System.currentTimeMillis()) / 1000} s in ${System.currentTimeMillis() - started} ms")
    }

    @Test
    fun revokesTheSignIn() {
        val alias = requireAlias()
        val saved = requireNotNull(store.get(alias)) { "the account '$alias' must be seeded first" }
        val provider = MailAccountOptions.parse(saved.accountJson, saved.secretKind).oauth!!.providerId
        val started = System.currentTimeMillis()
        TokenRevoker.revoke(context, alias)
        try {
            AccountSecrets.of(context).withUsableSecret(alias) { _, _ -> fail("a revoked record must not yield a token") }
        } catch (e: MailException) {
            assertEquals(MailErrorCode.AUTH_FAILED, e.code)
        }
        val revokedLink = MailAccountOptions.parse(store.get(alias)!!.accountJson, SecretKind.OAUTH2).oauth!!
        assertTrue(revokedLink.needsReauth)
        assertEquals(0L, revokedLink.expiresAt)
        assertSummary(alias, needsReauth = true, provider = provider)
        Log.i(TAG, "revoked alias=$alias provider=${provider.id} in ${System.currentTimeMillis() - started} ms; sessions now fail with ${MailErrorCode.AUTH_FAILED}")
    }

    @Test
    fun signsInAgain() {
        val alias = requireAlias()
        val seed = requireSeed()
        requireNotNull(store.get(alias)) { "the account '$alias' must be seeded first" }
        val started = System.currentTimeMillis()
        // what the sign-in screen does at the end of a re-authorization
        AccountSecrets.of(context).storeGrant(alias, seed.provider, seed.tokens)
        val usable = AccountSecrets.of(context).usableTokens(store.get(alias)!!)
        assertFalse(usable.expiresWithin(System.currentTimeMillis()))
        assertFalse(MailAccountOptions.parse(store.get(alias)!!.accountJson, SecretKind.OAUTH2).oauth!!.needsReauth)
        assertSummary(alias, needsReauth = false, provider = seed.provider)
        Log.i(TAG, "re-authorized alias=$alias provider=${seed.provider.id} expiresIn=${(usable.expiresAt - System.currentTimeMillis()) / 1000} s in ${System.currentTimeMillis() - started} ms")
    }

    @Test
    fun removesTheAccount() {
        val alias = requireAlias()
        val existed = store.get(alias) != null
        runCatching { store.remove(alias) }
        assertNull(store.get(alias))
        Log.i(TAG, "removed alias=$alias existed=$existed")
    }

    private class Seed(val form: AccountForm, val provider: OAuthProviderId, val tokens: OAuthTokens)

    private fun requireAlias(): String {
        val alias = arguments.getString("mailAlias")
        assumeTrue("mailAlias not given", !alias.isNullOrBlank())
        return alias!!
    }

    private fun requireSeed(): Seed {
        val address = arguments.getString("mailAddress")
        val presetId = arguments.getString("mailProvider")
        val providerId = OAuthProviderId.fromId(arguments.getString("oauthProvider"))
        val refresh = arguments.getString("oauthRefreshTokenB64")?.let(::decode)
        assumeTrue("mailAddress / mailProvider / oauthProvider / oauthRefreshTokenB64 not given", !address.isNullOrBlank() && !presetId.isNullOrBlank() && providerId != null && !refresh.isNullOrBlank())
        val preset = requireNotNull(ProviderPresets.resolve(presetId)) { "unknown provider preset $presetId" }
        assertEquals("the preset's sign-in provider", providerId, OAuthProviders.forPreset(preset.id))
        val access = arguments.getString("oauthAccessTokenB64")?.let(::decode)?.takeIf { it.isNotBlank() } ?: "stale"
        val expiresAt = arguments.getString("oauthExpiresAt")?.toLongOrNull()?.let { it * 1000 } ?: 0L
        val form = AccountFormPolicy.applyPreset(AccountForm(alias = requireAlias(), address = address!!), preset).copy(auth = AuthMethod.XOAUTH2, oauthProvider = providerId)
        assertEquals(SecretKind.OAUTH2, form.secretKind)
        return Seed(form, providerId!!, OAuthTokens(access, refresh, expiresAt))
    }

    private fun decode(base64: String): String = String(Base64.decode(base64, Base64.DEFAULT), Charsets.UTF_8)

    /** The accounts page shows the sign-in kind and, after a refusal, "sign in again"; never a token. */
    private fun assertSummary(alias: String, needsReauth: Boolean, provider: OAuthProviderId) {
        val kind = strings.getString(if (provider == OAuthProviderId.GOOGLE) R.string.accounts_summary_auth_oauth_google else R.string.accounts_summary_auth_oauth_microsoft)
        val marker = strings.getString(R.string.accounts_summary_needs_reauth)
        ActivityScenario.launch(AccountsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val texts = activity.root().descendants().filterIsInstance<TextView>().map { it.text.toString() }.toList()
                assertTrue(texts.toString(), alias in texts)
                val detail = texts.firstOrNull { it.contains(kind) }
                assertTrue("no summary line names the sign-in kind: $texts", detail != null)
                assertEquals("needs reauth marker in '$detail'", needsReauth, detail!!.contains(marker))
            }
        }
    }

    private fun Activity.root(): View = findViewById(android.R.id.content)

    private fun View.descendants(): Sequence<View> = sequence {
        yield(this@descendants)
        if (this@descendants is ViewGroup) {
            for (index in 0 until childCount) yieldAll(getChildAt(index).descendants())
        }
    }

    private companion object {
        const val TAG = "RealAccountOAuth"
    }
}
