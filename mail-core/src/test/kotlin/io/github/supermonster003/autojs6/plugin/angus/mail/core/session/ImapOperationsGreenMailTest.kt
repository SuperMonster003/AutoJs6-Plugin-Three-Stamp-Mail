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
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.FolderDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SearchResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.toFolderJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.toJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageArgs
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.util.ByteArrayDataSource
import jakarta.activation.DataHandler
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
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
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Date
import java.util.Properties

/**
 * Roadmap P2.3: folders, listing, search, bodies, downloads and flag / move / delete operations
 * of [MailSession] against GreenMail. Messages are seeded through `APPEND`, so UIDs are
 * deterministic (1..n in seeding order).
 */
class ImapOperationsGreenMailTest {

    private lateinit var greenMail: GreenMail
    private val sessions = ArrayList<MailSession>()
    private lateinit var session: MailSession

    @Before
    fun startServer() {
        greenMail = GreenMail(ServerSetupTest.ALL)
        greenMail.start()
        greenMail.setUser(ALICE, ALICE_LOGIN, ALICE_PASSWORD)
        session = MailSession(alice(), MailSecret(ALICE_PASSWORD)).also { sessions += it }
    }

    @After
    fun stopServer() {
        sessions.forEach { it.close() }
        greenMail.stop()
    }

    // ------------------------------------------------------------------ folders

    @Test
    fun foldersListStatusCreateRenameDelete() {
        val initial = session.listFolders(MessageArgs.foldersList("{}"))
        assertEquals(listOf("INBOX"), initial.map { it.path })
        assertEquals("inbox", initial.single().specialUse)
        assertNull(initial.single().messages)

        val created = session.createFolder("Work")
        assertEquals("Work", created.path)
        assertEquals("Work", created.name)
        assertTrue(created.selectable)
        session.createFolder("Work${created.delimiter}2026")
        session.createFolder("Sent Items")

        val tree = session.listFolders(MessageArgs.foldersList("""{"status": true}"""))
        val paths = flatten(tree).map { it.path }
        assertTrue(paths.toString(), paths.containsAll(listOf("INBOX", "Work", "Work${created.delimiter}2026", "Sent Items")))
        val work = flatten(tree).single { it.path == "Work" }
        assertEquals(listOf("2026"), work.children.map { it.name })
        assertEquals(0, work.messages)
        assertEquals(0, work.unseen)
        assertEquals("sent", flatten(tree).single { it.path == "Sent Items" }.specialUse)
        assertTrue(tree.toFolderJson().contains("\"children\""))

        seed("INBOX", 3)
        seed("Work", 2, seen = true)
        val inbox = session.folderStatus("INBOX")
        assertEquals(3, inbox.messages)
        assertEquals(3, inbox.unseen)
        assertNotNull(inbox.uidNext)
        assertNotNull(inbox.uidValidity)
        assertEquals(0, session.folderStatus("Work").unseen)

        val renamed = session.renameFolder(MessageArgs.rename("""{"path": "Sent Items", "newPath": "Outbox"}"""))
        assertEquals("Outbox", renamed.path)
        assertFalse(flatten(session.listFolders(MessageArgs.foldersList("{}"))).any { it.path == "Sent Items" })

        assertTrue(session.deleteFolder("Outbox"))
        assertFalse(flatten(session.listFolders(MessageArgs.foldersList("{}"))).any { it.path == "Outbox" })
        assertEquals(MailErrorCode.FOLDER_NOT_FOUND, failure { session.folderStatus("Outbox") }.code)
        assertEquals(MailErrorCode.FOLDER_NOT_FOUND, failure { session.deleteFolder("Outbox") }.code)
        assertEquals(MailErrorCode.FOLDER_NOT_FOUND, failure { session.renameFolder(MessageArgs.rename("""{"path": "Outbox", "newPath": "X"}""")) }.code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure { session.createFolder("Work") }.code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure { session.deleteFolder("INBOX") }.code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure { session.deleteFolder("inbox") }.code)
        assertEquals(MailErrorCode.FOLDER_NOT_FOUND, failure { session.listMessages(MessageArgs.list("""{"folder": "Nope"}""")) }.code)
        assertEquals(MailErrorCode.FOLDER_NOT_FOUND, failure { session.expunge("Nope") }.code)
    }

