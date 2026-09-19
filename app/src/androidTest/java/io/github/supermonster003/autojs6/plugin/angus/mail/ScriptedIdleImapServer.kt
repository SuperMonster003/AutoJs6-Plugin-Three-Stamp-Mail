package io.github.supermonster003.autojs6.plugin.angus.mail

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * A loopback IMAP server for the watch device tests (roadmap P5): CAPABILITY with IDLE, LOGIN,
 * EXAMINE / SELECT of one INBOX, `UID FETCH` and `FETCH` answering the envelope of every canned
 * message, NOOP, IDLE with `EXISTS` pushes ([push]), CLOSE and LOGOUT. Messages are plain-text
 * envelopes with a subject; every FETCH answers the full item list whatever was asked, which
 * Angus tolerates. Bound to `127.0.0.1` like the other scripted servers.
 */
internal class ScriptedIdleImapServer : AutoCloseable {

    private class Message(val uid: Long, val subject: String)

    private val server = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
    private val sockets = CopyOnWriteArrayList<Socket>()
    private val messages = ArrayList<Message>()
    private val idlers = CopyOnWriteArrayList<(String) -> Unit>()
    private var nextUid = 1L

    val port: Int get() = server.localPort
    val connections = AtomicInteger()
    val logins = AtomicInteger()
    val idles = AtomicInteger()

    init {
        Thread {
            try {
                while (true) {
                    val socket = server.accept()
                    connections.incrementAndGet()
                    sockets += socket
                    Thread { serve(socket) }.apply { isDaemon = true; start() }
                }
            } catch (_: Exception) {
            }
        }.apply { isDaemon = true; start() }
    }

    /** Connections inside IDLE right now. */
    val idling: Int get() = idlers.size

    fun awaitIdling(count: Int, timeoutMs: Long = 10_000) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (idlers.size < count) {
            check(System.nanoTime() < deadline) { "only ${idlers.size} of $count connections reached IDLE within $timeoutMs ms" }
            Thread.sleep(20)
        }
    }

    /** Adds a message and tells every idling connection with an untagged `EXISTS`; returns its UID. */
    fun push(subject: String): Long {
        val uid: Long
        val count: Int
        synchronized(messages) {
            uid = nextUid++
            messages += Message(uid, subject)
            count = messages.size
        }
        idlers.forEach { send -> runCatching { send("* $count EXISTS") } }
        return uid
    }

    private fun serve(socket: Socket) {
        var idleSend: ((String) -> Unit)? = null
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1))
            val writer = OutputStreamWriter(socket.getOutputStream(), StandardCharsets.ISO_8859_1)
            val send: (String) -> Unit = { text ->
                synchronized(writer) {
                    writer.write(text + "\r\n")
                    writer.flush()
                }
            }
            send("* OK scripted IMAP with IDLE ready")
            while (true) {
                val line = reader.readLine() ?: return
                val parts = line.split(' ', limit = 3)
                val tag = parts[0]
                val command = parts.getOrNull(1)?.uppercase() ?: continue
                val rest = parts.getOrNull(2).orEmpty()
                when (command) {
                    "CAPABILITY" -> {
                        send("* CAPABILITY IMAP4rev1 IDLE UIDPLUS")
                        send("$tag OK done")
                    }
                    "LOGIN" -> {
                        logins.incrementAndGet()
                        send("$tag OK logged in")
                    }
                    "EXAMINE", "SELECT" -> {
                        val (count, next) = synchronized(messages) { messages.size to nextUid }
                        send("* $count EXISTS")
                        send("* 0 RECENT")
                        send("* FLAGS (\\Seen)")
                        send("* OK [UIDVALIDITY 1] ok")
                        send("* OK [UIDNEXT $next] ok")
                        send("$tag OK [READ-ONLY] done")
                    }
                    "NOOP" -> {
                        val count = synchronized(messages) { messages.size }
                        send("* $count EXISTS")
                        send("$tag OK noop")
                    }
                    "UID" -> {
                        val sub = rest.substringBefore(' ').uppercase()
                        if (sub == "FETCH") {
                            val set = rest.substringAfter(' ').substringBefore(' ')
                            selectByUid(set).forEach { (seq, message) -> send(fetchLine(seq, message)) }
                        }
                        send("$tag OK done")
                    }
                    "FETCH" -> {
                        val set = rest.substringBefore(' ')
                        selectBySeq(set).forEach { (seq, message) -> send(fetchLine(seq, message)) }
                        send("$tag OK done")
                    }
                    "IDLE" -> {
                        idles.incrementAndGet()
                        send("+ idling")
                        idleSend = send
                        idlers += send
                        try {
                            while (true) {
                                val next = reader.readLine() ?: return
                                if (next.trim().equals("DONE", ignoreCase = true)) break
                            }
                        } finally {
                            idlers -= send
                            idleSend = null
                        }
                        send("$tag OK idle done")
                    }
                    "CLOSE" -> send("$tag OK closed")
                    "LOGOUT" -> {
                        send("* BYE")
                        send("$tag OK bye")
                        return
                    }
                    else -> send("$tag NO unsupported")
                }
            }
        } catch (_: Exception) {
        } finally {
            idleSend?.let { idlers -= it }
            runCatching { socket.close() }
        }
    }

    private fun selectByUid(set: String): List<Pair<Int, Message>> = synchronized(messages) {
        val (low, high) = range(set, Long.MAX_VALUE)
        val selected = messages.withIndex().filter { (_, message) -> message.uid in low..high }.map { (index, message) -> index + 1 to message }
        // `n:*` answers the last message even when every UID is below n (RFC 3501)
        if (selected.isEmpty() && set.endsWith("*") && messages.isNotEmpty()) listOf(messages.size to messages.last()) else selected
    }

    private fun selectBySeq(set: String): List<Pair<Int, Message>> = synchronized(messages) {
        set.split(',').flatMap { part ->
            val (low, high) = range(part, messages.size.toLong())
            messages.withIndex().filter { (index, _) -> (index + 1).toLong() in low..high }.map { (index, message) -> index + 1 to message }
        }
    }

    private fun range(set: String, star: Long): Pair<Long, Long> {
        val pieces = set.split(':')
        val low = pieces[0].let { if (it == "*") star else it.toLong() }
        val high = pieces.getOrNull(1)?.let { if (it == "*") star else it.toLong() } ?: low
        return minOf(low, high) to maxOf(low, high)
    }

    private fun fetchLine(seq: Int, message: Message): String {
        val subject = message.subject.replace("\\", "\\\\").replace("\"", "\\\"")
        val address = "((\"Alice\" NIL \"alice\" \"example.org\"))"
        return "* $seq FETCH (UID ${message.uid} FLAGS () INTERNALDATE \"01-Jan-2026 00:00:00 +0000\" RFC822.SIZE 42 " +
            "ENVELOPE (\"Thu, 1 Jan 2026 00:00:00 +0000\" \"$subject\" $address $address $address ((\"Bob\" NIL \"bob\" \"example.org\")) NIL NIL NIL \"<${message.uid}@example.org>\") " +
            "BODYSTRUCTURE (\"TEXT\" \"PLAIN\" (\"CHARSET\" \"UTF-8\") NIL NIL \"7BIT\" 42 1))"
    }

    override fun close() {
        runCatching { server.close() }
        sockets.forEach { runCatching { it.close() } }
    }
}
