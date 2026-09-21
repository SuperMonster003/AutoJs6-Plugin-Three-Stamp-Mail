package io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** The random material of an authorization request (roadmap P9): RFC 7636 `S256` and the `state`. */
class PkceTest {

    @Test
    fun `the challenge is the RFC 7636 appendix B vector`() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", Pkce.challengeOf("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
        assertEquals("S256", PkceChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk", "x").method)
    }

    @Test
    fun `a generated verifier has 43 unreserved characters and a fresh challenge every time`() {
        val first = Pkce.generate()
        val second = Pkce.generate()
        assertEquals(43, first.verifier.length)
        assertTrue(first.verifier, first.verifier.all { it.isLetterOrDigit() || it in "-._~" })
        assertEquals(Pkce.challengeOf(first.verifier), first.challenge)
        assertNotEquals(first.verifier, second.verifier)
        assertNotEquals(first.challenge, second.challenge)
    }

    @Test
    fun `a verifier outside the RFC bounds or alphabet is refused`() {
        assertThrows(IllegalArgumentException::class.java) { Pkce.challengeOf("short") }
        assertThrows(IllegalArgumentException::class.java) { Pkce.challengeOf("a".repeat(129)) }
        assertThrows(IllegalArgumentException::class.java) { Pkce.challengeOf("a".repeat(42) + "+") }
    }

    @Test
    fun `the state is 22 url-safe characters and matches itself only`() {
        val state = Pkce.state()
        assertEquals(22, state.length)
        assertTrue(state, state.all { it.isLetterOrDigit() || it in "-_" })
        assertTrue(Pkce.sameState(state, state))
        assertFalse(Pkce.sameState(state, state.dropLast(1)))
        assertFalse(Pkce.sameState(state, state + "x"))
        assertFalse(Pkce.sameState(state, null))
        assertFalse(Pkce.sameState(state, ""))
        assertFalse(Pkce.sameState("", ""))
        assertNotEquals(state, Pkce.state())
    }
}
