package io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.AddressDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MailJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TriggerFilterTest {

    private fun message(subject: String, from: AddressDocument? = null, sender: AddressDocument? = null): MessageDocument =
        MessageDocument(uid = JsonPrimitive(1), folder = "INBOX", subject = subject, from = from, sender = sender, size = 10)

    private fun filter(json: String): TriggerFilter = TriggerFilter.parse(MailJson.format.parseToJsonElement(json) as JsonObject)

    @Test
    fun anEmptyFilterMatchesEverything() {
        assertTrue(TriggerFilter.NONE.isEmpty)
        assertTrue(TriggerFilter.NONE.matches(message("anything")))
        assertTrue(TriggerFilter.parse(null).matches(message("")))
        assertTrue(filter("""{"from":[],"subject":""}""").isEmpty)
    }

    @Test
    fun fromEntriesMatchTheAddressOrTheDisplayNameCaseInsensitively() {
        val filter = filter("""{"from":["Alerts@Example.org","  ops team "]}""")
        assertEquals(listOf("alerts@example.org", "ops team"), filter.from)
        assertTrue(filter.matches(message("x", AddressDocument("Alerts", "ALERTS@example.org"))))
        assertTrue(filter.matches(message("x", AddressDocument("The Ops Team", "someone@example.org"))))
        assertFalse(filter.matches(message("x", AddressDocument("Bob", "bob@example.org"))))
        assertFalse("no sender at all", filter.matches(message("x")))
        assertTrue("Sender stands in for a missing From", filter.matches(message("x", sender = AddressDocument(null, "alerts@example.org"))))
    }

    @Test
    fun subjectEntriesAreSubstringsAndTheGroupsCombineWithAnd() {
        val filter = filter("""{"from":"bank","subject":["Invoice","statement"]}""")
        assertTrue(filter.matches(message("Your INVOICE 42", AddressDocument(null, "no-reply@bank.example"))))
        assertTrue(filter.matches(message("Monthly statement", AddressDocument(null, "no-reply@bank.example"))))
        assertFalse("subject matches, sender does not", filter.matches(message("Invoice", AddressDocument(null, "shop@example.org"))))
        assertFalse("sender matches, subject does not", filter.matches(message("Hello", AddressDocument(null, "no-reply@bank.example"))))
    }

    @Test
    fun unknownFieldsAndOverlongGroupsAreRefused() {
        assertCode(MailErrorCode.INVALID_ARGUMENT) { filter("""{"to":"x"}""") }
        assertCode(MailErrorCode.INVALID_ARGUMENT) { filter("""{"from":5}""") }
        val many = (1..TriggerFilter.MAX_ENTRIES + 1).joinToString(",") { "\"entry$it\"" }
        assertCode(MailErrorCode.INVALID_ARGUMENT) { filter("""{"subject":[$many]}""") }
        assertCode(MailErrorCode.INVALID_ARGUMENT) { filter("""{"subject":"${"x".repeat(TriggerFilter.MAX_ENTRY_LENGTH + 1)}"}""") }
        assertEquals("duplicates collapse", listOf("a"), filter("""{"from":["a","A"," a "]}""").from)
    }

    @Test
    fun theSettingsPageSplitsOnCommasSemicolonsAndLineBreaks() {
        assertEquals(listOf("a@x.org", "b", "c"), TriggerFilter.split(" a@x.org, b;\nc\r\n"))
        assertEquals(emptyList<String>(), TriggerFilter.split(null))
    }

    @Test
    fun theDocumentFormIsTwoArrays() {
        assertEquals("""{"from":["a"],"subject":[]}""", MailJson.format.encodeToString(TriggerFilter(listOf("a"))))
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
