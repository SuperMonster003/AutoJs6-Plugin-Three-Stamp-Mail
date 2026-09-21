package io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Base64

/** The token endpoint side of the code flow with PKCE (roadmap P9), against a scripted transport. */
class TokenClientTest {

    private class ScriptedPoster(private val answer: (String, List<Pair<String, String>>) -> FormAnswer) : FormPoster {
        val calls = ArrayList<Pair<String, List<Pair<String, String>>>>()

        override fun post(url: String, form: List<Pair<String, String>>): FormAnswer {
            calls += url to form
            return answer(url, form)
        }
    }

    private val now = 1_700_000_000_000L
    private val google = OAuthProviders.GOOGLE
    private val microsoft = OAuthProviders.microsoft()

    @Test
    fun `the exchange posts the public-client form with the verifier and yields tokens, expiry and the address`() {
        val idToken = "h." + Base64.getUrlEncoder().withoutPadding().encodeToString("""{"email":"alice@gmail.com"}""".toByteArray()) + ".s"
        val poster = ScriptedPoster { _, _ ->
            FormAnswer(200, """{"access_token":"ya29.a","expires_in":3599,"refresh_token":"1//r","scope":"https://mail.google.com/ openid email","token_type":"Bearer","id_token":"$idToken"}""")
        }
        val grant = TokenClient(poster) { now }.exchange(google, "123.apps.googleusercontent.com", "com.googleusercontent.apps.123:/oauth2redirect", "4/0Acode", "verifier-verifier-verifier-verifier-verifier")

        assertEquals(1, poster.calls.size)
        val (url, form) = poster.calls.single()
        assertEquals("https://oauth2.googleapis.com/token", url)
        assertEquals(
            listOf(
                "grant_type" to "authorization_code",
                "client_id" to "123.apps.googleusercontent.com",
                "redirect_uri" to "com.googleusercontent.apps.123:/oauth2redirect",
                "code" to "4/0Acode",
                "code_verifier" to "verifier-verifier-verifier-verifier-verifier",
            ),
            form,
        )
        assertFalse("public client: no secret", form.any { it.first == "client_secret" })
        assertEquals("ya29.a", grant.tokens.accessToken)
        assertEquals("1//r", grant.tokens.refreshToken)
        assertEquals(now + 3_599_000L, grant.tokens.expiresAt)
        assertEquals("https://mail.google.com/ openid email", grant.tokens.scope)
        assertEquals("alice@gmail.com", grant.email)
    }

    @Test
    fun `an answer without expires_in gets the default lifetime and no address`() {
        val poster = ScriptedPoster { _, _ -> FormAnswer(200, """{"access_token":"a","token_type":"Bearer"}""") }
        val grant = TokenClient(poster) { now }.exchange(google, "c", "r", "code", "verifier")
        assertEquals(now + OAuthTokens.DEFAULT_LIFETIME_MS, grant.tokens.expiresAt)
        assertNull(grant.tokens.refreshToken)
        assertNull(grant.email)
    }

    @Test
    fun `a Microsoft refresh repeats the mail scopes and keeps the refresh token the provider did not rotate`() {
        val poster = ScriptedPoster { _, _ -> FormAnswer(200, """{"access_token":"EwB.new","expires_in":3600}""") }
        val stored = OAuthTokens("EwB.old", "M.C5_refresh", now - 1, "scope")
        val refreshed = TokenClient(poster) { now }.refresh(microsoft, "client-ms", stored)

        val (url, form) = poster.calls.single()
        assertEquals("https://login.microsoftonline.com/consumers/oauth2/v2.0/token", url)
        assertEquals("refresh_token", form.first { it.first == "grant_type" }.second)
        assertEquals("M.C5_refresh", form.first { it.first == "refresh_token" }.second)
        assertEquals(
            "https://outlook.office.com/IMAP.AccessAsUser.All https://outlook.office.com/POP.AccessAsUser.All https://outlook.office.com/SMTP.Send offline_access",
            form.first { it.first == "scope" }.second,
        )
        assertEquals("EwB.new", refreshed.accessToken)
        assertEquals("M.C5_refresh", refreshed.refreshToken)
        assertEquals(now + 3_600_000L, refreshed.expiresAt)
        assertNull("the old scope is not carried into the new document", refreshed.scope)
    }

    @Test
    fun `a Google refresh sends no scope and takes a rotated refresh token`() {
        val poster = ScriptedPoster { _, _ -> FormAnswer(200, """{"access_token":"ya29.b","expires_in":100,"refresh_token":"1//rotated"}""") }
        val refreshed = TokenClient(poster) { now }.refresh(google, "c", OAuthTokens("ya29.a", "1//r", now))
        assertNull(poster.calls.single().second.firstOrNull { it.first == "scope" })
        assertEquals("1//rotated", refreshed.refreshToken)
    }

    @Test
    fun `a record without a refresh token needs a new sign-in before any request is made`() {
        val poster = ScriptedPoster { _, _ -> throw AssertionError("no request expected") }
        val error = assertThrows(MailException::class.java) { TokenClient(poster) { now }.refresh(google, "c", OAuthTokens("a", null, now)) }
        assertEquals(MailErrorCode.AUTH_FAILED, error.code)
        assertEquals(TokenClient.REAUTHORIZE, error.details)
        assertTrue(TokenClient.needsReauthorization(error))
        assertTrue(poster.calls.isEmpty())
    }