    // ------------------------------------------------------------------ listing

    @Test
    fun listingPagesByUidCursorsInBothDirections() {
        seed("INBOX", 120)
        val first = session.listMessages(MessageArgs.list("""{"limit": 50}"""))
        assertEquals(50, first.size)
        assertEquals(120L, first.first().uid())
        assertEquals(71L, first.last().uid())
        assertEquals("seed 120", first.first().subject)
        assertFalse(first.first().bodyLoaded)
        assertFalse(first.first().seen)
        assertFalse(first.first().hasAttachments)
        assertEquals("INBOX", first.first().folder)

        val second = session.listMessages(MessageArgs.list("""{"limit": 50, "before": ${first.last().uid()}}"""))
        assertEquals(70L, second.first().uid())
        assertEquals(21L, second.last().uid())
        val third = session.listMessages(MessageArgs.list("""{"limit": 50, "before": ${second.last().uid()}}"""))
        assertEquals(20, third.size)
        assertEquals(1L, third.last().uid())
        assertTrue(session.listMessages(MessageArgs.list("""{"limit": 50, "before": 1}""")).isEmpty())
        assertEquals((1L..120L).toList(), (first + second + third).map { it.uid() }.asReversed())

        val ascending = session.listMessages(MessageArgs.list("""{"limit": 10, "order": "asc"}"""))
        assertEquals((1L..10L).toList(), ascending.map { it.uid() })
        val newer = session.listMessages(MessageArgs.list("""{"limit": 10, "order": "asc", "after": 10}"""))
        assertEquals((11L..20L).toList(), newer.map { it.uid() })
        val window = session.listMessages(MessageArgs.list("""{"after": 100, "before": 111}"""))
        assertEquals((101L..110L).toList().asReversed(), window.map { it.uid() })

        // cursors that no longer exist still cut at the right position
        session.delete(MessageArgs.delete("""{"uids": [60], "expunge": true}"""))
        val aroundGap = session.listMessages(MessageArgs.list("""{"limit": 3, "before": 60}"""))
        assertEquals(listOf(59L, 58L, 57L), aroundGap.map { it.uid() })
        val afterGap = session.listMessages(MessageArgs.list("""{"limit": 3, "order": "asc", "after": 60}"""))
        assertEquals(listOf(61L, 62L, 63L), afterGap.map { it.uid() })
        assertTrue(session.listMessages(MessageArgs.list("""{"folder": "INBOX", "after": 500}""")).isEmpty())
    }

    @Test
    fun unseenOnlyPagesThroughTheServerSearch() {
        seed("INBOX", 30)
        session.setFlags(MessageArgs.flags("""{"uids": [${(1..30 step 2).joinToString(",")}], "flags": "seen"}"""))
        val unseen = session.listMessages(MessageArgs.list("""{"unseenOnly": true, "limit": 5}"""))
        assertEquals(listOf(30L, 28L, 26L, 24L, 22L), unseen.map { it.uid() })
        assertTrue(unseen.none { it.seen })
        val older = session.listMessages(MessageArgs.list("""{"unseenOnly": true, "limit": 5, "before": 22}"""))
        assertEquals(listOf(20L, 18L, 16L, 14L, 12L), older.map { it.uid() })
        assertTrue(session.listMessages(MessageArgs.list("""{"unseenOnly": true, "before": 2}""")).isEmpty())
        val empty = session.createFolder("Empty")
        assertTrue(session.listMessages(MessageArgs.list("""{"folder": "${empty.path}"}""")).isEmpty())
    }

    // ------------------------------------------------------------------ search

