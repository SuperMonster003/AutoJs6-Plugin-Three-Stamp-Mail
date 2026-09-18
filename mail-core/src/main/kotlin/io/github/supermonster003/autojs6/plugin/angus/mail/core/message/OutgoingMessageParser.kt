package io.github.supermonster003.autojs6.plugin.angus.mail.core.message

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MailJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.FlagMapper
import jakarta.mail.Flags
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.InternetAddress
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Parses the `message` object of `mail.send` and `messages.append` (protocol document: the
 * `MailSendMessage` of the script API with attachments as `{descriptorIndex, fileName, mimeType?,
 * contentId?, inline?, size}`) into an [OutgoingMessage]. Every limit of roadmap appendix B.5
 * that applies to outgoing mail is enforced here, and header injection is refused.
 */
object OutgoingMessageParser {

    object Fields {
        const val MESSAGE = "message"
        const val SAVE_TO_SENT = "saveToSent"
        const val FOLDER = "folder"
        const val FLAGS = "flags"

        const val FROM = "from"
        const val TO = "to"
        const val CC = "cc"
        const val BCC = "bcc"
        const val REPLY_TO = "replyTo"
        const val SUBJECT = "subject"
        const val TEXT = "text"
        const val HTML = "html"
        const val ATTACHMENTS = "attachments"
        const val HEADERS = "headers"
        const val PRIORITY = "priority"
        const val IN_REPLY_TO = "inReplyTo"
        const val REFERENCES = "references"
        const val DATE = "date"

        const val NAME = "name"
        const val ADDRESS = "address"

        const val DESCRIPTOR_INDEX = "descriptorIndex"
        const val FILE_NAME = "fileName"
        const val MIME_TYPE = "mimeType"
        const val CONTENT_ID = "contentId"
        const val INLINE = "inline"
        const val SIZE = "size"

        val MESSAGE_KNOWN: Set<String> = setOf(FROM, TO, CC, BCC, REPLY_TO, SUBJECT, TEXT, HTML, ATTACHMENTS, HEADERS, PRIORITY, IN_REPLY_TO, REFERENCES, DATE)
        val ATTACHMENT_KNOWN: Set<String> = setOf(DESCRIPTOR_INDEX, FILE_NAME, MIME_TYPE, CONTENT_ID, INLINE, SIZE)
    }

    /** Headers the composer owns; a script sets them through the dedicated fields or not at all. */
    val FORBIDDEN_HEADERS: Set<String> = setOf(
        "from", "sender", "to", "cc", "bcc", "reply-to", "subject", "date", "message-id", "in-reply-to", "references",
        "mime-version", "x-priority", "x-msmail-priority", "importance", "return-path", "received", "dkim-signature",
    )

    const val MAX_HEADERS = 32
    const val MAX_HEADER_VALUE_LENGTH = 998
    const val MAX_SUBJECT_LENGTH = 998
    const val MAX_FILE_NAME_LENGTH = 255

    /** `mail.send` args: the message plus the tri-state `saveToSent` (null = provider default). */
    class SendArgs(val message: OutgoingMessage, val saveToSent: Boolean?)

    /** `messages.append` args. */
    class AppendArgs(val folder: String, val message: OutgoingMessage, val flags: Flags)

    fun parseSendArgs(argsJson: String, sources: List<AttachmentSource>): SendArgs {
        val root = argsObject(argsJson)
        root.rejectUnknown("args", setOf(Fields.MESSAGE, Fields.SAVE_TO_SENT))
        val message = parse(root.obj(Fields.MESSAGE) ?: throw MailException.invalidArgument("'${Fields.MESSAGE}' is required"), sources)
        return SendArgs(message, root.bool(Fields.SAVE_TO_SENT))
    }

    fun parseAppendArgs(argsJson: String, sources: List<AttachmentSource>): AppendArgs {
        val root = argsObject(argsJson)
        root.rejectUnknown("args", setOf(Fields.FOLDER, Fields.MESSAGE, Fields.FLAGS))
        val folder = root.string(Fields.FOLDER) ?: throw MailException.invalidArgument("'${Fields.FOLDER}' is required")
        val message = parse(root.obj(Fields.MESSAGE) ?: throw MailException.invalidArgument("'${Fields.MESSAGE}' is required"), sources)
        val flags = FlagMapper.toFlags(root.stringList(Fields.FLAGS))
        return AppendArgs(folder, message, flags)
    }

    fun parse(json: String, sources: List<AttachmentSource>): OutgoingMessage = parse(argsObject(json, "message"), sources)

