package io.github.supermonster003.autojs6.plugin.angus.mail.core.message

import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.toJson
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Properties

/**
 * Roadmap D21 / P2.3: the synthetic `.eml` fixtures under `src/test/resources/mime` (generated
 * by a script, example.org addresses only) exercise the charset, file-name and structure quirks
 * of real providers without touching any account.
 */
class MessageMapperFixturesTest {

    @Test
    fun undeclaredGbkBodyIsRecoveredAsGb18030() {
        val doc = full("gbk-undeclared.eml")
        assertEquals(CN_SUBJECT, doc.subject)
        assertEquals(CN_BODY, doc.text)
        assertNull(doc.html)
        assertFalse(doc.hasAttachments)
        assertTrue(doc.attachments.isEmpty())
        assertTrue(doc.bodyLoaded)
        assertEquals("<fixture-gbk-undeclared@example.org>", doc.messageId)
        assertEquals("sender@example.org", doc.from?.address)
        assertEquals("Sender", doc.from?.name)
        assertEquals(listOf("receiver@example.org"), doc.to.map { it.address })
        assertNotNull(doc.date)
    }

    @Test
    fun declaredGb2312AndIso2022JpDecodeThroughTheirCharsets() {
        assertEquals(CN_BODY, full("gb2312-declared.eml").text)
        assertEquals(CN_SUBJECT, full("gb2312-declared.eml").subject)
        val jp = full("iso-2022-jp.eml")
        assertEquals(JP_BODY, jp.text)
        assertEquals(JP_BODY.take(5), jp.subject)
    }

    @Test
    fun unknownCharsetFallsBackToUtf8Detection() {
        assertEquals(CN_BODY, full("unknown-charset.eml").text)
    }

    @Test
    fun raw8BitHeadersAndFileNamesAreRepaired() {
        val doc = full("raw-8bit-headers.eml")
        assertEquals(CN_SUBJECT, doc.subject)
        assertEquals("张三", doc.from?.name)
        assertEquals("zhangsan@example.org", doc.from?.address)
        assertEquals(CN_BODY, doc.text)
        assertTrue(doc.hasAttachments)
        val attachment = doc.attachments.single()
        assertEquals("2", attachment.partId)
        assertEquals("报表.csv", attachment.fileName)
        assertEquals("text/csv", attachment.mimeType)
        assertFalse(attachment.inline)
        assertTrue(attachment.size > 0)
        assertEquals(CN_SUBJECT, doc.headers["Subject"]?.single())
    }

    @Test
    fun rfc2231AndRfc2047FileNamesAreDecoded() {
        val doc = full("encoded-filenames.eml")
        assertEquals(listOf("2", "3"), doc.attachments.map { it.partId })
        assertEquals("报表.csv", doc.attachments[0].fileName)
        assertEquals("application/octet-stream", doc.attachments[0].mimeType)
        assertEquals("合同.pdf", doc.attachments[1].fileName)
        assertEquals("application/pdf", doc.attachments[1].mimeType)
    }

    @Test
    fun nestedMessagesAreOneDownloadableLeaf() {
        val doc = full("nested-rfc822.eml")
        assertEquals("see the forwarded message", doc.text)
        assertTrue(doc.hasAttachments)
        val nested = doc.attachments.single()
        assertEquals("2", nested.partId)
        assertEquals("message/rfc822", nested.mimeType)
        assertEquals("part-2.eml", nested.fileName)
        assertFalse(nested.inline)
        val leaves = MimeTree.leaves(message("nested-rfc822.eml"))
        assertEquals(listOf("1", "2"), leaves.map { it.partId })
        assertTrue(String(leaves[1].part.inputStream.readBytes(), Charsets.ISO_8859_1).contains("Subject: forwarded inner"))
    }

