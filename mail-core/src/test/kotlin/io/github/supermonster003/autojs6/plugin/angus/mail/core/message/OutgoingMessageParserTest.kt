package io.github.supermonster003.autojs6.plugin.angus.mail.core.message

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import jakarta.mail.Flags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.InputStream

class OutgoingMessageParserTest {

    private class SizedSource(override val size: Long) : AttachmentSource {
        override fun open(): InputStream = InputStream.nullInputStream()
    }

    private val sources = listOf(BytesAttachmentSource("one".toByteArray()), BytesAttachmentSource("two".toByteArray()))

    @Test
    fun fullMessageParses() {
        val message = OutgoingMessageParser.parse(
            """{
                "from": {"name": "Alice", "address": "alice@example.org"},
                "to": ["bob@example.org", {"name": "Carol 陈", "address": "carol@example.org"}, "Dan <dan@example.org>, eve@example.org"],
                "cc": "frank@example.org",
                "bcc": [{"address": "grace@example.org"}],
                "replyTo": "reply@example.org",
                "subject": "报表 Report",
                "text": "plain",
                "html": "<p>rich <img src=\"cid:logo\"></p>",
                "attachments": [
                    {"descriptorIndex": 1, "fileName": "logo.png", "contentId": "<logo>", "size": 3},
                    {"descriptorIndex": 0, "fileName": "../report.csv", "mimeType": "text/csv", "inline": false}
                ],
                "headers": {"X-Campaign": "spring", "X-Tags": ["a", "b"]},
                "priority": "high",
                "inReplyTo": "abc@example.org",
                "references": ["<x@example.org>", "y@example.org"],
                "date": 1700000000000
            }""",
            sources,
        )
        assertEquals(MailAddressSpec("alice@example.org", "Alice"), message.from)
        assertEquals(listOf("bob@example.org", "carol@example.org", "dan@example.org", "eve@example.org"), message.to.map { it.address })
        assertEquals(listOf(null, "Carol 陈", "Dan", null), message.to.map { it.name })
        assertEquals(listOf("frank@example.org"), message.cc.map { it.address })
        assertEquals(listOf("grace@example.org"), message.bcc.map { it.address })
        assertEquals(listOf("reply@example.org"), message.replyTo.map { it.address })
        assertEquals("报表 Report", message.subject)
        assertEquals("plain", message.text)
        assertTrue(message.html!!.contains("cid:logo"))
        assertEquals(2, message.attachments.size)
        val logo = message.attachments[0]
        assertEquals("logo.png", logo.fileName)
        assertEquals("image/png", logo.mimeType)
        assertEquals("logo", logo.contentId)
        assertTrue(logo.inline)
        assertEquals(3L, logo.size)
        val report = message.attachments[1]
        assertEquals("sanitized file name", ".._report.csv", report.fileName)
        assertEquals("text/csv", report.mimeType)
        assertNull(report.contentId)
        assertFalse(report.inline)
        assertEquals(listOf("X-Campaign" to "spring", "X-Tags" to "a", "X-Tags" to "b"), message.headers)
        assertEquals(Priority.HIGH, message.priority)
        assertEquals("<abc@example.org>", message.inReplyTo)
        assertEquals(listOf("<x@example.org>", "<y@example.org>"), message.references)
        assertEquals(1700000000000L, message.dateMillis)
        assertEquals(6, message.recipients.size)
    }

    @Test
    fun minimalMessageNeedsOnlyRecipientAndSubject() {
        val message = OutgoingMessageParser.parse("""{"to": "bob@example.org", "subject": ""}""", emptyList())
        assertNull(message.text)
        assertNull(message.html)
        assertEquals(Priority.NORMAL, message.priority)
        assertTrue(message.attachments.isEmpty())
        assertNull(message.from)
        assertEquals("", message.subject)
    }

