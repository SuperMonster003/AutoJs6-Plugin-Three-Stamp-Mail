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
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Jakarta [Message] -> [MessageDocument] (roadmap P2.3 `MessageMapper`). [envelope] reads only
 * what an ENVELOPE / FLAGS / RFC822.SIZE / BODYSTRUCTURE fetch provides, so listings never
 * download bodies; [full] adds the decoded text and html (first part of each kind, html-only
 * mail gets its text derived by [HtmlToText]), every header, and the attachment list, honouring
 * the inline body budget `MAX_INLINE_BODY_BYTES`.
 *
 * Every document is bounded (roadmap P6 hostile input, D39): the four address lists share
 * `MAX_RECIPIENTS`, each address and name is cut to `MAX_ADDRESS_CHARS`, the subject, the ids
 * and every header value to `MAX_HEADER_VALUE_CHARS`, the `headers` map to `MAX_HEADERS_BYTES`,
 * and a body or raw source of unknown size is read no further than the inline budget. So one
 * message document always fits the response envelope, whatever the message carries.
 */
object MessageMapper {

    init {
        MimeLeniency.install()
    }

    fun envelope(message: Message, folder: String, uid: JsonPrimitive): MessageDocument {
        val leaves = MimeTree.leaves(message)
        return base(message, folder, uid, leaves)
    }

    /**
     * POP3 listings (roadmap P2.4): the envelope from the headers a `TOP n 0` delivered, without
     * touching the content. `hasAttachments` is a Content-Type heuristic here (`multipart/mixed`
     * carries attachments by convention); `messages.get` enumerates the real parts.
     */
    fun envelopeFromHeaders(message: Message, folder: String, uid: JsonPrimitive): MessageDocument {
        val contentType = runCatching { message.contentType }.getOrNull()?.substringBefore(';')?.trim()?.lowercase()
        return base(message, folder, uid, emptyList(), hasAttachments = contentType == "multipart/mixed")
    }