    @Test
    fun `an invalid_grant is an authentication failure that a new sign-in cures`() {
        val poster = ScriptedPoster { _, _ -> FormAnswer(400, """{"error":"invalid_grant","error_description":"AADSTS70000: The refresh token has expired. Trace ID: t"}""") }
        val error = assertThrows(MailException::class.java) { TokenClient(poster) { now }.refresh(microsoft, "c", OAuthTokens("a", "r", now)) }
        assertEquals(MailErrorCode.AUTH_FAILED, error.code)
        assertTrue(error.message, error.message.contains("sign in again"))
        assertTrue(error.details!!, error.details!!.startsWith("invalid_grant: AADSTS70000"))
        assertFalse(error.retryable)
        assertTrue(TokenClient.needsReauthorization(error))
    }

    @Test
    fun `provider outages are server errors to retry, unreachable endpoints connection failures`() {
        val broken = ScriptedPoster { _, _ -> FormAnswer(503, "<html>down</html>") }
        val outage = assertThrows(MailException::class.java) { TokenClient(broken) { now }.exchange(google, "c", "r", "code", "v") }
        assertEquals(MailErrorCode.SERVER_ERROR, outage.code)
        assertTrue(outage.retryable)
        assertEquals("HTTP 503", outage.details)

        val temporarily = ScriptedPoster { _, _ -> FormAnswer(400, """{"error":"temporarily_unavailable"}""") }
        assertEquals(MailErrorCode.SERVER_ERROR, assertThrows(MailException::class.java) { TokenClient(temporarily) { now }.exchange(google, "c", "r", "code", "v") }.code)

        val unreadable = ScriptedPoster { _, _ -> FormAnswer(200, "not json") }
        val garbage = assertThrows(MailException::class.java) { TokenClient(unreadable) { now }.exchange(google, "c", "r", "code", "v") }
        assertEquals(MailErrorCode.SERVER_ERROR, garbage.code)
        assertTrue(garbage.message, garbage.message.contains("unreadable"))

        val noToken = ScriptedPoster { _, _ -> FormAnswer(200, """{"token_type":"Bearer"}""") }
        assertEquals(MailErrorCode.SERVER_ERROR, assertThrows(MailException::class.java) { TokenClient(noToken) { now }.exchange(google, "c", "r", "code", "v") }.code)

        val refusedWithoutError = ScriptedPoster { _, _ -> FormAnswer(401, "") }
        val bare = assertThrows(MailException::class.java) { TokenClient(refusedWithoutError) { now }.exchange(google, "c", "r", "code", "v") }
        assertEquals(MailErrorCode.SERVER_ERROR, bare.code)
        assertFalse(bare.retryable)

        val offline = ScriptedPoster { _, _ -> throw IOException("unresolved host") }
        val unreachable = assertThrows(MailException::class.java) { TokenClient(offline) { now }.exchange(google, "c", "r", "code", "v") }
        assertEquals(MailErrorCode.CONNECT_FAILED, unreachable.code)
        assertEquals("oauth2.googleapis.com: IOException", unreachable.details)
        assertTrue(unreachable.retryable)

        val slow = ScriptedPoster { _, _ -> throw SocketTimeoutException("read timed out") }
        val timeout = assertThrows(MailException::class.java) { TokenClient(slow) { now }.exchange(google, "c", "r", "code", "v") }
        assertEquals(MailErrorCode.TIMEOUT, timeout.code)
        assertEquals("oauth2.googleapis.com", timeout.details)
    }

    @Test
    fun `no failure message names the code, the verifier or a token`() {
        val answers = listOf(
            FormAnswer(400, """{"error":"invalid_grant","error_description":"bad"}"""),
            FormAnswer(500, "boom"),
            FormAnswer(200, "{}"),
            FormAnswer(200, "?"),
        )
        answers.forEach { answer ->
            val poster = ScriptedPoster { _, _ -> answer }
            val error = assertThrows(MailException::class.java) { TokenClient(poster) { now }.exchange(google, "client", "redirect", "THE-CODE", "THE-VERIFIER") }
            val text = error.message + " " + error.details
            assertFalse(text, text.contains("THE-CODE"))
            assertFalse(text, text.contains("THE-VERIFIER"))
        }
        val refreshError = assertThrows(MailException::class.java) {
            TokenClient(ScriptedPoster { _, _ -> FormAnswer(400, """{"error":"invalid_grant"}""") }) { now }.refresh(google, "c", OAuthTokens("THE-ACCESS", "THE-REFRESH", now))
        }
        assertFalse(refreshError.message + refreshError.details, (refreshError.message + refreshError.details).contains("THE-"))
    }

    @Test
    fun `the exchange refuses blank code or verifier locally`() {
        val poster = ScriptedPoster { _, _ -> throw AssertionError("no request expected") }
        assertThrows(IllegalArgumentException::class.java) { TokenClient(poster).exchange(google, "c", "r", "", "v") }
        assertThrows(IllegalArgumentException::class.java) { TokenClient(poster).exchange(google, "c", "r", "code", " ") }
    }

    @Test
    fun `revocation is best effort and only where the provider offers it`() {
        val accepted = ScriptedPoster { _, _ -> FormAnswer(200, "") }
        assertTrue(TokenClient(accepted).revoke(google, "c", "1//r"))
        assertEquals("https://oauth2.googleapis.com/revoke", accepted.calls.single().first)
        assertEquals(listOf("token" to "1//r", "client_id" to "c"), accepted.calls.single().second)

        val refused = ScriptedPoster { _, _ -> FormAnswer(400, """{"error":"invalid_token"}""") }
        assertFalse(TokenClient(refused).revoke(google, "c", "stale"))

        val offline = ScriptedPoster { _, _ -> throw IOException("offline") }
        assertFalse(TokenClient(offline).revoke(google, "c", "r"))

        val untouched = ScriptedPoster { _, _ -> throw AssertionError("no request expected") }
        assertFalse("Microsoft personal accounts have no revocation endpoint", TokenClient(untouched).revoke(microsoft, "c", "r"))
        assertTrue(untouched.calls.isEmpty())
    }
}
