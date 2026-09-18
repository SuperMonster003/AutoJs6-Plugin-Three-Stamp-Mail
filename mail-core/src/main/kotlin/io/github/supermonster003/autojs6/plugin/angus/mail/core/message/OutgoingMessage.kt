package io.github.supermonster003.autojs6.plugin.angus.mail.core.message

import jakarta.mail.internet.InternetAddress
import java.io.File

/** One address of an outgoing message; [name] is the display name, encoded per RFC 2047 when needed. */
data class MailAddressSpec(val address: String, val name: String? = null) {
    init {
        require(address.isNotBlank()) { "address must not be blank" }
    }

    fun toInternetAddress(): InternetAddress = InternetAddress(address, name?.takeIf { it.isNotBlank() }, "UTF-8")

    override fun toString(): String = if (name.isNullOrBlank()) address else "$name <$address>"
}

/** `priority` of `MailSendMessage`: mapped to `X-Priority`, `X-MSMail-Priority` and `Importance`. */
enum class Priority(val id: String, val xPriority: String, val importance: String) {
    HIGH("high", "1", "High"),
    NORMAL("normal", "3", "Normal"),
    LOW("low", "5", "Low"),
    ;

    companion object {
        fun fromId(id: String): Priority? = entries.firstOrNull { it.id == id }
    }
}

/**
 * An attachment of an outgoing message. Inline parts with a [contentId] are referenced from the
 * HTML body as `cid:` and travel in `multipart/related`; everything else goes to `multipart/mixed`.
 */
class OutgoingAttachment(
    val fileName: String,
    val mimeType: String,
    val contentId: String? = null,
    val inline: Boolean = false,
    val source: AttachmentSource,
) {
    init {
        require(fileName.isNotBlank()) { "fileName must not be blank" }
        require(mimeType.isNotBlank()) { "mimeType must not be blank" }
    }

    val size: Long get() = source.size

    override fun toString(): String = "OutgoingAttachment($fileName, $mimeType, ${size}B${if (inline) ", inline" else ""})"
}

/** The `MailSendMessage` of the script API (roadmap appendix A.3) after validation. */
data class OutgoingMessage(
    val to: List<MailAddressSpec>,
    val subject: String,
    val text: String? = null,
    val html: String? = null,
    val cc: List<MailAddressSpec> = emptyList(),
    val bcc: List<MailAddressSpec> = emptyList(),
    val replyTo: List<MailAddressSpec> = emptyList(),
    val attachments: List<OutgoingAttachment> = emptyList(),
    /** Extra headers in order; names are validated by the parser, values are encoded by the composer. */
    val headers: List<Pair<String, String>> = emptyList(),
    val priority: Priority = Priority.NORMAL,
    val inReplyTo: String? = null,
    val references: List<String> = emptyList(),
    /** UTC milliseconds of the `Date` header; null means the time of sending. */
    val dateMillis: Long? = null,
    /** Overrides the account's `From`; null uses the account address and display name. */
    val from: MailAddressSpec? = null,
) {
    init {
        require(to.isNotEmpty() || cc.isNotEmpty() || bcc.isNotEmpty()) { "at least one recipient is required" }
    }

    val recipients: List<MailAddressSpec> get() = to + cc + bcc

    companion object {
        /** The P0 shape: plain recipients, optional HTML, file attachments. */
        fun simple(to: List<String>, subject: String, text: String, html: String? = null, attachments: List<File> = emptyList()): OutgoingMessage =
            OutgoingMessage(
                to = to.map { MailAddressSpec(it) },
                subject = subject,
                text = text,
                html = html,
                attachments = attachments.map { file ->
                    OutgoingAttachment(file.name, MimeTypes.forName(file.name), source = FileAttachmentSource(file))
                },
            )
    }
}