    @Test
    fun htmlOnlyMailGetsDerivedText() {
        val doc = full("html-only.eml")
        assertNotNull(doc.html)
        assertTrue(doc.html!!.contains("<h1>Your code</h1>"))
        val text = doc.text!!
        assertTrue(text, text.contains("Your code"))
        assertTrue(text, text.contains("Use 123456 within 10 minutes & keep it safe."))
        assertTrue(text, text.contains("- one\n- two"))
        assertTrue(text, text.contains("a\tb\nc\td"))
        assertTrue(text, text.contains("Verify now <https://example.org/verify?x=1> or https://example.org/"))
        assertTrue(text, text.contains("你好"))
        assertFalse(text, text.contains("alert(1)"))
        assertFalse(text, text.contains("ignored"))
        assertFalse(text, text.contains("color:red"))
        assertFalse(doc.hasAttachments)
    }

    @Test
    fun hostileFileNamesLoseSeparatorsAndControlCharacters() {
        val doc = full("traversal-filename.eml")
        assertEquals(listOf("2", "3", "4"), doc.attachments.map { it.partId })
        val traversal = doc.attachments[0].fileName
        assertFalse(traversal, traversal.contains('/') || traversal.contains('\\'))
        assertEquals(".._.._windows_system.ini", traversal)
        val control = doc.attachments[1].fileName
        assertEquals("tab_name_.bin", control)
        val cid = doc.attachments[2]
        assertEquals("logo.png", cid.fileName)
        assertEquals("logo@example.org", cid.contentId)
        assertTrue(cid.inline)
        assertTrue(doc.hasAttachments)
    }

    @Test
    fun alternativeAndRelatedPartsSplitIntoBodyInlineAndAttachment() {
        val doc = full("alternative-related.eml")
        assertEquals("plain version", doc.text)
        assertTrue(doc.html!!.contains("html version"))
        assertEquals(listOf("1.2", "2"), doc.attachments.map { it.partId })
        val logo = doc.attachments[0]
        assertEquals("logo.png", logo.fileName)
        assertEquals("logo", logo.contentId)
        assertTrue(logo.inline)
        assertEquals("image/png", logo.mimeType)
        val invite = doc.attachments[1]
        assertEquals("invite.ics", invite.fileName)
        assertEquals("text/calendar", invite.mimeType)
        assertFalse(invite.inline)
        assertTrue(doc.hasAttachments)
        assertFalse(doc.bodyTruncated)
        assertTrue(doc.bodyParts.isEmpty())
        val envelope = MessageMapper.envelope(message("alternative-related.eml"), "INBOX", JsonPrimitive(1))
        assertFalse(envelope.bodyLoaded)
        assertNull(envelope.text)
        assertTrue(envelope.hasAttachments)
        assertTrue(envelope.attachments.isEmpty())
    }

    @Test
    fun rawSourceIsBytePreservingAndTheDocumentSerializes() {
        val doc = MessageMapper.full(message("gbk-undeclared.eml"), "INBOX", JsonPrimitive(77), includeRaw = true)
        val raw = doc.raw!!
        assertFalse(doc.rawTruncated)
        assertTrue(raw.startsWith("From: Sender <sender@example.org>\r\n"))
        assertEquals(CN_BODY, TextRecovery.decodeGuessing(raw.substringAfter("\r\n\r\n").trim().toByteArray(Charsets.ISO_8859_1)))
        val json = doc.toJson()
        assertTrue(json, json.contains("\"uid\":77"))
        assertTrue(json, json.contains("\"bodyLoaded\":true"))
        assertTrue(json, json.contains("\"text\":\"$CN_BODY\""))
    }

    private fun full(name: String): MessageDocument = MessageMapper.full(message(name), "INBOX", JsonPrimitive(1))

    private fun message(name: String): MimeMessage {
        val stream = checkNotNull(javaClass.getResourceAsStream("/mime/$name")) { "missing fixture $name" }
        return stream.use { MimeMessage(Session.getInstance(Properties()), it) }
    }

    private companion object {
        const val CN_SUBJECT = "月度报表"
        const val CN_BODY = "你好，这是一封测试邮件。"
        const val JP_BODY = "こんにちは、テストメールです。"
    }
}
