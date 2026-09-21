package io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/** The stored token document of a browser sign-in and the id token's address (roadmap P9). */
class OAuthTokensTest {

    @Test
    fun `the token document round-trips and keeps its optional fields`() {
        val tokens = OAuthTokens("ya29.access", "1//refresh", 1_800_000_000_000L, "https://mail.google.com/")
        val parsed = OAuthTokens.parse(tokens.toJson())
        assertEquals(tokens, parsed)
        val minimal = OAuthTokens.parse("""{"accessToken":"a","expiresAt":5}""")
        assertNull(minimal.refreshToken)
        assertNull(minimal.scope)
        assertEquals(5L, minimal.expiresAt)
    }

    @Test
    fun `an unreadable document is an invalid argument that names no token`() {
        val error = assertThrows(MailException::class.java) { OAuthTokens.parse("""{"accessToken":"leak-me"}""") }
        assertEquals(MailErrorCode.INVALID_ARGUMENT, error.code)
        assertFalse(error.message.contains("leak-me"))
        assertThrows(IllegalArgumentException::class.java) { OAuthTokens(" ", null, 1) }
        assertThrows(IllegalArgumentException::class.java) { OAuthTokens("a", null, -1) }
    }

    @Test
    fun `expiry is judged with the refresh margin`() {
        val tokens = OAuthTokens("a", null, expiresAt = 10_000_000L)
        assertFalse(tokens.expiresWithin(now = 10_000_000L - OAuthTokens.REFRESH_MARGIN_MS - 1))
        assertTrue(tokens.expiresWithin(now = 10_000_000L - OAuthTokens.REFRESH_MARGIN_MS))
        assertTrue(tokens.expiresWithin(now = 10_000_000L + 1))
        assertFalse(tokens.expiresWithin(now = 9_000_000L, marginMs = 0))
        assertEquals(5 * 60_000L, OAuthTokens.REFRESH_MARGIN_MS)
        assertEquals(3_600_000L, OAuthTokens.DEFAULT_LIFETIME_MS)
    }

    @Test
    fun `the id token's address comes from email, preferred_username or upn and nothing else`() {
        assertEquals("alice@gmail.com", IdTokenClaims.emailOf(jwt("""{"sub":"1","email":"alice@gmail.com"}""")))
        assertEquals("bob@outlook.com", IdTokenClaims.emailOf(jwt("""{"preferred_username":"bob@outlook.com"}""")))
        assertEquals("carol@contoso.com", IdTokenClaims.emailOf(jwt("""{"upn":"carol@contoso.com"}""")))
        assertNull("a claim without an at sign is not an address", IdTokenClaims.emailOf(jwt("""{"preferred_username":"live.com#bob"}""")))
        assertNull(IdTokenClaims.emailOf(jwt("""{"name":"Dana"}""")))
        assertNull(IdTokenClaims.emailOf(null))
        assertNull(IdTokenClaims.emailOf("not.a.jwt"))
        assertNull(IdTokenClaims.emailOf("a.!!!.c"))
        assertNull(IdTokenClaims.emailOf("h." + Base64.getUrlEncoder().withoutPadding().encodeToString("[1,2]".toByteArray()) + ".s"))
    }

    private fun jwt(payload: String): String = "eyJhbGciOiJub25lIn0." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray()) + ".sig"
}
