package io.github.supermonster003.autojs6.plugin.angus.mail.core.message

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.toJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSessionProperties
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.Transfer
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.TransferProgress
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Properties

/**
 * Hostile input (mail roadmap P6, D39): the MIME the mapper and the tree meet in the wild, built
 * in memory from raw bytes. Every case maps to a bounded document or a definite error code,
 * never to an exception out of the mapper, and one message document always fits the response
 * envelope (`MAX_ENVELOPE_BYTES`). `HostileInputGreenMailTest` replays the same messages over
 * IMAP and POP3; `docs/dev/p6-hostile-input.md` records the results.
 */
class HostileInputTest {

    private val session: Session = Session.getInstance(MimeLeniency.apply(Properties()))

    @Test
    fun deeplyNestedMultipartsStopAtTheDepthCapWithoutOverflowing() {
        val leaves = MimeTree.leaves(parse(Hostile.nested(2000)))
        val leaf = leaves.single()
        assertEquals(MailLimits.MAX_MIME_DEPTH, leaf.partId.split('.').size)
        assertEquals("multipart/mixed", leaf.mimeType)
        assertEquals(PartKind.ATTACHMENT, leaf.kind)
        assertTrue("the capped multipart is downloadable as raw bytes", String(leaf.part.inputStream.readBytes(), StandardCharsets.ISO_8859_1).contains("deep"))
        val doc = full(Hostile.nested(2000))
        assertNull(doc.text)
        assertTrue(doc.hasAttachments)
        assertEquals(leaf.partId, doc.attachments.single().partId)
        assertTrue(doc.attachments.single().fileName.startsWith("part-"))
        assertFits(doc)
        assertEquals("a tree below the cap is walked to its text", "deep", full(Hostile.nested(10)).text)
    }

    @Test
    fun wideMultipartsStopAtThePartCap() {
        val message = parse(Hostile.wide(5000))
        val leaves = MimeTree.leaves(message)
        assertEquals(MailLimits.MAX_MIME_PARTS, leaves.size)
        assertEquals("${MailLimits.MAX_MIME_PARTS}", leaves.last().partId)
        assertNull(MimeTree.find(message, "${MailLimits.MAX_MIME_PARTS + 1}"))
        val doc = MessageMapper.full(message, "INBOX", JsonPrimitive(1))
        assertEquals(MailLimits.MAX_MIME_PARTS, doc.attachments.size)
        assertFits(doc)
    }

    @Test
    fun twentyThousandRecipientsAreCappedAndTheEnvelopeFits() {
        val raw = Hostile.recipients(20000)
        val envelope = envelope(raw)
        assertEquals(MailLimits.MAX_RECIPIENTS, envelope.to.size)
        assertEquals("u1@example.org", envelope.to.first().address)
        assertTrue("the address budget is shared by the four lists", envelope.cc.isEmpty() && envelope.bcc.isEmpty() && envelope.replyTo.isEmpty())
        assertEquals("a@example.org", envelope.from?.address)
        assertFits(envelope)
        val headersOnly = MessageMapper.envelopeFromHeaders(parse(raw), "INBOX", JsonPrimitive("pop"))
        assertEquals(MailLimits.MAX_RECIPIENTS, headersOnly.to.size)
        assertFits(headersOnly)
        val doc = full(raw)
        val to = doc.headers.getValue("To").single()
        assertTrue("the To header is cut to the bound (${to.length} chars after unfolding)", to.length in 3000..MailLimits.MAX_HEADER_VALUE_CHARS)
        assertEquals("body", doc.text)
        assertFits(doc)
    }