    @Test
    fun searchRunsOnTheServerAndOnTheClient() {
        seed("INBOX", 10)
        append("INBOX", message(subject = "Invoice 2026", from = "billing@example.org", text = "please pay 报表", date = Date(1_700_000_000_000L)), Flags(Flags.Flag.FLAGGED))
        append("INBOX", message(subject = "Re: Invoice 2026", from = "bob@example.org", text = "paid", headers = mapOf("X-Campaign" to "spring", "Message-ID" to "<re@example.org>")), Flags(Flags.Flag.SEEN))

        val bySubject = session.searchMessages(MessageArgs.search("""{"query": {"subject": "invoice"}}"""))
        assertEquals(SearchResult.FALLBACK_SERVER, bySubject.fallback)
        assertEquals(listOf(12L, 11L), bySubject.messages.map { it.uid() })
        // GreenMail matches FROM / TO on the whole address only; real servers and the client fallback take substrings
        assertEquals(listOf(11L), session.searchMessages(MessageArgs.search("""{"query": {"from": "billing@example.org"}}""")).messages.map { it.uid() })
        assertEquals(listOf(12L), session.searchMessages(MessageArgs.search("""{"query": {"messageId": "<re@example.org>"}}""")).messages.map { it.uid() })
        assertEquals(listOf(12L), session.searchMessages(MessageArgs.search("""{"query": {"header": {"X-Campaign": "spring"}}}""")).messages.map { it.uid() })
        assertEquals(listOf(12L), session.searchMessages(MessageArgs.search("""{"query": {"body": "paid"}}""")).messages.map { it.uid() })
        assertEquals(listOf(11L), session.searchMessages(MessageArgs.search("""{"query": {"text": "报表"}}""")).messages.map { it.uid() })
        assertEquals(12, session.searchMessages(MessageArgs.search("""{"query": {"since": "2020-01-01"}}""")).messages.size)
        assertEquals(listOf(11L), session.searchMessages(MessageArgs.search("""{"query": {"flagged": true}}""")).messages.map { it.uid() })
        assertEquals(listOf(12L), session.searchMessages(MessageArgs.search("""{"query": {"subject": "invoice", "seen": true}}""")).messages.map { it.uid() })
        assertEquals(listOf(11L), session.searchMessages(MessageArgs.search("""{"query": {"subject": "invoice", "not": {"seen": true}}}""")).messages.map { it.uid() })
        assertEquals(listOf(12L, 11L, 10L), session.searchMessages(MessageArgs.search("""{"query": {"or": [{"subject": "invoice"}, {"subject": "seed 10"}]}}""")).messages.map { it.uid() })
        assertEquals(listOf(12L, 11L), session.searchMessages(MessageArgs.search("""{"query": {"or": [{"from": "billing@example.org"}, {"from": "bob@example.org"}]}}""")).messages.map { it.uid() })
        assertEquals(listOf(11L), session.searchMessages(MessageArgs.search("""{"query": {"subject": "invoice"}, "before": 12}""")).messages.map { it.uid() })
        assertEquals(listOf(11L), session.searchMessages(MessageArgs.search("""{"query": {"uid": "1:11", "subject": "invoice"}}""")).messages.map { it.uid() })
        assertEquals(listOf(12L, 11L, 10L), session.searchMessages(MessageArgs.search("""{"query": {"uid": "10:*"}}""")).messages.map { it.uid() })
        assertEquals(listOf(12L), session.searchMessages(MessageArgs.search("""{"query": {"uid": "*"}}""")).messages.map { it.uid() })
        assertEquals(listOf(3L, 1L), session.searchMessages(MessageArgs.search("""{"query": {"uid": [1, "3", "40:50"]}}""")).messages.map { it.uid() })
        assertTrue(session.searchMessages(MessageArgs.search("""{"query": {"uid": "400:500"}}""")).messages.isEmpty())
        assertEquals(2, session.searchMessages(MessageArgs.search("""{"query": {"subject": "seed"}, "limit": 2}""")).messages.size)
        assertEquals(listOf(11L), session.searchMessages(MessageArgs.search("""{"query": {"sentBefore": "2024-01-01"}}""")).messages.map { it.uid() })

        // the same queries evaluated on the client (what happens when a provider refuses the SEARCH)
        session.imap { mailbox ->
            val client = mailbox.search(MessageArgs.search("""{"query": {"subject": "invoice", "not": {"seen": true}}}"""), serverSearch = false)
            assertEquals(SearchResult.FALLBACK_CLIENT, client.fallback)
            assertEquals(listOf(11L), client.messages.map { it.uid() })
            val header = mailbox.search(MessageArgs.search("""{"query": {"header": {"X-Campaign": "spring"}}}"""), serverSearch = false)
            assertEquals(listOf(12L), header.messages.map { it.uid() })
            assertEquals(listOf(11L), mailbox.search(MessageArgs.search("""{"query": {"from": "billing"}}"""), serverSearch = false).messages.map { it.uid() })
            assertEquals(listOf(12L), mailbox.search(MessageArgs.search("""{"query": {"messageId": "re@example.org"}}"""), serverSearch = false).messages.map { it.uid() })
            assertEquals(12, mailbox.search(MessageArgs.search("""{"query": {"larger": 10, "smaller": 100000}}"""), serverSearch = false).messages.size)
            assertEquals(listOf(11L), mailbox.search(MessageArgs.search("""{"query": {"sentBefore": "2024-01-01"}}"""), serverSearch = false).messages.map { it.uid() })
            val text = mailbox.search(MessageArgs.search("""{"query": {"text": "报表"}}"""), serverSearch = false)
            assertEquals(listOf(11L), text.messages.map { it.uid() })
            val restricted = mailbox.search(MessageArgs.search("""{"query": {"uid": "1:5", "subject": "seed"}, "limit": 2}"""), serverSearch = false)
            assertEquals(listOf(5L, 4L), restricted.messages.map { it.uid() })
        }
        val always = session.searchMessages(MessageArgs.search("""{"query": {"from": "billing"}, "fallback": "always"}"""))
        assertEquals(SearchResult.FALLBACK_CLIENT, always.fallback)
        assertEquals(listOf(11L), always.messages.map { it.uid() })
        assertTrue(session.searchMessages(MessageArgs.search("""{"query": {"subject": "x"}, "folder": "${session.createFolder("Void").path}"}""")).messages.isEmpty())
    }

