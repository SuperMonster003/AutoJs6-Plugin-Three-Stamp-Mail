package io.github.supermonster003.autojs6.plugin.angus.mail.store

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import java.text.Normalizer
import java.util.Locale

/**
 * The name a script uses for a saved account (`mail.connect('work')`, roadmap D8 / P4).
 *
 * Aliases are compared after normalization: surrounding whitespace is dropped, the text is
 * NFC-normalized and lower-cased in the root locale, so `Work`, ` work ` and `WORK` name the same
 * record. A normalized alias starts with a letter or digit and continues with letters, digits,
 * `.`, `_` or `-`, at most [MAX_LENGTH] characters; anything else is refused as
 * `INVALID_ARGUMENT`. Aliases are not secrets and may appear in error messages.
 */
object AccountAlias {

    const val MAX_LENGTH = 64

    private val VALID = Regex("^[\\p{L}\\p{N}][\\p{L}\\p{N}._-]{0,${MAX_LENGTH - 1}}$")

    /** The canonical form of [raw], or `INVALID_ARGUMENT` when it is not an alias. */
    fun normalize(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) throw MailException.invalidArgument("account alias must not be blank")
        val alias = Normalizer.normalize(trimmed, Normalizer.Form.NFC).lowercase(Locale.ROOT)
        if (!VALID.matches(alias)) {
            throw MailException.invalidArgument(
                "account alias '${describe(trimmed)}' must start with a letter or digit and use only letters, digits, '.', '_' or '-' (at most $MAX_LENGTH characters)",
            )
        }
        return alias
    }

    fun isValid(raw: String?): Boolean = try {
        normalize(raw)
        true
    } catch (_: MailException) {
        false
    }

    /** A bounded, control-character-free rendering of a rejected alias for messages. */
    private fun describe(text: String): String {
        val printable = text.map { if (it.isISOControl()) '?' else it }.joinToString("")
        return if (printable.length <= 40) printable else printable.take(37) + "..."
    }
}
