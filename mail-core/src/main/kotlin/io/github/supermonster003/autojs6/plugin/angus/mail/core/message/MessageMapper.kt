package io.github.supermonster003.autojs6.plugin.angus.mail.core.message

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.AddressDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.AttachmentDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.FlagMapper
import jakarta.mail.Address
import jakarta.mail.Flags
import jakarta.mail.Header
import jakarta.mail.Message
import jakarta.mail.Part
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeUtility
import kotlinx.serialization.json.JsonPrimitive
import org.eclipse.angus.mail.imap.IMAPMessage
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Jakarta [Message] -> [MessageDocument] (roadmap P2.3 `MessageMapper`). [envelope] reads only
 * what an ENVELOPE / FLAGS / RFC822.SIZE / BODYSTRUCTURE fetch provides, so listings never
 * download bodies; [full] adds the decoded text and html (first part of each kind, html-only
 * mail gets its text derived by [HtmlToText]), every header, and the attachment list, honouring
 * the inline body budget `MAX_INLINE_BODY_BYTES`.
 */
object MessageMapper {

    fun envelope(message: Message, folder: String, uid: JsonPrimitive): MessageDocument {
        val leaves = MimeTree.leaves(message)
        return base(message, folder, uid, leaves)
    }

    fun full(message: Message, folder: String, uid: JsonPrimitive, includeRaw: Boolean = false): MessageDocument {
        val leaves = MimeTree.leaves(message)
        val body = Body.extract(leaves)
        val headers = headers(message)
        val raw = if (includeRaw) raw(message) else null
        return base(message, folder, uid, leaves).copy(
            references = headers.entries.filter { it.key.equals("References", ignoreCase = true) }.flatMap { it.value }.flatMap { it.split(Regex("\\s+")) }.filter { it.isNotBlank() },
            bodyLoaded = true,
            text = body.text,
            html = body.html,
            headers = headers,
            attachments = attachments(leaves),
            bodyTruncated = body.truncated,
            bodyParts = body.leftOut,
            raw = raw?.first,
            rawTruncated = raw?.second ?: false,
        )
    }

    /** The attachment documents of [message] in section order (inline parts included). */
    fun attachments(message: Message): List<AttachmentDocument> = attachments(MimeTree.leaves(message))

    fun attachments(leaves: List<MimeLeaf>): List<AttachmentDocument> {
        val seenText = HashSet<PartKind>()
        return leaves.mapNotNull { leaf ->
            when (leaf.kind) {
                PartKind.ATTACHMENT -> attachment(leaf)
                // Only the first text and the first html part are the body; further ones stay reachable as attachments.
                PartKind.TEXT, PartKind.HTML -> if (seenText.add(leaf.kind)) null else attachment(leaf)
            }
        }
    }

    fun attachment(leaf: MimeLeaf): AttachmentDocument = AttachmentDocument(
        partId = leaf.partId,
        fileName = fileName(leaf),
        mimeType = leaf.mimeType,
        size = leaf.size,
        contentId = leaf.contentId,
        inline = leaf.inline,
    )

    /** A safe file name: the declared one without separators and control characters, else derived from the content id or the part. */
    fun fileName(leaf: MimeLeaf): String {
        val declared = leaf.fileName?.let(OutgoingMessageParser::sanitizeFileName)?.takeIf { it != "attachment" }
        if (declared != null) return declared
        val extension = MimeTypes.extensionFor(leaf.mimeType)?.let { ".$it" } ?: ""
        val stem = leaf.contentId?.let(OutgoingMessageParser::sanitizeFileName)?.takeIf { it != "attachment" }?.substringBefore('@')
        return (stem ?: "part-${leaf.partId}") + extension
    }

    fun headers(message: Message): Map<String, List<String>> {
        val out = LinkedHashMap<String, MutableList<String>>()
        val all = runCatching { message.allHeaders }.getOrNull() ?: return out
        while (all.hasMoreElements()) {
            val header = all.nextElement() as Header
            val value = TextRecovery.repairHeader(MimeUtility.unfold(header.value)) ?: continue
            out.getOrPut(header.name) { ArrayList() }.add(value)
        }
        return out
    }