    fun parse(root: JsonObject, sources: List<AttachmentSource>): OutgoingMessage {
        root.rejectUnknown(Fields.MESSAGE, Fields.MESSAGE_KNOWN)
        val to = root.addresses(Fields.TO)
        val cc = root.addresses(Fields.CC)
        val bcc = root.addresses(Fields.BCC)
        if (to.isEmpty() && cc.isEmpty() && bcc.isEmpty()) throw MailException.invalidArgument("at least one recipient is required in '${Fields.TO}', '${Fields.CC}' or '${Fields.BCC}'")
        val recipients = to.size + cc.size + bcc.size
        if (recipients > MailLimits.MAX_RECIPIENTS) {
            throw MailException(MailErrorCode.LIMIT_EXCEEDED, "$recipients recipients exceed the limit of ${MailLimits.MAX_RECIPIENTS}", retryable = false)
        }
        val subject = root.string(Fields.SUBJECT, allowBlank = true) ?: throw MailException.invalidArgument("'${Fields.SUBJECT}' is required")
        if (subject.length > MAX_SUBJECT_LENGTH) throw MailException.invalidArgument("'${Fields.SUBJECT}' exceeds $MAX_SUBJECT_LENGTH characters")
        return OutgoingMessage(
            to = to,
            cc = cc,
            bcc = bcc,
            replyTo = root.addresses(Fields.REPLY_TO),
            from = root.addresses(Fields.FROM).let { list ->
                if (list.size > 1) throw MailException.invalidArgument("'${Fields.FROM}' accepts a single address")
                list.firstOrNull()
            },
            subject = subject,
            text = root.string(Fields.TEXT, allowBlank = true),
            html = root.string(Fields.HTML, allowBlank = true),
            attachments = attachments(root, sources),
            headers = headers(root),
            priority = root.string(Fields.PRIORITY)?.let { id ->
                Priority.fromId(id) ?: throw MailException.invalidArgument("'${Fields.PRIORITY}' must be one of ${Priority.entries.joinToString(", ") { it.id }}: '$id'")
            } ?: Priority.NORMAL,
            inReplyTo = root.string(Fields.IN_REPLY_TO)?.let { messageId(Fields.IN_REPLY_TO, it) },
            references = root.stringList(Fields.REFERENCES).map { messageId(Fields.REFERENCES, it) },
            dateMillis = root.long(Fields.DATE)?.also { if (it < 0) throw MailException.invalidArgument("'${Fields.DATE}' must be UTC milliseconds") },
        )
    }

    private fun attachments(root: JsonObject, sources: List<AttachmentSource>): List<OutgoingAttachment> {
        val element = root[Fields.ATTACHMENTS] ?: return emptyList()
        if (element is JsonNull) return emptyList()
        val array = element as? JsonArray ?: throw MailException.invalidArgument("'${Fields.ATTACHMENTS}' must be an array")
        if (array.size > MailLimits.MAX_ATTACHMENTS_PER_MESSAGE) {
            throw MailException(MailErrorCode.LIMIT_EXCEEDED, "${array.size} attachments exceed the limit of ${MailLimits.MAX_ATTACHMENTS_PER_MESSAGE}", retryable = false)
        }
        val used = HashSet<Int>()
        return array.mapIndexed { position, item ->
            val label = "${Fields.ATTACHMENTS}[$position]"
            val obj = item as? JsonObject ?: throw MailException.invalidArgument("'$label' must be an object")
            obj.rejectUnknown(label, Fields.ATTACHMENT_KNOWN)
            val index = obj.long("$label.${Fields.DESCRIPTOR_INDEX}")?.toInt()
                ?: throw MailException.invalidArgument("'$label.${Fields.DESCRIPTOR_INDEX}' is required")
            if (index !in sources.indices) throw MailException.invalidArgument("'$label.${Fields.DESCRIPTOR_INDEX}' $index has no descriptor (${sources.size} supplied)")
            if (!used.add(index)) throw MailException.invalidArgument("'$label.${Fields.DESCRIPTOR_INDEX}' $index is used twice")
            val fileName = obj.string("$label.${Fields.FILE_NAME}")?.let(::sanitizeFileName)
                ?: throw MailException.invalidArgument("'$label.${Fields.FILE_NAME}' is required")
            val mimeType = obj.string("$label.${Fields.MIME_TYPE}")?.also {
                if (!MimeTypes.isValid(it)) throw MailException.invalidArgument("'$label.${Fields.MIME_TYPE}' is not a MIME type: '$it'")
            } ?: MimeTypes.forName(fileName)
            val contentId = obj.string("$label.${Fields.CONTENT_ID}")?.let { cid ->
                val bare = cid.removePrefix("<").removeSuffix(">")
                if (bare.isEmpty() || bare.any { it.isWhitespace() || it == '<' || it == '>' }) throw MailException.invalidArgument("'$label.${Fields.CONTENT_ID}' is not a content id")
                bare
            }
            val inline = obj.bool("$label.${Fields.INLINE}") ?: (contentId != null)
            val source = sources[index]
            val declared = obj.long("$label.${Fields.SIZE}")
            val size = if (source.size >= 0) source.size else declared ?: -1L
            if (size > MailLimits.MAX_ATTACHMENT_BYTES) {
                throw MailException(MailErrorCode.LIMIT_EXCEEDED, "'$label' ($size bytes) exceeds the attachment limit of ${MailLimits.MAX_ATTACHMENT_BYTES} bytes", retryable = false)
            }
            OutgoingAttachment(fileName, mimeType, contentId, inline, source)
        }
    }