    // ------------------------------------------------------------------ get and downloads

    @Test
    fun getReturnsBodiesHeadersAndAttachmentsAndHonoursPeek() {
        val png = ByteArray(30_000) { (it * 7).toByte() }
        append("INBOX", richMessage(png), Flags())
        val doc = session.getMessage(MessageArgs.get("""{"uid": 1}"""))
        assertTrue(doc.bodyLoaded)
        assertEquals("Rich 报表", doc.subject)
        assertEquals("plain 你好", doc.text)
        assertTrue(doc.html!!.contains("<b>html</b>"))
        assertEquals("<rich@example.org>", doc.messageId)
        assertEquals("<parent@example.org>", doc.inReplyTo)
        assertEquals(listOf("<root@example.org>", "<parent@example.org>"), doc.references)
        assertEquals("spring", doc.headers["X-Campaign"]?.single())
        assertEquals("Alice 王", doc.from?.name)
        assertEquals(ALICE, doc.from?.address)
        assertTrue(doc.hasAttachments)
        assertEquals(listOf("2", "3"), doc.attachments.map { it.partId })
        assertEquals("logo.png", doc.attachments[0].fileName)
        assertEquals("image/png", doc.attachments[0].mimeType)
        assertEquals("logo", doc.attachments[0].contentId)
        assertTrue(doc.attachments[0].inline)
        assertTrue("${doc.attachments[0].size}", doc.attachments[0].size >= png.size)
        assertEquals("报表 2026.csv", doc.attachments[1].fileName)
        assertEquals("text/csv", doc.attachments[1].mimeType)
        assertFalse(doc.attachments[1].inline)
        assertFalse(doc.seen)
        assertFalse(doc.bodyTruncated)
        assertNull(doc.raw)
        assertFalse(doc.rawTruncated)
        assertTrue(doc.toJson().contains("\"uid\":1"))

        // peek (default) left the message unread; peek = false marks it
        assertFalse(session.getMessage(MessageArgs.get("""{"uid": 1}""")).seen)
        assertEquals(1, session.folderStatus("INBOX").unseen)
        val marked = session.getMessage(MessageArgs.get("""{"uid": 1, "peek": false}"""))
        assertTrue(marked.seen)
        assertTrue(marked.flags.contains("seen"))
        assertEquals(0, session.folderStatus("INBOX").unseen)

        val raw = session.getMessage(MessageArgs.get("""{"uid": 1, "includeRaw": true}"""))
        assertNotNull(raw.raw)
        assertFalse(raw.rawTruncated)
        assertTrue(raw.raw!!.startsWith("From: ") || raw.raw!!.contains("\r\nSubject: "))
        assertTrue(raw.raw!!.contains("Content-ID: <logo>"))

        assertEquals(MailErrorCode.MESSAGE_NOT_FOUND, failure { session.getMessage(MessageArgs.get("""{"uid": 999}""")) }.code)
    }

