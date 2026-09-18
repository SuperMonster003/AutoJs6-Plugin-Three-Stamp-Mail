package io.github.supermonster003.autojs6.plugin.angus.mail.core.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive

/** `MailAddress` of the script API (roadmap appendix A.4). */
@Serializable
data class AddressDocument(
    val name: String? = null,
    val address: String,
)

/** `MailAttachment` without its `download` method: the part is addressed by `partId` (IMAP section path). */
@Serializable
data class AttachmentDocument(
    val partId: String,
    val fileName: String,
    val mimeType: String,
    /** Size of the encoded part as the server reports it (BODYSTRUCTURE), or of the decoded bytes for local messages; -1 when unknown. */
    val size: Long,
    val contentId: String? = null,
    val inline: Boolean,
)

/**
 * `MailMessage` (roadmap appendix A.4, dates as UTC milliseconds per D26). Envelope listings
 * leave `bodyLoaded` false; `messages.get` fills `text` / `html` / `headers` / `attachments`.
 * `uid` is a number for IMAP and a string (UIDL) for POP3, hence the JSON primitive.
 */
@Serializable
data class MessageDocument(
    val uid: JsonPrimitive,
    val folder: String,
    val messageId: String? = null,
    val inReplyTo: String? = null,
    val references: List<String> = emptyList(),
    val subject: String,
    val from: AddressDocument? = null,
    val sender: AddressDocument? = null,
    val replyTo: List<AddressDocument> = emptyList(),
    val to: List<AddressDocument> = emptyList(),
    val cc: List<AddressDocument> = emptyList(),
    val bcc: List<AddressDocument> = emptyList(),
    val date: Long? = null,
    val receivedDate: Long? = null,
    val size: Long,
    val flags: List<String> = emptyList(),
    val seen: Boolean = false,
    val flagged: Boolean = false,
    val answered: Boolean = false,
    val draft: Boolean = false,
    val deleted: Boolean = false,
    val hasAttachments: Boolean = false,
    val bodyLoaded: Boolean = false,
    val text: String? = null,
    val html: String? = null,
    val headers: Map<String, List<String>> = emptyMap(),
    val attachments: List<AttachmentDocument> = emptyList(),
    /** True when a text or html part was left out because the inline budget (`MAX_INLINE_BODY_BYTES`) was exceeded. */
    val bodyTruncated: Boolean = false,
    /** The body parts left out; each can be fetched with `attachments.download` by its `partId`. */
    val bodyParts: List<AttachmentDocument> = emptyList(),
    /** RFC 822 source with every byte mapped to one character (ISO-8859-1), only with `includeRaw` and within the inline budget. */
    val raw: String? = null,
    val rawTruncated: Boolean = false,
)

/** `MailFolder` (roadmap appendix A.3 `folders()`): a tree node with optional counts. */
@Serializable
data class FolderDocument(
    val name: String,
    val path: String,
    val delimiter: String,
    /** `inbox`, `sent`, `drafts`, `trash`, `junk`, `archive`, `all`, `flagged` or `important`; null when unknown. */
    val specialUse: String? = null,
    val selectable: Boolean = true,
    val subscribed: Boolean? = null,
    val messages: Int? = null,
    val unseen: Int? = null,
    val children: List<FolderDocument> = emptyList(),
)

/** Result of `folders.status`. */
@Serializable
data class FolderStatusDocument(
    val name: String,
    val path: String,
    val messages: Int,
    val unseen: Int,
    val recent: Int? = null,
    val uidNext: Long? = null,
    val uidValidity: Long? = null,
)

/** Result of `messages.search`: the page plus where the matches came from (`server` or `client`). */
@Serializable
data class SearchResult(
    val messages: List<MessageDocument>,
    val fallback: String,
) {
    companion object {
        const val FALLBACK_SERVER = "server"
        const val FALLBACK_CLIENT = "client"
    }
}

/** Result of `messages.setFlags` and `messages.delete`: the UIDs that were found and changed. */
@Serializable
data class UidsResult(val uids: List<Long>)

/** Result of `messages.move` / `messages.copy`: the UIDs in the target folder when the server reports them (UIDPLUS), else null. */
@Serializable
data class TargetUidsResult(val uids: List<Long>? = null)

/** Result of `messages.expunge`. */
@Serializable
data class ExpungeResult(val count: Int)

/** Result of `messages.raw` and `attachments.download`: what went into the host's descriptor. */
@Serializable
data class TransferResult(
    val bytes: Long,
    val fileName: String,
    val mimeType: String,
)

/** The UID as an IMAP number; POP3 documents carry string UIDLs and answer null. */
val MessageDocument.imapUid: Long? get() = if (uid.isString) null else uid.content.toLongOrNull()

fun MessageDocument.toJson(): String = MailJson.format.encodeToString(this)

fun List<MessageDocument>.toJson(): String = MailJson.format.encodeToString(this)

fun FolderDocument.toJson(): String = MailJson.format.encodeToString(this)

fun List<FolderDocument>.toFolderJson(): String = MailJson.format.encodeToString(this)

fun FolderStatusDocument.toJson(): String = MailJson.format.encodeToString(this)

fun SearchResult.toJson(): String = MailJson.format.encodeToString(this)

fun UidsResult.toJson(): String = MailJson.format.encodeToString(this)

fun TargetUidsResult.toJson(): String = MailJson.format.encodeToString(this)

fun ExpungeResult.toJson(): String = MailJson.format.encodeToString(this)

fun TransferResult.toJson(): String = MailJson.format.encodeToString(this)
