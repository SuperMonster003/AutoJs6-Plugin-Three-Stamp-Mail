package io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.rejectUnknown
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.stringList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.util.Locale

/**
 * Which arrived messages a background watch or a trigger subscription reports (contract version 2
 * `filter`, roadmap P8). Every group that is not empty must match; inside a group one entry is
 * enough: a [from] entry is a case-insensitive substring of the sender's address or display name
 * (the `From` header, else `Sender`), a [subject] entry a case-insensitive substring of the
 * subject. Entries are kept trimmed and lower-cased; an empty filter matches everything.
 */
@Serializable
data class TriggerFilter(
    val from: List<String> = emptyList(),
    val subject: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = from.isEmpty() && subject.isEmpty()

    fun matches(message: MessageDocument): Boolean {
        if (from.isNotEmpty()) {
            val sender = message.from ?: message.sender
            val haystacks = listOfNotNull(sender?.address, sender?.name).map { it.lowercase(Locale.ROOT) }
            if (from.none { needle -> haystacks.any { it.contains(needle) } }) return false
        }
        if (subject.isNotEmpty()) {
            val text = message.subject.lowercase(Locale.ROOT)
            if (subject.none { text.contains(it) }) return false
        }
        return true
    }

    /** The canonical form: entries trimmed, lower-cased, blank ones dropped, duplicates collapsed, ceilings enforced. */
    fun normalized(label: String = "filter"): TriggerFilter =
        TriggerFilter(entries(from, "$label.$FIELD_FROM"), entries(subject, "$label.$FIELD_SUBJECT"))

    companion object {
        val NONE = TriggerFilter()

        /** Entries per group. */
        const val MAX_ENTRIES = 16

        /** Longest entry (UTF-16 units). */
        const val MAX_ENTRY_LENGTH = 256

        const val FIELD_FROM = "from"
        const val FIELD_SUBJECT = "subject"
        val FIELDS: Set<String> = setOf(FIELD_FROM, FIELD_SUBJECT)

        /**
         * Reads a `filter` object: each group is a string or an array of strings, blank entries
         * are dropped, duplicates collapse, unknown fields are refused; null means no filter.
         */
        fun parse(filter: JsonObject?, label: String = "filter"): TriggerFilter {
            filter ?: return NONE
            filter.rejectUnknown(label, FIELDS)
            return TriggerFilter(filter.stringList(FIELD_FROM), filter.stringList(FIELD_SUBJECT)).normalized(label)
        }

        /** Normalizes the entries of one group. */
        fun entries(raw: List<String>, label: String): List<String> {
            val normalized = raw.map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }.distinct()
            if (normalized.size > MAX_ENTRIES) throw MailException.invalidArgument("'$label' holds more than $MAX_ENTRIES entries")
            normalized.firstOrNull { it.length > MAX_ENTRY_LENGTH }?.let {
                throw MailException.invalidArgument("'$label' entry exceeds $MAX_ENTRY_LENGTH characters")
            }
            return normalized
        }

        /** Splits one text field of the settings page into entries: commas, semicolons and line breaks separate them. */
        fun split(text: String?): List<String> = text.orEmpty().split(',', ';', '\n', '\r').map { it.trim() }.filter { it.isNotEmpty() }
    }
}
