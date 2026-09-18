package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SearchResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.toJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.OutgoingMessage
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageArgs
import jakarta.activation.DataHandler
import jakarta.mail.Flags
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.util.ByteArrayDataSource
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.Properties

/**
 * Roadmap P2.4: the POP3 subset of [MailSession] against GreenMail. Messages are seeded through
 * IMAP `APPEND` on the same mailbox (GreenMail serves one store over both protocols), so the POP3
 * side sees them in seeding order; GreenMail's UIDLs are the IMAP UIDs as decimal strings, which
 * the tests treat as opaque. The POP3 session traces (`debug`), so the tests can check how many
 * candidates a client search scanned.
 */
class Pop3OperationsGreenMailTest {

    private lateinit var greenMail: GreenMail
    private val sessions = ArrayList<MailSession>()
    private lateinit var imap: MailSession
    private lateinit var pop3: MailSession

    @Before
    fun startServer() {
        greenMail = GreenMail(ServerSetupTest.ALL)
        greenMail.start()
        greenMail.setUser(ALICE, ALICE_LOGIN, ALICE_PASSWORD)
        imap = MailSession(alice(receive = "imap"), MailSecret(ALICE_PASSWORD)).also { sessions += it }
        pop3 = MailSession(alice(receive = "pop3"), MailSecret(ALICE_PASSWORD)).also { sessions += it }
    }

    @After
    fun stopServer() {
        sessions.forEach { it.close() }
        greenMail.stop()
    }

    @Test
    fun foldersListAnswersInboxOnlyAndTheImapOnlyOpsAreRefusedWithoutConnecting() {
        seed(3)
        val plain = pop3.listFolders(MessageArgs.foldersList("{}"))
        assertEquals(listOf("INBOX"), plain.map { it.path })
        assertEquals("inbox", plain.single().specialUse)
        assertNull(plain.single().messages)

        val counted = pop3.listFolders(MessageArgs.foldersList("""{"status": true}""")).single()
        assertEquals(3, counted.messages)
        assertNull("POP3 keeps no seen flag", counted.unseen)
        assertTrue(counted.selectable)
        assertEquals(1, pop3.connectCount(MailProtocol.POP3))

        val refused = listOf(
            "folders.status" to { pop3.folderStatus("INBOX") },
            "folders.create" to { pop3.createFolder("Work") },
            "folders.delete" to { pop3.deleteFolder("Work") },
            "folders.rename" to { pop3.renameFolder(MessageArgs.rename("""{"path": "Work", "newPath": "Done"}""")) },
            "messages.expunge" to { pop3.expunge("INBOX") },
            "messages.append" to { pop3.append("Drafts", OutgoingMessage.simple(listOf(ALICE), "draft", "body"), Flags()) },
            "messages.setFlags" to { pop3.setFlags(MessageArgs.flags("""{"uids": [1], "flags": "seen"}""")) },
            "messages.move" to { pop3.move(MessageArgs.target("""{"uids": [1], "target": "Archive"}""")) },
            "messages.copy" to { pop3.copy(MessageArgs.target("""{"uids": [1], "target": "Archive"}""", op = "messages.copy")) },
        )
        refused.forEach { (op, call) ->
            val error = failure(call)
            assertEquals(op, MailErrorCode.UNSUPPORTED_OPERATION, error.code)
            assertTrue(error.message, error.message.contains(op) && error.message.contains("pop3"))
            assertEquals(error, pop3.lastError)
        }
        // what POP3 cannot do is refused before connecting, not after
        assertEquals(MailErrorCode.FOLDER_NOT_FOUND, failure { pop3.listMessages(MessageArgs.list("""{"folder": "Archive"}""", MailProtocol.POP3)) }.code)
        assertEquals(MailErrorCode.UNSUPPORTED_OPERATION, failure { pop3.listMessages(MessageArgs.list("""{"unseenOnly": true}""", MailProtocol.POP3)) }.code)
        assertEquals(MailErrorCode.UNSUPPORTED_OPERATION, failure { pop3.searchMessages(MessageArgs.search("""{"query": {"text": "x"}}""", MailProtocol.POP3)) }.code)
        assertEquals(MailErrorCode.UNSUPPORTED_OPERATION, failure { pop3.searchMessages(MessageArgs.search("""{"query": {"uid": "1:*"}}""", MailProtocol.POP3)) }.code)
        assertEquals(MailErrorCode.FOLDER_NOT_FOUND, failure { pop3.getMessage(MessageArgs.get("""{"folder": "Sent", "uid": "1"}""", MailProtocol.POP3)) }.code)
        assertEquals(1, pop3.connectCount(MailProtocol.POP3))
        assertEquals(0, pop3.connectCount(MailProtocol.IMAP))
    }