    @Test
    fun oversizedBodiesAreLeftOutAndStayDownloadable() {
        val huge = "x".repeat(MailLimits.MAX_INLINE_BODY_BYTES + 1024)
        append("INBOX", message(subject = "big", text = huge, html = "<p>small html</p>"), Flags())
        val doc = session.getMessage(MessageArgs.get("""{"uid": 1}"""))
        assertTrue(doc.bodyTruncated)
        assertNull(doc.text)
        assertEquals("<p>small html</p>", doc.html)
        assertEquals(listOf("1"), doc.bodyParts.map { it.partId })
        assertEquals("text/plain", doc.bodyParts.single().mimeType)
        val out = ByteArrayOutputStream()
        val result = session.downloadAttachment(MessageArgs.download("""{"uid": 1, "partId": "1"}"""), out)
        assertEquals(huge, String(out.toByteArray(), Charsets.UTF_8).trimEnd())
        assertEquals("text/plain", result.mimeType)
        assertEquals(out.size().toLong(), result.bytes)
        val rawDoc = session.getMessage(MessageArgs.get("""{"uid": 1, "includeRaw": true}"""))
        assertNull(rawDoc.raw)
        assertTrue(rawDoc.rawTruncated)
    }

    @Test
    fun downloadsStreamWithProgressAndReportErrors() {
        val big = ByteArray(10 * 1024 * 1024) { (it % 251).toByte() }
        append("INBOX", richMessage(big), Flags())

        val reports = ArrayList<Pair<Long, Long?>>()
        val out = ByteArrayOutputStream()
        val result = session.downloadAttachment(MessageArgs.download("""{"uid": 1, "partId": "2"}"""), out) { transferred, total -> reports += transferred to total }
        assertArrayEquals(big, out.toByteArray())
        assertEquals(big.size.toLong(), result.bytes)
        assertEquals("logo.png", result.fileName)
        assertEquals("image/png", result.mimeType)
        assertTrue("progress during the transfer: ${reports.size}", reports.size >= 5)
        assertEquals(big.size.toLong(), reports.last().first)
        assertTrue(reports.zipWithNext().all { (a, b) -> a.first < b.first })
        assertFalse("downloads never mark the message read", session.getMessage(MessageArgs.get("""{"uid": 1}""")).seen)

        val csv = ByteArrayOutputStream()
        val small = session.downloadAttachment(MessageArgs.download("""{"uid": 1, "partId": "3"}"""), csv)
        // a CRLF right before a boundary belongs to the boundary (RFC 2046), so the fixture ends without one
        assertEquals("id,total\r\n1,42", String(csv.toByteArray()))
        assertEquals("报表 2026.csv", small.fileName)
        assertEquals("text/csv", small.mimeType)

        val rawReports = ArrayList<Long>()
        val raw = ByteArrayOutputStream()
        val rawResult = session.downloadRaw(MessageArgs.raw("""{"uid": 1}"""), raw) { transferred, _ -> rawReports += transferred }
        assertEquals("1.eml", rawResult.fileName)
        assertEquals("message/rfc822", rawResult.mimeType)
        assertEquals(raw.size().toLong(), rawResult.bytes)
        assertTrue(rawReports.size >= 5)
        val reparsed = MimeMessage(Session.getInstance(Properties()), raw.toByteArray().inputStream())
        assertEquals("Rich 报表", reparsed.subject)
        val mixed = reparsed.content as MimeMultipart
        assertArrayEquals(big, (mixed.getBodyPart(1) as MimeBodyPart).inputStream.readBytes())

        assertEquals(MailErrorCode.ATTACHMENT_NOT_FOUND, failure { session.downloadAttachment(MessageArgs.download("""{"uid": 1, "partId": "9"}"""), ByteArrayOutputStream()) }.code)
        assertEquals(MailErrorCode.ATTACHMENT_NOT_FOUND, failure { session.downloadAttachment(MessageArgs.download("""{"uid": 1, "partId": "2.1"}"""), ByteArrayOutputStream()) }.code)
        assertEquals(MailErrorCode.MESSAGE_NOT_FOUND, failure { session.downloadRaw(MessageArgs.raw("""{"uid": 77}"""), ByteArrayOutputStream()) }.code)

        val broken = object : OutputStream() {
            var written = 0
            override fun write(b: Int) = throw IOException("Broken pipe")
            override fun write(b: ByteArray, off: Int, len: Int) {
                written += len
                if (written > 200_000) throw IOException("Broken pipe")
            }
        }
        val io = failure { session.downloadAttachment(MessageArgs.download("""{"uid": 1, "partId": "2"}"""), broken) }
        assertEquals(io.message, MailErrorCode.IO_FAILED, io.code)
        assertTrue("the connection survives a broken sink", session.imap { it.isConnected })
        assertEquals(1, session.connectCount(MailProtocol.IMAP))
    }