    private fun headers(root: JsonObject): List<Pair<String, String>> {
        val obj = root.obj(Fields.HEADERS) ?: return emptyList()
        if (obj.size > MAX_HEADERS) throw MailException(MailErrorCode.LIMIT_EXCEEDED, "${obj.size} headers exceed the limit of $MAX_HEADERS", retryable = false)
        val result = ArrayList<Pair<String, String>>()
        obj.forEach { (name, element) ->
            if (!HEADER_NAME.matches(name)) throw MailException.invalidArgument("'${Fields.HEADERS}.$name' is not a header name")
            val lower = name.lowercase()
            if (lower in FORBIDDEN_HEADERS || lower.startsWith("content-")) {
                throw MailException.invalidArgument("'${Fields.HEADERS}.$name' is set by the plugin; use the message fields instead")
            }
            val values = when (element) {
                is JsonNull -> emptyList()
                is JsonPrimitive -> listOf(element.stringValue("${Fields.HEADERS}.$name"))
                is JsonArray -> element.map { (it as? JsonPrimitive)?.stringValue("${Fields.HEADERS}.$name") ?: throw MailException.invalidArgument("'${Fields.HEADERS}.$name' must hold strings") }
                else -> throw MailException.invalidArgument("'${Fields.HEADERS}.$name' must be a string or an array of strings")
            }
            values.forEach { value ->
                if (value.any { it == '\r' || it == '\n' || it == '\u0000' }) throw MailException.invalidArgument("'${Fields.HEADERS}.$name' must not contain line breaks")
                if (value.length > MAX_HEADER_VALUE_LENGTH) throw MailException.invalidArgument("'${Fields.HEADERS}.$name' exceeds $MAX_HEADER_VALUE_LENGTH characters")
                result += name to value
            }
        }
        return result
    }