    @Test
    fun sendAndAppendArgs() {
        val send = OutgoingMessageParser.parseSendArgs("""{"message": {"to": "bob@example.org", "subject": "s"}, "saveToSent": false}""", emptyList())
        assertEquals(false, send.saveToSent)
        assertNull(OutgoingMessageParser.parseSendArgs("""{"message": {"to": "bob@example.org", "subject": "s"}}""", emptyList()).saveToSent)
        val append = OutgoingMessageParser.parseAppendArgs("""{"folder": "Drafts", "message": {"to": "bob@example.org", "subject": "s"}, "flags": ["seen", "draft", "${'$'}Label1"]}""", emptyList())
        assertEquals("Drafts", append.folder)
        assertTrue(append.flags.contains(Flags.Flag.SEEN) && append.flags.contains(Flags.Flag.DRAFT))
        assertTrue(append.flags.contains("\$Label1"))
        assertInvalid("""{"message": {"to": "bob@example.org", "subject": "s"}, "folder": "x"}""", "unknown field 'args.folder'") { OutgoingMessageParser.parseSendArgs(it, emptyList()) }
        assertInvalid("""{"message": {"to": "bob@example.org", "subject": "s"}}""", "'folder' is required") { OutgoingMessageParser.parseAppendArgs(it, emptyList()) }
        assertInvalid("""{"folder": "Drafts", "message": {"to": "bob@example.org", "subject": "s"}, "flags": ["recent"]}""", "read-only") { OutgoingMessageParser.parseAppendArgs(it, emptyList()) }
        assertInvalid("""{"folder": "Drafts", "message": {"to": "bob@example.org", "subject": "s"}, "flags": ["bad flag"]}""", "not a flag name") { OutgoingMessageParser.parseAppendArgs(it, emptyList()) }
        assertInvalid("[]", "must be a JSON object") { OutgoingMessageParser.parseSendArgs(it, emptyList()) }
        assertInvalid("{", "not valid JSON") { OutgoingMessageParser.parseSendArgs(it, emptyList()) }
    }

    @Test
    fun recipientsAndAddressesAreValidated() {
        assertInvalid("""{"subject": "s"}""", "at least one recipient")
        assertInvalid("""{"to": [], "subject": "s"}""", "at least one recipient")
        assertInvalid("""{"to": "not an address", "subject": "s"}""", "not a valid address list")
        assertInvalid("""{"to": {"name": "x"}, "subject": "s"}""", "'to.address' is required")
        assertInvalid("""{"to": {"address": "bob"}, "subject": "s"}""", "not a valid email address")
        assertInvalid("""{"to": 5, "subject": "s"}""", "must be a string")
        assertInvalid("""{"to": [["bob@example.org"]], "subject": "s"}""", "must be a string or an object")
        assertInvalid("""{"to": " ", "subject": "s"}""", "must not be blank")
        assertInvalid("""{"to": "bob@example.org", "from": ["a@example.org", "b@example.org"], "subject": "s"}""", "single address")
        assertInvalid("""{"to": {"name": "line\nbreak", "address": "bob@example.org"}, "subject": "s"}""", "invalid display name")
        val many = (1..MailLimits.MAX_RECIPIENTS + 1).joinToString(",") { "\"r$it@example.org\"" }
        assertEquals(MailErrorCode.LIMIT_EXCEEDED, failure("""{"to": [$many], "subject": "s"}""").code)
        val exact = (1..MailLimits.MAX_RECIPIENTS).joinToString(",") { "\"r$it@example.org\"" }
        assertEquals(MailLimits.MAX_RECIPIENTS, OutgoingMessageParser.parse("""{"to": [$exact], "subject": "s"}""", emptyList()).recipients.size)
    }