    @Test
    fun overlongSubjectsHeadersAndIdsAreClamped() {
        val doc = full(Hostile.headerBombs())
        assertEquals(MailLimits.MAX_HEADER_VALUE_CHARS, doc.subject.length)
        assertEquals(MailLimits.MAX_HEADER_VALUE_CHARS, doc.messageId!!.length)
        assertEquals(MailLimits.MAX_HEADER_VALUE_CHARS, doc.inReplyTo!!.length)
        val bytes = doc.headers.entries.sumOf { (name, values) -> values.sumOf { name.toByteArray().size + it.toByteArray().size } }
        assertTrue("headers hold $bytes bytes", bytes <= MailLimits.MAX_HEADERS_BYTES)
        assertTrue("later small headers still fit after a bomb was dropped", doc.headers.containsKey("X-After"))
        assertEquals(MailLimits.MAX_ADDRESS_CHARS, doc.headers.keys.maxOf { it.length })
        assertEquals("body", doc.text)
        assertFits(doc)
    }

    @Test
    fun partsWithoutContentTypeOrBoundaryStayReadable() {
        assertEquals("no content type here", full(Hostile.untyped()).text)
        val untypedPart = full(Hostile.untypedPart())
        assertEquals("untyped part", untypedPart.text)
        assertTrue(untypedPart.html!!.contains("<p>h</p>"))
        assertEquals("guessed boundary", full(Hostile.missingBoundaryParameter()).text)
        val empty = full(Hostile.emptyMultipart())
        assertNull(empty.text)
        assertTrue(empty.attachments.isEmpty())
        assertFalse(empty.hasAttachments)
        val garbage = full(Hostile.unparsableMultipart())
        assertNull(garbage.text)
        assertEquals("multipart/mixed", garbage.attachments.single().mimeType)
        assertEquals("1", garbage.attachments.single().partId)
    }

    @Test
    fun invalidBase64AndUnknownEncodingsDecodeLeniently() {
        val message = parse(Hostile.damagedEncodings())
        val doc = MessageMapper.full(message, "INBOX", JsonPrimitive(1))
        assertEquals("hello world", doc.text)
        assertEquals(listOf("2", "3"), doc.attachments.map { it.partId })
        val identity = MimeTree.find(message, "2")!!
        val out = ByteArrayOutputStream()
        Transfer.copy(identity.part.inputStream, out, null, TransferProgress.NONE)
        assertEquals("identity bytes here", String(out.toByteArray(), StandardCharsets.ISO_8859_1).trim())
        val short = MimeTree.find(message, "3")!!
        assertTrue(String(BodyExtractor.bytes(short.part, 1024)!!, StandardCharsets.ISO_8859_1).startsWith("ABC"))
    }

    @Test
    fun recursiveRfc822MessagesAreOneLeafWhateverTheirDepth() {
        val message = parse(Hostile.rfc822(100))
        val leaves = MimeTree.leaves(message)
        assertEquals(listOf("1", "2"), leaves.map { it.partId })
        assertEquals("message/rfc822", leaves[1].mimeType)
        assertTrue(String(leaves[1].part.inputStream.readBytes(), StandardCharsets.ISO_8859_1).contains("Subject: innermost"))
        val doc = MessageMapper.full(message, "INBOX", JsonPrimitive(1))
        assertEquals("see inside", doc.text)
        assertEquals("part-2.eml", doc.attachments.single().fileName)
        assertFits(doc)
    }

    @Test
    fun hostileFileNamesAreSanitizedAndBounded() {
        val doc = full(Hostile.fileNames())
        val names = doc.attachments.map { it.fileName }
        assertEquals(8, names.size)
        names.forEach { name -> assertFalse(name, name.any { it == '/' || it == '\\' || it < ' ' || it == '\u007f' }) }
        assertEquals(".._.._etc_passwd", names[0])
        assertEquals(".._.._win.ini", names[1])
        assertEquals(".._.._x.txt", names[2])
        assertEquals("nul_byte.txt", names[3])
        assertEquals(OutgoingMessageParser.MAX_FILE_NAME_LENGTH, names[4].length)
        assertTrue(names[5], names[5].startsWith("part-6"))
        assertEquals("evil.exe", names[6])
        assertEquals("image/png", doc.attachments[6].mimeType)
        assertEquals("only-type.png", names[7])
        assertEquals("image/png", doc.attachments[7].mimeType)
        assertEquals("attachment", OutgoingMessageParser.sanitizeFileName(".."))
        assertEquals("attachment", OutgoingMessageParser.sanitizeFileName("..."))
        assertEquals("attachment", OutgoingMessageParser.sanitizeFileName(" "))
        assertEquals(".env", OutgoingMessageParser.sanitizeFileName(".env"))
        assertFits(doc)
    }

