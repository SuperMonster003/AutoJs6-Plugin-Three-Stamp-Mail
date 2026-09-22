package io.github.supermonster003.autojs6.plugin.angus.mail

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.CallerGuard
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.MailPluginBinder
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
import org.autojs.plugin.mail.api.IMailCallCallback
import org.autojs.plugin.mail.api.IMailSession
import org.autojs.plugin.mail.api.IMailSessionCallback
import org.autojs.plugin.mail.api.MailContract
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * A real browser sign-in's record on a device without typing a password on the device (roadmap
 * P9, `.python/run_oauth_device.py`): the tokens the maintainer obtained on the PC with the same
 * client registration (`.python/outlook_oauth_login.py`) arrive as instrumentation arguments
 * (`mailAlias`, `mailAddress`, `mailProvider` = outlook / office365 / gmail, `oauthProvider`,
 * `oauthAccessTokenB64`, `oauthRefreshTokenB64`, `oauthExpiresAt` in Unix seconds) and are stored
 * exactly as [io.github.supermonster003.autojs6.plugin.angus.mail.oauth.OAuthSignInActivity]
 * stores a grant; the record then goes through what every alias session does: the access token is
 * renewed at the provider when it is stale, a revoked record fails with `AUTH_FAILED` and the
 * accounts page says "sign in again", and a new grant on the same record restores it; a record the
 * maintainer signed in with on the device (no seed) goes through the same steps under its alias.
 * [opensASessionOverTheSavedAlias] is what `mail.connect(alias)` does on the host, inside the
 * plugin process: the binder over the plugin's store, `session.test` with XOAUTH2 at every
 * endpoint and `messages.list` of the inbox (the live half of the evidence on a phone whose host
 * state must stay, `.python/run_oauth_device.py --skip-host`). Runs through `am instrument` so the
 * record survives for the host's `mail.connect(alias)`; [removesTheAccount] cleans up. Logs carry
 * the alias, the provider, counts and durations only.
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

    /**
     * The refresh path over the record the maintainer signed in with (the seed covers it for a seeded
     * record): the stored access token is made stale (the refresh token stays) and the next use renews
     * it at the provider, as every alias session does once the hour is over.
     */
    @Test
    fun renewsAStaleAccessTokenAtTheProvider() {
        val alias = requireAlias()
        val saved = requireNotNull(store.get(alias)) { "the account '$alias' must be seeded or signed in first" }
        val provider = MailAccountOptions.parse(saved.accountJson, saved.secretKind).oauth!!.providerId
        val tokens = store.withSecret(alias) { _, chars -> OAuthTokens.parse(String(chars)) }
        assertNotNull("the record carries a refresh token", tokens.refreshToken)
        val stale = store.put(alias, saved.accountJson, SecretKind.OAUTH2, tokens.copy(expiresAt = 0).toJson().toCharArray())
        val started = System.currentTimeMillis()
        val usable = AccountSecrets.of(context).usableTokens(stale)
        assertFalse("the stale token is renewed at the provider", usable.expiresWithin(System.currentTimeMillis()))
        val after = MailAccountOptions.parse(store.get(alias)!!.accountJson, SecretKind.OAUTH2).oauth!!
        assertEquals("the link carries the new expiry", usable.expiresAt, after.expiresAt)
        assertFalse(after.needsReauth)
        Log.i(TAG, "renewed alias=$alias provider=${provider.id} replaced=${usable.accessToken != tokens.accessToken} expiresIn=${(usable.expiresAt - System.currentTimeMillis()) / 1000} s in ${System.currentTimeMillis() - started} ms")
    }

    @Test
    fun opensASessionOverTheSavedAlias() {
        val alias = requireAlias()
        val saved = requireNotNull(store.get(alias)) { "the account '$alias' must be seeded or signed in first" }
        val provider = MailAccountOptions.parse(saved.accountJson, saved.secretKind).oauth!!.providerId
        val started = System.currentTimeMillis()
        val statuses = LinkedBlockingQueue<JSONObject>()
        val statusCallback = object : IMailSessionCallback.Stub() {
            override fun onStatus(status: Bundle?) {
                statuses.add(JSONObject(status?.getString(MailContract.KEY_STATUS_JSON).orEmpty()))
            }
        }
        // the plugin's own store (AccountSecrets.of(context, store) is then the process-wide helper: one renewal lock)
        val plugin = MailPluginBinder(context, CallerGuard.trusting(), store)
        val session = requireNotNull(plugin.openSession(aliasBundle(alias), statusCallback)) { "the saved alias must open: ${statuses.poll(5, TimeUnit.SECONDS)}" }
        try {
            val test = call(session, "t1", MailContract.OP_SESSION_TEST)
            assertTrue(test.toString().take(400), test.getBoolean(MailContract.FIELD_OK))
            val probes = test.getJSONObject(MailContract.FIELD_RESULT)
            if (probes.has("ok")) assertTrue(probes.toString().take(400), probes.getBoolean("ok"))
            val endpoints = listOf("imap", "pop3", "smtp").filter { probes.has(it) }
            assertTrue("at least one endpoint probed: $probes", endpoints.isNotEmpty())
            endpoints.forEach { assertTrue("$it probe failed: ${probes.getJSONObject(it).toString().take(300)}", probes.getJSONObject(it).getBoolean("ok")) }
            val testMs = System.currentTimeMillis() - started
            val listStarted = System.currentTimeMillis()
            val listed = call(session, "l1", MailContract.OP_MESSAGES_LIST, """{"limit":1}""")
            assertTrue(listed.toString().take(400), listed.getBoolean(MailContract.FIELD_OK))
            val count = messageCount(listed.get(MailContract.FIELD_RESULT))
            val listMs = System.currentTimeMillis() - listStarted
            Log.i(TAG, "session over alias=$alias provider=${provider.id}: ${endpoints.joinToString("/")} ok in $testMs ms, listed $count message(s) in $listMs ms")
        } finally {
            session.close()
            val closed = statuses.poll(10, TimeUnit.SECONDS)
            assertEquals("the session reports closed: $closed", MailContract.STATE_CLOSED, closed?.optString(MailContract.FIELD_STATE))
        }
    }

    @Test
    fun revokesTheSignIn() {
        val alias = requireAlias()
        val saved = requireNotNull(store.get(alias)) { "the account '$alias' must be seeded first" }
        val provider = MailAccountOptions.parse(saved.accountJson, saved.secretKind).oauth!!.providerId
        val started = System.currentTimeMillis()
        val providerSide = TokenRevoker.revoke(context, alias)
        try {
            AccountSecrets.of(context).withUsableSecret(alias) { _, _ -> fail("a revoked record must not yield a token") }
        } catch (e: MailException) {
            assertEquals(MailErrorCode.AUTH_FAILED, e.code)
        }
        val revokedLink = MailAccountOptions.parse(store.get(alias)!!.accountJson, SecretKind.OAUTH2).oauth!!
        assertTrue(revokedLink.needsReauth)
        assertEquals(0L, revokedLink.expiresAt)
        assertSummary(alias, needsReauth = true, provider = provider)
        val localMs = System.currentTimeMillis() - started
        // the provider-side half runs on the revoker's background thread and the instrumentation ends this process with the
        // test: wait for its outcome (a provider with a revocation endpoint must accept the real token; Microsoft has none)
        val endpoint = OAuthClients.of(context).provider(provider).revocationEndpoint
        val providerSideOutcome = when {
            providerSide == null -> "no provider-side revocation (no token or no client)"
            endpoint == null -> {
                providerSide.get(30, TimeUnit.SECONDS)
                "no revocation endpoint at the provider (the local half is the whole of it)"
            }
            else -> {
                assertTrue("the provider must accept the real token's revocation", providerSide.get(30, TimeUnit.SECONDS))
                "provider-side revocation accepted in ${System.currentTimeMillis() - started} ms"
            }
        }
        Log.i(TAG, "revoked alias=$alias provider=${provider.id} in $localMs ms; sessions now fail with ${MailErrorCode.AUTH_FAILED}; $providerSideOutcome")
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

    /** The alias form of the host's account bundle (contract B.1): the alias alone, no secret. */
    private fun aliasBundle(alias: String): Bundle = Bundle().apply {
        putInt(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
        putLong(MailContract.KEY_HOST_VERSION_CODE, AngusMailPlugin.REQUIRED_HOST_VERSION)
        putString(MailContract.KEY_ACCOUNT_ALIAS, alias)
    }

    /** One request through the session binder, answered within a minute (a real provider over the phone's network). */
    private fun call(session: IMailSession, id: String, op: String, args: String = "{}"): JSONObject {
        val results = LinkedBlockingQueue<JSONObject>()
        val request = Bundle().apply {
            putInt(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
            putString(MailContract.KEY_REQUEST_JSON, """{"id":"$id","op":"$op","args":$args}""")
        }
        val callback = object : IMailCallCallback.Stub() {
            override fun onProgress(progress: Bundle?) = Unit
            override fun onResult(response: Bundle?) {
                results.add(JSONObject(response?.getString(MailContract.KEY_RESPONSE_JSON).orEmpty()))
            }
        }
        assertEquals(id, session.call(request, null, callback))
        return requireNotNull(results.poll(60, TimeUnit.SECONDS)) { "no answer to $op within 60 s" }
    }

    /** How many messages a `messages.list` result carries (an array, or an object with a `messages` array). */
    private fun messageCount(result: Any): Int = when (result) {
        is JSONArray -> result.length()
        is JSONObject -> result.optJSONArray("messages")?.length() ?: result.optInt("total", -1)
        else -> -1
    }

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
