package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.ProviderPreset
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SendResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.toJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.BytesAttachmentSource
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MailAddressSpec
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.OutgoingAttachment
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.OutgoingMessage
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.OutgoingMessageParser
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.Priority
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** Roadmap P2.2: `mail.send` and `messages.append` through [MailSession] against GreenMail. */
class SmtpSendGreenMailTest {

    private lateinit var greenMail: GreenMail
    private val sessions = ArrayList<MailSession>()

    @Before
    fun startServer() {
        greenMail = GreenMail(ServerSetupTest.ALL)
        greenMail.start()
        greenMail.setUser(ALICE, ALICE_LOGIN, ALICE_PASSWORD)
        greenMail.setUser(BOB, BOB_LOGIN, BOB_PASSWORD)
        greenMail.setUser(CAROL, CAROL_LOGIN, CAROL_PASSWORD)
    }

    @After
    fun stopServer() {
        sessions.forEach { it.close() }
        greenMail.stop()
    }

    @Test
    fun sendDeliversToEveryRecipientKindWithoutLeakingBcc() {
        val result = session(alice()).send(
            OutgoingMessage(
                to = listOf(MailAddressSpec(BOB, "Bob 王")),
                cc = listOf(MailAddressSpec(CAROL)),
                bcc = listOf(MailAddressSpec(ALICE)),
                subject = "群发 Broadcast",
                text = "hello",
                priority = Priority.LOW,
            ),
        )
        assertTrue(result.messageId.startsWith("<"))
        assertEquals(listOf(BOB, CAROL, ALICE), result.accepted)
        assertTrue(result.rejected.isEmpty())
        assertFalse("no preset: nothing is filed by default", result.savedToSent)
        assertEquals(SendResult.SENT_COPY_NONE, result.sentCopy)
        assertTrue(greenMail.waitForIncomingEmail(5_000, 3))
        val received = greenMail.receivedMessages
        assertEquals(3, received.size)
        received.forEach { message ->
            assertEquals("群发 Broadcast", message.subject)
            assertNull("Bcc must not appear in delivered headers", message.getHeader("Bcc"))
            assertEquals("5", message.getHeader("X-Priority", null))
        }
        assertTrue(inbox(bob(), BOB_PASSWORD).single().subject == "群发 Broadcast")
        assertTrue(inbox(carol(), CAROL_PASSWORD).single().subject == "群发 Broadcast")
        assertTrue(inbox(alice(), ALICE_PASSWORD).single().subject == "群发 Broadcast")
    }

    @Test
    fun richMessageArrivesWithInlineImageAndAttachment() {
        val png = ByteArray(2048) { (it * 13).toByte() }
        val csv = "id,total\r\n1,42\r\n".toByteArray()
        val message = OutgoingMessageParser.parse(
            """{
                "to": "$BOB",
                "subject": "报表 Report",
                "text": "see the logo",
                "html": "<p>see <img src=\"cid:logo\"></p>",
                "attachments": [
                    {"descriptorIndex": 0, "fileName": "logo.png", "contentId": "logo"},
                    {"descriptorIndex": 1, "fileName": "报表 2026.csv", "mimeType": "text/csv"}
                ],
                "headers": {"X-Campaign": "spring"},
                "inReplyTo": "<parent@example.org>"
            }""",
            listOf(BytesAttachmentSource(png), BytesAttachmentSource(csv)),
        )
        session(alice()).send(message)
        assertTrue(greenMail.waitForIncomingEmail(5_000, 1))
        val received = greenMail.receivedMessages.single()
        assertEquals("报表 Report", received.subject)
        assertEquals("spring", received.getHeader("X-Campaign", null))
        assertEquals("<parent@example.org>", received.getHeader("In-Reply-To", null))
        val mixed = received.content as MimeMultipart
        assertEquals(2, mixed.count)
        val related = mixed.getBodyPart(0).content as MimeMultipart
        assertTrue(related.contentType.startsWith("multipart/related"))
        val logo = related.getBodyPart(1) as MimeBodyPart
        assertEquals("<logo>", logo.contentID)
        assertArrayEquals(png, logo.inputStream.readBytes())
        val attachment = mixed.getBodyPart(1) as MimeBodyPart
        assertEquals("报表 2026.csv", attachment.fileName)
        assertArrayEquals(csv, attachment.inputStream.readBytes())
    }