    @Test
    fun shapeErrorsNameTheField() {
        assertInvalid("""{"to": "bob@example.org"}""", "'subject' is required")
        assertInvalid("""{"to": "bob@example.org", "subject": 1}""", "'subject' must be a string")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "priority": "urgent"}""", "'priority' must be one of")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "inReplyTo": "a b"}""", "not a message id")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "references": [1]}""", "must be a string")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "date": -1}""", "UTC milliseconds")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "date": "yesterday"}""", "must be an integer")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "signature": "x"}""", "unknown field 'message.signature'")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "attachments": {}}""", "must be an array")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "headers": []}""", "'headers' must be an object")
    }

    @Test
    fun attachmentsAreBoundToDescriptors() {
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "attachments": [{"fileName": "a.txt"}]}""", "'attachments[0].descriptorIndex' is required")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "attachments": [{"descriptorIndex": 2, "fileName": "a.txt"}]}""", "has no descriptor (2 supplied)")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "attachments": [{"descriptorIndex": 0, "fileName": "a.txt"}, {"descriptorIndex": 0, "fileName": "b.txt"}]}""", "used twice")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "attachments": [{"descriptorIndex": 0}]}""", "'attachments[0].fileName' is required")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "attachments": [{"descriptorIndex": 0, "fileName": "a.txt", "mimeType": "nope"}]}""", "not a MIME type")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "attachments": [{"descriptorIndex": 0, "fileName": "a.txt", "contentId": "a b"}]}""", "not a content id")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "attachments": [{"descriptorIndex": 0, "fileName": "a.txt", "path": "/x"}]}""", "unknown field 'attachments[0].path'")
        val parsed = OutgoingMessageParser.parse("""{"to": "bob@example.org", "subject": "s", "attachments": [{"descriptorIndex": 0, "fileName": "noext", "size": 99}]}""", sources)
        assertEquals("application/octet-stream", parsed.attachments.single().mimeType)
        assertEquals("the descriptor size wins over the declared size", 3L, parsed.attachments.single().size)
        val unknownSize = OutgoingMessageParser.parse("""{"to": "bob@example.org", "subject": "s", "attachments": [{"descriptorIndex": 0, "fileName": "a.bin", "size": 99}]}""", listOf(SizedSource(-1)))
        assertEquals(-1L, unknownSize.attachments.single().size)
    }

    @Test
    fun attachmentLimitsAreEnforced() {
        val tooMany = (0..MailLimits.MAX_ATTACHMENTS_PER_MESSAGE).joinToString(",") { """{"descriptorIndex": $it, "fileName": "f$it.txt"}""" }
        val manySources = List(MailLimits.MAX_ATTACHMENTS_PER_MESSAGE + 1) { SizedSource(1) }
        assertEquals(MailErrorCode.LIMIT_EXCEEDED, failure("""{"to": "bob@example.org", "subject": "s", "attachments": [$tooMany]}""", manySources).code)
        val huge = failure("""{"to": "bob@example.org", "subject": "s", "attachments": [{"descriptorIndex": 0, "fileName": "big.bin"}]}""", listOf(SizedSource(MailLimits.MAX_ATTACHMENT_BYTES + 1)))
        assertEquals(MailErrorCode.LIMIT_EXCEEDED, huge.code)
        assertFalse(huge.retryable)
        val declaredHuge = failure("""{"to": "bob@example.org", "subject": "s", "attachments": [{"descriptorIndex": 0, "fileName": "big.bin", "size": ${MailLimits.MAX_ATTACHMENT_BYTES + 1}}]}""", listOf(SizedSource(-1)))
        assertEquals(MailErrorCode.LIMIT_EXCEEDED, declaredHuge.code)
    }

    @Test
    fun headersAreValidated() {
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "headers": {"From": "x@example.org"}}""", "set by the plugin")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "headers": {"content-type": "text/html"}}""", "set by the plugin")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "headers": {"X-Priority": "1"}}""", "set by the plugin")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "headers": {"X-Bad": "a\r\nBcc: evil@example.org"}}""", "line breaks")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "headers": {"Bad Name": "x"}}""", "not a header name")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "headers": {"X-Obj": {"a": 1}}}""", "must be a string or an array")
        assertInvalid("""{"to": "bob@example.org", "subject": "s", "headers": {"X-Long": "${"v".repeat(OutgoingMessageParser.MAX_HEADER_VALUE_LENGTH + 1)}"}}""", "exceeds")
        val tooMany = (1..OutgoingMessageParser.MAX_HEADERS + 1).joinToString(",") { "\"X-H$it\": \"v\"" }
        assertEquals(MailErrorCode.LIMIT_EXCEEDED, failure("""{"to": "bob@example.org", "subject": "s", "headers": {$tooMany}}""").code)
        val ok = OutgoingMessageParser.parse("""{"to": "bob@example.org", "subject": "s", "headers": {"X-Spaces": "hello world", "List-Unsubscribe": "<mailto:u@example.org>"}}""", emptyList())
        assertEquals(listOf("X-Spaces" to "hello world", "List-Unsubscribe" to "<mailto:u@example.org>"), ok.headers)
    }

    @Test
    fun fileNamesLoseSeparatorsAndControlCharacters() {
        assertEquals("a_b_c.txt", OutgoingMessageParser.sanitizeFileName("a/b\\c.txt"))
        assertEquals("tab_name.txt", OutgoingMessageParser.sanitizeFileName("tab\tname.txt"))
        assertEquals("attachment", OutgoingMessageParser.sanitizeFileName("   "))
        assertEquals(OutgoingMessageParser.MAX_FILE_NAME_LENGTH, OutgoingMessageParser.sanitizeFileName("x".repeat(400)).length)
        assertEquals("报表.xlsx", OutgoingMessageParser.sanitizeFileName("报表.xlsx"))
    }

    private fun failure(json: String, sources: List<AttachmentSource> = this.sources, block: (String) -> Any = { OutgoingMessageParser.parse(it, sources) }): MailException {
        try {
            block(json)
        } catch (e: MailException) {
            return e
        }
        fail("expected a MailException for $json")
        throw AssertionError()
    }

    private fun assertInvalid(json: String, expected: String, block: (String) -> Any = { OutgoingMessageParser.parse(it, sources) }) {
        val error = failure(json, sources, block)
        assertEquals(error.message, MailErrorCode.INVALID_ARGUMENT, error.code)
        assertTrue("expected '$expected' in '${error.message}'", error.message.contains(expected))
    }
}
