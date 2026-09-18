package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import jakarta.activation.DataHandler
import jakarta.activation.FileDataSource
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.eclipse.angus.mail.smtp.SMTPTransport
import java.io.Closeable
import java.io.File
import java.util.Date

/** Minimal outgoing message; roadmap P2.2 extends it with cc / bcc / inline images / headers / priority. */
data class OutgoingMessage(
    val to: List<String>,
    val subject: String,
    val text: String,
    val html: String? = null,
    val attachments: List<File> = emptyList(),
    val from: String? = null,
) {
    init {
        require(to.isNotEmpty()) { "at least one recipient is required" }
    }
}

/**
 * Sends messages through the account's SMTP endpoint over one transport that stays connected
 * between calls; the session's [ConnectionGuard] owns its lifetime and idle expiry.
 */
class SmtpSender(
    private val account: MailAccount,
    private val secret: MailSecret,
    private val trace: ProtocolTrace = ProtocolTrace.disabled(),
) : Closeable {

    private var transport: SMTPTransport? = null

    val isConnected: Boolean get() = transport?.isConnected == true

    /** Opens the transport when it is not connected yet; returns this for chaining. */
    fun connect(): SmtpSender {
        if (!isConnected) {
            transport?.let { stale -> runCatching { stale.close() } }
            transport = MailSessionFactory.connectTransport(account, secret, trace) as SMTPTransport
        }
        return this
    }

    /** EHLO extensions the server advertised, in [KNOWN_EXTENSIONS] order; connects when needed. */
    fun extensions(): List<String> {
        val live = connect().transport!!
        return KNOWN_EXTENSIONS.filter { live.supportsExtension(it) }
    }

    /** Builds the MIME message without sending it, so tests can parse the exact bytes that would go out. */
    fun compose(message: OutgoingMessage): MimeMessage =
        compose(MailSessionFactory.session(account, MailProtocol.SMTP, secret), message)

    /** Sends [message] and returns its `Message-ID`. */
    fun send(message: OutgoingMessage): String {
        val mime = compose(message)
        val live = connect().transport!!
        trace.timed(MailProtocol.SMTP.id, "send to=${mime.allRecipients.size}") {
            live.sendMessage(mime, mime.allRecipients)
        }
        return mime.messageID
    }

    override fun close() {
        val live = transport ?: return
        transport = null
        try {
            live.close()
        } catch (_: MessagingException) {
        }
    }

    private fun compose(session: Session, message: OutgoingMessage): MimeMessage {
        val mime = MimeMessage(session)
        mime.setFrom(message.from?.let { InternetAddress(it) } ?: InternetAddress(account.address, account.displayName, "UTF-8"))
        mime.setRecipients(Message.RecipientType.TO, message.to.map { InternetAddress(it) }.toTypedArray())
        mime.setSubject(message.subject, "UTF-8")
        mime.sentDate = Date()

        if (message.attachments.isEmpty() && message.html == null) {
            mime.setText(message.text, "UTF-8")
        } else {
            val body = if (message.html == null) {
                MimeBodyPart().apply { setText(message.text, "UTF-8") }
            } else {
                MimeBodyPart().apply {
                    setContent(MimeMultipart("alternative").apply {
                        addBodyPart(MimeBodyPart().apply { setText(message.text, "UTF-8") })
                        addBodyPart(MimeBodyPart().apply { setContent(message.html, "text/html; charset=UTF-8") })
                    })
                }
            }
            mime.setContent(MimeMultipart("mixed").apply {
                addBodyPart(body)
                message.attachments.forEach { file ->
                    addBodyPart(MimeBodyPart().apply {
                        dataHandler = DataHandler(FileDataSource(file))
                        fileName = file.name
                        disposition = MimeBodyPart.ATTACHMENT
                        // Byte-exact attachments: a text part sent as 7bit loses the line break that
                        // precedes the closing MIME boundary and is subject to line-ending rewrites.
                        setHeader("Content-Transfer-Encoding", "base64")
                    })
                }
            })
        }
        mime.saveChanges()
        return mime
    }

    companion object {
        /** Extensions reported by `session.test`. */
        val KNOWN_EXTENSIONS: List<String> = listOf("SIZE", "8BITMIME", "SMTPUTF8", "PIPELINING", "STARTTLS", "AUTH", "DSN", "CHUNKING", "ENHANCEDSTATUSCODES")
    }
}