    @Test
    fun listingPagesByUidlCursorsWithHeadersOnly() {
        assertTrue(pop3.listMessages(MessageArgs.list("{}", MailProtocol.POP3)).isEmpty())
        seed(7)
        append(message(subject = "with attachment", text = "see attached", attachment = "id,total\r\n1,42".toByteArray()))

        val newest = pop3.listMessages(MessageArgs.list("""{"limit": 3}""", MailProtocol.POP3))
        assertEquals(listOf("with attachment", "seed 7", "seed 6"), newest.map { it.subject })
        newest.forEach { doc ->
            assertTrue("POP3 uids are strings: ${doc.uid}", doc.uid.isString)
            assertEquals("INBOX", doc.folder)
            assertFalse(doc.bodyLoaded)
            assertFalse(doc.seen)
            assertTrue(doc.flags.isEmpty())
            assertTrue(doc.size > 0)
            assertEquals(ALICE, doc.from?.address)
        }
        assertTrue("multipart/mixed counts as attachments", newest[0].hasAttachments)
        assertFalse(newest[1].hasAttachments)
        assertTrue(newest[0].toJson().contains("\"uid\":\"${newest[0].uid.content}\""))
        assertEquals(8, pop3.listFolders(MessageArgs.foldersList("""{"status": true}""")).single().messages)

        val older = pop3.listMessages(MessageArgs.list("""{"limit": 3, "before": ${newest.last().uid}}""", MailProtocol.POP3))
        assertEquals(listOf("seed 5", "seed 4", "seed 3"), older.map { it.subject })
        val oldest = pop3.listMessages(MessageArgs.list("""{"limit": 3, "before": ${older.last().uid}}""", MailProtocol.POP3))
        assertEquals(listOf("seed 2", "seed 1"), oldest.map { it.subject })
        assertTrue(pop3.listMessages(MessageArgs.list("""{"before": ${oldest.last().uid}}""", MailProtocol.POP3)).isEmpty())

        val ascending = pop3.listMessages(MessageArgs.list("""{"limit": 2, "after": ${older.last().uid}, "order": "asc"}""", MailProtocol.POP3))
        assertEquals(listOf("seed 4", "seed 5"), ascending.map { it.subject })
        val between = pop3.listMessages(MessageArgs.list("""{"after": ${oldest.last().uid}, "before": ${newest.last().uid}}""", MailProtocol.POP3))
        assertEquals(listOf("seed 5", "seed 4", "seed 3", "seed 2"), between.map { it.subject })

        val gone = failure { pop3.listMessages(MessageArgs.list("""{"before": "no-such-uidl"}""", MailProtocol.POP3)) }
        assertEquals(MailErrorCode.MESSAGE_NOT_FOUND, gone.code)
        assertTrue(gone.message, gone.message.contains("cursor"))
        assertEquals(1, pop3.connectCount(MailProtocol.POP3))
    }

