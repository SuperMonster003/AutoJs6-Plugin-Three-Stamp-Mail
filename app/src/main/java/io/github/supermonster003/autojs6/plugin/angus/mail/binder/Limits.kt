package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import org.autojs.plugin.mail.api.MailContract

/**
 * The Binder-level ceilings of roadmap appendix B.5 that the session Binder applies before and
 * after the router (roadmap P2.5 `Limits`): the JSON envelope of a request and of a response,
 * the depth of the per-session queue, and the length of an error message. Descriptor counts and
 * the per-op argument ceilings (page sizes, recipients, attachments) live in the router and the
 * argument parsers. Pure functions, so the JVM tests cover them without a Binder.
 */
internal object Limits {

    /** Calls waiting behind the one in flight; the 34th outstanding call of a session is refused. */
    const val QUEUE_CAPACITY = MailContract.MAX_QUEUED_CALLS

    /** UTF-8 length without allocating the encoded bytes. */
    fun utf8Length(text: String): Int {
        var length = 0
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            length += when {
                codePoint < 0x80 -> 1
                codePoint < 0x800 -> 2
                codePoint < 0x10000 -> 3
                else -> 4
            }
            index += Character.charCount(codePoint)
        }
        return length
    }

    /** The refusal of a request envelope larger than `MAX_ENVELOPE_BYTES`, or null when it fits (or is absent). */
    fun checkRequest(envelope: String?): MailException? = envelope?.let { check(it, "request") }

    /** The refusal of a response envelope larger than `MAX_ENVELOPE_BYTES`, or null when it fits. */
    fun checkResponse(envelope: String): MailException? = check(envelope, "response")

    private fun check(envelope: String, label: String): MailException? {
        val size = utf8Length(envelope)
        if (size <= MailContract.MAX_ENVELOPE_BYTES) return null
        return MailException(MailErrorCode.LIMIT_EXCEEDED, "$label envelope of $size bytes exceeds ${MailContract.MAX_ENVELOPE_BYTES} bytes", retryable = false)
    }

    /** The refusal of a call that finds the session queue full. */
    fun queueFull(): MailException =
        MailException(MailErrorCode.LIMIT_EXCEEDED, "the session already has $QUEUE_CAPACITY queued calls (MAX_QUEUED_CALLS)", retryable = false)

    /** Truncates [message] to `MAX_ERROR_MESSAGE_BYTES` of UTF-8 without splitting a code point. */
    fun clampErrorMessage(message: String): String {
        if (utf8Length(message) <= MailContract.MAX_ERROR_MESSAGE_BYTES) return message
        val marker = "..."
        val budget = MailContract.MAX_ERROR_MESSAGE_BYTES - marker.length
        val kept = StringBuilder()
        var used = 0
        var index = 0
        while (index < message.length) {
            val codePoint = message.codePointAt(index)
            val width = utf8Length(String(Character.toChars(codePoint)))
            if (used + width > budget) break
            kept.appendCodePoint(codePoint)
            used += width
            index += Character.charCount(codePoint)
        }
        return kept.append(marker).toString()
    }
}
