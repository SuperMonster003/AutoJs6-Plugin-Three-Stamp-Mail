package io.github.supermonster003.autojs6.plugin.angus.mail.core.query

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import jakarta.mail.Flags

/**
 * Script flag names <-> Jakarta [Flags] (roadmap appendix A.4 `flags`): the five system flags by
 * their lower-case names, everything else as an IMAP keyword (atom characters only).
 */
object FlagMapper {

    val SYSTEM: Map<String, Flags.Flag> = linkedMapOf(
        "seen" to Flags.Flag.SEEN,
        "flagged" to Flags.Flag.FLAGGED,
        "answered" to Flags.Flag.ANSWERED,
        "draft" to Flags.Flag.DRAFT,
        "deleted" to Flags.Flag.DELETED,
    )

    private val KEYWORD = Regex("[^\\s\\\\(){}%*\"\\]\\x00-\\x1f\\x7f]{1,64}")

    fun toFlags(names: List<String>): Flags {
        val flags = Flags()
        names.forEach { raw ->
            val name = raw.trim()
            val system = SYSTEM[name.lowercase()]
            when {
                system != null -> flags.add(system)
                name.equals("recent", ignoreCase = true) -> throw MailException.invalidArgument("the recent flag is read-only")
                KEYWORD.matches(name) -> flags.add(name)
                else -> throw MailException.invalidArgument("'$name' is not a flag name")
            }
        }
        return flags
    }

    fun toNames(flags: Flags): List<String> {
        val names = ArrayList<String>()
        SYSTEM.forEach { (name, flag) -> if (flags.contains(flag)) names += name }
        if (flags.contains(Flags.Flag.RECENT)) names += "recent"
        names += flags.userFlags.sorted()
        return names
    }
}
