package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MessageComposer
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.OutgoingMessage
import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import org.eclipse.angus.mail.smtp.SMTPTransport
import java.io.Closeable

/** What [SmtpSender.send] returns: the message as it went out, for the `saveToSent` copy. */
class SentMessage(val mime: MimeMessage, val messageId: String, val accepted: List<String>)

/**
 * Sends messages through the account's SMTP endpoint over one transport that stays connected
 * between calls; the session's [ConnectionGuard] owns its lifetime and idle expiry. Delivery is
 * all-or-nothing: Jakarta's `sendpartial` stays off, so a rejected recipient aborts the
 * transaction before `DATA` and the failure carries the rejected and unsent addresses.
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
        MessageComposer.compose(MailSessionFactory.session(account, MailProtocol.SMTP, secret), account, message)

    /** Sends [message] to every recipient (To, Cc and Bcc) and returns the outgoing message with its `Message-ID`. */
    fun send(message: OutgoingMessage): SentMessage {
        val mime = compose(message)
        val recipients = mime.allRecipients
        val live = connect().transport!!
        trace.timed(MailProtocol.SMTP.id, "send recipients=${recipients.size} attachments=${message.attachments.size}") {
            live.sendMessage(mime, recipients)
        }
        return SentMessage(mime, mime.messageID, message.recipients.map { it.address })
    }

    override fun close() {
        val live = transport ?: return
        transport = null
        try {
            live.close()
        } catch (_: MessagingException) {
        }
    }

    companion object {
        /** Extensions reported by `session.test`. */
        val KNOWN_EXTENSIONS: List<String> = listOf("SIZE", "8BITMIME", "SMTPUTF8", "PIPELINING", "STARTTLS", "AUTH", "DSN", "CHUNKING", "ENHANCEDSTATUSCODES")
    }
}
