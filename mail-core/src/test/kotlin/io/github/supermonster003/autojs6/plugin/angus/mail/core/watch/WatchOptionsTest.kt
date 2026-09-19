package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WatchOptionsTest {

    @Test
    fun defaultsFollowTheProtocolDocument() {
        listOf(null, "", "{}").forEach { json ->
            val options = WatchOptions.parse(json, MailProtocol.IMAP)
            assertEquals("INBOX", options.folder)
            assertNull(options.mode)
            assertEquals(MailLimits.DEFAULT_POLL_INTERVAL_MS, options.pollIntervalMs)
            assertFalse(options.fetchBody)
        }
    }

    @Test
    fun everyFieldIsReadAndTheGenerationIsIgnored() {
        val options = WatchOptions.parse("""{"folder":"Archive","mode":"poll","pollIntervalMs":30000,"fetchBody":true,"generation":7}""", MailProtocol.IMAP)
        assertEquals("Archive", options.folder)
        assertEquals(WatchMode.POLL, options.mode)
        assertEquals(30_000L, options.pollIntervalMs)
        assertTrue(options.fetchBody)
    }

    @Test
    fun thePollIntervalIsClampedIntoTheContractRange() {
        assertEquals(MailLimits.MIN_POLL_INTERVAL_MS, WatchOptions.parse("""{"pollIntervalMs":1}""", MailProtocol.IMAP).pollIntervalMs)
        assertEquals(MailLimits.MAX_POLL_INTERVAL_MS, WatchOptions.parse("""{"pollIntervalMs":99999999}""", MailProtocol.IMAP).pollIntervalMs)
        assertCode(MailErrorCode.INVALID_ARGUMENT) { WatchOptions.parse("""{"pollIntervalMs":0}""", MailProtocol.IMAP) }
        assertCode(MailErrorCode.INVALID_ARGUMENT) { WatchOptions.parse("""{"pollIntervalMs":"soon"}""", MailProtocol.IMAP) }
    }

    @Test
    fun unknownFieldsAndUnknownModesAreRefused() {
        assertCode(MailErrorCode.INVALID_ARGUMENT) { WatchOptions.parse("""{"interval":5}""", MailProtocol.IMAP) }
        assertCode(MailErrorCode.INVALID_ARGUMENT) { WatchOptions.parse("""{"mode":"push"}""", MailProtocol.IMAP) }
        assertCode(MailErrorCode.INVALID_ARGUMENT) { WatchOptions.parse("""[]""", MailProtocol.IMAP) }
        assertCode(MailErrorCode.INVALID_ARGUMENT) { WatchOptions.parse("""{"folder":""}""", MailProtocol.IMAP) }
    }

    @Test
    fun pop3AccountsWatchTheInboxByPollingOnly() {
        assertEquals(WatchMode.POLL, WatchOptions.parse("""{"mode":"poll"}""", MailProtocol.POP3).mode)
        assertEquals("INBOX", WatchOptions.parse("""{"folder":"inbox"}""", MailProtocol.POP3).folder.uppercase())
        assertCode(MailErrorCode.FOLDER_NOT_FOUND) { WatchOptions.parse("""{"folder":"Sent"}""", MailProtocol.POP3) }
        assertCode(MailErrorCode.UNSUPPORTED_OPERATION) { WatchOptions.parse("""{"mode":"idle"}""", MailProtocol.POP3) }
    }

    private fun assertCode(code: String, block: () -> Unit) {
        try {
            block()
            fail("expected $code")
        } catch (e: MailException) {
            assertEquals(e.toString(), code, e.code)
        }
    }
}
