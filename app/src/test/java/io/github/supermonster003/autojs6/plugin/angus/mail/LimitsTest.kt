package io.github.supermonster003.autojs6.plugin.angus.mail

import io.github.supermonster003.autojs6.plugin.angus.mail.binder.Limits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import org.autojs.plugin.mail.api.MailContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Binder-level ceilings of roadmap appendix B.5 (roadmap P2.5 `Limits`). */
class LimitsTest {

    @Test
    fun utf8LengthCountsBytesNotChars() {
        assertEquals(0, Limits.utf8Length(""))
        assertEquals(5, Limits.utf8Length("hello"))
        assertEquals(6, Limits.utf8Length("ééé"))
        assertEquals(9, Limits.utf8Length("你好吗"))
        assertEquals(4, Limits.utf8Length("😀"))
        val sample = "aé你😀"
        assertEquals(sample.toByteArray(Charsets.UTF_8).size, Limits.utf8Length(sample))
    }

    @Test
    fun envelopesAtTheCeilingPassAndOneByteMoreIsRefused() {
        val exact = "x".repeat(MailContract.MAX_ENVELOPE_BYTES)
        assertNull(Limits.checkRequest(exact))
        assertNull(Limits.checkResponse(exact))
        assertNull("an absent envelope is the router's problem, not the size check's", Limits.checkRequest(null))
        val request = Limits.checkRequest(exact + "y")
        assertNotNull(request)
        assertEquals(MailErrorCode.LIMIT_EXCEEDED, request!!.code)
        assertFalse(request.retryable)
        assertTrue(request.message, request.message.startsWith("request envelope of ${MailContract.MAX_ENVELOPE_BYTES + 1} bytes exceeds"))
        val multibyte = "你".repeat(MailContract.MAX_ENVELOPE_BYTES / 3 + 1)
        val response = Limits.checkResponse(multibyte)
        assertNotNull("multi-byte text is measured in UTF-8 bytes", response)
        assertTrue(response!!.message, response.message.startsWith("response envelope"))
    }

    @Test
    fun theQueueHoldsTheContractDepth() {
        assertEquals(MailContract.MAX_QUEUED_CALLS, Limits.QUEUE_CAPACITY)
        val full = Limits.queueFull()
        assertEquals(MailErrorCode.LIMIT_EXCEEDED, full.code)
        assertFalse(full.retryable)
        assertTrue(full.message, full.message.contains("MAX_QUEUED_CALLS"))
    }

    @Test
    fun errorMessagesAreClampedWithoutSplittingACodePoint() {
        val short = "the server rejected the command"
        assertEquals(short, Limits.clampErrorMessage(short))
        val exact = "e".repeat(MailContract.MAX_ERROR_MESSAGE_BYTES)
        assertEquals(exact, Limits.clampErrorMessage(exact))
        val long = "e".repeat(MailContract.MAX_ERROR_MESSAGE_BYTES + 100)
        val clamped = Limits.clampErrorMessage(long)
        assertEquals(MailContract.MAX_ERROR_MESSAGE_BYTES, Limits.utf8Length(clamped))
        assertTrue(clamped.endsWith("..."))
        val wide = "你".repeat(MailContract.MAX_ERROR_MESSAGE_BYTES)
        val clampedWide = Limits.clampErrorMessage(wide)
        assertTrue(Limits.utf8Length(clampedWide) <= MailContract.MAX_ERROR_MESSAGE_BYTES)
        assertTrue(clampedWide.endsWith("..."))
        assertEquals("no split code point", clampedWide.dropLast(3), clampedWide.dropLast(3).filter { it == '你' })
    }
}
