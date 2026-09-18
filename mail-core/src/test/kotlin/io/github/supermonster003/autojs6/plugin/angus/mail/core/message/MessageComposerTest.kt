package io.github.supermonster003.autojs6.plugin.angus.mail.core.message

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSessionFactory
import jakarta.mail.Message
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Date

/** The MIME tree and header encoding of [MessageComposer], checked on the serialized bytes. */
class MessageComposerTest {

    private val account = MailAccount("alice@example.org", displayName = "Alice 张", smtp = MailEndpoint("smtp.example.org", 465, TlsMode.SSL))
    private val session = MailSessionFactory.session(account, MailProtocol.SMTP, MailSecret("unused"))

    private fun roundTrip(message: OutgoingMessage): Pair<MimeMessage, String> {
        val composed = MessageComposer.compose(session, account, message)
        val bytes = ByteArrayOutputStream().also { composed.writeTo(it) }.toByteArray()
        return MimeMessage(session, ByteArrayInputStream(bytes)) to bytes.toString(Charsets.ISO_8859_1)
    }

    @Test
    fun plainTextStaysSinglePart() {
        val (parsed, raw) = roundTrip(OutgoingMessage(to = listOf(MailAddressSpec("bob@example.org")), subject = "Hi", text = "hello"))
        assertTrue(parsed.isMimeType("text/plain"))
        assertEquals("hello", (parsed.content as String).trim())
        assertEquals("Hi", parsed.subject)
        val from = parsed.from.single() as InternetAddress
        assertEquals("alice@example.org", from.address)
        assertEquals("Alice 张", from.personal)
        assertTrue("display names are RFC 2047 encoded, never raw", raw.contains("=?UTF-8?") && !raw.contains("张"))
        assertNull(parsed.getHeader("X-Priority"))
        assertNull(parsed.getHeader("Cc"))
        assertNull(parsed.getHeader("Bcc"))
        assertTrue(parsed.messageID.startsWith("<"))
    }

    @Test
    fun textAndHtmlBecomeAlternative() {
        val (parsed, _) = roundTrip(OutgoingMessage(to = listOf(MailAddressSpec("bob@example.org")), subject = "s", text = "plain", html = "<b>rich</b>"))
        val alternative = parsed.content as MimeMultipart
        assertTrue(alternative.contentType.startsWith("multipart/alternative"))
        assertEquals(2, alternative.count)
        assertTrue(alternative.getBodyPart(0).isMimeType("text/plain"))
        assertTrue(alternative.getBodyPart(1).isMimeType("text/html"))
        assertEquals("<b>rich</b>", (alternative.getBodyPart(1).content as String).trim())
    }

    @Test
    fun htmlOnlyStaysSinglePart() {
        val (parsed, _) = roundTrip(OutgoingMessage(to = listOf(MailAddressSpec("bob@example.org")), subject = "s", html = "<i>x</i>"))
        assertTrue(parsed.isMimeType("text/html"))
    }

