package io.github.supermonster003.autojs6.plugin.three.stamp.mail.oauth

import io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.account.OAuthLink
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.oauth.FormAnswer
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.oauth.FormPoster
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.oauth.OAuthProviderId
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.oauth.OAuthTokens
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.oauth.TokenClient
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.store.AccountStore
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.store.SavedAccount
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** The usable secret of a saved account (roadmap P9): refresh before expiry, mark on refusal, wipe afterwards. */
class AccountSecretsTest {

    private class MemoryAccountStore : AccountStore {
        private val records = LinkedHashMap<String, Pair<SavedAccount, CharArray>>()
        private var default: String? = null
        var clock = 1_000L

        override fun put(alias: String, accountJson: String, secretKind: SecretKind, secret: CharArray): SavedAccount {
            val saved = SavedAccount(alias, accountJson, secretKind, clock, default == alias)
            records[alias] = saved to secret.copyOf()
            return saved
        }

        override fun get(alias: String): SavedAccount? = records[alias]?.first
        override fun list(): List<SavedAccount> = records.values.map { it.first }
        override fun remove(alias: String): Boolean = records.remove(alias) != null
        override fun setDefault(alias: String?) {
            default = alias
        }

        override fun defaultAlias(): String? = default
        override fun <T> withSecret(alias: String, action: (SavedAccount, CharArray) -> T): T {
            val (saved, secret) = records[alias] ?: throw MailException(MailErrorCode.ACCOUNT_NOT_FOUND, "no saved account named '$alias'", retryable = false)
            return action(saved, secret.copyOf())
        }

        fun secretText(alias: String): String = String(records.getValue(alias).second)
    }

    private class ScriptedPoster(private val answer: (List<Pair<String, String>>) -> FormAnswer) : FormPoster {
        val forms = ArrayList<List<Pair<String, String>>>()
        override fun post(url: String, form: List<Pair<String, String>>): FormAnswer {
            forms += form
            return answer(form)
        }
    }

    private val now = 1_700_000_000_000L
    private val clients = OAuthClients("123-abc.apps.googleusercontent.com", "ms-client", null, "io.github.example.mail")
    private val gmailJson = """{"address":"alice@gmail.com","provider":"gmail","auth":"xoauth2","oauth":{"provider":"google","authorizedAt":1,"expiresAt":2,"needsReauth":false}}"""

    private fun secrets(store: AccountStore, poster: FormPoster, clock: () -> Long = { now }) =
        AccountSecrets(store, clients, TokenClient(poster, clock), clock)

    @Test
    fun `a password record passes through untouched`() {
        val store = MemoryAccountStore()
        store.put("work", """{"address":"a@qq.com","provider":"qq"}""", SecretKind.PASSWORD, "pw".toCharArray())
        val poster = ScriptedPoster { throw AssertionError("no request expected") }
        val seen = secrets(store, poster).withUsableSecret("work") { saved, chars -> saved.alias + ":" + String(chars) }
        assertEquals("work:pw", seen)
        assertTrue(poster.forms.isEmpty())
    }

    @Test
    fun `a fresh access token is handed over alone and wiped after the action`() {
        val store = MemoryAccountStore()
        store.put("g", gmailJson, SecretKind.OAUTH2, OAuthTokens("ya29.fresh", "1//r", now + 3_600_000L).toJson().toCharArray())
        val poster = ScriptedPoster { throw AssertionError("no request expected") }
        var handed: CharArray? = null
        val seen = secrets(store, poster).withUsableSecret("g") { saved, chars ->
            handed = chars
            assertEquals(SecretKind.OAUTH2, saved.secretKind)
            String(chars)
        }
        assertEquals("ya29.fresh", seen)
        assertFalse("the refresh token never reaches the session", seen.contains("1//r"))
        assertTrue("wiped", handed!!.all { it == '\u0000' })
        assertTrue(poster.forms.isEmpty())
    }

    @Test
    fun `an access token about to expire is refreshed first and the record rewritten`() {
        val store = MemoryAccountStore()
        store.clock = 5L
        store.put("g", gmailJson, SecretKind.OAUTH2, OAuthTokens("ya29.old", "1//r", now + 60_000L, "s").toJson().toCharArray())
        val poster = ScriptedPoster { FormAnswer(200, """{"access_token":"ya29.new","expires_in":3600}""") }
        val seen = secrets(store, poster).withUsableSecret("g") { _, chars -> String(chars) }

        assertEquals("ya29.new", seen)
        assertEquals(listOf("grant_type" to "refresh_token", "client_id" to "123-abc.apps.googleusercontent.com", "refresh_token" to "1//r"), poster.forms.single())
        val stored = OAuthTokens.parse(store.secretText("g"))
        assertEquals("ya29.new", stored.accessToken)
        assertEquals("1//r", stored.refreshToken)
        assertEquals(now + 3_600_000L, stored.expiresAt)
        val link = MailAccountOptions.parse(store.get("g")!!.accountJson, SecretKind.OAUTH2).oauth!!
        assertEquals(now + 3_600_000L, link.expiresAt)
        assertEquals(1L, link.authorizedAt)
        assertFalse(link.needsReauth)
        assertEquals("the address and preset survive the rewrite", "alice@gmail.com", MailAccountOptions.parse(store.get("g")!!.accountJson, SecretKind.OAUTH2).address)

        // the second use is served from the store without a request
        secrets(store, poster).withUsableSecret("g") { _, chars -> String(chars) }
        assertEquals(1, poster.forms.size)
    }