    private fun base(message: Message, folder: String, uid: JsonPrimitive, leaves: List<MimeLeaf>): MessageDocument {
        val flags = runCatching { message.flags }.getOrDefault(Flags())
        return MessageDocument(
            uid = uid,
            folder = folder,
            messageId = runCatching { (message as? MimeMessage)?.messageID }.getOrNull()?.trim(),
            inReplyTo = inReplyTo(message),
            subject = TextRecovery.repairHeader(runCatching { message.subject }.getOrNull()) ?: "",
            from = addresses(runCatching { message.from }.getOrNull()).firstOrNull(),
            sender = runCatching { (message as? MimeMessage)?.sender }.getOrNull()?.let { address(it) },
            replyTo = addresses(runCatching { message.replyTo }.getOrNull()),
            to = addresses(runCatching { message.getRecipients(Message.RecipientType.TO) }.getOrNull()),
            cc = addresses(runCatching { message.getRecipients(Message.RecipientType.CC) }.getOrNull()),
            bcc = addresses(runCatching { message.getRecipients(Message.RecipientType.BCC) }.getOrNull()),
            date = runCatching { message.sentDate?.time }.getOrNull(),
            receivedDate = runCatching { message.receivedDate?.time }.getOrNull(),
            size = runCatching { message.size.toLong() }.getOrDefault(-1L),
            flags = FlagMapper.toNames(flags),
            seen = flags.contains(Flags.Flag.SEEN),
            flagged = flags.contains(Flags.Flag.FLAGGED),
            answered = flags.contains(Flags.Flag.ANSWERED),
            draft = flags.contains(Flags.Flag.DRAFT),
            deleted = flags.contains(Flags.Flag.DELETED),
            hasAttachments = leaves.any { it.kind == PartKind.ATTACHMENT },
        )
    }

    private fun inReplyTo(message: Message): String? = runCatching {
        when (message) {
            is IMAPMessage -> message.inReplyTo
            is MimeMessage -> message.getHeader("In-Reply-To", null)
            else -> null
        }
    }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    private fun addresses(list: Array<Address>?): List<AddressDocument> = list.orEmpty().map(::address)

    private fun address(address: Address): AddressDocument = when (address) {
        is InternetAddress -> AddressDocument(
            name = TextRecovery.repairHeader(address.personal)?.takeIf { it.isNotBlank() },
            address = TextRecovery.repairLatin1(address.address ?: ""),
        )
        else -> AddressDocument(address = address.toString())
    }

    /** RFC 822 source as a byte-preserving string, or null with the truncated flag when it exceeds the inline budget. */
    private fun raw(message: Message): Pair<String?, Boolean> {
        val size = runCatching { message.size.toLong() }.getOrDefault(-1L)
        if (size > MailLimits.MAX_INLINE_BODY_BYTES) return null to true
        val bytes = ByteArrayOutputStream().also { message.writeTo(it) }.toByteArray()
        if (bytes.size > MailLimits.MAX_INLINE_BODY_BYTES) return null to true
        return String(bytes, StandardCharsets.ISO_8859_1) to false
    }

    /** The decoded body texts within the inline budget. */
    class Body(val text: String?, val html: String?, val truncated: Boolean, val leftOut: List<AttachmentDocument>) {
        companion object {
            fun extract(leaves: List<MimeLeaf>): Body {
                var budget = MailLimits.MAX_INLINE_BODY_BYTES.toLong()
                var text: String? = null
                var html: String? = null
                var truncated = false
                var textLeftOut = false
                val leftOut = ArrayList<AttachmentDocument>()
                for (leaf in leaves) {
                    val wanted = (leaf.kind == PartKind.TEXT && text == null && !textLeftOut) || (leaf.kind == PartKind.HTML && html == null)
                    if (!wanted) continue
                    val size = leaf.size
                    val decoded = if (size > budget) null else BodyExtractor.text(leaf).trimEnd('\r', '\n')
                    val cost = decoded?.toByteArray(StandardCharsets.UTF_8)?.size?.toLong() ?: Long.MAX_VALUE
                    if (decoded == null || cost > budget) {
                        truncated = true
                        if (leaf.kind == PartKind.TEXT) textLeftOut = true
                        leftOut += MessageMapper.attachment(leaf)
                        continue
                    }
                    budget -= cost
                    if (leaf.kind == PartKind.TEXT) text = decoded else html = decoded
                }
                // html-only mail gets a derived text; a text part that was merely left out stays absent (it is in bodyParts)
                if (text == null && html != null && !textLeftOut) text = HtmlToText.convert(html)
                return Body(text, html, truncated, leftOut)
            }
        }
    }
}

/** Reads a text part with the charset tolerance of [TextRecovery]; the transfer encoding is undone by Jakarta. */
object BodyExtractor {

    fun text(leaf: MimeLeaf): String = text(leaf.part, leaf.charset)

    fun text(part: Part, charset: String?): String {
        val bytes = part.inputStream.use { it.readBytes() }
        return TextRecovery.decode(bytes, charset)
    }
}