    @Test
    fun inlineImagesAndAttachmentsBuildRelatedInsideMixed() {
        val png = ByteArray(300) { (it * 7).toByte() }
        val csv = "id,total\r\n1,42\r\n".toByteArray()
        val message = OutgoingMessage(
            to = listOf(MailAddressSpec("bob@example.org", "Bob")),
            cc = listOf(MailAddressSpec("carol@example.org")),
            bcc = listOf(MailAddressSpec("dave@example.org")),
            replyTo = listOf(MailAddressSpec("reply@example.org", "Reply 组")),
            subject = "报表 Report",
            text = "see logo",
            html = "<img src=\"cid:logo\">",
            attachments = listOf(
                OutgoingAttachment("logo.png", "image/png", contentId = "logo", inline = true, source = BytesAttachmentSource(png)),
                OutgoingAttachment("报表 2026.csv", "text/csv", source = BytesAttachmentSource(csv)),
            ),
            headers = listOf("X-Campaign" to "spring 春", "X-Tags" to "a", "X-Tags" to "b"),
            priority = Priority.HIGH,
            inReplyTo = "<parent@example.org>",
            references = listOf("<root@example.org>", "<parent@example.org>"),
            dateMillis = 1_700_000_000_000L,
        )
        val (parsed, raw) = roundTrip(message)
        assertEquals("报表 Report", parsed.subject)
        assertFalse("subject and file names are encoded", raw.contains("报表"))
        assertEquals(listOf("Bob <bob@example.org>"), parsed.getRecipients(Message.RecipientType.TO).map { it.toString() })
        assertEquals(listOf("carol@example.org"), parsed.getRecipients(Message.RecipientType.CC).map { it.toString() })
        // The composed message keeps Bcc so the SMTP envelope covers it; the transport strips the
        // header on the wire (checked against GreenMail in SmtpSendGreenMailTest).
        assertEquals(listOf("dave@example.org"), parsed.getRecipients(Message.RecipientType.BCC).map { it.toString() })
        assertEquals("Reply 组", (parsed.replyTo.single() as InternetAddress).personal)
        assertEquals("reply@example.org", (parsed.replyTo.single() as InternetAddress).address)
        assertEquals(Date(1_700_000_000_000L).time / 1000, parsed.sentDate.time / 1000)
        assertEquals("<parent@example.org>", parsed.getHeader("In-Reply-To", null))
        assertEquals("<root@example.org> <parent@example.org>", parsed.getHeader("References", " "))
        assertEquals("1", parsed.getHeader("X-Priority", null))
        assertEquals("High", parsed.getHeader("Importance", null))
        assertEquals("High", parsed.getHeader("X-MSMail-Priority", null))
        assertEquals("spring 春", jakarta.mail.internet.MimeUtility.decodeText(parsed.getHeader("X-Campaign", null)))
        assertArrayEquals(arrayOf("a", "b"), parsed.getHeader("X-Tags"))

        val mixed = parsed.content as MimeMultipart
        assertTrue(mixed.contentType.startsWith("multipart/mixed"))
        assertEquals(2, mixed.count)
        val related = mixed.getBodyPart(0).content as MimeMultipart
        assertTrue(related.contentType.startsWith("multipart/related"))
        assertEquals(2, related.count)
        val alternative = related.getBodyPart(0).content as MimeMultipart
        assertTrue(alternative.contentType.startsWith("multipart/alternative"))
        val logo = related.getBodyPart(1) as MimeBodyPart
        assertEquals("<logo>", logo.contentID)
        assertEquals(MimeBodyPart.INLINE, logo.disposition)
        assertEquals("logo.png", logo.fileName)
        assertTrue(logo.isMimeType("image/png"))
        assertArrayEquals(png, logo.inputStream.readBytes())
        val attachment = mixed.getBodyPart(1) as MimeBodyPart
        assertEquals(MimeBodyPart.ATTACHMENT, attachment.disposition)
        assertEquals("报表 2026.csv", attachment.fileName)
        assertTrue(attachment.isMimeType("text/csv"))
        assertEquals("base64", attachment.encoding)
        assertArrayEquals(csv, attachment.inputStream.readBytes())
    }

    @Test
    fun explicitFromOverridesTheAccount() {
        val (parsed, _) = roundTrip(OutgoingMessage(to = listOf(MailAddressSpec("bob@example.org")), subject = "s", text = "t", from = MailAddressSpec("alias@example.org", "Alias")))
        assertEquals(InternetAddress("alias@example.org", "Alias"), parsed.from.single())
    }

    @Test
    fun attachmentsWithoutBodyGetAnEmptyTextPart() {
        val (parsed, _) = roundTrip(
            OutgoingMessage(
                to = listOf(MailAddressSpec("bob@example.org")),
                subject = "s",
                attachments = listOf(OutgoingAttachment("a.bin", "application/octet-stream", source = BytesAttachmentSource(byteArrayOf(1, 2, 3)))),
            ),
        )
        val mixed = parsed.content as MimeMultipart
        assertEquals(2, mixed.count)
        assertTrue(mixed.getBodyPart(0).isMimeType("text/plain"))
        assertEquals("", (mixed.getBodyPart(0).content as String).trim())
        assertArrayEquals(byteArrayOf(1, 2, 3), (mixed.getBodyPart(1) as MimeBodyPart).inputStream.readBytes())
    }

    @Test
    fun sourcesAreReopenedForEverySerialization() {
        var opened = 0
        val source = object : AttachmentSource {
            override val size: Long = 3
            override fun open() = ByteArrayInputStream(byteArrayOf(7, 8, 9)).also { opened++ }
        }
        val composed = MessageComposer.compose(
            session,
            account,
            OutgoingMessage(to = listOf(MailAddressSpec("bob@example.org")), subject = "s", attachments = listOf(OutgoingAttachment("a.bin", "application/octet-stream", source = source))),
        )
        val first = ByteArrayOutputStream().also { composed.writeTo(it) }.toByteArray()
        val second = ByteArrayOutputStream().also { composed.writeTo(it) }.toByteArray()
        assertArrayEquals(first, second)
        assertTrue("send plus APPEND read the attachment twice: $opened", opened >= 2)
    }
}