    @Test
    fun getRawAndDownloadFetchTheWholeMessage() {
        val csv = "id,total\r\n1,42".toByteArray()
        append(message(subject = "Rich 报表", text = "plain 你好", html = "<p><b>html</b> 你好</p>", attachment = csv, headers = mapOf("X-Campaign" to "spring")))
        val listed = pop3.listMessages(MessageArgs.list("{}", MailProtocol.POP3)).single()

        val full = pop3.getMessage(MessageArgs.get("""{"uid": ${listed.uid}, "includeRaw": true}""", MailProtocol.POP3))
        assertEquals(listed.uid, full.uid)
        assertTrue(full.bodyLoaded)
        assertEquals("Rich 报表", full.subject)
        assertEquals("plain 你好", full.text)
        assertTrue(full.html.orEmpty().contains("<b>html</b>"))
        assertEquals(listOf("spring"), full.headers.entries.first { it.key.equals("X-Campaign", ignoreCase = true) }.value)
        assertEquals(1, full.attachments.size)
        assertEquals("report.csv", full.attachments.single().fileName)
        assertTrue(full.hasAttachments)
        assertFalse(full.seen)
        assertTrue(full.raw.orEmpty().contains("Subject:"))
        assertFalse(full.rawTruncated)
        // peek is meaningless on POP3 and ignored
        assertFalse(pop3.getMessage(MessageArgs.get("""{"uid": ${listed.uid}, "peek": false}""", MailProtocol.POP3)).seen)

        val part = full.attachments.single()
        val bytes = ByteArrayOutputStream()
        val reports = ArrayList<Long>()
        val download = pop3.downloadAttachment(MessageArgs.download("""{"uid": ${listed.uid}, "partId": "${part.partId}"}""", MailProtocol.POP3), bytes) { transferred, _ -> reports += transferred }
        assertArrayEquals(csv, bytes.toByteArray())
        assertEquals(csv.size.toLong(), download.bytes)
        assertEquals("report.csv", download.fileName)
        assertEquals("text/csv", download.mimeType)
        assertEquals(listOf(csv.size.toLong()), reports)
        assertEquals(MailErrorCode.ATTACHMENT_NOT_FOUND, failure { pop3.downloadAttachment(MessageArgs.download("""{"uid": ${listed.uid}, "partId": "9"}""", MailProtocol.POP3), ByteArrayOutputStream()) }.code)

        val raw = ByteArrayOutputStream()
        val rawResult = pop3.downloadRaw(MessageArgs.raw("""{"uid": ${listed.uid}}""", MailProtocol.POP3), raw)
        assertEquals(raw.size().toLong(), rawResult.bytes)
        assertEquals("${listed.uid.content}.eml", rawResult.fileName)
        assertEquals("message/rfc822", rawResult.mimeType)
        val reparsed = MimeMessage(Session.getInstance(Properties()), ByteArrayInputStream(raw.toByteArray()))
        assertEquals("Rich 报表", reparsed.subject)
        assertTrue(rawResult.bytes > 0)

        assertEquals(MailErrorCode.MESSAGE_NOT_FOUND, failure { pop3.getMessage(MessageArgs.get("""{"uid": "missing"}""", MailProtocol.POP3)) }.code)
        assertEquals(MailErrorCode.MESSAGE_NOT_FOUND, failure { pop3.downloadRaw(MessageArgs.raw("""{"uid": "missing"}""", MailProtocol.POP3), ByteArrayOutputStream()) }.code)
        assertEquals(1, pop3.connectCount(MailProtocol.POP3))
    }

