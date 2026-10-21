package io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.runtime

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
import java.security.MessageDigest
import java.util.Date
import java.util.Properties

/**
 * Loopback-only IMAP/SMTP/POP3 fixture for physical runtime and generated-application tests.
 * All accounts and content are synthetic. The caller owns the chosen ports and stop/ready files.
 * Run with the existing writeTestClasspath output; arguments are key=value.
 */
fun main(args: Array<String>) {
    val options = args.associate { it.substringBefore('=') to it.substringAfter('=') }
    val imap = options.getValue("imapPort").toInt()
    val smtp = options.getValue("smtpPort").toInt()
    val pop3 = options.getValue("pop3Port").toInt()
    require(listOf(imap, smtp, pop3).all { it in 1024..65535 })
    require(setOf(imap, smtp, pop3).size == 3)
    val ready = File(options.getValue("readyFile"))
    val stop = File(options.getValue("stopFile"))
    require(!ready.exists() && !stop.exists())
    val server = GreenMail(arrayOf(
        ServerSetup(imap, "127.0.0.1", ServerSetup.PROTOCOL_IMAP),
        ServerSetup(smtp, "127.0.0.1", ServerSetup.PROTOCOL_SMTP),
        ServerSetup(pop3, "127.0.0.1", ServerSetup.PROTOCOL_POP3),
    ).onEach { it.serverStartupTimeout = 30_000 })
    try {
        server.start()
        val user = server.setUser(RuntimeMailFixtures.ADDRESS, RuntimeMailFixtures.ADDRESS, RuntimeMailFixtures.PASSWORD)
        val bytes = ByteArray(96 * 1024) { (it % 251).toByte() }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val message = MimeMessage(Session.getInstance(Properties())).apply {
            setFrom(InternetAddress("sender@example.invalid", "Runtime fixture"))
            setRecipient(Message.RecipientType.TO, InternetAddress(RuntimeMailFixtures.ADDRESS))
            subject = RuntimeMailFixtures.SUBJECT
            sentDate = Date(1_700_000_000_000L)
            setContent(MimeMultipart().apply {
                addBodyPart(MimeBodyPart().apply { setText(RuntimeMailFixtures.MARKER.repeat(RuntimeMailFixtures.MARKER_COUNT), "UTF-8") })
                addBodyPart(MimeBodyPart().apply {
                    dataHandler = DataHandler(ByteArrayDataSource(bytes, "application/octet-stream"))
                    fileName = "fixture.bin"
                    disposition = "attachment"
                })
            })
            saveChanges()
        }
        user.deliver(message)
        val info = "{\"imapPort\":" + imap + ",\"smtpPort\":" + smtp + ",\"pop3Port\":" + pop3 +
            ",\"markerCount\":" + RuntimeMailFixtures.MARKER_COUNT + ",\"attachmentBytes\":" + bytes.size +
            ",\"attachmentSha256\":\"" + digest + "\"}"
        ready.writeText(info + "\n")
        println("RUNTIME_MAIL_READY")
        System.out.flush()
        while (!stop.exists()) Thread.sleep(250)
    } finally {
        server.stop()
        println("RUNTIME_MAIL_STOPPED")
    }
}

object RuntimeMailFixtures {
    const val ADDRESS = "runtime@example.invalid"
    const val PASSWORD = "runtime-mail-synthetic"
    const val SUBJECT = "Runtime mail fixture"
    const val MARKER = "Mail \u96ea\uD83D\uDCE7 sample. "
    const val MARKER_COUNT = 5_000
}
