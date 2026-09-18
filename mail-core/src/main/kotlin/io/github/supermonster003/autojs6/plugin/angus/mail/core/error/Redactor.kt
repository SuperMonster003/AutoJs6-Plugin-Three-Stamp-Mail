package io.github.supermonster003.autojs6.plugin.angus.mail.core.error

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import java.util.Base64

/**
 * Scrubs text that is about to leave the plugin process: the account secret in any of the forms it
 * travels in (raw, inside a SASL PLAIN or XOAUTH2 payload, Base64), long Base64 runs, and anything
 * beyond [MailLimits.MAX_ERROR_MESSAGE_BYTES]. Servers echo credentials in error replies rarely,
 * but the roadmap hard constraint (credentials never enter logs, JSON, or exception messages) is
 * enforced here rather than trusted.
 */
class Redactor(private val username: String, private val secret: MailSecret?) {

    /** Literal strings that must never appear in output: the secret itself and its encoded forms. */
    private val forbidden: List<String> by lazy { forbiddenForms() }

    fun scrubOrNull(text: String?): String? = text?.let(::scrub)

    fun scrub(text: String): String {
        var scrubbed = text
        forbidden.forEach { form -> scrubbed = scrubbed.replace(form, MASK) }
        scrubbed = BASE64_RUN.replace(scrubbed) { MASK }
        return truncate(scrubbed, MailLimits.MAX_ERROR_MESSAGE_BYTES)
    }

    private fun forbiddenForms(): List<String> {
        val revealed = secret?.takeUnless { it.isEmpty }?.reveal() ?: return emptyList()
        val encoder = Base64.getEncoder()
        val plainSasl = "\u0000$username\u0000$revealed"
        val xoauth2 = "user=$username\u0001auth=Bearer $revealed\u0001\u0001"
        return listOf(
            revealed,
            encoder.encodeToString(revealed.toByteArray(Charsets.UTF_8)),
            encoder.encodeToString(plainSasl.toByteArray(Charsets.UTF_8)),
            encoder.encodeToString(xoauth2.toByteArray(Charsets.UTF_8)),
        ).filter { it.length >= MIN_FORBIDDEN_LENGTH }.distinct().sortedByDescending { it.length }
    }

    companion object {
        const val MASK = "***"

        /** Secrets shorter than this would mask ordinary words; they are still covered by the Base64 forms. */
        private const val MIN_FORBIDDEN_LENGTH = 4

        /** A Base64 run long enough to carry a credential; shorter runs (message ids, boundaries) stay readable. */
        private val BASE64_RUN = Regex("[A-Za-z0-9+/]{32,}={0,2}")

        /** Truncates on a UTF-8 character boundary so the result never exceeds [maxBytes]. */
        fun truncate(text: String, maxBytes: Int): String {
            if (text.toByteArray(Charsets.UTF_8).size <= maxBytes) return text
            val marker = "..."
            val budget = maxBytes - marker.length
            var end = 0
            var bytes = 0
            while (end < text.length) {
                val codePoint = text.codePointAt(end)
                val width = Character.charCount(codePoint)
                val size = String(Character.toChars(codePoint)).toByteArray(Charsets.UTF_8).size
                if (bytes + size > budget) break
                bytes += size
                end += width
            }
            return text.substring(0, end) + marker
        }
    }
}
