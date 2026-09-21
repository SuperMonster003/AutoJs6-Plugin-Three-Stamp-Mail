package io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.net.URLDecoder

/** The authorization URL the browser is sent to and the redirect it comes back with (roadmap P9). */
class AuthorizationRequestTest {

    private val pkce = PkceChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk", "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
    private val state = "c2VjcmV0LXN0YXRlLTEyMw"
    private val redirect = "io.github.example.mail://oauth2/microsoft"

    @Test
    fun `the Microsoft request carries the code flow, PKCE, the mail and identity scopes and the login hint`() {
        val request = AuthorizationRequest(OAuthProviders.microsoft(), "client-1", redirect, state, pkce, loginHint = "alice@outlook.com")
        val url = request.url()
        assertTrue(url, url.startsWith("https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?"))
        val query = AuthorizationResponses.queryOf(URI(url))
        assertEquals("code", query["response_type"])
        assertEquals("client-1", query["client_id"])
        assertEquals(redirect, query["redirect_uri"])
        assertEquals(state, query["state"])
        assertEquals(pkce.challenge, query["code_challenge"])
        assertEquals("S256", query["code_challenge_method"])
        assertEquals("alice@outlook.com", query["login_hint"])
        assertEquals(
            "https://outlook.office.com/IMAP.AccessAsUser.All https://outlook.office.com/POP.AccessAsUser.All https://outlook.office.com/SMTP.Send offline_access openid email",
            query["scope"],
        )
        assertNull("no prompt parameter on Microsoft", query["prompt"])
        assertTrue("spaces are percent-encoded, not plus-encoded", url.contains("scope=https%3A%2F%2Foutlook.office.com%2FIMAP.AccessAsUser.All%20"))
    }

    @Test
    fun `the Google request asks for offline access with a consent prompt and the mail scope`() {
        val request = AuthorizationRequest(OAuthProviders.GOOGLE, "123-abc.apps.googleusercontent.com", "com.googleusercontent.apps.123-abc:/oauth2redirect", state, pkce)
        val query = AuthorizationResponses.queryOf(URI(request.url()))
        assertEquals("offline", query["access_type"])
        assertEquals("consent", query["prompt"])
        assertEquals("https://mail.google.com/ openid email", query["scope"])
        assertNull(query["login_hint"])
        assertEquals(listOf("https://mail.google.com/", "openid", "email"), request.scopes)
    }

    @Test
    fun `blank client id, redirect or state are refused before any URL exists`() {
        assertThrows(IllegalArgumentException::class.java) { AuthorizationRequest(OAuthProviders.GOOGLE, " ", redirect, state, pkce) }
        assertThrows(IllegalArgumentException::class.java) { AuthorizationRequest(OAuthProviders.GOOGLE, "c", "", state, pkce) }
        assertThrows(IllegalArgumentException::class.java) { AuthorizationRequest(OAuthProviders.GOOGLE, "c", redirect, "", pkce) }
    }

    @Test
    fun `a redirect with the request's state and a code is granted`() {
        val response = AuthorizationResponses.parse("$redirect?code=M.C5_abc-123&state=$state&session_state=ignored", redirect, state)
        assertEquals(AuthorizationResponse.Granted("M.C5_abc-123"), response)
    }

    @Test
    fun `a redirect of another URI, another state or without a code is rejected without echoing it`() {
        val foreignTarget = AuthorizationResponses.parse("io.github.example.mail://oauth2/google?code=x&state=$state", redirect, state)
        assertEquals(AuthorizationResponse.Rejected("the redirect targets another URI"), foreignTarget)
        val foreignState = AuthorizationResponses.parse("$redirect?code=SECRET-CODE&state=other", redirect, state)
        assertEquals(AuthorizationResponse.Rejected("the redirect carries no matching state"), foreignState)
        assertFalse(foreignState.toString().contains("SECRET-CODE"))
        val noState = AuthorizationResponses.parse("$redirect?code=x", redirect, state)
        assertEquals(AuthorizationResponse.Rejected("the redirect carries no matching state"), noState)
        val noCode = AuthorizationResponses.parse("$redirect?state=$state", redirect, state)
        assertEquals(AuthorizationResponse.Rejected("the redirect carries no code"), noCode)
        val notAUri = AuthorizationResponses.parse("::not a uri::", redirect, state)
        assertEquals(AuthorizationResponse.Rejected("the redirect is not a URI"), notAUri)
    }

    @Test
    fun `the scheme is matched case-insensitively and a trailing slash is tolerated`() {
        val response = AuthorizationResponses.parse("IO.GITHUB.EXAMPLE.MAIL://oauth2/microsoft/?state=$state&code=c", redirect, state)
        assertEquals(AuthorizationResponse.Granted("c"), response)
        val google = AuthorizationResponses.parse("com.googleusercontent.apps.123:/oauth2redirect?state=$state&code=4%2F0Ab", "com.googleusercontent.apps.123:/oauth2redirect", state)
        assertEquals(AuthorizationResponse.Granted("4/0Ab"), google)
    }

    @Test
    fun `a refusal is reported with its error code and a bounded description, after the state check`() {
        val denied = AuthorizationResponses.parse("$redirect?error=access_denied&error_description=The%20user%20cancelled&state=$state", redirect, state)
        assertEquals(AuthorizationResponse.Denied("access_denied", "The user cancelled"), denied)
        val unmatched = AuthorizationResponses.parse("$redirect?error=access_denied&state=other", redirect, state)
        assertTrue(unmatched is AuthorizationResponse.Rejected)
        val longDescription = "x".repeat(400)
        val bounded = AuthorizationResponses.parse("$redirect?error=${"e".repeat(100)}&error_description=$longDescription&state=$state", redirect, state) as AuthorizationResponse.Denied
        assertEquals(64, bounded.error.length)
        assertEquals(300, bounded.description!!.length)
    }

    @Test
    fun `the query decoder keeps the first value of a repeated key and decodes once`() {
        val query = AuthorizationResponses.queryOf(URI("x://y/z?a=1&a=2&b&&d=%20&e=x%2By&f=x+y"))
        assertEquals("1", query["a"])
        assertEquals("", query["b"])
        assertEquals(" ", query["d"])
        assertEquals("x+y", query["e"])
        assertEquals("x y", query["f"])
        assertEquals(URLDecoder.decode("%20", "UTF-8"), query["d"])
        assertTrue(AuthorizationResponses.queryOf(URI("x://y/z")).isEmpty())
    }
}
