package io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TriggerConfigTest {

    @Test
    fun triggerIdsFollowTheAliasRules() {
        assertEquals("work-inbox", TriggerId.normalize("  Work-Inbox "))
        assertTrue(TriggerId.isValid("a"))
        assertFalse(TriggerId.isValid(""))
        assertFalse(TriggerId.isValid("-leading"))
        assertFalse(TriggerId.isValid("has space"))
        assertFalse(TriggerId.isValid("x".repeat(TriggerId.MAX_LENGTH + 1)))
        assertCode(MailErrorCode.INVALID_ARGUMENT) { TriggerId.normalize("bad/id") }
    }

    @Test
    fun validationNormalizesAndClampsAndRefusesTheRest() {
        val config = TriggerConfig("Alerts", "Work", folder = " Notifications ", pollIntervalMs = 1, filter = TriggerFilter(listOf(" A "))).validated()
        assertEquals("alerts", config.triggerId)
        assertEquals("work", config.alias)
        assertEquals("Notifications", config.folder)
        assertEquals(MailLimits.MIN_POLL_INTERVAL_MS, config.pollIntervalMs)
        assertEquals(listOf("a"), config.filter.from)
        assertNull(config.watchMode)
        assertEquals(MailLimits.MAX_POLL_INTERVAL_MS, TriggerConfig("a", "b", pollIntervalMs = Long.MAX_VALUE).validated().pollIntervalMs)
        assertEquals(WatchMode.POLL, TriggerConfig("a", "b", mode = "poll").validated().watchMode)
        assertCode(MailErrorCode.INVALID_ARGUMENT) { TriggerConfig("a", "b", mode = "push").validated() }
        assertCode(MailErrorCode.INVALID_ARGUMENT) { TriggerConfig("a", "b", folder = " ").validated() }
        assertCode(MailErrorCode.INVALID_ARGUMENT) { TriggerConfig("a", " ").validated() }
        assertCode(MailErrorCode.INVALID_ARGUMENT) { TriggerConfig("a", "b", pollIntervalMs = 0).validated() }
    }

    @Test
    fun theWatchOptionsFollowTheAccountProtocol() {
        val config = TriggerConfig("a", "b", folder = "Archive", mode = "poll", pollIntervalMs = 30_000).validated()
        val imap = config.watchOptions(MailProtocol.IMAP)
        assertEquals("Archive", imap.folder)
        assertEquals(WatchMode.POLL, imap.mode)
        assertEquals(30_000L, imap.pollIntervalMs)
        assertFalse(imap.fetchBody)
        assertCode(MailErrorCode.FOLDER_NOT_FOUND) { config.watchOptions(MailProtocol.POP3) }
        assertCode(MailErrorCode.UNSUPPORTED_OPERATION) { TriggerConfig("a", "b", mode = "idle").watchOptions(MailProtocol.POP3) }
    }

    @Test
    fun documentsRoundTripAndTolerateUnknownFields() {
        val config = TriggerConfig("alerts", "work", enabled = false, folder = "INBOX", mode = "idle", pollIntervalMs = 60_000, filter = TriggerFilter(subject = listOf("invoice")), since = 1_700_000_000_000)
        assertEquals(config, TriggerConfig.parse(config.toJson()))
        assertEquals(
            """{"triggerId":"alerts","alias":"work","enabled":false,"folder":"INBOX","mode":"idle","pollIntervalMs":60000,"filter":{"from":[],"subject":["invoice"]},"since":1700000000000}""",
            config.toJson(),
        )
        val list = TriggerConfigsDocument.parse("""{"triggers":[{"triggerId":"A","alias":"b","future":1}],"extra":true}""")
        assertEquals(1, list.triggers.size)
        assertEquals("A", list.triggers.single().triggerId)
        assertEquals("a", list.triggers.single().validated().triggerId)
        assertCode(MailErrorCode.INVALID_ARGUMENT) { TriggerConfig.parse("""{"alias":"b"}""") }
        assertCode(MailErrorCode.INVALID_ARGUMENT) { TriggerConfig.parse("""[]""") }
    }

    @Test
    fun subscriptionOptionsNameTheWatchAndMayAddAFilter() {
        val options = TriggerOptions.parse("""{"triggerId":"Alerts","filter":{"subject":"Invoice"},"generation":3}""")
        assertEquals("alerts", options.triggerId)
        assertEquals(listOf("invoice"), options.filter.subject)
        assertTrue(TriggerOptions.parse("""{"triggerId":"a"}""").filter.isEmpty)
        assertCode(MailErrorCode.INVALID_ARGUMENT) { TriggerOptions.parse(null) }
        assertCode(MailErrorCode.INVALID_ARGUMENT) { TriggerOptions.parse("""{"triggerId":"a","folder":"INBOX"}""") }
        assertCode(MailErrorCode.INVALID_ARGUMENT) { TriggerOptions.parse("""{"triggerId":"a","filter":[]}""") }
    }

    @Test
    fun theLenientListParserLeavesDamagedEntriesOut() {
        val stored = TriggerConfigsDocument.parseLenient(
            """{"triggers":[{"triggerId":"good","alias":"work"},{"triggerId":"bad id","alias":"work"},{"alias":"x"},7,{"triggerId":"late","alias":"work","mode":"push"}]}""",
        )
        assertEquals(listOf("good"), stored.triggers.map { it.triggerId })
        assertTrue(TriggerConfigsDocument.parseLenient("not json").triggers.isEmpty())
        assertTrue(TriggerConfigsDocument.parseLenient("[]").triggers.isEmpty())
        assertTrue(TriggerConfigsDocument.parseLenient("""{"triggers":{}}""").triggers.isEmpty())
    }

    private fun assertCode(code: String, block: () -> Unit) {
        try {
            block()
            fail("expected $code")
        } catch (e: MailException) {
            assertEquals(e.message, code, e.code)
        }
    }
}