    // ------------------------------------------------------------------ flags, move, copy, delete, expunge

    @Test
    fun flagsAreAddedRemovedAndReplaced() {
        seed("INBOX", 4)
        val added = session.setFlags(MessageArgs.flags("""{"uids": [1, 2, 99], "flags": ["seen", "flagged", "Work"]}"""))
        assertEquals(listOf(1L, 2L), added.uids)
        val one = session.getMessage(MessageArgs.get("""{"uid": 1}"""))
        assertTrue(one.seen && one.flagged)
        assertTrue(one.flags.toString(), one.flags.containsAll(listOf("seen", "flagged", "Work")))

        session.setFlags(MessageArgs.flags("""{"uids": 1, "flags": "flagged", "mode": "remove"}"""))
        val removed = session.getMessage(MessageArgs.get("""{"uid": 1}"""))
        assertTrue(removed.seen)
        assertFalse(removed.flagged)
        assertTrue(removed.flags.contains("Work"))

        session.setFlags(MessageArgs.flags("""{"uids": [1, 2], "flags": ["answered"], "mode": "set"}"""))
        listOf(1L, 2L).forEach { uid ->
            val doc = session.getMessage(MessageArgs.get("""{"uid": $uid}"""))
            assertTrue(doc.answered)
            assertFalse(doc.seen)
            assertFalse(doc.flagged)
            assertFalse(doc.flags.toString(), doc.flags.contains("Work"))
        }
        session.setFlags(MessageArgs.flags("""{"uids": 2, "flags": [], "mode": "set"}"""))
        assertTrue(session.getMessage(MessageArgs.get("""{"uid": 2}""")).flags.isEmpty())

        assertEquals(MailErrorCode.MESSAGE_NOT_FOUND, failure { session.setFlags(MessageArgs.flags("""{"uids": [50, 51], "flags": "seen"}""")) }.code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure { session.setFlags(MessageArgs.flags("""{"uids": 1, "flags": "recent"}""")) }.code)
    }