    private fun messageId(field: String, raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.any { it.isWhitespace() }) throw MailException.invalidArgument("'$field' is not a message id: '$trimmed'")
        val bare = trimmed.removePrefix("<").removeSuffix(">")
        if (bare.isEmpty() || bare.any { it == '<' || it == '>' }) throw MailException.invalidArgument("'$field' is not a message id: '$trimmed'")
        return "<$bare>"
    }

    /** Outgoing file names keep everything except path separators and control characters. */
    fun sanitizeFileName(name: String): String {
        val cleaned = name.map { if (it == '/' || it == '\\' || it < ' ' || it == '\u007f') '_' else it }.joinToString("").trim()
        val bounded = if (cleaned.length > MAX_FILE_NAME_LENGTH) cleaned.take(MAX_FILE_NAME_LENGTH) else cleaned
        return bounded.ifEmpty { "attachment" }
    }

    private fun JsonObject.addresses(field: String): List<MailAddressSpec> {
        val element = this[field] ?: return emptyList()
        if (element is JsonNull) return emptyList()
        if (element is JsonArray) return element.flatMapIndexed { index, item -> address("$field[$index]", item) }
        return address(field, element)
    }

    private fun address(label: String, element: JsonElement): List<MailAddressSpec> = when (element) {
        is JsonNull -> emptyList()
        is JsonPrimitive -> {
            val text = element.stringValue(label).trim()
            if (text.isEmpty()) throw MailException.invalidArgument("'$label' must not be blank")
            val parsed = try {
                InternetAddress.parse(text, true)
            } catch (e: AddressException) {
                throw MailException.invalidArgument("'$label' is not a valid address list: '${text.take(120)}'", e.message?.take(200))
            }
            if (parsed.isEmpty()) throw MailException.invalidArgument("'$label' holds no address")
            parsed.map { spec(label, it.address, it.personal) }
        }
        is JsonObject -> {
            element.rejectUnknown(label, setOf(Fields.NAME, Fields.ADDRESS))
            val address = element.string("$label.${Fields.ADDRESS}") ?: throw MailException.invalidArgument("'$label.${Fields.ADDRESS}' is required")
            listOf(spec(label, address, element.string("$label.${Fields.NAME}", allowBlank = true)))
        }
        else -> throw MailException.invalidArgument("'$label' must be a string or an object with name and address")
    }

    private fun spec(label: String, address: String, name: String?): MailAddressSpec {
        try {
            InternetAddress(address, true).validate()
        } catch (e: AddressException) {
            throw MailException.invalidArgument("'$label' is not a valid email address: '${address.take(120)}'", e.message?.take(200))
        }
        if (address.length > MailLimits.MAX_OPTION_STRING_LENGTH) throw MailException.invalidArgument("'$label' exceeds ${MailLimits.MAX_OPTION_STRING_LENGTH} characters")
        val displayName = name?.trim()?.takeIf { it.isNotEmpty() }
        if (displayName != null && (displayName.length > MailLimits.MAX_OPTION_STRING_LENGTH || displayName.any { it == '\r' || it == '\n' })) {
            throw MailException.invalidArgument("'$label' has an invalid display name")
        }
        return MailAddressSpec(address, displayName)
    }

    private fun argsObject(json: String, label: String = "args"): JsonObject {
        val element = try {
            MailJson.format.parseToJsonElement(json)
        } catch (e: Exception) {
            throw MailException.invalidArgument("'$label' is not valid JSON", e.message?.take(200))
        }
        return element as? JsonObject ?: throw MailException.invalidArgument("'$label' must be a JSON object")
    }

    private fun JsonPrimitive.stringValue(path: String): String {
        if (!isString) throw MailException.invalidArgument("'$path' must be a string")
        return content
    }

    private fun JsonObject.rejectUnknown(label: String, known: Set<String>) {
        keys.firstOrNull { it !in known }?.let { key ->
            throw MailException.invalidArgument("unknown field '$label.$key'", "known fields: ${known.joinToString(", ")}")
        }
    }

    private fun JsonObject.string(path: String, allowBlank: Boolean = false): String? {
        val element = this[path.substringAfterLast('.')] ?: return null
        if (element is JsonNull) return null
        val primitive = element as? JsonPrimitive
        if (primitive == null || !primitive.isString) throw MailException.invalidArgument("'$path' must be a string")
        val text = if (allowBlank) primitive.content else primitive.content.trim()
        if (!allowBlank && text.isEmpty()) throw MailException.invalidArgument("'$path' must not be blank")
        return text
    }

    private fun JsonObject.stringList(field: String): List<String> {
        val element = this[field] ?: return emptyList()
        return when (element) {
            is JsonNull -> emptyList()
            is JsonPrimitive -> listOf(element.stringValue(field))
            is JsonArray -> element.mapIndexed { index, item -> (item as? JsonPrimitive)?.stringValue("$field[$index]") ?: throw MailException.invalidArgument("'$field[$index]' must be a string") }
            else -> throw MailException.invalidArgument("'$field' must be a string or an array of strings")
        }
    }

    private fun JsonObject.long(path: String): Long? {
        val element = this[path.substringAfterLast('.')] ?: return null
        if (element is JsonNull) return null
        val primitive = element as? JsonPrimitive
        return primitive?.takeUnless { it.isString }?.longOrNull ?: throw MailException.invalidArgument("'$path' must be an integer")
    }

    private fun JsonObject.bool(path: String): Boolean? {
        val element = this[path.substringAfterLast('.')] ?: return null
        if (element is JsonNull) return null
        val primitive = element as? JsonPrimitive
        return primitive?.takeUnless { it.isString }?.booleanOrNull ?: throw MailException.invalidArgument("'$path' must be a boolean")
    }

    private fun JsonObject.obj(field: String): JsonObject? {
        val element = this[field] ?: return null
        if (element is JsonNull) return null
        return element as? JsonObject ?: throw MailException.invalidArgument("'$field' must be an object")
    }

    private val HEADER_NAME = Regex("[!-9;-~]{1,64}")
}
