package io.github.supermonster003.autojs6.plugin.angus.mail.store

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Alias normalization of the saved-account store (roadmap P4.1). */
class AccountAliasTest {

    @Test
    fun trimsLowerCasesAndNfcNormalizes() {
        assertEquals("work", AccountAlias.normalize("  Work "))
        assertEquals("work", AccountAlias.normalize("WORK"))
        assertEquals("qq.a_1-x", AccountAlias.normalize("QQ.A_1-x"))
        assertEquals("工作", AccountAlias.normalize("工作"))
        assertEquals("ünïcode", AccountAlias.normalize("ÜNÏCODE"))
        // U+0065 U+0301 (decomposed) becomes U+00E9 (composed).
        assertEquals("caf\u00e9", AccountAlias.normalize("cafe\u0301"))
    }

    @Test
    fun refusesBlankPunctuationAndOverlongAliases() {
        listOf(null, "", "   ", ".work", "-work", "_work", "wo rk", "work/1", "work\\1", "work:1", "a@b", "wo\nrk", "a".repeat(AccountAlias.MAX_LENGTH + 1))
            .forEach { candidate ->
                val error = assertThrows("alias '$candidate'", MailException::class.java) { AccountAlias.normalize(candidate) }
                assertEquals(MailErrorCode.INVALID_ARGUMENT, error.code)
                assertFalse(AccountAlias.isValid(candidate))
            }
        assertTrue(AccountAlias.isValid("a".repeat(AccountAlias.MAX_LENGTH)))
        assertTrue(AccountAlias.isValid("1"))
    }

    @Test
    fun messagesNameTheAliasWithoutControlCharacters() {
        val error = assertThrows(MailException::class.java) { AccountAlias.normalize("bad\u0007alias!") }
        assertTrue(error.message, error.message.contains("'bad?alias!'"))
        val long = assertThrows(MailException::class.java) { AccountAlias.normalize("x".repeat(80) + "!") }
        assertTrue(long.message, long.message.contains("..."))
        assertFalse(long.message, long.message.contains("x".repeat(60)))
    }
}