    @Test
    fun moveCopyDeleteAndExpunge() {
        seed("INBOX", 6)
        session.createFolder("Archive")
        session.createFolder("Copies")
        val uidplus = session.imap { it.hasCapability("UIDPLUS") }
        val hasMove = session.imap { it.hasCapability("MOVE") }

        val moved = session.move(MessageArgs.target("""{"uids": [2, 3], "target": "Archive"}"""))
        if (uidplus) assertEquals(listOf(1L, 2L), moved.uids) else assertNull(moved.uids)
        assertEquals(listOf(6L, 5L, 4L, 1L), session.listMessages(MessageArgs.list("{}")).map { it.uid() })
        assertEquals(listOf("seed 3", "seed 2"), session.listMessages(MessageArgs.list("""{"folder": "Archive"}""")).map { it.subject })

        // the COPY + \Deleted + expunge path servers without MOVE take
        session.imap { mailbox ->
            val result = mailbox.move("INBOX", listOf(4L), "Archive", useMove = false)
            if (uidplus) assertEquals(listOf(3L), result.uids) else assertNull(result.uids)
        }
        assertEquals(listOf(6L, 5L, 1L), session.listMessages(MessageArgs.list("{}")).map { it.uid() })
        assertEquals(3, session.folderStatus("Archive").messages)
        assertTrue("MOVE capability: $hasMove", session.listMessages(MessageArgs.list("{}")).none { it.deleted })

        val copied = session.copy(MessageArgs.target("""{"uids": [1, 5, 77], "target": "Copies"}"""))
        if (uidplus) assertEquals(listOf(1L, 2L), copied.uids) else assertNull(copied.uids)
        assertEquals(listOf(6L, 5L, 1L), session.listMessages(MessageArgs.list("{}")).map { it.uid() })
        assertEquals(listOf("seed 5", "seed 1"), session.listMessages(MessageArgs.list("""{"folder": "Copies"}""")).map { it.subject })

        val flagged = session.delete(MessageArgs.delete("""{"uids": [5]}"""))
        assertEquals(listOf(5L), flagged.uids)
        val still = session.listMessages(MessageArgs.list("{}"))
        assertEquals(listOf(6L, 5L, 1L), still.map { it.uid() })
        assertTrue(still.single { it.uid() == 5L }.deleted)
        assertEquals(1, session.expunge("INBOX").count)
        assertEquals(listOf(6L, 1L), session.listMessages(MessageArgs.list("{}")).map { it.uid() })
        assertEquals(0, session.expunge("INBOX").count)

        val gone = session.delete(MessageArgs.delete("""{"uids": [1, 6, 42], "expunge": true}"""))
        assertEquals(listOf(1L, 6L), gone.uids)
        assertTrue(session.listMessages(MessageArgs.list("{}")).isEmpty())
        assertEquals(0, session.folderStatus("INBOX").messages)

        assertEquals(MailErrorCode.MESSAGE_NOT_FOUND, failure { session.delete(MessageArgs.delete("""{"uids": [1]}""")) }.code)
        assertEquals(MailErrorCode.FOLDER_NOT_FOUND, failure { session.move(MessageArgs.target("""{"folder": "Archive", "uids": [1], "target": "Nowhere"}""")) }.code)
        assertEquals(MailErrorCode.MESSAGE_NOT_FOUND, failure { session.copy(MessageArgs.target("""{"folder": "Archive", "uids": [500], "target": "Copies"}""")) }.code)
        assertEquals(3, session.folderStatus("Archive").messages)
    }

    @Test
    fun pop3AccountsAnswerUnsupportedUntilP24() {
        val pop3 = MailAccountOptions.parse(
            """{"address":"$ALICE","user":"$ALICE_LOGIN","pop3":{"host":"$HOST","port":${ServerSetupTest.POP3.port},"tls":"none"}}""",
            SecretKind.PASSWORD,
        )
        val session = MailSession(pop3, MailSecret(ALICE_PASSWORD)).also { sessions += it }
        val error = failure { session.listMessages(MessageArgs.list("{}")) }
        assertEquals(MailErrorCode.UNSUPPORTED_OPERATION, error.code)
        assertTrue(error.message, error.message.contains("P2.4"))
        assertEquals(error, session.lastError)
        assertEquals(0, session.connectCount(MailProtocol.POP3))
    }

    // ------------------------------------------------------------------ helpers

    private fun MessageDocument.uid(): Long = uid.jsonPrimitive.long

