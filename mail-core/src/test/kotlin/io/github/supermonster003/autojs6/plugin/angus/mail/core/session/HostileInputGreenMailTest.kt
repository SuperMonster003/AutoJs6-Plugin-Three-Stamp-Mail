package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.toJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.Hostile
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MimeLeniency
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageArgs
import jakarta.mail.Flags
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Properties

/**
 * The hostile messages of [Hostile] replayed through a server (mail roadmap P6): appended to
 * GreenMail and read back through [MailSession] over IMAP (BODYSTRUCTURE-driven tree, section
 * downloads) and POP3 (`TOP` envelopes, `RETR` parts). UIDs follow the seeding order:
 * 1 recipients, 2 nested, 3 damaged encodings, 4 file names, 5 wide.
 */
class HostileInputGreenMailTest {

    private lateinit var greenMail: GreenMail
    private val sessions = ArrayList<MailSession>()
    private lateinit var imap: MailSession

    @Before
    fun startServer() {
        greenMail = GreenMail(ServerSetupTest.ALL)
        greenMail.start()
        greenMail.setUser(ALICE, ALICE_LOGIN, ALICE_PASSWORD)
        imap = MailSession(alice("imap"), MailSecret(ALICE_PASSWORD)).also { sessions += it }
        val jakarta = Session.getInstance(MimeLeniency.apply(Properties()))
        listOf(Hostile.recipients(20000), Hostile.nested(40), Hostile.damagedEncodings(), Hostile.fileNames(), Hostile.wide(1000)).forEach { raw ->
            val mime = MimeMessage(jakarta, ByteArrayInputStream(raw.toByteArray(StandardCharsets.ISO_8859_1)))
            imap.imap { it.append("INBOX", mime, Flags()) }
        }
    }

    @After
    fun stopServer() {
        sessions.forEach { it.close() }
        greenMail.stop()
    }

    @Test
    fun listingHostileMailOverImapAndPop3StaysWithinTheEnvelope() {
        val listed = imap.listMessages(MessageArgs.list("""{"folder":"INBOX","order":"asc"}"""))
        assertEquals(5, listed.size)
        listed.forEach(::assertFits)
        assertEquals(MailLimits.MAX_RECIPIENTS, listed[0].to.size)
        assertTrue(listed[0].cc.isEmpty())
        assertTrue(listed[1].hasAttachments)
        assertTrue(listed[4].hasAttachments)

        val pop3 = MailSession(alice("pop3"), MailSecret(ALICE_PASSWORD)).also { sessions += it }
        val popped = pop3.listMessages(MessageArgs.list("""{"order":"asc"}""", MailProtocol.POP3))
        assertEquals(5, popped.size)
        popped.forEach(::assertFits)
        assertEquals(MailLimits.MAX_RECIPIENTS, popped[0].to.size)
        val full = pop3.getMessage(MessageArgs.get("""{"uid":${popped[2].uid}}""", MailProtocol.POP3))
        assertEquals("hello world", full.text)
        assertFits(full)
    }

    @Test
    fun damagedEncodingsDoNotCostTheConnection() {
        val doc = imap.getMessage(MessageArgs.get("""{"uid":3}"""))
        assertEquals("hello world", doc.text)
        assertEquals(listOf("2", "3"), doc.attachments.map { it.partId })
        val identity = download("""{"uid":3,"partId":"2"}""")
        assertEquals("identity bytes here", String(identity, StandardCharsets.ISO_8859_1).trim())
        assertTrue(String(download("""{"uid":3,"partId":"3"}"""), StandardCharsets.ISO_8859_1).startsWith("ABC"))
        assertEquals("no reconnect happened", 1, imap.connectCount(MailProtocol.IMAP))
    }

    @Test
    fun deepTreesAreDownloadableAtTheCapAndNotBeyond() {
        val doc = imap.getMessage(MessageArgs.get("""{"uid":2}"""))
        assertNull(doc.text)
        val capped = doc.attachments.single()
        assertEquals(MailLimits.MAX_MIME_DEPTH, capped.partId.split('.').size)
        assertEquals("multipart/mixed", capped.mimeType)
        assertTrue(String(download("""{"uid":2,"partId":"${capped.partId}"}"""), StandardCharsets.ISO_8859_1).contains("deep"))
        assertEquals(MailErrorCode.ATTACHMENT_NOT_FOUND, failure { download("""{"uid":2,"partId":"${capped.partId}.1"}""") }.code)
        assertFits(doc)
    }

    @Test
    fun hostileFileNamesArriveSanitizedOverImap() {
        val doc = imap.getMessage(MessageArgs.get("""{"uid":4}"""))
        val names = doc.attachments.map { it.fileName }
        assertEquals(8, names.size)
        names.forEach { name -> assertFalse(name, name.any { it == '/' || it == '\\' || it < ' ' || it == '\u007f' }) }
        assertEquals(".._.._etc_passwd", names[0])
        assertEquals(".._.._win.ini", names[1])
        assertEquals(".._.._x.txt", names[2])
        assertEquals("nul_byte.txt", names[3])
        assertEquals("evil.exe", names[6])
        assertEquals("image/png", doc.attachments[6].mimeType)
        assertTrue(names[5], names[5].startsWith("part-6"))
    }

    @Test
    fun wideTreesStopAtThePartCapOverImap() {
        val doc = imap.getMessage(MessageArgs.get("""{"uid":5}"""))
        assertEquals(MailLimits.MAX_MIME_PARTS, doc.attachments.size)
        assertFits(doc)
        assertEquals("x", String(download("""{"uid":5,"partId":"${MailLimits.MAX_MIME_PARTS}"}"""), StandardCharsets.ISO_8859_1).trim())
        assertEquals(MailErrorCode.ATTACHMENT_NOT_FOUND, failure { download("""{"uid":5,"partId":"${MailLimits.MAX_MIME_PARTS + 1}"}""") }.code)
    }

    private fun download(args: String): ByteArray {
        val out = ByteArrayOutputStream()
        imap.downloadAttachment(MessageArgs.download(args), out)
        return out.toByteArray()
    }

    private fun assertFits(doc: MessageDocument) {
        val bytes = doc.toJson().toByteArray(StandardCharsets.UTF_8).size
        assertTrue("uid ${doc.uid}: $bytes bytes must fit the response envelope", bytes < MailLimits.MAX_ENVELOPE_BYTES)
    }

    private fun failure(block: () -> Any?): MailException {
        try {
            block()
        } catch (e: MailException) {
            return e
        }
        throw AssertionError("expected a MailException")
    }

    private fun alice(receive: String): MailAccount = MailAccountOptions.parse(
        """{"address":"$ALICE","user":"$ALICE_LOGIN","receive":"$receive",
            "imap":{"host":"$HOST","port":${ServerSetupTest.IMAP.port},"tls":"none"},
            "pop3":{"host":"$HOST","port":${ServerSetupTest.POP3.port},"tls":"none"},
            "smtp":{"host":"$HOST","port":${ServerSetupTest.SMTP.port},"tls":"none"},
            "timeout":{"connect":5000,"read":30000}}""",
        SecretKind.PASSWORD,
    )

    private companion object {
        const val HOST = "127.0.0.1"
        const val ALICE = "alice@localhost"
        const val ALICE_LOGIN = "alice"
        const val ALICE_PASSWORD = "alice-secret"
    }
}