    fun full(message: Message, folder: String, uid: JsonPrimitive, includeRaw: Boolean = false): MessageDocument {
        val leaves = MimeTree.leaves(message)
        val body = Body.extract(leaves)
        val headers = headers(message)
        val raw = if (includeRaw) raw(message) else null
        return base(message, folder, uid, leaves).copy(
            references = headers.entries.filter { it.key.equals("References", ignoreCase = true) }.flatMap { it.value }.flatMap { it.split(WHITESPACE) }.filter { it.isNotBlank() },
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

    /** Every header within the document bounds: names and values cut, a header that does not fit the byte budget dropped. */
    fun headers(message: Message): Map<String, List<String>> {
        val out = LinkedHashMap<String, MutableList<String>>()
        val all = runCatching { message.allHeaders }.getOrNull() ?: return out
        var budget = MailLimits.MAX_HEADERS_BYTES
        while (all.hasMoreElements()) {
            val header = all.nextElement() as Header
            val name = clamp(header.name, MailLimits.MAX_ADDRESS_CHARS)
            val value = TextRecovery.repairHeader(MimeUtility.unfold(clamp(header.value ?: "", MailLimits.MAX_HEADER_VALUE_CHARS)))
                ?.let { clamp(it, MailLimits.MAX_HEADER_VALUE_CHARS) } ?: continue
            val cost = utf8Length(name) + utf8Length(value)
            if (cost > budget) continue
            budget -= cost
            out.getOrPut(name) { ArrayList() }.add(value)
        }
        return out
    }

    /** Cuts [text] to [max] UTF-16 units without splitting a surrogate pair. */
    fun clamp(text: String, max: Int): String {
        if (text.length <= max) return text
        val end = if (max > 0 && Character.isHighSurrogate(text[max - 1])) max - 1 else max
        return text.substring(0, end)
    }

    private fun utf8Length(text: String): Int {
        var length = 0
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            length += when {
                codePoint < 0x80 -> 1
                codePoint < 0x800 -> 2
                codePoint < 0x10000 -> 3
                else -> 4
            }
            index += Character.charCount(codePoint)
        }
        return length
    }

    private fun base(message: Message, folder: String, uid: JsonPrimitive, leaves: List<MimeLeaf>, hasAttachments: Boolean = leaves.any { it.kind == PartKind.ATTACHMENT }): MessageDocument {
        val flags = runCatching { message.flags }.getOrDefault(Flags())
        val budget = AddressBudget(MailLimits.MAX_RECIPIENTS)
        val to = budget.take(runCatching { message.getRecipients(Message.RecipientType.TO) }.getOrNull())
        val cc = budget.take(runCatching { message.getRecipients(Message.RecipientType.CC) }.getOrNull())
        val bcc = budget.take(runCatching { message.getRecipients(Message.RecipientType.BCC) }.getOrNull())
        val replyTo = budget.take(runCatching { message.replyTo }.getOrNull())
        return MessageDocument(
            uid = uid,
            folder = folder,
            messageId = runCatching { (message as? MimeMessage)?.messageID }.getOrNull()?.trim()?.let { clamp(it, MailLimits.MAX_HEADER_VALUE_CHARS) },
            inReplyTo = inReplyTo(message)?.let { clamp(it, MailLimits.MAX_HEADER_VALUE_CHARS) },
            subject = clamp(TextRecovery.repairHeader(runCatching { message.subject }.getOrNull()) ?: "", MailLimits.MAX_HEADER_VALUE_CHARS),
            from = runCatching { message.from }.getOrNull()?.firstOrNull()?.let(::address),
            sender = runCatching { (message as? MimeMessage)?.sender }.getOrNull()?.let(::address),
            replyTo = replyTo,
            to = to,
            cc = cc,
            bcc = bcc,
            date = runCatching { message.sentDate?.time }.getOrNull(),
            receivedDate = runCatching { message.receivedDate?.time }.getOrNull(),
            size = runCatching { message.size.toLong() }.getOrDefault(-1L),
            flags = FlagMapper.toNames(flags),
            seen = flags.contains(Flags.Flag.SEEN),
            flagged = flags.contains(Flags.Flag.FLAGGED),
            answered = flags.contains(Flags.Flag.ANSWERED),
            draft = flags.contains(Flags.Flag.DRAFT),
            deleted = flags.contains(Flags.Flag.DELETED),
            hasAttachments = hasAttachments,
        )
    }

    private fun inReplyTo(message: Message): String? = runCatching {
        when (message) {
            is IMAPMessage -> message.inReplyTo
            is MimeMessage -> message.getHeader("In-Reply-To", null)
            else -> null
        }
    }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    /** The addresses of a document, `MAX_RECIPIENTS` in total across the lists it is applied to. */
    private class AddressBudget(private var remaining: Int) {
        fun take(list: Array<Address>?): List<AddressDocument> {
            if (list == null || list.isEmpty() || remaining <= 0) return emptyList()
            val kept = list.take(remaining).map(::address)
            remaining -= kept.size
            return kept
        }
    }

    private fun address(address: Address): AddressDocument = when (address) {
        is InternetAddress -> AddressDocument(
            name = address.personal?.let { clamp(it, MailLimits.MAX_HEADER_VALUE_CHARS) }?.let(TextRecovery::repairHeader)?.takeIf { it.isNotBlank() }?.let { clamp(it, MailLimits.MAX_ADDRESS_CHARS) },
            address = clamp(TextRecovery.repairLatin1(address.address ?: ""), MailLimits.MAX_ADDRESS_CHARS),
        )
        else -> AddressDocument(address = clamp(address.toString(), MailLimits.MAX_ADDRESS_CHARS))
    }

    /** RFC 822 source as a byte-preserving string, or null with the truncated flag when it exceeds the inline budget. */
    private fun raw(message: Message): Pair<String?, Boolean> {
        val size = runCatching { message.size.toLong() }.getOrDefault(-1L)
        if (size > MailLimits.MAX_INLINE_BODY_BYTES) return null to true
        val sink = BoundedBytes(MailLimits.MAX_INLINE_BODY_BYTES)
        return try {
            message.writeTo(sink)
            String(sink.toByteArray(), StandardCharsets.ISO_8859_1) to false
        } catch (_: BoundedBytes.Overflow) {
            null to true
        }
    }

    /** A byte sink that refuses to grow past its limit, so a source of unknown size never fills the heap. */
    private class BoundedBytes(private val limit: Int) : ByteArrayOutputStream() {
        class Overflow : IOException("the inline budget is exhausted")

        override fun write(b: Int) {
            if (count + 1 > limit) throw Overflow()
            super.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            if (count + len > limit) throw Overflow()
            super.write(b, off, len)
        }
    }

    private val WHITESPACE = Regex("\\s+")

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
                    // a part of unknown size is read up to the remaining budget and no further
                    val decoded = if (size > budget) null else BodyExtractor.bytes(leaf.part, budget)?.let { TextRecovery.decode(it, leaf.charset).trimEnd('\r', '\n') }
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

    private const val BUFFER_BYTES = 64 * 1024

    fun text(leaf: MimeLeaf): String = text(leaf.part, leaf.charset)

    fun text(part: Part, charset: String?): String {
        val bytes = part.inputStream.use { it.readBytes() }
        return TextRecovery.decode(bytes, charset)
    }

    /** The decoded bytes of [part], or null as soon as they exceed [limit] (nothing beyond the limit is buffered). */
    fun bytes(part: Part, limit: Long): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        part.inputStream.use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (out.size() + read > limit) return null
                out.write(buffer, 0, read)
            }
        }
        return out.toByteArray()
    }
}