    @Test
    fun saveToSentAppendsACopyWhenRequested() {
        createFolder(bob(), BOB_PASSWORD, "Sent")
        val result = session(bob()).send(OutgoingMessage(to = listOf(MailAddressSpec(ALICE)), subject = "kept", text = "copy me"), saveToSent = true)
        assertTrue(result.toJson(), result.savedToSent)
        assertEquals(SendResult.SENT_COPY_APPENDED, result.sentCopy)
        assertEquals("Sent", result.sentFolder)
        assertNull(result.saveError)
        assertTrue(greenMail.waitForIncomingEmail(5_000, 1))
        val copies = messages(bob(), BOB_PASSWORD, "Sent")
        assertEquals(1, copies.size)
        assertEquals("kept", copies.single().subject)
        assertTrue("the copy is filed as read", copies.single().flags.contains(Flags.Flag.SEEN))
        assertEquals(result.messageId, copies.single().messageID)
    }

    @Test
    fun saveToSentFollowsThePresetByDefault() {
        createFolder(bob(), BOB_PASSWORD, "Outbox Copies")
        val manual = ProviderPreset(id = "test", name = "Test", smtp = null, authHint = "x", autoSavesSent = false, sentFolder = "Outbox Copies", docsUrl = "https://example.org")
        val filed = session(bob().copy(provider = manual)).send(OutgoingMessage(to = listOf(MailAddressSpec(ALICE)), subject = "auto", text = "x"))
        assertTrue(filed.savedToSent)
        assertEquals("Outbox Copies", filed.sentFolder)
        assertEquals(1, messages(bob(), BOB_PASSWORD, "Outbox Copies").size)

        val automatic = manual.copy(autoSavesSent = true)
        val skipped = session(bob().copy(provider = automatic)).send(OutgoingMessage(to = listOf(MailAddressSpec(ALICE)), subject = "auto2", text = "x"))
        assertFalse(skipped.savedToSent)
        assertEquals(SendResult.SENT_COPY_SERVER, skipped.sentCopy)
        assertNull(skipped.sentFolder)
        assertEquals(1, messages(bob(), BOB_PASSWORD, "Outbox Copies").size)

        // Even an explicit request never duplicates the copy a provider files itself (QQ rejects it, Gmail doubles it).
        val forcedOnAutomatic = session(bob().copy(provider = automatic)).send(OutgoingMessage(to = listOf(MailAddressSpec(ALICE)), subject = "auto2b", text = "x"), saveToSent = true)
        assertFalse(forcedOnAutomatic.savedToSent)
        assertEquals(SendResult.SENT_COPY_SERVER, forcedOnAutomatic.sentCopy)
        assertEquals(1, messages(bob(), BOB_PASSWORD, "Outbox Copies").size)

        val forcedOff = session(bob().copy(provider = manual)).send(OutgoingMessage(to = listOf(MailAddressSpec(ALICE)), subject = "auto3", text = "x"), saveToSent = false)
        assertFalse(forcedOff.savedToSent)
        assertEquals(SendResult.SENT_COPY_NONE, forcedOff.sentCopy)
        assertEquals(1, messages(bob(), BOB_PASSWORD, "Outbox Copies").size)
    }

    @Test
    fun missingSentFolderIsReportedNotCreated() {
        val result = session(carol()).send(OutgoingMessage(to = listOf(MailAddressSpec(ALICE)), subject = "nowhere", text = "x"), saveToSent = true)
        assertFalse(result.savedToSent)
        assertEquals(SendResult.SENT_COPY_NONE, result.sentCopy)
        assertNull(result.sentFolder)
        assertNull(result.saveError)
        assertTrue(greenMail.waitForIncomingEmail(5_000, 1))
        assertFalse(ImapMailbox.connect(carol(), MailSecret(CAROL_PASSWORD)).use { it.folderExists("Sent") })
    }

    @Test
    fun sendOnlyAccountsSkipTheCopyAndStillSend() {
        val sendOnly = MailAccountOptions.parse("""{"address":"$ALICE","user":"$ALICE_LOGIN","smtp":{"host":"$HOST","port":${ServerSetupTest.SMTP.port},"tls":"none"}}""", SecretKind.PASSWORD)
        val result = session(sendOnly, ALICE_PASSWORD).send(OutgoingMessage(to = listOf(MailAddressSpec(BOB)), subject = "only smtp", text = "x"), saveToSent = true)
        assertFalse(result.savedToSent)
        assertEquals(SendResult.SENT_COPY_NONE, result.sentCopy)
        assertTrue(greenMail.waitForIncomingEmail(5_000, 1))
    }

