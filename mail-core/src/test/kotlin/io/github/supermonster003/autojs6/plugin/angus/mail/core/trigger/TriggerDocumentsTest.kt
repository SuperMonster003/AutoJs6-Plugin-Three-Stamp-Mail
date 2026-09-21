package io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.AddressDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.ErrorDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MailJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerEntryDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerEventDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerListDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerRecord
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerRecordsDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerStatusDocument
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerDocumentsTest {

    private val message = MessageDocument(
        uid = JsonPrimitive(42),
        folder = "INBOX",
        messageId = "<m@x>",
        subject = "s".repeat(TriggerRecord.MAX_SUBJECT_CHARS + 10),
        from = AddressDocument("Alice", "alice@example.org"),
        date = 5L,
        size = 1234,
        text = "a body that must not be recorded",
        bodyLoaded = true,
    )

    @Test
    fun recordsKeepTheEnvelopeSummaryOnlyNewestFirstAndBounded() {
        val record = TriggerRecord.of(message, receivedAt = 9L)
        assertEquals(TriggerRecord.MAX_SUBJECT_CHARS, record.subject.length)
        assertEquals("alice@example.org", record.from?.address)
        assertEquals(42L, record.uid.content.toLong())
        assertFalse(record.toString().contains("body"))
        var document = TriggerRecordsDocument()
        repeat(MailLimits.MAX_TRIGGER_RECORDS + 5) { index -> document = document.prepend(record.copy(receivedAt = index.toLong())) }
        assertEquals(MailLimits.MAX_TRIGGER_RECORDS, document.records.size)
        assertEquals((MailLimits.MAX_TRIGGER_RECORDS + 4).toLong(), document.records.first().receivedAt)
        assertEquals(document, TriggerRecordsDocument.parse(document.toJson()))
        assertFalse(document.toJson().contains("must not be recorded"))
    }

    @Test
    fun theEventCarriesTheWatchTheAccountAndTheMessage() {
        val event = TriggerEventDocument(triggerId = "alerts", alias = "work", address = "me@example.org", folder = "INBOX", message = message, receivedAt = 7L)
        val json = event.toJson()
        val root = MailJson.format.parseToJsonElement(json).jsonObject
        assertEquals(TriggerEventDocument.TYPE_MAIL, root.getValue("type").jsonPrimitive.content)
        assertEquals("alerts", root.getValue("triggerId").jsonPrimitive.content)
        assertEquals("42", (root.getValue("message") as JsonObject).getValue("uid").jsonPrimitive.content)
        assertEquals(event, TriggerEventDocument.parse(json))
    }

    @Test
    fun statusAndListDocumentsRenderTheContractShape() {
        assertEquals(setOf("stopped", "connecting", "connected", "failed"), TriggerStatusDocument.STATES)
        val status = TriggerStatusDocument("alerts", TriggerStatusDocument.STATE_FAILED, lastError = ErrorDocument("AUTH_FAILED", "no", retryable = false), since = 3L)
        val config = TriggerConfig("alerts", "work", since = 1L).validated()
        val list = TriggerListDocument(listOf(TriggerEntryDocument.of(config, null, 3, status)))
        val root = MailJson.format.parseToJsonElement(list.toJson()).jsonObject
        val entry = (root.getValue("triggers") as kotlinx.serialization.json.JsonArray).single().jsonObject
        assertEquals("alerts", entry.getValue("triggerId").jsonPrimitive.content)
        assertNull("no address when the account is missing", entry["address"])
        assertEquals("true", entry.getValue("enabled").jsonPrimitive.content)
        assertEquals("failed", entry.getValue("status").jsonObject.getValue("state").jsonPrimitive.content)
        assertEquals("AUTH_FAILED", entry.getValue("status").jsonObject.getValue("lastError").jsonObject.getValue("code").jsonPrimitive.content)
        assertTrue(entry.getValue("filter").jsonObject.containsKey("from"))
        assertEquals("3", entry.getValue("records").jsonPrimitive.content)
    }
}
