package io.github.supermonster003.autojs6.plugin.angus.mail.core.message

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import jakarta.activation.DataHandler
import jakarta.activation.DataSource
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.internet.MimeUtility
import java.io.InputStream
import java.io.OutputStream
import java.util.Date

/**
 * Builds the MIME tree of an [OutgoingMessage] (roadmap P2.2):
 *
 * ```
 * multipart/mixed                      (only with regular attachments)
 *   multipart/related                  (only with inline parts)
 *     multipart/alternative            (only with text and html)
 *       text/plain; charset=UTF-8
 *       text/html; charset=UTF-8
 *     inline parts (Content-ID, Content-Disposition: inline)
 *   attachments (Content-Disposition: attachment, base64)
 * ```
 *
 * Display names, subject and file names are RFC 2047 encoded by Jakarta Mail; custom headers are
 * encoded and folded here. The composer never touches the network.
 */
object MessageComposer {

    fun compose(session: Session, account: MailAccount, message: OutgoingMessage): MimeMessage {
        val mime = MimeMessage(session)
        val from = message.from ?: MailAddressSpec(account.address, account.displayName)
        mime.setFrom(from.toInternetAddress())
        mime.setRecipients(Message.RecipientType.TO, message.to.map { it.toInternetAddress() }.toTypedArray())
        if (message.cc.isNotEmpty()) mime.setRecipients(Message.RecipientType.CC, message.cc.map { it.toInternetAddress() }.toTypedArray())
        if (message.bcc.isNotEmpty()) mime.setRecipients(Message.RecipientType.BCC, message.bcc.map { it.toInternetAddress() }.toTypedArray())
        if (message.replyTo.isNotEmpty()) mime.replyTo = message.replyTo.map { it.toInternetAddress() }.toTypedArray()
        mime.setSubject(message.subject, "UTF-8")
        mime.sentDate = message.dateMillis?.let(::Date) ?: Date()
        message.inReplyTo?.let { mime.setHeader("In-Reply-To", it) }
        if (message.references.isNotEmpty()) mime.setHeader("References", MimeUtility.fold(12, message.references.joinToString(" ")))
        if (message.priority != Priority.NORMAL) {
            mime.setHeader("X-Priority", message.priority.xPriority)
            mime.setHeader("X-MSMail-Priority", message.priority.importance)
            mime.setHeader("Importance", message.priority.importance)
        }
        message.headers.forEach { (name, value) ->
            mime.addHeader(name, MimeUtility.fold(name.length + 2, MimeUtility.encodeText(value, "UTF-8", null)))
        }

        val inline = message.attachments.filter { it.inline && it.contentId != null }
        val regular = message.attachments - inline.toSet()
        val body = bodyPart(message)
        val related = if (inline.isEmpty()) body else MimeBodyPart().apply {
            setContent(MimeMultipart("related").apply {
                addBodyPart(body)
                inline.forEach { addBodyPart(attachmentPart(it)) }
            })
        }
        if (regular.isEmpty()) {
            copyContent(related, mime)
        } else {
            mime.setContent(MimeMultipart("mixed").apply {
                addBodyPart(related)
                regular.forEach { addBodyPart(attachmentPart(it)) }
            })
        }
        mime.saveChanges()
        return mime
    }

    private fun bodyPart(message: OutgoingMessage): MimeBodyPart {
        val text = message.text
        val html = message.html
        return when {
            text != null && html != null -> MimeBodyPart().apply {
                setContent(MimeMultipart("alternative").apply {
                    addBodyPart(textPart(text))
                    addBodyPart(htmlPart(html))
                })
            }
            html != null -> htmlPart(html)
            else -> textPart(text.orEmpty())
        }
    }

    private fun textPart(text: String): MimeBodyPart = MimeBodyPart().apply { setText(text, "UTF-8") }

    private fun htmlPart(html: String): MimeBodyPart = MimeBodyPart().apply { setContent(html, "text/html; charset=UTF-8") }

    private fun attachmentPart(attachment: OutgoingAttachment): MimeBodyPart = MimeBodyPart().apply {
        dataHandler = DataHandler(AttachmentDataSource(attachment))
        fileName = attachment.fileName
        disposition = if (attachment.inline) MimeBodyPart.INLINE else MimeBodyPart.ATTACHMENT
        attachment.contentId?.let { setContentID("<$it>") }
        // Byte-exact attachments: a text part sent as 7bit loses the line break that precedes the
        // closing MIME boundary and is subject to line-ending rewrites.
        setHeader("Content-Transfer-Encoding", "base64")
    }

    /** Moves the content of a single body part into the message so simple mails stay single-part. */
    private fun copyContent(part: MimeBodyPart, mime: MimeMessage) {
        val content = part.content
        if (content is MimeMultipart) {
            mime.setContent(content)
        } else {
            // The data handler carries the exact content type (text/html; charset=UTF-8); the part's
            // Content-Type header only exists once its own headers have been updated.
            mime.dataHandler = part.dataHandler
        }
    }

    private class AttachmentDataSource(private val attachment: OutgoingAttachment) : DataSource {
        override fun getInputStream(): InputStream = attachment.source.open()
        override fun getOutputStream(): OutputStream = throw UnsupportedOperationException("read-only attachment")
        override fun getContentType(): String = attachment.mimeType
        override fun getName(): String = attachment.fileName
    }
}