    @Test
    fun appendStoresADraftAndReturnsItsUid() {
        createFolder(bob(), BOB_PASSWORD, "Drafts")
        val args = OutgoingMessageParser.parseAppendArgs(
            """{"folder": "Drafts", "message": {"to": "$ALICE", "subject": "draft 草稿", "text": "unfinished"}, "flags": ["draft", "seen"]}""",
            emptyList(),
        )
        val result = session(bob()).append(args.folder, args.message, args.flags)
        assertEquals("Drafts", result.folder)
        val uidplus = ImapMailbox.connect(bob(), MailSecret(BOB_PASSWORD)).use { it.hasCapability("UIDPLUS") }
        if (uidplus) assertNotNull("UIDPLUS servers return the new UID", result.uid) else assertNull(result.uid)
        val stored = messages(bob(), BOB_PASSWORD, "Drafts").single()
        assertEquals("draft 草稿", stored.subject)
        assertTrue(stored.flags.contains(Flags.Flag.DRAFT) && stored.flags.contains(Flags.Flag.SEEN))
        assertTrue("APPEND never delivers to the recipient", inbox(alice(), ALICE_PASSWORD).isEmpty())
    }

    @Test
    fun appendToAMissingFolderFailsWithFolderNotFound() {
        try {
            session(bob()).append("Nope", OutgoingMessage(to = listOf(MailAddressSpec(ALICE)), subject = "x", text = "x"), Flags())
            fail("a missing folder must be reported")
        } catch (e: MailException) {
            assertEquals(e.message, MailErrorCode.FOLDER_NOT_FOUND, e.code)
            assertFalse(e.retryable)
        }
    }

    @Test
    fun invalidRecipientsFailBeforeAnythingIsSent() {
        try {
            session(alice()).send(OutgoingMessageParser.parse("""{"to": "nobody", "subject": "x"}""", emptyList()))
            fail("an invalid address must be rejected")
        } catch (e: MailException) {
            assertEquals(MailErrorCode.INVALID_ARGUMENT, e.code)
        }
        assertEquals(0, greenMail.receivedMessages.size)
    }

    private fun inbox(account: MailAccount, password: String) = ImapMailbox.connect(account, MailSecret(password)).use { it.listInbox(5) }

    private class Stored(val subject: String?, val flags: Flags, val messageID: String?)

    private fun messages(account: MailAccount, password: String, folder: String): List<Stored> =
        ImapMailbox.connect(account, MailSecret(password)).use { mailbox ->
            mailbox.withFolder(folder, Folder.READ_ONLY) { f -> f.messages.map { it as MimeMessage }.map { Stored(it.subject, it.flags, it.messageID) } }
        }

    private fun createFolder(account: MailAccount, password: String, name: String) {
        MailSessionFactory.connectStore(account, io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol.IMAP, MailSecret(password)).use { store ->
            val folder = store.getFolder(name)
            if (!folder.exists()) assertTrue(folder.create(Folder.HOLDS_MESSAGES))
        }
    }

    private fun session(account: MailAccount, password: String = passwordOf(account)): MailSession =
        MailSession(account, MailSecret(password)).also { sessions += it }

    private fun passwordOf(account: MailAccount): String = when (account.address) {
        ALICE -> ALICE_PASSWORD
        BOB -> BOB_PASSWORD
        else -> CAROL_PASSWORD
    }

    private fun alice() = account(ALICE, ALICE_LOGIN)

    private fun bob() = account(BOB, BOB_LOGIN)

    private fun carol() = account(CAROL, CAROL_LOGIN)

    private fun account(address: String, login: String): MailAccount = MailAccountOptions.parse(
        """{"address":"$address","user":"$login",
            "imap":{"host":"$HOST","port":${ServerSetupTest.IMAP.port},"tls":"none"},
            "smtp":{"host":"$HOST","port":${ServerSetupTest.SMTP.port},"tls":"none"},
            "timeout":{"connect":5000,"read":10000}}""",
        SecretKind.PASSWORD,
    )

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
        const val CAROL_PASSWORD = "carol-secret"
    }
}
