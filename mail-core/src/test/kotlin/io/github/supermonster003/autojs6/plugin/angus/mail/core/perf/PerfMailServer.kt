package io.github.supermonster003.autojs6.plugin.angus.mail.core.perf

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetup
import jakarta.activation.DataHandler
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.util.ByteArrayDataSource
import java.io.File
import java.util.Date
import java.util.Properties
import java.util.Random
import java.util.zip.CRC32

/**
 * A seeded GreenMail server for the P6 performance baseline (`.python/run_performance_baseline.py`):
 * a plain IMAP / SMTP / POP3 server on fixed ports with one account whose INBOX holds `messages`
 * small messages (every hundredth subject carries the marker `标记` for the client-side search)
 * and whose folder `Perf` holds one message with a random attachment of `attachmentMiB` MiB whose
 * subject states its size and CRC32. It runs out of process so the JVM probe and the device test measure their
 * own memory, not the server's; the runner starts it with the test runtime classpath
 * (`:mail-core:writeTestClasspath`), waits for the `READY` line, and ends it with the stop file.
 *
 * Arguments are `key=value`: `imapPort` (3143), `smtpPort` (3025), `pop3Port` (3110), `bind`
 * (0.0.0.0), `messages` (10000), `attachmentMiB` (50), `stopFile`, `readyFile`.
 */
fun main(args: Array<String>) {
    val options = args.mapNotNull { arg -> arg.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
    val imapPort = options["imapPort"]?.toInt() ?: 3143
    val smtpPort = options["smtpPort"]?.toInt() ?: 3025
    val pop3Port = options["pop3Port"]?.toInt() ?: 3110
    val bind = options["bind"] ?: "0.0.0.0"
    val messages = options["messages"]?.toInt() ?: 10_000
    val attachmentMiB = options["attachmentMiB"]?.toInt() ?: 50
    val stopFile = options["stopFile"]?.let(::File)
    val readyFile = options["readyFile"]?.let(::File)

    val setups = arrayOf(
        ServerSetup(imapPort, bind, ServerSetup.PROTOCOL_IMAP),
        ServerSetup(smtpPort, bind, ServerSetup.PROTOCOL_SMTP),
        ServerSetup(pop3Port, bind, ServerSetup.PROTOCOL_POP3),
    ).onEach { it.serverStartupTimeout = 30_000 }
    val greenMail = GreenMail(setups)
    greenMail.start()
    val user = greenMail.setUser(PerfFixtures.ADDRESS, PerfFixtures.LOGIN, PerfFixtures.PASSWORD)
    val session = Session.getInstance(Properties())

    val seedStarted = System.currentTimeMillis()
    val base = System.currentTimeMillis() - messages * 60_000L
    for (index in 1..messages) {
        val message = MimeMessage(session)
        message.setFrom(InternetAddress("sender${index % 97}@example.com", "Sender ${index % 97}"))
        message.setRecipient(Message.RecipientType.TO, InternetAddress(PerfFixtures.ADDRESS, "Perf Inbox"))
        message.subject = PerfFixtures.subject(index)
        message.sentDate = Date(base + index * 60_000L)
        message.setHeader("X-Perf-Index", index.toString())
        message.setText(PerfFixtures.body(index), "UTF-8")
        message.saveChanges()
        user.deliver(message)
    }
    val seededMs = System.currentTimeMillis() - seedStarted

    val attachmentStarted = System.currentTimeMillis()
    val bytes = ByteArray(attachmentMiB * 1024 * 1024)
    Random(PerfFixtures.ATTACHMENT_SEED).nextBytes(bytes)
    val crc = CRC32().apply { update(bytes) }.value
    val big = MimeMessage(session)
    big.setFrom(InternetAddress("sender-big@example.com", "Big Sender"))
    big.setRecipient(Message.RecipientType.TO, InternetAddress(PerfFixtures.ADDRESS, "Perf Inbox"))
    big.subject = PerfFixtures.attachmentSubject(bytes.size.toLong(), crc)
    big.sentDate = Date()
    big.setContent(MimeMultipart().apply {
        addBodyPart(MimeBodyPart().apply { setText("The attachment holds $attachmentMiB MiB of pseudo-random bytes (seed ${PerfFixtures.ATTACHMENT_SEED}).\n", "UTF-8") })
        addBodyPart(MimeBodyPart().apply {
            dataHandler = DataHandler(ByteArrayDataSource(bytes, "application/octet-stream"))
            fileName = PerfFixtures.ATTACHMENT_NAME
            disposition = "attachment"
        })
    })
    big.saveChanges()
    // Its own folder: GreenMail answers every FETCH in O(messages of the selected folder), so the
    // download rows measure the client, not the 10000-message INBOX.
    greenMail.managers.imapHostManager.createMailbox(user, PerfFixtures.ATTACHMENT_FOLDER).store(big)
    val attachmentMs = System.currentTimeMillis() - attachmentStarted

    val ready = "READY imap=$imapPort smtp=$smtpPort pop3=$pop3Port bind=$bind messages=$messages attachmentFolder=${PerfFixtures.ATTACHMENT_FOLDER} attachmentBytes=${bytes.size} crc32=${"%08x".format(crc)} seedMs=$seededMs attachmentMs=$attachmentMs heapMiB=${Runtime.getRuntime().totalMemory() / 1024 / 1024}"
    println(ready)
    System.out.flush()
    readyFile?.writeText(ready + "\n")

    val stdin = Thread {
        runCatching { while (System.`in`.read() >= 0) { /* wait for EOF */ } }
    }.apply { isDaemon = true; start() }
    while (stdin.isAlive && (stopFile == null || !stopFile.exists())) Thread.sleep(500)
    println("STOPPING")
    greenMail.stop()
    readyFile?.delete()
    println("STOPPED")
}

/** Shapes shared by the seeding server, the JVM probe and the device test (the device copies the constants). */
object PerfFixtures {
    const val ADDRESS = "perf@localhost"
    const val LOGIN = "perf"
    const val PASSWORD = "perf-secret"
    const val ATTACHMENT_NAME = "perf-random.bin"
    const val ATTACHMENT_FOLDER = "Perf"
    const val ATTACHMENT_SEED = 42L
    const val MARKER = "标记"
    const val ATTACHMENT_SUBJECT_PREFIX = "Perf attachment "

    fun subject(index: Int): String = if (index % 100 == 0) "Perf marker $MARKER $index" else "Perf message $index"

    fun body(index: Int): String = buildString {
        append("perf body ").append(index).append('\n')
        repeat(6) { append("Line ").append(it).append(" of a short plain-text message used by the P6 performance baseline.\n") }
    }

    fun attachmentSubject(bytes: Long, crc: Long): String = "$ATTACHMENT_SUBJECT_PREFIX$bytes bytes crc32=${"%08x".format(crc)}"

    /** Parses `Perf attachment <bytes> bytes crc32=<hex>` back into (bytes, crc). */
    fun parseAttachmentSubject(subject: String): Pair<Long, Long>? {
        val match = Regex("""Perf attachment (\d+) bytes crc32=([0-9a-f]{8})""").find(subject) ?: return null
        return match.groupValues[1].toLong() to match.groupValues[2].toLong(16)
    }
}