    @Test
    fun searchFiltersHeadersOnTheClient() {
        seed(5)
        append(message(subject = "Invoice 2026", from = "billing@example.org", text = "pay", headers = mapOf("X-Campaign" to "spring", "Message-ID" to "<invoice@example.org>")))
        append(message(subject = "Invoice reminder", from = "billing@example.org", text = "pay now"))

        val bySubject = pop3.searchMessages(MessageArgs.search("""{"query": {"subject": "invoice"}}""", MailProtocol.POP3))
        assertEquals(SearchResult.FALLBACK_CLIENT, bySubject.fallback)
        assertEquals(listOf("Invoice reminder", "Invoice 2026"), bySubject.messages.map { it.subject })
        bySubject.messages.forEach { assertTrue(it.uid.isString); assertFalse(it.bodyLoaded) }

        val combined = pop3.searchMessages(MessageArgs.search("""{"query": {"from": "billing", "not": {"subject": "reminder"}}, "limit": 5}""", MailProtocol.POP3))
        assertEquals(listOf("Invoice 2026"), combined.messages.map { it.subject })
        val byHeader = pop3.searchMessages(MessageArgs.search("""{"query": {"header": {"X-Campaign": "spring"}}}""", MailProtocol.POP3))
        assertEquals(listOf("Invoice 2026"), byHeader.messages.map { it.subject })
        val byMessageId = pop3.searchMessages(MessageArgs.search("""{"query": {"messageId": "invoice@example.org"}}""", MailProtocol.POP3))
        assertEquals(listOf("Invoice 2026"), byMessageId.messages.map { it.subject })
        val sinceEpoch = pop3.searchMessages(MessageArgs.search("""{"query": {"sentSince": 1000, "seen": false}, "limit": 2}""", MailProtocol.POP3))
        assertEquals(listOf("Invoice reminder", "Invoice 2026"), sinceEpoch.messages.map { it.subject })

        val newest = pop3.listMessages(MessageArgs.list("""{"limit": 1}""", MailProtocol.POP3)).single()
        val before = pop3.searchMessages(MessageArgs.search("""{"query": {"from": "billing"}, "before": ${newest.uid}}""", MailProtocol.POP3))
        assertEquals(listOf("Invoice 2026"), before.messages.map { it.subject })
        assertTrue(pop3.searchMessages(MessageArgs.search("""{"query": {"subject": "nothing like this"}}""", MailProtocol.POP3)).messages.isEmpty())

        // newest first, one TOP per candidate, and the scan stops once `limit` messages match
        pop3.trace.drain()
        val newestOnly = pop3.searchMessages(MessageArgs.search("""{"query": {"from": "billing"}, "limit": 1}""", MailProtocol.POP3))
        assertEquals(listOf("Invoice reminder"), newestOnly.messages.map { it.subject })
        val scans = pop3.trace.drain().filter { "client filter scanned" in it }
        assertEquals(scans.toString(), 1, scans.size)
        assertTrue(scans.single(), scans.single().endsWith("client filter scanned 1 matched 1"))
        pop3.searchMessages(MessageArgs.search("""{"query": {"subject": "nothing like this"}}""", MailProtocol.POP3))
        assertTrue(pop3.trace.drain().any { it.endsWith("client filter scanned 7 matched 0") })
        assertEquals(1, pop3.connectCount(MailProtocol.POP3))
    }

    @Test
    fun deleteCommitsWhenTheFolderCloses() {
        seed(4)
        val listed = pop3.listMessages(MessageArgs.list("{}", MailProtocol.POP3))
        assertEquals(4, listed.size)
        val target = listed[1]
        val second = listed[3]

        // expunge false still deletes: POP3 has no "marked but kept" state
        val deleted = pop3.delete(MessageArgs.delete("""{"uids": [${target.uid}, "not-there"], "expunge": false}""", MailProtocol.POP3))
        assertEquals(listOf(target.uid.content), deleted.uidStrings)
        assertTrue(deleted.uids.all { it.isString } && deleted.imapUids.isEmpty())
        assertTrue(deleted.toJson().contains("\"uids\":[\"${target.uid.content}\"]"))
        val remaining = pop3.listMessages(MessageArgs.list("{}", MailProtocol.POP3))
        assertEquals(listOf("seed 4", "seed 2", "seed 1"), remaining.map { it.subject })
        assertEquals(MailErrorCode.MESSAGE_NOT_FOUND, failure { pop3.getMessage(MessageArgs.get("""{"uid": ${target.uid}}""", MailProtocol.POP3)) }.code)
        // the IMAP view of the same mailbox agrees
        assertEquals(3, imap.folderStatus("INBOX").messages)

        assertEquals(listOf(second.uid.content), pop3.delete(MessageArgs.delete("""{"uids": ${second.uid}, "expunge": true}""", MailProtocol.POP3)).uidStrings)
        assertEquals(listOf("seed 4", "seed 2"), pop3.listMessages(MessageArgs.list("{}", MailProtocol.POP3)).map { it.subject })
        assertEquals(MailErrorCode.MESSAGE_NOT_FOUND, failure { pop3.delete(MessageArgs.delete("""{"uids": ["nothing"]}""", MailProtocol.POP3)) }.code)
        assertEquals(2, imap.folderStatus("INBOX").messages)
        assertEquals(1, pop3.connectCount(MailProtocol.POP3))
    }

