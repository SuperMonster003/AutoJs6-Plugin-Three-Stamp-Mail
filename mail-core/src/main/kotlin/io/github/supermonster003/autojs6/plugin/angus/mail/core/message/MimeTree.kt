package io.github.supermonster003.autojs6.plugin.angus.mail.core.message

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
 */
object MimeTree {

    fun leaves(root: Part): List<MimeLeaf> {
        val out = ArrayList<MimeLeaf>()
        walk(root, "", out)
        return out
    }

    fun find(root: Part, partId: String): MimeLeaf? = leaves(root).firstOrNull { it.partId == partId }

    private fun walk(part: Part, prefix: String, out: MutableList<MimeLeaf>) {
        if (isMultipart(part)) {
            val multipart = runCatching { part.content as? Multipart }.getOrNull()
            if (multipart == null) {
                out += leaf(part, if (prefix.isEmpty()) "1" else prefix)
                return
            }
            for (index in 0 until multipart.count) {
                val child = multipart.getBodyPart(index)
                val id = if (prefix.isEmpty()) "${index + 1}" else "$prefix.${index + 1}"
                walk(child, id, out)
            }
            return
        }
        out += leaf(part, if (prefix.isEmpty()) "1" else prefix)
    }

    private fun isMultipart(part: Part): Boolean = runCatching { part.isMimeType("multipart/*") }.getOrDefault(false)

    private fun leaf(part: Part, partId: String): MimeLeaf {
        val contentType = runCatching { ContentType(part.contentType) }.getOrNull()
        val mimeType = contentType?.baseType?.lowercase() ?: MimeTypes.OCTET_STREAM
        val charset = contentType?.getParameter("charset")
        val disposition = runCatching { part.disposition?.lowercase() }.getOrNull()
        val fileName = runCatching { part.fileName }.getOrNull()?.let(TextRecovery::repairHeader)?.trim()?.takeIf { it.isNotEmpty() }
        val contentId = runCatching { (part as? MimePart)?.contentID }.getOrNull()?.trim()?.removePrefix("<")?.removeSuffix(">")?.takeIf { it.isNotEmpty() }
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
