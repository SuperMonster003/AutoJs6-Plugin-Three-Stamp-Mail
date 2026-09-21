package io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import java.text.Normalizer
import java.util.Locale

/**
 * The name of one background watch (contract version 2 `triggerId`, roadmap P8): the user picks
 * it in the settings page and the host's "mail arrived" task refers to it. Same rules as a saved
 * account alias so both read alike in scripts and messages: trimmed, NFC-normalized, lower-cased
 * in the root locale, starting with a letter or digit and continuing with letters, digits, `.`,
 * `_` or `-`, at most [MAX_LENGTH] characters. Not a secret; may appear in messages and logs.
 */
object TriggerId {

    const val MAX_LENGTH = 64

    private val VALID = Regex("^[\\p{L}\\p{N}][\\p{L}\\p{N}._-]{0,${MAX_LENGTH - 1}}$")

    /** The canonical form of [raw], or `INVALID_ARGUMENT` when it is not a trigger id. */
    fun normalize(raw: String?, label: String = "triggerId"): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) throw MailException.invalidArgument("'$label' must not be blank")
        val id = Normalizer.normalize(trimmed, Normalizer.Form.NFC).lowercase(Locale.ROOT)
        if (!VALID.matches(id)) {
            throw MailException.invalidArgument(
                "'$label' '${describe(trimmed)}' must start with a letter or digit and use only letters, digits, '.', '_' or '-' (at most $MAX_LENGTH characters)",
            )
        }
        return id
    }

    fun isValid(raw: String?): Boolean = try {
        normalize(raw)
        true
    } catch (_: MailException) {
        false
    }

    private fun describe(text: String): String {
        val printable = text.map { if (it.isISOControl()) '?' else it }.joinToString("")
        return if (printable.length <= 40) printable else printable.take(37) + "..."
    }
}