    @Test
    fun declaredSizesAreHintsAndTheTransferCountsRealBytes() {
        val payload = ByteArray(1000) { (it % 251).toByte() }
        val message = parse(Hostile.base64Attachment(payload))
        val doc = MessageMapper.full(message, "INBOX", JsonPrimitive(1))
        val attachment = doc.attachments.single()
        assertTrue("the document carries the encoded size (${attachment.size})", attachment.size > payload.size)
        val leaf = MimeTree.find(message, attachment.partId)!!
        val out = ByteArrayOutputStream()
        assertEquals(payload.size.toLong(), Transfer.copy(leaf.part.inputStream, out, null, TransferProgress.NONE))
        assertTrue(payload.contentEquals(out.toByteArray()))
        try {
            Transfer.copy(leaf.part.inputStream, ByteArrayOutputStream(), null, TransferProgress.NONE, limit = 500)
            fail("the transfer limit must stop the copy")
        } catch (e: MailException) {
            assertEquals(MailErrorCode.LIMIT_EXCEEDED, e.code)
            assertFalse(e.retryable)
        }
    }

    @Test
    fun invalidUtf8AndUnknownCharsetsNeverThrow() {
        val utf8 = full(Hostile.subject("=?UTF-8?B?/v/A?="))
        assertTrue(utf8.subject, utf8.subject.isNotEmpty() && utf8.subject.contains('�'))
        val gb = full(Hostile.subject("=?GB2312?B?gIA=?="))
        assertTrue(gb.subject, gb.subject.isNotEmpty())
        assertEquals("ÿþý raw", full(Hostile.subject("ÿþý raw")).subject)
        assertEquals("=?UNKNOWN-CS?Q?abc?=", full(Hostile.subject("=?UNKNOWN-CS?Q?abc?=")).subject)
        val name = full(Hostile.from("\"ÿþ\" <x@example.org>"))
        assertEquals("x@example.org", name.from?.address)
        assertNotNull(name.from?.name)
        val charset = full(Hostile.charset("../../x"))
        assertEquals("charset fallback", charset.text)
        assertEquals("charset fallback", full(Hostile.charset("notacharset")).text)
        listOf(utf8, gb, name, charset).forEach { doc -> assertTrue(Json.parseToJsonElement(doc.toJson()) is JsonObject) }
    }

    @Test
    fun oversizedBodiesStayOutOfTheInlineDocument() {
        val size = MailLimits.MAX_INLINE_BODY_BYTES + 50 * 1024
        val message = parse(Hostile.bigText(size))
        val doc = MessageMapper.full(message, "INBOX", JsonPrimitive(1), includeRaw = true)
        assertTrue(doc.bodyTruncated)
        assertNull(doc.text)
        assertEquals("1", doc.bodyParts.single().partId)
        assertTrue(doc.rawTruncated)
        assertNull(doc.raw)
        val part = MimeTree.leaves(message).single().part
        assertNull(BodyExtractor.bytes(part, MailLimits.MAX_INLINE_BODY_BYTES.toLong()))
        assertEquals(size, BodyExtractor.bytes(part, size + 1024L)!!.size)
        assertFits(doc)
    }

    @Test
    fun theLeniencySwitchesAreInstalledForTheProcessAndTheTestJvm() {
        MimeLeniency.install()
        MimeLeniency.PROPERTIES.forEach { (key, value) -> assertEquals(key, value, System.getProperty(key)) }
        assertTrue(MimeLeniency.STATIC_KEYS.all { it in MimeLeniency.PROPERTIES })
        assertEquals(MimeLeniency.SESSION_KEYS, MimeLeniency.apply(Properties()).keys)
        val account = MailAccountOptions.parse("""{"provider":"qq","address":"alice@qq.com"}""", SecretKind.PASSWORD)
        MailProtocol.entries.forEach { protocol ->
            val properties = MailSessionProperties.build(account, protocol)
            MimeLeniency.SESSION_KEYS.forEach { key -> assertEquals("$protocol $key", "true", properties.getProperty(key)) }
        }
    }

