package io.github.supermonster003.autojs6.plugin.angus.mail.core.message

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.internet.ContentType
import jakarta.mail.internet.MimePart

/** How a leaf part is presented to scripts. */
enum class PartKind { TEXT, HTML, ATTACHMENT }

/**
 * One leaf of a MIME tree with its IMAP section id (`1`, `2.1`, ...): the id scripts use as
 * `partId` for `attachments.download`. `message/rfc822` parts are leaves (downloadable as `.eml`);
 * their inner structure is not enumerated.
 */
class MimeLeaf(
    val partId: String,
    val part: Part,
    val kind: PartKind,
    val mimeType: String,
    val charset: String?,
    val fileName: String?,
    val contentId: String?,
    val inline: Boolean,
) {
    /** Encoded size the server or the parsed message reports; -1 when unknown. */
    val size: Long get() = runCatching { part.size.toLong() }.getOrDefault(-1L)
}

/**
 * Walks a message or part tree without reading leaf content: on IMAP the tree comes from the
 * BODYSTRUCTURE fetch and every `Multipart` / nested message is materialized lazily, so listing
 * envelopes with `hasAttachments` costs no body download.
 *
 * Hostile trees are bounded (roadmap P6, D39): a multipart nested deeper than `MAX_MIME_DEPTH`
 * is one leaf (downloadable as the raw multipart), the walk stops after `MAX_MIME_PARTS` leaves,
 * and a multipart whose parts cannot be parsed (none found although it has content) is one leaf
 * as well. Declared file names and content ids are cut to the document bounds before anything
 * else looks at them.
 */
object MimeTree {

    init {
        MimeLeniency.install()
    }

    fun leaves(root: Part): List<MimeLeaf> {
        val out = ArrayList<MimeLeaf>()
        walk(root, "", 0, out)
        return out
    }

    fun find(root: Part, partId: String): MimeLeaf? = leaves(root).firstOrNull { it.partId == partId }

    private fun walk(part: Part, prefix: String, depth: Int, out: MutableList<MimeLeaf>) {
        if (out.size >= MailLimits.MAX_MIME_PARTS) return
        val id = prefix.ifEmpty { "1" }
        if (!isMultipart(part) || depth >= MailLimits.MAX_MIME_DEPTH) {
            out += leaf(part, id)
            return
        }
        val multipart = runCatching { part.content as? Multipart }.getOrNull()
        val count = multipart?.let { runCatching { it.count }.getOrNull() }
        // unparsable (no parts although there is content): one leaf, so the content stays downloadable
        if (multipart == null || count == null || (count == 0 && runCatching { part.size }.getOrDefault(0) > 0)) {
            out += leaf(part, id)
            return
        }
        for (index in 0 until count) {
            if (out.size >= MailLimits.MAX_MIME_PARTS) return
            val child = runCatching { multipart.getBodyPart(index) }.getOrNull() ?: continue
            walk(child, if (prefix.isEmpty()) "${index + 1}" else "$prefix.${index + 1}", depth + 1, out)
        }
    }

    private fun isMultipart(part: Part): Boolean = runCatching { part.isMimeType("multipart/*") }.getOrDefault(false)

    private fun leaf(part: Part, partId: String): MimeLeaf {
        val contentType = runCatching { ContentType(part.contentType) }.getOrNull()
        val mimeType = contentType?.baseType?.lowercase()?.takeIf(MimeTypes::isValid) ?: MimeTypes.OCTET_STREAM
        val charset = contentType?.getParameter("charset")
        val disposition = runCatching { part.disposition?.lowercase() }.getOrNull()
        val fileName = runCatching { part.fileName }.getOrNull()
            ?.let { MessageMapper.clamp(it, MailLimits.MAX_HEADER_VALUE_CHARS) }
            ?.let(TextRecovery::repairHeader)?.trim()?.takeIf { it.isNotEmpty() }
        val contentId = runCatching { (part as? MimePart)?.contentID }.getOrNull()
            ?.trim()?.removePrefix("<")?.removeSuffix(">")?.takeIf { it.isNotEmpty() }
            ?.let { MessageMapper.clamp(it, MailLimits.MAX_ADDRESS_CHARS) }
        val isBody = disposition != Part.ATTACHMENT && fileName == null
        val kind = when {
            isBody && mimeType == "text/plain" -> PartKind.TEXT
            isBody && mimeType == "text/html" -> PartKind.HTML
            else -> PartKind.ATTACHMENT
        }
        val inline = kind == PartKind.ATTACHMENT && (disposition == Part.INLINE || (disposition == null && contentId != null))
        return MimeLeaf(partId, part, kind, mimeType, charset, fileName, contentId, inline)
    }
}
