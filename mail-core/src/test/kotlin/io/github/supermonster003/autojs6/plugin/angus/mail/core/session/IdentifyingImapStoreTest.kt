package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import jakarta.mail.Folder
import jakarta.mail.MessagingException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/**
 * Drives [ImapMailbox] against a scripted IMAP server that behaves like NetEase: every
 * connection has to send `ID` before it may `SELECT` or `EXAMINE`, otherwise the server answers
 * `NO ... Unsafe Login`. GreenMail cannot stand in here because it does not implement the `ID`
 * extension. The nested folder open below forces Angus Mail to take a second connection from
 * its pool, which is the case that failed against 163 on 2026-09-18.
 */
class IdentifyingImapStoreTest {

    private lateinit var server: FakeImapServer

    @Before
    fun start() {
        server = FakeImapServer(advertiseId = true).also { it.start() }
    }

    @After
    fun stop() {
        server.close()
    }

    @Test
    fun everyConnectionIdentifiesItselfBeforeOpeningAFolder() {
        val account = account("""{"name":"AutoJs6-Plugin-Angus-Mail","version":"1.0.0"}""")
        ImapMailbox.connect(account, MailSecret(PASSWORD)).use { mailbox ->
            assertEquals(mapOf("name" to "FakeIMAP"), mailbox.serverId)
            val count = mailbox.withFolder("INBOX", Folder.READ_ONLY) {
                // Needs a second pooled connection while INBOX holds the first one.
                mailbox.withFolder("Archive", Folder.READ_ONLY) { inner -> inner.messageCount }
            }
            assertEquals(0, count)
        }
        assertEquals("two connections", 2, server.sessions.size)
        server.sessions.forEachIndexed { index, session ->
            val commands = session.commands.toList()
            val login = commands.indexOf("LOGIN")
            val id = commands.indexOf("ID")
            val open = commands.indexOfFirst { it == "SELECT" || it == "EXAMINE" }
            assertTrue("connection ${index + 1} logs in before identifying: $commands", login in 0 until id)
            assertTrue("connection ${index + 1} identifies before opening a folder: $commands", id in 0 until open)
            assertEquals("connection ${index + 1} identifies once: $commands", 1, commands.count { it == "ID" })
            val payload = session.idPayload.orEmpty()
            assertTrue(payload, payload.contains("AutoJs6-Plugin-Angus-Mail") && payload.contains("1.0.0"))
        }
    }

    @Test
    fun withoutAClientIdNothingIsSentAndTheServerRefusesTheFolder() {
        ImapMailbox.connect(account("{}"), MailSecret(PASSWORD)).use { mailbox ->
            assertNull(mailbox.serverId)
            try {
                mailbox.withFolder("INBOX", Folder.READ_ONLY) { }
                fail("the server refuses EXAMINE on an unidentified connection")
            } catch (e: MessagingException) {
                assertTrue(e.message.orEmpty(), e.message.orEmpty().contains("Unsafe Login"))
            }
        }
        assertTrue(server.sessions.none { "ID" in it.commands })
    }

    @Test
    fun serversWithoutTheIdCapabilityAreLeftAlone() {
        server.close()
        server = FakeImapServer(advertiseId = false).also { it.start() }
        ImapMailbox.connect(account("""{"name":"AutoJs6-Plugin-Angus-Mail"}"""), MailSecret(PASSWORD)).use { mailbox ->
            assertNull(mailbox.serverId)
            assertEquals(0, mailbox.withFolder("INBOX", Folder.READ_ONLY) { it.messageCount })
        }
        assertTrue(server.sessions.isNotEmpty())
        assertTrue(server.sessions.none { "ID" in it.commands })
    }

    @Test
    fun deleteFallsBackToExpungeWhenTheServerCannotParseUidExpunge() {
        server.messages = 1
        ImapMailbox.connect(account("""{"name":"AutoJs6-Plugin-Angus-Mail"}"""), MailSecret(PASSWORD)).use { mailbox ->
            assertEquals(listOf(1L), mailbox.delete("INBOX", listOf(1L), expunge = true).imapUids)
            assertTrue(mailbox.uidExpungeRefused)
        }
        val folderSession = server.sessions.first { "UID EXPUNGE" in it.commands }
        val commands = folderSession.commands.toList()
        assertTrue(commands.toString(), commands.indexOf("UID EXPUNGE") in 0 until commands.indexOf("EXPUNGE"))
        assertEquals(0, server.messages)
    }

    private fun account(clientIdJson: String): MailAccount = MailAccountOptions.parse(
        """{"address":"alice@example.org","user":"alice",
            "imap":{"host":"127.0.0.1","port":${server.port},"tls":"none"},
            "smtp":{"host":"127.0.0.1","port":${server.port},"tls":"none"},
            "clientId":$clientIdJson,
            "timeout":{"connect":5000,"read":10000}}""",
        SecretKind.PASSWORD,
    )