    private fun parse(raw: String): MimeMessage = MimeMessage(session, ByteArrayInputStream(raw.toByteArray(StandardCharsets.ISO_8859_1)))

    private fun full(raw: String): MessageDocument = MessageMapper.full(parse(raw), "INBOX", JsonPrimitive(1))

    private fun envelope(raw: String): MessageDocument = MessageMapper.envelope(parse(raw), "INBOX", JsonPrimitive(1))

    private fun assertFits(doc: MessageDocument) {
        val bytes = doc.toJson().toByteArray(StandardCharsets.UTF_8).size
        assertTrue("one message document must fit the response envelope: $bytes bytes", bytes < MailLimits.MAX_ENVELOPE_BYTES)
    }
}

/** The hostile messages as raw RFC 822 text (ISO-8859-1 bytes); shared with the GreenMail replay. */
object Hostile {

    private const val HEAD = "From: a@example.org\r\nTo: b@example.org\r\nDate: Thu, 18 Sep 2026 10:00:00 +0800\r\n"

    fun nested(depth: Int): String {
        val sb = StringBuilder(HEAD).append("Subject: nested $depth\r\nContent-Type: multipart/mixed; boundary=\"b0\"\r\n\r\n")
        for (level in 0 until depth) sb.append("--b$level\r\nContent-Type: multipart/mixed; boundary=\"b${level + 1}\"\r\n\r\n")
        sb.append("--b$depth\r\nContent-Type: text/plain\r\n\r\ndeep\r\n--b$depth--\r\n")
        for (level in depth - 1 downTo 0) sb.append("--b$level--\r\n")
        return sb.toString()
    }

    fun wide(parts: Int): String {
        val sb = StringBuilder(HEAD).append("Subject: wide $parts\r\nContent-Type: multipart/mixed; boundary=\"w\"\r\n\r\n")
        repeat(parts) { index -> sb.append("--w\r\nContent-Type: application/octet-stream\r\nContent-Disposition: attachment; filename=\"p$index.bin\"\r\n\r\nx\r\n") }
        return sb.append("--w--\r\n").toString()
    }

    fun recipients(count: Int): String {
        val to = (1..count).joinToString(",\r\n ") { "u$it@example.org" }
        val cc = (1..count).joinToString(",\r\n ") { "c$it@example.org" }
        return "From: a@example.org\r\nTo: $to\r\nCc: $cc\r\nBcc: $cc\r\nReply-To: $to\r\nSubject: recipients $count\r\n\r\nbody\r\n"
    }

    fun headerBombs(): String {
        val subject = "s".repeat(1024 * 1024)
        val id = "<" + "m".repeat(10000) + "@example.org>"
        val bombs = (1..3000).joinToString("") { "X-Bomb-$it: " + "v".repeat(1000) + "\r\n" }
        val longName = "X-" + "n".repeat(2000) + ": short\r\n"
        return "From: a@example.org\r\nSubject: $subject\r\nMessage-ID: $id\r\nIn-Reply-To: $id\r\n$bombs${longName}X-After: fits\r\n\r\nbody\r\n"
    }

    fun untyped(): String = "From: a@example.org\r\nSubject: untyped\r\n\r\nno content type here\r\n"

    fun untypedPart(): String = HEAD + "Subject: untyped part\r\nContent-Type: multipart/alternative; boundary=\"u\"\r\n\r\n--u\r\n\r\nuntyped part\r\n--u\r\nContent-Type: text/html\r\n\r\n<p>h</p>\r\n--u--\r\n"

    fun missingBoundaryParameter(): String = HEAD + "Subject: no boundary\r\nContent-Type: multipart/mixed\r\n\r\n--zz\r\nContent-Type: text/plain\r\n\r\nguessed boundary\r\n--zz--\r\n"

    fun emptyMultipart(): String = HEAD + "Subject: empty\r\nContent-Type: multipart/mixed; boundary=\"e\"\r\n\r\n"