    private fun flatten(tree: List<FolderDocument>): List<FolderDocument> = tree.flatMap { listOf(it) + flatten(it.children) }

    private fun seed(folder: String, count: Int, seen: Boolean = false) {
        val flags = if (seen) Flags(Flags.Flag.SEEN) else Flags()
        for (index in 1..count) append(folder, message(subject = "seed $index", text = "body $index", date = Date(1_750_000_000_000L + index * 60_000L)), flags)
    }

    private fun append(folder: String, mime: MimeMessage, flags: Flags) {
        session.imap { it.append(folder, mime, flags) }
    }

    private fun message(subject: String, from: String = ALICE, text: String? = "body", html: String? = null, date: Date = Date(1_750_000_000_000L), headers: Map<String, String> = emptyMap()): MimeMessage {
        val mime = MimeMessage(Session.getInstance(Properties()))
        mime.setFrom(InternetAddress(from))
        mime.setRecipients(jakarta.mail.Message.RecipientType.TO, ALICE)
        mime.setSubject(subject, "UTF-8")
        mime.sentDate = date
        if (html == null) {
            mime.setText(text ?: "", "UTF-8")
        } else {
            val alternative = MimeMultipart("alternative")
            text?.let { alternative.addBodyPart(MimeBodyPart().apply { setText(it, "UTF-8") }) }
            alternative.addBodyPart(MimeBodyPart().apply { setContent(html, "text/html; charset=UTF-8") })
            mime.setContent(alternative)
        }
        mime.saveChanges()
        // after saveChanges, which would replace a caller-supplied Message-ID
        headers.forEach { (name, value) -> mime.setHeader(name, value) }
        return mime
    }

    /** text + html alternative, an inline png (part 2) and a csv attachment (part 3). */
    private fun richMessage(png: ByteArray): MimeMessage {
        val mime = MimeMessage(Session.getInstance(Properties()))
        mime.setFrom(InternetAddress(ALICE, "Alice 王"))
        mime.setRecipients(jakarta.mail.Message.RecipientType.TO, "bob@example.org")
        mime.setSubject("Rich 报表", "UTF-8")
        mime.sentDate = Date(1_750_000_000_000L)
        mime.setHeader("Message-ID", "<rich@example.org>")
        mime.setHeader("In-Reply-To", "<parent@example.org>")
        mime.setHeader("References", "<root@example.org>\r\n <parent@example.org>")
        mime.setHeader("X-Campaign", "spring")
        val alternative = MimeMultipart("alternative")
        alternative.addBodyPart(MimeBodyPart().apply { setText("plain 你好", "UTF-8") })
        alternative.addBodyPart(MimeBodyPart().apply { setContent("<p><b>html</b> 你好</p>", "text/html; charset=UTF-8") })
        val mixed = MimeMultipart("mixed")
        mixed.addBodyPart(MimeBodyPart().apply { setContent(alternative) })
        mixed.addBodyPart(MimeBodyPart().apply {
            dataHandler = DataHandler(ByteArrayDataSource(png, "image/png"))
            fileName = "logo.png"
            contentID = "<logo>"
            disposition = jakarta.mail.Part.INLINE
        })
        mixed.addBodyPart(MimeBodyPart().apply {
            dataHandler = DataHandler(ByteArrayDataSource("id,total\r\n1,42".toByteArray(), "text/csv"))
            fileName = "报表 2026.csv"
            disposition = jakarta.mail.Part.ATTACHMENT
        })
        mime.setContent(mixed)
        mime.saveChanges()
        mime.setHeader("Message-ID", "<rich@example.org>")
        return mime
    }

    private fun failure(block: () -> Any?): MailException {
        try {
            block()
        } catch (e: MailException) {
            return e
        }
        fail("expected a MailException")
        throw AssertionError()
    }

    private fun alice(): MailAccount = MailAccountOptions.parse(
        """{"address":"$ALICE","user":"$ALICE_LOGIN",
            "imap":{"host":"$HOST","port":${ServerSetupTest.IMAP.port},"tls":"none"},
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