    @Test
    fun sessionTestAndSendWorkForPop3Accounts() {
        val report = pop3.test()
        assertTrue(report.ok)
        assertNull(report.imap)
        assertTrue(report.pop3!!.ok)
        assertTrue(report.smtp!!.ok)
        assertEquals("pop3", report.account.receive)
        val sent = pop3.send(OutgoingMessage.simple(listOf(ALICE), "from a pop3 account", "hello"), saveToSent = true)
        assertEquals(1, sent.accepted.size)
        assertFalse("no IMAP endpoint, so no sent copy", sent.savedToSent)
        assertNull(sent.sentFolder)
        assertTrue(greenMail.waitForIncomingEmail(5_000, 1))
        assertEquals(listOf("from a pop3 account"), pop3.listMessages(MessageArgs.list("{}", MailProtocol.POP3)).map { it.subject })
        assertEquals(listOf(MailProtocol.POP3, MailProtocol.SMTP), pop3.connectedProtocols)
    }

    // ------------------------------------------------------------------ helpers

    private fun seed(count: Int) {
        for (index in 1..count) append(message(subject = "seed $index", text = "body $index", date = Date(1_750_000_000_000L + index * 60_000L)))
    }

    private fun append(mime: MimeMessage) {
        imap.imap { it.append("INBOX", mime, Flags()) }
    }

    private fun message(
        subject: String,
        from: String = ALICE,
        text: String? = "body",
        html: String? = null,
        attachment: ByteArray? = null,
        date: Date = Date(1_750_000_000_000L),
        headers: Map<String, String> = emptyMap(),
    ): MimeMessage {
        val mime = MimeMessage(Session.getInstance(Properties()))
        mime.setFrom(InternetAddress(from))
        mime.setRecipients(jakarta.mail.Message.RecipientType.TO, ALICE)
        mime.setSubject(subject, "UTF-8")
        mime.sentDate = date
        val body: MimeBodyPart = MimeBodyPart().apply {
            if (html == null) {
                setText(text ?: "", "UTF-8")
            } else {
                val alternative = MimeMultipart("alternative")
                text?.let { alternative.addBodyPart(MimeBodyPart().apply { setText(it, "UTF-8") }) }
                alternative.addBodyPart(MimeBodyPart().apply { setContent(html, "text/html; charset=UTF-8") })
                setContent(alternative)
            }
        }
        if (attachment == null && html == null) {
            mime.setText(text ?: "", "UTF-8")
        } else if (attachment == null) {
            mime.setContent(body.content as MimeMultipart)
        } else {
            val mixed = MimeMultipart("mixed")
            mixed.addBodyPart(body)
            mixed.addBodyPart(MimeBodyPart().apply {
                dataHandler = DataHandler(ByteArrayDataSource(attachment, "text/csv"))
                fileName = "report.csv"
                disposition = jakarta.mail.Part.ATTACHMENT
            })
            mime.setContent(mixed)
        }
        mime.saveChanges()
        // after saveChanges, which would replace a caller-supplied Message-ID
        headers.forEach { (name, value) -> mime.setHeader(name, value) }
        return mime
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
        """{"address":"$ALICE","user":"$ALICE_LOGIN","receive":"$receive","debug":${receive == "pop3"},
            "imap":{"host":"$HOST","port":${ServerSetupTest.IMAP.port},"tls":"none"},
            "pop3":{"host":"$HOST","port":${ServerSetupTest.POP3.port},"tls":"none"},
            "smtp":{"host":"$HOST","port":${ServerSetupTest.SMTP.port},"tls":"none"},
            "timeout":{"connect":5000,"read":10000}}""",
        SecretKind.PASSWORD,
    )

    companion object {
        const val HOST = "127.0.0.1"
        const val ALICE = "alice@localhost"
        const val ALICE_LOGIN = "alice"
        const val ALICE_PASSWORD = "alice-secret"
    }
}
