package io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.message

import jakarta.mail.Address
import jakarta.mail.Flags
import jakarta.mail.Message
import jakarta.mail.UIDFolder
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeUtility

/** Envelope-level view of a message as listed by `ImapMailbox` and `Pop3Mailbox` (roadmap appendix B.2 `MailMessage` subset). */
data class MessageSummary(
    /** IMAP UID; null for POP3, where only the sequence number is stable within one session. */
    val uid: Long?,
    val number: Int,
    val subject: String?,
    val from: List<String>,
    val to: List<String>,
    val sentAtMillis: Long?,
    val seen: Boolean,
    val size: Int,
) {
    companion object {
        fun of(message: Message): MessageSummary {
            val uid = (message.folder as? UIDFolder)?.getUID(message)?.takeIf { it >= 0 }
            return MessageSummary(
                uid = uid,
                number = message.messageNumber,
                subject = message.subject?.let(::decode),
                from = message.from.orEmpty().map { it.display() },
                to = message.getRecipients(Message.RecipientType.TO).orEmpty().map { it.display() },
                sentAtMillis = message.sentDate?.time,
                seen = message.isSet(Flags.Flag.SEEN),
                size = message.size,
            )
        }

        private fun decode(text: String): String = runCatching { MimeUtility.decodeText(text) }.getOrDefault(text)

        private fun Address.display(): String = when (this) {
            is InternetAddress -> if (personal.isNullOrBlank()) address else "$personal <$address>"
            else -> toString()
        }
    }
}
