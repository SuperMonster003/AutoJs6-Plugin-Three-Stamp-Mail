package io.github.supermonster003.autojs6.plugin.angus.mail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.OAuthLink
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.FormAnswer
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.FormPoster
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthProviderId
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthTokens
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.TokenClient
import io.github.supermonster003.autojs6.plugin.angus.mail.oauth.AccountSecrets
import io.github.supermonster003.autojs6.plugin.angus.mail.oauth.OAuthClients
import io.github.supermonster003.autojs6.plugin.angus.mail.oauth.OAuthSignInActivity
import io.github.supermonster003.autojs6.plugin.angus.mail.oauth.TokenRevoker
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStores
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AndroidKeystoreAccountCipher
import io.github.supermonster003.autojs6.plugin.angus.mail.store.EncryptedAccountStore
import io.github.supermonster003.autojs6.plugin.angus.mail.store.FileAccountRecordStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.AssumptionViolatedException
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * The browser sign-in on a device (roadmap P9): the sign-in screen opens the provider's page in
 * the browser, the exported redirect activity hands a redirect back to it, the screen refuses a
 * redirect whose `state` is not the one it issued, exchanges the code of a matching one at the
 * provider's token endpoint over HTTPS (a made-up code, so the provider refuses it and the screen
 * says so), and spends the request either way; an OAuth 2.0 record round-trips through the
 * Keystore-encrypted store, is renewed through the refresh token and marked `needsReauth` when
 * the provider refuses, and a revoked record refuses every session until a new sign-in (a Google
 * record also asks Google's revocation endpoint). The sign-in and Google cases need the client
 * ids of this build (`oauth-clients.properties`) and skip without them; nothing here types a
 * password anywhere, and no real token is involved.
 */
@RunWith(AndroidJUnit4::class)
class OAuthDeviceTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val context: Context
        get() = instrumentation.targetContext

    /** Resources in the language the plugin's screens render (may differ from the system locale). */
    private val strings: Context
        get() = AppConfiguration.wrap(context)

    private val clients: OAuthClients
        get() = OAuthClients.of(context)

    @Test
    fun opensTheProviderPageInTheBrowser() = opensTheProviderPage(OAuthProviderId.MICROSOFT)

    @Test
    fun opensTheGooglePageInTheBrowser() = opensTheProviderPage(OAuthProviderId.GOOGLE)

    @Test
    fun refusesAForeignStateAndExchangesTheMatchingOneAtTheProvider() = refusesAForeignStateAndExchangesTheMatchingOne(OAuthProviderId.MICROSOFT)

    @Test
    fun refusesAForeignStateAndExchangesTheMatchingOneAtGoogle() = refusesAForeignStateAndExchangesTheMatchingOne(OAuthProviderId.GOOGLE)

    private fun opensTheProviderPage(provider: OAuthProviderId) {
        assumeConfigured(provider)
        val activity = launchSignIn(provider)
        try {
            waitUntil("the browser is opened", 15_000) { activity.isBrowserOpened }
            val state = onMain { activity.pendingState }
            assertNotNull("a request is waiting", state)
            assertEquals(strings.getString(R.string.oauth_waiting, providerName(provider)), onMain { activity.statusText.toString() })
            Log.i(TAG, "browser opened for provider=${provider.id}, state length ${state!!.length}")
            // the driver looks at the top activity and takes a screenshot meanwhile
            Thread.sleep(8_000)
        } finally {
            onMain { activity.finish() }
        }
    }

    private fun refusesAForeignStateAndExchangesTheMatchingOne(provider: OAuthProviderId) {
        assumeConfigured(provider)
        val redirect = clients.redirectUri(provider)
        val rejectedPrefix = strings.getString(R.string.oauth_rejected, "").trim()
        val failedPrefix = strings.getString(R.string.oauth_failed, "").trim()
        val activity = launchSignIn(provider)
        try {
            waitUntil("the request is prepared", 15_000) { activity.pendingState != null }
            val state = onMain { activity.pendingState }!!

            // 1. a redirect that carries another state: refused, the request stays open
            deliver("$redirect?code=made-up&state=${state}x")
            waitUntil("the foreign redirect is refused", 20_000, activity) { activity.statusText.toString().startsWith(rejectedPrefix) }
            assertEquals("the request is still waiting", state, onMain { activity.pendingState })
            Log.i(TAG, "foreign state refused: ${onMain { activity.statusText }}")

            // 2. the matching state with a code the provider never issued: the exchange reaches the token endpoint and fails there
            deliver("$redirect?code=made-up&state=$state")
            waitUntil("the exchange fails at the provider", 60_000, activity) { activity.statusText.toString().startsWith(failedPrefix) }
            assertNull("one code, one exchange: the request is spent", onMain { activity.pendingState })
            val failure = onMain { activity.statusText.toString() }
            assertFalse("the failure text names no code or verifier", failure.contains("made-up"))
            Log.i(TAG, "matching state exchanged, provider answered (${provider.id}): $failure")

            // 3. a late redirect after the exchange: nothing is waiting any more
            deliver("$redirect?code=made-up&state=$state")
            waitUntil("the late redirect is refused", 20_000, activity) { activity.statusText.toString().startsWith(rejectedPrefix) }
            Log.i(TAG, "late redirect refused: ${onMain { activity.statusText }}")
        } finally {
            onMain { activity.finish() }
        }
    }

    @Test
    fun anOAuthRecordRoundTripsRefreshesAndMarksARefusal() {
        val root = File(context.noBackupFilesDir, "oauth-device-test-${UUID.randomUUID()}").also { it.mkdirs() }
        val cipher = AndroidKeystoreAccountCipher(TEST_KEY_ALIAS).also { it.deleteKey() }
        try {
            val store = EncryptedAccountStore(FileAccountRecordStorage(root), cipher)
            var answer: (List<Pair<String, String>>) -> FormAnswer = { fail("no refresh expected"); throw IllegalStateException() }
            val posts = ArrayList<List<Pair<String, String>>>()
            val poster = FormPoster { url, form ->
                assertEquals(OAuthClients.of(context).provider(OAuthProviderId.MICROSOFT).tokenEndpoint, url)
                posts += form
                answer(form)
            }
            var now = 1_800_000_000_000L
            val secrets = AccountSecrets(store, OAuthClients("", "device-test-client", "consumers", context.packageName), TokenClient(poster) { now }, { now })
            val link = OAuthLink("microsoft", authorizedAt = now, expiresAt = now + 3_600_000L)
            val document = AccountSecrets.withLink(ACCOUNT_JSON, link)

            // a fresh record: the session gets the access token as stored, and the files never hold it
            store.put("oauth-rt", document, SecretKind.OAUTH2, OAuthTokens("access-one", "refresh-one", now + 3_600_000L).toJson().toCharArray())
            assertEquals("access-one", secrets.withUsableSecret("oauth-rt") { saved, chars ->
                assertEquals(SecretKind.OAUTH2, saved.secretKind)
                String(chars)
            })
            val files = requireNotNull(File(root, FileAccountRecordStorage.DIRECTORY_NAME).listFiles { file -> file.name.endsWith(".bin") })
            assertEquals(1, files.size)
            val bytes = files.single().readBytes()
            assertFalse(bytes.containsSequence("access-one".toByteArray(StandardCharsets.UTF_8)))
            assertFalse(bytes.containsSequence("refresh-one".toByteArray(StandardCharsets.UTF_8)))
            assertTrue(posts.isEmpty())

            // the access token about to expire: renewed through the refresh token, the record rewritten
            now += 3_600_000L - 60_000L
            answer = { form ->
                assertEquals("refresh_token", form.first { it.first == "grant_type" }.second)
                assertEquals("refresh-one", form.first { it.first == "refresh_token" }.second)
                FormAnswer(200, """{"access_token":"access-two","refresh_token":"refresh-two","expires_in":3600,"token_type":"Bearer"}""")
            }
            assertEquals("access-two", secrets.withUsableSecret("oauth-rt") { _, chars -> String(chars) })
            assertEquals(1, posts.size)
            val renewed = MailAccountOptions.parse(store.get("oauth-rt")!!.accountJson, SecretKind.OAUTH2).oauth!!
            assertEquals(now + 3_600_000L, renewed.expiresAt)
            assertFalse(renewed.needsReauth)
            assertEquals("access-two", secrets.withUsableSecret("oauth-rt") { _, chars -> String(chars) })
            assertEquals("a valid token is not refreshed again", 1, posts.size)

            // the provider refuses the refresh token: AUTH_FAILED and the record marked for a new sign-in
            now += 3_600_000L
            answer = { FormAnswer(400, """{"error":"invalid_grant","error_description":"AADSTS70000: the refresh token has expired"}""") }
            try {
                secrets.withUsableSecret("oauth-rt") { _, _ -> fail("the refused refresh must not yield a token") }
            } catch (e: MailException) {
                assertEquals(MailErrorCode.AUTH_FAILED, e.code)
            }
            assertTrue(MailAccountOptions.parse(store.get("oauth-rt")!!.accountJson, SecretKind.OAUTH2).oauth!!.needsReauth)
            Log.i(TAG, "record round trip ok: renewed once, refusal marked needsReauth, posts ${posts.size}")
        } finally {
            cipher.deleteKey()
            root.deleteRecursively()
        }
    }

    @Test
    fun aRevokedRecordRefusesEverySessionUntilANewSignIn() {
        aRevokedRecordRefusesEverySession(OAuthProviderId.MICROSOFT, ACCOUNT_JSON)  // no revocation endpoint: the local half is the whole of it
    }

    /**
     * The same with a Google record: Google offers a revocation endpoint, so the revoker also posts
     * the (made-up) refresh token there on its background thread; Google refuses it and the
     * `MailOAuth` state line says "not done", which the driver records. The revoker asks the
     * provider only for a configured client, hence the assumption.
     */
    @Test
    fun aRevokedGoogleRecordRefusesEverySessionUntilANewSignIn() {
        assumeConfigured(OAuthProviderId.GOOGLE)
        val providerSide = aRevokedRecordRefusesEverySession(OAuthProviderId.GOOGLE, GOOGLE_ACCOUNT_JSON)
        // the made-up token reaches Google's revocation endpoint on the revoker's background thread; waiting for the outcome
        // (refused) keeps the state line ahead of the end of the instrumentation process
        val accepted = requireNotNull(providerSide) { "a configured Google client posts the revocation" }.get(30, TimeUnit.SECONDS)
        assertFalse("Google does not accept a made-up token", accepted)
    }

    /** Returns the provider-side revocation request of the revoked record (null when nothing was posted). */
    private fun aRevokedRecordRefusesEverySession(provider: OAuthProviderId, accountJson: String): Future<Boolean>? {
        val store = AccountStores.of(context)
        val alias = "oauth-device-${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val link = OAuthLink(provider.id, authorizedAt = now, expiresAt = now + 3_600_000L)
        store.put(alias, AccountSecrets.withLink(accountJson, link), SecretKind.OAUTH2, OAuthTokens("access-live", "refresh-live", now + 3_600_000L).toJson().toCharArray())
        try {
            assertEquals("access-live", AccountSecrets.of(context).withUsableSecret(alias) { _, chars -> String(chars) })

            val providerSide = TokenRevoker.revoke(context, alias)  // Microsoft has no revocation endpoint (the local half is the whole of it); Google is asked on a background thread

            val stored = store.withSecret(alias) { _, chars -> OAuthTokens.parse(String(chars)) }
            assertEquals(TokenRevoker.REVOKED_TOKENS.accessToken, stored.accessToken)
            assertNull(stored.refreshToken)
            assertEquals(0L, stored.expiresAt)
            val revokedLink = MailAccountOptions.parse(store.get(alias)!!.accountJson, SecretKind.OAUTH2).oauth!!
            assertTrue(revokedLink.needsReauth)
            assertEquals("the link reports no expiry left, as mail.accounts.list() shows", 0L, revokedLink.expiresAt)
            try {
                AccountSecrets.of(context).withUsableSecret(alias) { _, _ -> fail("a revoked record must not yield a token") }
            } catch (e: MailException) {
                assertEquals(MailErrorCode.AUTH_FAILED, e.code)
            }

            // "Sign in again" stores a fresh grant on the same record
            AccountSecrets.of(context).storeGrant(alias, provider, OAuthTokens("access-new", "refresh-new", now + 7_200_000L))
            assertEquals("access-new", AccountSecrets.of(context).withUsableSecret(alias) { _, chars -> String(chars) })
            assertFalse(MailAccountOptions.parse(store.get(alias)!!.accountJson, SecretKind.OAUTH2).oauth!!.needsReauth)
            Log.i(TAG, "revoked record refused, re-authorization restored it (provider=${provider.id})")
            return providerSide
        } finally {
            store.remove(alias)
        }
    }

    /**
     * A fresh sign-in screen on a cleared task: the previous case leaves the browser's Custom Tab
     * on top of the plugin's task, and a plain launch into that task never reached the resumed
     * state in the same process (`startActivitySync` timed out), while a cleared task starts clean.
     */
    private fun launchSignIn(provider: OAuthProviderId): OAuthSignInActivity {
        val intent = OAuthSignInActivity.intent(context, provider).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return instrumentation.startActivitySync(intent) as OAuthSignInActivity
    }

    private fun assumeConfigured(provider: OAuthProviderId) =
        assumeTrue("this build carries no ${provider.id} client id", clients.isConfigured(provider))

    private fun providerName(provider: OAuthProviderId): String = strings.getString(
        when (provider) {
            OAuthProviderId.GOOGLE -> R.string.editor_oauth_provider_google
            OAuthProviderId.MICROSOFT -> R.string.editor_oauth_provider_microsoft
        },
    )

    /** What the browser does with the redirect URI: an implicit VIEW that the manifest routes to the redirect activity. */
    private fun deliver(uri: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun <T> onMain(action: () -> T): T {
        var value: T? = null
        instrumentation.runOnMainSync { value = action() }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    /**
     * Waits for [condition] on the main thread. With [screen] given, a screen the platform finished
     * meanwhile ends the case as a skipped assumption: the emulator's Chrome crashes in its
     * `onTrimMemory` handler now and then (a `SIGILL` of its own check, seen on the API 37 image
     * when its UI was hidden before the Custom Tab was launched again), and the platform finishes a
     * paused activity underneath a crashing one, which is an environment condition, not the screen's.
     */
    private fun waitUntil(what: String, timeoutMs: Long, screen: OAuthSignInActivity? = null, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!onMain(condition)) {
            if (screen != null && onMain { screen.isDestroyed || screen.isFinishing }) {
                Log.i(TAG, "the sign-in screen was finished underneath the browser before $what (the browser crashed on top of it)")
                throw AssumptionViolatedException("the sign-in screen was finished underneath the browser before $what (the browser crashed on top of it)")
            }
            check(System.currentTimeMillis() < deadline) { "timed out waiting until $what" }
            Thread.sleep(250)
        }
    }

    private fun ByteArray.containsSequence(candidate: ByteArray): Boolean {
        if (candidate.isEmpty() || candidate.size > size) return false
        outer@ for (start in 0..size - candidate.size) {
            for (offset in candidate.indices) {
                if (this[start + offset] != candidate[offset]) continue@outer
            }
            return true
        }
        return false
    }

    private companion object {
        const val TAG = "OAuthDevice"
        const val TEST_KEY_ALIAS = "io.github.supermonster003.autojs6.plugin.angus.mail.accounts.oauth-test"
        const val ACCOUNT_JSON = """{"provider":"outlook","address":"alice@outlook.com","auth":"xoauth2"}"""
        const val GOOGLE_ACCOUNT_JSON = """{"provider":"gmail","address":"alice@gmail.com","auth":"xoauth2"}"""
    }
}