    fun unparsableMultipart(): String = HEAD + "Subject: garbage\r\nContent-Type: multipart/mixed; boundary=\"g\"\r\n\r\njust text, no boundary lines at all\r\n"

    fun damagedEncodings(): String = HEAD + "Subject: damaged\r\nContent-Type: multipart/mixed; boundary=\"x\"\r\n\r\n" +
        "--x\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Transfer-Encoding: base64\r\n\r\naGVsbG8g!!!d29ybGQ=\r\n" +
        "--x\r\nContent-Type: application/octet-stream\r\nContent-Transfer-Encoding: x-unknown\r\nContent-Disposition: attachment; filename=\"raw.bin\"\r\n\r\nidentity bytes here\r\n" +
        "--x\r\nContent-Type: application/octet-stream\r\nContent-Transfer-Encoding: base64\r\nContent-Disposition: attachment; filename=\"short.bin\"\r\n\r\nQUJD RA\r\n" +
        "--x--\r\n"

    fun rfc822(depth: Int): String {
        fun inner(level: Int): String = if (level == 0) "Content-Type: text/plain\r\nSubject: innermost\r\n\r\ncore\r\n" else "Content-Type: message/rfc822\r\nSubject: level $level\r\n\r\n" + inner(level - 1)
        return HEAD + "Subject: rfc822 $depth\r\nContent-Type: multipart/mixed; boundary=\"r\"\r\n\r\n--r\r\nContent-Type: text/plain\r\n\r\nsee inside\r\n--r\r\n" + inner(depth) + "--r--\r\n"
    }

    fun fileNames(): String {
        val parts = listOf(
            "Content-Type: application/octet-stream\r\nContent-Disposition: attachment; filename=\"../../etc/passwd\"",
            "Content-Type: application/octet-stream\r\nContent-Disposition: attachment; filename*=UTF-8''..%5C..%5Cwin.ini",
            "Content-Type: application/octet-stream\r\nContent-Disposition: attachment; filename*=UTF-8''..%2F..%2Fx.txt",
            "Content-Type: application/octet-stream\r\nContent-Disposition: attachment; filename=\"nul\u0000byte.txt\"",
            "Content-Type: application/octet-stream\r\nContent-Disposition: attachment; filename=\"" + "n".repeat(2000) + ".txt\"",
            "Content-Type: application/octet-stream\r\nContent-Disposition: attachment; filename=\"..\"",
            "Content-Type: image/png; name=\"pic.png\"\r\nContent-Disposition: attachment; filename=\"evil.exe\"",
            "Content-Type: image/png; name=\"only-type.png\"\r\nContent-Disposition: attachment",
        )
        return HEAD + "Subject: file names\r\nContent-Type: multipart/mixed; boundary=\"f\"\r\n\r\n" + parts.joinToString("") { "--f\r\n$it\r\n\r\nx\r\n" } + "--f--\r\n"
    }

    fun base64Attachment(payload: ByteArray): String =
        HEAD + "Subject: sizes\r\nContent-Type: multipart/mixed; boundary=\"s\"\r\n\r\n--s\r\nContent-Type: application/octet-stream\r\nContent-Transfer-Encoding: base64\r\nContent-Disposition: attachment; filename=\"blob.bin\"\r\n\r\n" +
            Base64.getMimeEncoder(76, "\r\n".toByteArray()).encodeToString(payload) + "\r\n--s--\r\n"

    fun subject(subject: String): String = "From: a@example.org\r\nSubject: $subject\r\n\r\nbody\r\n"

    fun from(from: String): String = "From: $from\r\nSubject: from\r\n\r\nbody\r\n"

    fun charset(charset: String): String = "From: a@example.org\r\nSubject: charset\r\nContent-Type: text/plain; charset=\"$charset\"\r\n\r\ncharset fallback\r\n"

    fun bigText(bytes: Int): String = "From: a@example.org\r\nSubject: big\r\nContent-Type: text/plain\r\n\r\n" + "t".repeat(bytes)
}
