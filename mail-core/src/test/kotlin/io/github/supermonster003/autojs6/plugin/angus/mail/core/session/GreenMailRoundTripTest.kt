package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.GreenMailUtil
import com.icegreen.greenmail.util.ServerSetup
import com.icegreen.greenmail.util.ServerSetupTest
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.AuthMethod
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailTimeouts
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.OutgoingMessage
import jakarta.mail.AuthenticationFailedException
import jakarta.mail.Folder
import jakarta.mail.MessagingException
import jakarta.mail.event.MessageCountAdapter
import jakarta.mail.event.MessageCountEvent
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMultipart
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Roadmap P0.2 feasibility spike against a local GreenMail server: one message goes out through
 * SMTP and comes back through IMAP and POP3, on plain, implicit TLS, and STARTTLS endpoints, with
 * password and XOAUTH2 authentication, and IMAP IDLE delivers a new-mail event.
 */
class GreenMailRoundTripTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var greenMail: GreenMail

    @Before
    fun startServer() {
        greenMail = GreenMail(ServerSetupTest.ALL)
        greenMail.start()
        greenMail.setUser(ALICE, ALICE_LOGIN, ALICE_PASSWORD)
        greenMail.setUser(BOB, BOB_LOGIN, BOB_PASSWORD)
    }

    @After
    fun stopServer() {
        greenMail.stop()
    }

    @Test
    fun smtpSendsAMultipartMessageThatImapListsAndParses() {
        val attachment = temporaryFolder.newFile("report.csv").apply { writeText("id,total\n1,42\n") }
        val messageId = SmtpSender(alice(TlsMode.NONE), MailSecret(ALICE_PASSWORD)).use { sender ->
            sender.send(
                OutgoingMessage.simple(
                    to = listOf(BOB),
                    subject = SUBJECT,
                    text = "See the attachment",
                    html = "<p>See the <b>attachment</b></p>",
                    attachments = listOf(attachment),
                ),
            ).messageId
        }
        assertTrue(messageId.startsWith("<") && messageId.endsWith(">"))
        assertTrue(greenMail.waitForIncomingEmail(5_000, 1))

        ImapMailbox.connect(bob(TlsMode.NONE), MailSecret(BOB_PASSWORD)).use { mailbox ->
            val summaries = mailbox.listInbox(5)
            assertEquals(1, summaries.size)
            val summary = summaries.single()
            assertEquals(SUBJECT, summary.subject)
            assertEquals(listOf(ALICE), summary.from)
            assertEquals(listOf(BOB), summary.to)
            assertNotNull(summary.uid)
            assertFalse("listing must not mark the message as read", summary.seen)
            assertTrue(summary.size > 0)
            assertNotNull(summary.sentAtMillis)

            mailbox.withFolder(ImapMailbox.INBOX, Folder.READ_ONLY) { folder ->
                val message = folder.getMessage(1)
                val mixed = message.content as MimeMultipart
                assertEquals(2, mixed.count)
                val alternative = (mixed.getBodyPart(0) as MimeBodyPart).content as MimeMultipart
                assertEquals("See the attachment", (alternative.getBodyPart(0).content as String).trim())
                assertTrue(alternative.getBodyPart(1).isMimeType("text/html"))
                val part = mixed.getBodyPart(1) as MimeBodyPart
                assertEquals("report.csv", part.fileName)
                assertEquals("id,total\n1,42\n", part.inputStream.readBytes().toString(Charsets.UTF_8))
            }
            assertFalse(mailbox.listInbox(5).single().seen)
        }
    }

    @Test
    fun pop3ListsTheSameMessage() {
        sendPlainText(ServerSetupTest.SMTP)
        Pop3Mailbox.connect(bob(TlsMode.NONE), MailSecret(BOB_PASSWORD)).use { mailbox ->
            val summary = mailbox.listInbox(5).single()
            assertEquals(SUBJECT, summary.subject)
            assertEquals(listOf(ALICE), summary.from)
            assertEquals(null, summary.uid)
        }
    }

    @Test
    fun emptyMailboxesListNothing() {
        ImapMailbox.connect(bob(TlsMode.NONE), MailSecret(BOB_PASSWORD)).use { assertTrue(it.listInbox(5).isEmpty()) }
        Pop3Mailbox.connect(bob(TlsMode.NONE), MailSecret(BOB_PASSWORD)).use { assertTrue(it.listInbox(5).isEmpty()) }
    }

    @Test
    fun implicitTlsEndpointsWorkWithTrustAll() {
        SmtpSender(alice(TlsMode.SSL), MailSecret(ALICE_PASSWORD)).use { it.send(OutgoingMessage.simple(listOf(BOB), SUBJECT, "over TLS")) }
        assertTrue(greenMail.waitForIncomingEmail(5_000, 1))
        ImapMailbox.connect(bob(TlsMode.SSL), MailSecret(BOB_PASSWORD)).use { assertEquals(SUBJECT, it.listInbox(1).single().subject) }
        Pop3Mailbox.connect(bob(TlsMode.SSL), MailSecret(BOB_PASSWORD)).use { assertEquals(SUBJECT, it.listInbox(1).single().subject) }
    }

    @Test
    fun starttlsNeverDowngradesToPlainText() {
        // GreenMail 2.1.13 does not offer STARTTLS on its plain ports, which makes it the ideal
        // hostile server: an account that requires STARTTLS must refuse to continue in the clear.
        // Successful STARTTLS sessions are verified against real providers (port 587 / 143 / 110).
        assertStarttlsRefused("SMTP") {
            SmtpSender(alice(TlsMode.STARTTLS), MailSecret(ALICE_PASSWORD)).use { it.send(OutgoingMessage.simple(listOf(BOB), SUBJECT, "over STARTTLS")) }
        }
        assertStarttlsRefused("IMAP") { ImapMailbox.connect(bob(TlsMode.STARTTLS), MailSecret(BOB_PASSWORD)).close() }
        assertStarttlsRefused("POP3") { Pop3Mailbox.connect(bob(TlsMode.STARTTLS), MailSecret(BOB_PASSWORD)).close() }
        assertEquals("no message may reach the server over a refused upgrade", 0, greenMail.receivedMessages.size)
    }

    private fun assertStarttlsRefused(protocol: String, connect: () -> Unit) {
        try {
            connect()
            fail("$protocol must not continue without the required STARTTLS upgrade")
        } catch (expected: MessagingException) {
            // SMTP and IMAP report "STARTTLS", POP3 reports the command name "STLS" (RFC 2595).
            val message = expected.message.orEmpty()
            assertTrue("$protocol: $message", message.contains("STARTTLS", ignoreCase = true) || message.contains("STLS", ignoreCase = true))
        }
    }

    @Test
    fun wrongPasswordIsRejectedWithoutFallingBackToAnotherMechanism() {
        try {
            ImapMailbox.connect(bob(TlsMode.NONE), MailSecret("not-the-password"))
            fail("IMAP login with a wrong password must fail")
        } catch (expected: AuthenticationFailedException) {
            assertFalse(expected.message.orEmpty().contains("not-the-password"))
        }
        try {
            SmtpSender(alice(TlsMode.NONE), MailSecret("not-the-password")).use { it.send(OutgoingMessage.simple(listOf(BOB), SUBJECT, "x")) }
            fail("SMTP login with a wrong password must fail")
        } catch (expected: AuthenticationFailedException) {
            assertFalse(expected.message.orEmpty().contains("not-the-password"))
        }
    }

    @Test
    fun xoauth2AuthenticatesImapPop3AndSmtp() {
        // GreenMail validates the XOAUTH2 bearer token against the user's password.
        val token = "ya29.spike-access-token"
        greenMail.setUser(CAROL, CAROL_LOGIN, token)
        val carol = account(CAROL, CAROL_LOGIN, TlsMode.NONE).copy(auth = AuthMethod.XOAUTH2)

        SmtpSender(carol, MailSecret(token)).use { it.send(OutgoingMessage.simple(listOf(CAROL), SUBJECT, "to myself over XOAUTH2")) }
        assertTrue(greenMail.waitForIncomingEmail(5_000, 1))
        ImapMailbox.connect(carol, MailSecret(token)).use { assertEquals(SUBJECT, it.listInbox(1).single().subject) }
        Pop3Mailbox.connect(carol, MailSecret(token)).use { assertEquals(SUBJECT, it.listInbox(1).single().subject) }

        try {
            ImapMailbox.connect(carol, MailSecret("expired-token"))
            fail("an invalid token must be rejected")
        } catch (expected: AuthenticationFailedException) {
            assertFalse(expected.message.orEmpty().contains("expired-token"))
        }
    }

    @Test
    fun imapIdleDeliversNewMessageEvents() {
        ImapMailbox.connect(bob(TlsMode.NONE), MailSecret(BOB_PASSWORD)).use { mailbox ->
            assertTrue("GreenMail must advertise IDLE for the JVM watch tests of roadmap P4", mailbox.hasCapability("IDLE"))
            mailbox.withFolder(ImapMailbox.INBOX, Folder.READ_ONLY) { folder ->
                val added = CountDownLatch(1)
                folder.addMessageCountListener(object : MessageCountAdapter() {
                    override fun messagesAdded(event: MessageCountEvent) {
                        added.countDown()
                    }
                })
                val idler = Thread {
                    // idle(true) returns after the first untagged response, i.e. the EXISTS for the new message.
                    folder.idle(true)
                }.apply { start() }
                Thread.sleep(500)
                sendPlainText(ServerSetupTest.SMTP)
                assertTrue("IDLE did not report the new message", added.await(10, TimeUnit.SECONDS))
                idler.join(10_000)
                assertFalse("the IDLE call must return once the event is delivered", idler.isAlive)
                assertEquals(1, folder.messageCount)
            }
        }
    }

    private fun sendPlainText(setup: ServerSetup) {
        GreenMailUtil.sendTextEmail(BOB, ALICE, SUBJECT, "plain text", setup)
        assertTrue(greenMail.waitForIncomingEmail(5_000, 1))
    }

    private fun alice(tls: TlsMode) = account(ALICE, ALICE_LOGIN, tls)

    private fun bob(tls: TlsMode) = account(BOB, BOB_LOGIN, tls)

    private fun account(address: String, login: String, tls: TlsMode): MailAccount {
        val implicit = tls == TlsMode.SSL
        return MailAccount(
            address = address,
            username = login,
            imap = MailEndpoint(HOST, (if (implicit) ServerSetupTest.IMAPS else ServerSetupTest.IMAP).port, tls),
            pop3 = MailEndpoint(HOST, (if (implicit) ServerSetupTest.POP3S else ServerSetupTest.POP3).port, tls),
            smtp = MailEndpoint(HOST, (if (implicit) ServerSetupTest.SMTPS else ServerSetupTest.SMTP).port, tls),
            // GreenMail presents a self-signed certificate.
            trustAll = tls != TlsMode.NONE,
            timeouts = MailTimeouts.uniform(10_000),
        )
    }

    private companion object {
        const val HOST = "127.0.0.1"
        const val ALICE = "alice@localhost"
        const val ALICE_LOGIN = "alice"
        const val ALICE_PASSWORD = "alice-secret"
        const val BOB = "bob@localhost"
        const val BOB_LOGIN = "bob"
        const val BOB_PASSWORD = "bob-secret"
        const val CAROL = "carol@localhost"
        const val CAROL_LOGIN = "carol"
        const val SUBJECT = "测试报表 Report"
    }
}
