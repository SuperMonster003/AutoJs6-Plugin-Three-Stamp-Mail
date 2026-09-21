package io.github.supermonster003.autojs6.plugin.angus.mail.trigger

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.AddressDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerRecord
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerConfig
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerFilter
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** The background-watch store on a temp directory (roadmap P8): configs, records and their ceilings. */
class TriggerStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private var now = 1_000L
    private val store by lazy { TriggerStore(folder.root, clock = { now }) }

    private fun record(uid: Long, subject: String = "s"): TriggerRecord =
        TriggerRecord.of(MessageDocument(uid = JsonPrimitive(uid), folder = "INBOX", subject = subject, from = AddressDocument("A", "a@x.org"), size = 1), receivedAt = now)

    @Test
    fun putNormalizesKeepsTheCreationTimeAndReplacesByTriggerId() {
        val first = store.put(TriggerConfig("Alerts", "Work", folder = " INBOX "))
        assertEquals("alerts", first.triggerId)
        assertEquals("work", first.alias)
        assertEquals(1_000L, first.since)
        now = 2_000L
        val replaced = store.put(TriggerConfig("alerts", "work", enabled = false, filter = TriggerFilter(subject = listOf("Invoice"))))
        assertEquals("the creation time survives an edit", 1_000L, replaced.since)
        assertFalse(replaced.enabled)
        assertEquals(listOf("invoice"), replaced.filter.subject)
        assertEquals(listOf(replaced), store.list())
        assertEquals(replaced, store.get("ALERTS"))
        assertNull(store.get("other"))
        assertFalse(store.hasEnabled())
        assertEquals(true, store.setEnabled("alerts", true)?.enabled)
        assertTrue(store.hasEnabled())
        assertNull(store.setEnabled("missing", true))
    }

    @Test
    fun theSeventeenthWatchIsRefusedAndRemovalFreesASlot() {
        repeat(MailLimits.MAX_TRIGGERS) { store.put(TriggerConfig("w$it", "work")) }
        assertEquals(MailLimits.MAX_TRIGGERS, store.list().size)
        assertCode(MailErrorCode.LIMIT_EXCEEDED) { store.put(TriggerConfig("one-more", "work")) }
        store.put(TriggerConfig("w0", "work", folder = "Archive"))
        assertTrue(store.remove("w0"))
        assertFalse(store.remove("w0"))
        store.put(TriggerConfig("one-more", "work"))
        assertEquals(MailLimits.MAX_TRIGGERS, store.list().size)
    }

    @Test
    fun recordsAreNewestFirstBoundedAndRemovedWithTheWatch() {
        store.put(TriggerConfig("alerts", "work"))
        repeat(MailLimits.MAX_TRIGGER_RECORDS + 7) { index ->
            now = 10_000L + index
            assertEquals(minOf(index + 1, MailLimits.MAX_TRIGGER_RECORDS), store.record("alerts", record(index.toLong())))
        }
        val records = store.records("alerts")
        assertEquals(MailLimits.MAX_TRIGGER_RECORDS, records.size)
        assertEquals((MailLimits.MAX_TRIGGER_RECORDS + 6).toLong(), records.first().uid.content.toLong())
        assertEquals(7L, records.last().uid.content.toLong())
        store.clearRecords("alerts")
        assertTrue(store.records("alerts").isEmpty())
        store.record("alerts", record(1))
        store.remove("alerts")
        assertTrue("the records file goes with the watch", store.records("alerts").isEmpty())
        assertEquals(listOf(TriggerStore.CONFIGS_FILE, TriggerStore.LOCK_FILE).sorted(), File(folder.root, TriggerStore.DIRECTORY).list()!!.sorted())
    }

    @Test
    fun damagedEntriesAreLeftOutAndUnusableConfigsRefused() {
        store.put(TriggerConfig("good", "work"))
        val file = File(folder.root, "${TriggerStore.DIRECTORY}/${TriggerStore.CONFIGS_FILE}")
        file.writeText("""{"triggers":[{"triggerId":"good","alias":"work"},{"triggerId":"bad id","alias":"work"},{"alias":"x"}]}""")
        assertEquals(listOf("good"), store.list().map { it.triggerId })
        file.writeText("not json")
        assertTrue(store.list().isEmpty())
        assertCode(MailErrorCode.INVALID_ARGUMENT) { store.put(TriggerConfig("", "work")) }
        assertCode(MailErrorCode.INVALID_ARGUMENT) { store.put(TriggerConfig("x", "work", mode = "push")) }
        assertCode(MailErrorCode.INVALID_ARGUMENT) { store.get("bad id") }
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