    @Test
    fun `a refused refresh marks the record as needing a new sign-in and fails with AUTH_FAILED`() {
        val store = MemoryAccountStore()
        store.put("g", gmailJson, SecretKind.OAUTH2, OAuthTokens("ya29.old", "1//r", now - 1).toJson().toCharArray())
        val poster = ScriptedPoster { FormAnswer(400, """{"error":"invalid_grant","error_description":"Token has been expired or revoked."}""") }
        val error = assertThrows(MailException::class.java) { secrets(store, poster).withUsableSecret("g") { _, chars -> String(chars) } }

        assertEquals(MailErrorCode.AUTH_FAILED, error.code)
        assertTrue(error.message, error.message.contains("sign in again"))
        val link = MailAccountOptions.parse(store.get("g")!!.accountJson, SecretKind.OAUTH2).oauth!!
        assertTrue(link.needsReauth)
        assertEquals("the tokens are kept for the provider-side revocation", "1//r", OAuthTokens.parse(store.secretText("g")).refreshToken)

        // a second attempt asks the provider again (it may have recovered) but does not rewrite the marker
        val updatedAtBefore = store.get("g")!!.updatedAt
        store.clock = 99L
        assertThrows(MailException::class.java) { secrets(store, poster).withUsableSecret("g") { _, _ -> } }
        assertEquals(2, poster.forms.size)
        assertEquals(updatedAtBefore, store.get("g")!!.updatedAt)
    }

    @Test
    fun `an outage of the token endpoint is retryable and leaves the record alone`() {
        val store = MemoryAccountStore()
        store.put("g", gmailJson, SecretKind.OAUTH2, OAuthTokens("ya29.old", "1//r", now - 1).toJson().toCharArray())
        val poster = ScriptedPoster { FormAnswer(502, "bad gateway") }
        val error = assertThrows(MailException::class.java) { secrets(store, poster).withUsableSecret("g") { _, _ -> } }
        assertEquals(MailErrorCode.SERVER_ERROR, error.code)
        assertTrue(error.retryable)
        assertFalse(MailAccountOptions.parse(store.get("g")!!.accountJson, SecretKind.OAUTH2).oauth!!.needsReauth)
    }

    @Test
    fun `a build without the provider's client cannot refresh and says so`() {
        val store = MemoryAccountStore()
        val outlookJson = """{"address":"bob@outlook.com","provider":"outlook","oauth":{"provider":"microsoft"}}"""
        store.put("o", outlookJson, SecretKind.OAUTH2, OAuthTokens("EwB", "M.R", now).toJson().toCharArray())
        val poster = ScriptedPoster { throw AssertionError("no request expected") }
        val unconfigured = AccountSecrets(store, OAuthClients("g", null, null, "app"), TokenClient(poster) { now }, { now })
        val error = assertThrows(MailException::class.java) { unconfigured.withUsableSecret("o") { _, _ -> } }
        assertEquals(MailErrorCode.AUTH_FAILED, error.code)
        assertEquals(OAuthClients.NOT_CONFIGURED, error.details)
        assertTrue(poster.forms.isEmpty())
    }

    @Test
    fun `an unknown alias is ACCOUNT_NOT_FOUND`() {
        val error = assertThrows(MailException::class.java) { secrets(MemoryAccountStore(), ScriptedPoster { throw AssertionError() }).withUsableSecret("nobody") { _, _ -> } }
        assertEquals(MailErrorCode.ACCOUNT_NOT_FOUND, error.code)
    }

    @Test
    fun `storing a grant replaces the tokens and the link but nothing else of the document`() {
        val store = MemoryAccountStore()
        store.put("g", """{"address":"alice@gmail.com","provider":"gmail","name":"Alice","oauth":{"provider":"google","authorizedAt":1,"expiresAt":2,"needsReauth":true}}""", SecretKind.OAUTH2, OAuthTokens("stale", null, 0).toJson().toCharArray())
        secrets(store, ScriptedPoster { throw AssertionError() }).storeGrant("g", OAuthProviderId.GOOGLE, OAuthTokens("ya29.granted", "1//granted", now + 10_000L))
        val saved = store.get("g")!!
        val root = Json.parseToJsonElement(saved.accountJson).jsonObject
        assertEquals("Alice", root.getValue("name").jsonPrimitive.content)
        val link = MailAccountOptions.parse(saved.accountJson, SecretKind.OAUTH2).oauth!!
        assertEquals(now, link.authorizedAt)
        assertEquals(now + 10_000L, link.expiresAt)
        assertFalse(link.needsReauth)
        assertEquals("1//granted", OAuthTokens.parse(store.secretText("g")).refreshToken)
        assertNull("no secret in the document", root["accessToken"])
        assertFalse(saved.accountJson.contains("granted"))
    }

    @Test
    fun `withLink keeps the other keys in place and rejects a non-object document`() {
        val json = AccountSecrets.withLink("""{"address":"a@b.c","oauth":{"provider":"google"},"receive":"pop3"}""", OAuthLink("microsoft", 5, 6, true))
        assertEquals("""{"address":"a@b.c","receive":"pop3","oauth":{"provider":"microsoft","authorizedAt":5,"expiresAt":6,"needsReauth":true}}""", json)
        assertThrows(MailException::class.java) { AccountSecrets.withLink("[]", OAuthLink("google")) }
    }
}