    private companion object {
        const val PASSWORD = "alice-secret"
    }
}

/**
 * A minimal scripted IMAP server for [IdentifyingImapStoreTest]: it understands only the
 * commands Angus Mail issues there and, when [advertiseId] is set, refuses `SELECT` / `EXAMINE`
 * on connections that have not sent `ID`, like NetEase does.
 */
internal class FakeImapServer(private val advertiseId: Boolean) : Closeable {

    class Session {
        val commands = CopyOnWriteArrayList<String>()

        @Volatile
        var idPayload: String? = null

        @Volatile
        var identified: Boolean = false
    }

    private val socket = ServerSocket(0, 8, InetAddress.getLoopbackAddress())
    private val executor = Executors.newCachedThreadPool { runnable -> Thread(runnable, "fake-imap").apply { isDaemon = true } }
    val sessions = CopyOnWriteArrayList<Session>()
    val port: Int get() = socket.localPort

    /** How many messages every folder reports; message n has UID n. */
    @Volatile
    var messages: Int = 0

    fun start() {
        executor.execute {
            while (!socket.isClosed) {
                val client = try {
                    socket.accept()
                } catch (_: IOException) {
                    break
                }
                executor.execute { serve(client) }
            }
        }
    }

    private fun serve(client: Socket) {
        val session = Session().also { sessions += it }
        client.use { connection ->
            val reader = BufferedReader(InputStreamReader(connection.getInputStream(), Charsets.ISO_8859_1))
            val writer = PrintWriter(OutputStreamWriter(connection.getOutputStream(), Charsets.ISO_8859_1))
            fun send(line: String) {
                writer.print(line)
                writer.print(CRLF)
                writer.flush()
            }
            send("* OK Fake IMAP ready")
            while (true) {
                val line = reader.readLine() ?: return
                val parts = line.split(' ', limit = 3)
                val tag = parts[0]
                val command = parts.getOrElse(1) { "" }.uppercase()
                session.commands += command
                when (command) {
                    "CAPABILITY" -> {
                        send(if (advertiseId) "* CAPABILITY IMAP4rev1 UIDPLUS ID" else "* CAPABILITY IMAP4rev1 UIDPLUS")
                        send("$tag OK CAPABILITY completed")
                    }
                    "UID" -> {
                        val sub = parts.getOrElse(2) { "" }.substringBefore(' ').uppercase()
                        session.commands += "UID $sub"
                        when (sub) {
                            // Every message has the UID of its sequence number.
                            "FETCH" -> {
                                for (number in 1..messages) send("* $number FETCH (UID $number)")
                                send("$tag OK UID FETCH completed")
                            }
                            // NetEase advertises UIDPLUS and still cannot parse UID EXPUNGE.
                            "EXPUNGE" -> send("$tag BAD Parse command error")
                            else -> send("$tag OK UID $sub completed")
                        }
                    }
                    "EXPUNGE" -> {
                        for (number in messages downTo 1) send("* $number EXPUNGE")
                        messages = 0
                        send("$tag OK EXPUNGE completed")
                    }
                    "LOGIN" -> send("$tag OK LOGIN completed")
                    "LIST" -> {
                        // Angus turns a refused open into "not found" unless LIST confirms the folder.
                        val mailbox = parts.getOrElse(2) { "" }.substringAfterLast(' ').trim('"')
                        send("""* LIST () "/" "$mailbox"""")
                        send("$tag OK LIST completed")
                    }
                    "ID" -> if (advertiseId) {
                        session.identified = true
                        session.idPayload = parts.getOrNull(2)
                        send("""* ID ("name" "FakeIMAP")""")
                        send("$tag OK ID completed")
                    } else {
                        send("$tag BAD ID not supported")
                    }
                    "SELECT", "EXAMINE" -> if (session.identified || !advertiseId) {
                        send("* $messages EXISTS")
                        send("* 0 RECENT")
                        send("* OK [UIDVALIDITY 1] UIDs valid")
                        send("$tag OK [${if (command == "SELECT") "READ-WRITE" else "READ-ONLY"}] $command completed")
                    } else {
                        send("$tag NO $command Unsafe Login. Please contact support for help")
                    }
                    "LOGOUT" -> {
                        send("* BYE Fake IMAP signing off")
                        send("$tag OK LOGOUT completed")
                        return
                    }
                    else -> send("$tag OK $command completed")
                }
            }
        }
    }

    override fun close() {
        socket.close()
        executor.shutdownNow()
    }

    private companion object {
        val CRLF: String = String(charArrayOf(13.toChar(), 10.toChar()))
    }
}
