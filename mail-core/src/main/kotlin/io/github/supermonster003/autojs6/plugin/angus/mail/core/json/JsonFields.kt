package io.github.supermonster003.autojs6.plugin.angus.mail.core.json

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Field readers shared by every `args` parser of the mail core. Each reader names the offending
 * path in its `INVALID_ARGUMENT` message (`'message.to[1].address' must be a string`) and treats
 * JSON `null` like an absent field.
 */

/** Parses [json] and requires a JSON object; [label] names it in errors. */
internal fun argsObject(json: String, label: String = "args"): JsonObject {
    val element = try {
        MailJson.format.parseToJsonElement(json)
    } catch (e: Exception) {
        throw MailException.invalidArgument("'$label' is not valid JSON", e.message?.take(200))
    }
    return element as? JsonObject ?: throw MailException.invalidArgument("'$label' must be a JSON object")
}

internal fun JsonPrimitive.stringValue(path: String): String {
    if (!isString) throw MailException.invalidArgument("'$path' must be a string")
    return content
}

internal fun JsonObject.rejectUnknown(label: String, known: Set<String>) {
    keys.firstOrNull { it !in known }?.let { key ->
        throw MailException.invalidArgument("unknown field '$label.$key'", "known fields: ${known.joinToString(", ")}")
    }
}

/** A string field; the key is the last path segment, [path] is what errors mention. Blank values are rejected unless [allowBlank]. */
internal fun JsonObject.string(path: String, allowBlank: Boolean = false): String? {
    val element = this[path.substringAfterLast('.')] ?: return null
    if (element is JsonNull) return null
    val primitive = element as? JsonPrimitive
    if (primitive == null || !primitive.isString) throw MailException.invalidArgument("'$path' must be a string")
    val text = if (allowBlank) primitive.content else primitive.content.trim()
    if (!allowBlank && text.isEmpty()) throw MailException.invalidArgument("'$path' must not be blank")
    return text
}

/** A string or an array of strings. */
internal fun JsonObject.stringList(field: String): List<String> {
    val element = this[field] ?: return emptyList()
    return when (element) {
        is JsonNull -> emptyList()
        is JsonPrimitive -> listOf(element.stringValue(field))
        is JsonArray -> element.mapIndexed { index, item -> (item as? JsonPrimitive)?.stringValue("$field[$index]") ?: throw MailException.invalidArgument("'$field[$index]' must be a string") }
        else -> throw MailException.invalidArgument("'$field' must be a string or an array of strings")
    }
}

internal fun JsonObject.long(path: String): Long? {
    val element = this[path.substringAfterLast('.')] ?: return null
    if (element is JsonNull) return null
    val primitive = element as? JsonPrimitive
    return primitive?.takeUnless { it.isString }?.longOrNull ?: throw MailException.invalidArgument("'$path' must be an integer")
}

internal fun JsonObject.int(path: String): Int? = long(path)?.let { value ->
    if (value < Int.MIN_VALUE || value > Int.MAX_VALUE) throw MailException.invalidArgument("'$path' is out of range")
    value.toInt()
}

internal fun JsonObject.bool(path: String): Boolean? {
    val element = this[path.substringAfterLast('.')] ?: return null
    if (element is JsonNull) return null
    val primitive = element as? JsonPrimitive
    return primitive?.takeUnless { it.isString }?.booleanOrNull ?: throw MailException.invalidArgument("'$path' must be a boolean")
}

internal fun JsonObject.obj(field: String): JsonObject? {
    val element = this[field] ?: return null
    if (element is JsonNull) return null
    return element as? JsonObject ?: throw MailException.invalidArgument("'$field' must be an object")
}

internal fun JsonObject.array(field: String): JsonArray? {
    val element = this[field] ?: return null
    if (element is JsonNull) return null
    return element as? JsonArray ?: throw MailException.invalidArgument("'$field' must be an array")
}

/** An enum-like string field restricted to [allowed] (case-sensitive), or null when absent. */
internal fun JsonObject.oneOf(path: String, allowed: Collection<String>): String? = string(path)?.also { value ->
    if (value !in allowed) throw MailException.invalidArgument("'$path' must be one of ${allowed.joinToString(", ")}: '$value'")
}

/**
 * A UID or a list of UIDs: numbers, numeric strings, or an array of them (script API A.1). POP3
 * string UIDLs arrive as strings and are kept verbatim by [uidStrings]; IMAP callers use [uids].
 */
internal fun JsonObject.uidStrings(field: String): List<String> {
    val element = this[field] ?: return emptyList()
    fun one(item: JsonElement, path: String): String = when {
        item is JsonPrimitive && item.isString -> item.content.trim().ifEmpty { throw MailException.invalidArgument("'$path' must not be blank") }
        item is JsonPrimitive && item.longOrNull != null -> item.content
        item is JsonObject -> item.string("$path.uid") ?: item.long("$path.uid")?.toString() ?: throw MailException.invalidArgument("'$path.uid' is required")
        else -> throw MailException.invalidArgument("'$path' must be a UID (number or string)")
    }
    return when (element) {
        is JsonNull -> emptyList()
        is JsonArray -> element.mapIndexed { index, item -> one(item, "$field[$index]") }
        else -> listOf(one(element, field))
    }
}

/** IMAP UIDs: positive integers, distinct, in the order given. */
internal fun JsonObject.uids(field: String): List<Long> = uidStrings(field).map { text ->
    text.toLongOrNull()?.takeIf { it > 0 } ?: throw MailException.invalidArgument("'$field' holds an invalid IMAP UID: '${text.take(40)}'")
}.distinct()

/** A single IMAP UID. */
internal fun JsonObject.uid(field: String): Long? {
    val values = uidStrings(field)
    if (values.isEmpty()) return null
    if (values.size > 1) throw MailException.invalidArgument("'$field' accepts a single UID")
    return values.single().toLongOrNull()?.takeIf { it > 0 } ?: throw MailException.invalidArgument("'$field' is not a valid IMAP UID: '${values.single().take(40)}'")
}
