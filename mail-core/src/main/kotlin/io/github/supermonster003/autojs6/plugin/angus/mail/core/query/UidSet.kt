package io.github.supermonster003.autojs6.plugin.angus.mail.core.query

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import jakarta.mail.UIDFolder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * An IMAP UID sequence set as scripts write it (`100:*`, `1:50`, `1,5,9:12`, `4711`, or an array
 * of those). `*` ("the highest UID") is kept as [STAR] so that ranges order and contain
 * naturally; the IMAP layer turns it into [UIDFolder.LASTUID] when fetching. Ranges are
 * normalized so that `first <= last`.
 */
class UidSet private constructor(val ranges: List<LongRange>) {

    fun contains(uid: Long): Boolean = ranges.any { uid in it }

    val isEmpty: Boolean get() = ranges.isEmpty()

    override fun toString(): String = ranges.joinToString(",") { range ->
        val first = if (range.first == STAR) "*" else range.first.toString()
        val last = if (range.last == STAR) "*" else range.last.toString()
        if (range.first == range.last) first else "$first:$last"
    }

    companion object {
        /** The in-set representation of `*`. */
        const val STAR = Long.MAX_VALUE

        /** What the IMAP layer sends for [STAR]. */
        fun wire(bound: Long): Long = if (bound == STAR) UIDFolder.LASTUID else bound

        fun parse(element: JsonElement, path: String = "uid"): UidSet {
            val ranges = ArrayList<LongRange>()
            fun one(item: JsonElement, label: String) {
                when {
                    item is JsonPrimitive && !item.isString && item.longOrNull != null -> ranges += single(item.longOrNull!!, label)
                    item is JsonPrimitive && item.isString -> item.content.split(',').map(String::trim).filter { it.isNotEmpty() }.forEach { ranges += range(it, label) }
                    else -> throw MailException.invalidArgument("'$label' must be a UID, a range like '100:*' or an array of them")
                }
            }
            when (element) {
                is JsonArray -> element.forEachIndexed { index, item -> one(item, "$path[$index]") }
                else -> one(element, path)
            }
            if (ranges.isEmpty()) throw MailException.invalidArgument("'$path' holds no UID")
            return UidSet(ranges)
        }

        fun of(uids: Collection<Long>): UidSet = UidSet(uids.sorted().map { it..it })

        private fun single(uid: Long, label: String): LongRange {
            if (uid < 1) throw MailException.invalidArgument("'$label' must be a positive UID: $uid")
            return uid..uid
        }

        private fun range(text: String, label: String): LongRange {
            val parts = text.split(':')
            if (parts.size > 2) throw MailException.invalidArgument("'$label' is not a UID range: '$text'")
            val first = bound(parts[0], label, text)
            val last = if (parts.size == 2) bound(parts[1], label, text) else first
            return if (first <= last) first..last else last..first
        }

        private fun bound(text: String, label: String, whole: String): Long {
            if (text == "*") return STAR
            val value = text.toLongOrNull() ?: throw MailException.invalidArgument("'$label' is not a UID range: '$whole'")
            if (value < 1) throw MailException.invalidArgument("'$label' must hold positive UIDs: '$whole'")
            return value
        }
    }
}
