package io.github.supermonster003.autojs6.plugin.angus.mail.core.message

import jakarta.mail.internet.MimeUtility
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Tolerance for the non-standard mail the plugin meets in the wild (roadmap D21 and the P2.3
 * fixtures): headers with raw 8-bit bytes instead of RFC 2047 words, bodies without a declared
 * charset, and charsets Java does not know. The rule is always the same: bytes that are valid
 * UTF-8 are UTF-8, everything else is read as GB 18030 (a superset of GBK and GB 2312, the
 * common case for Chinese providers), and ISO-8859-1 is the last resort that never fails.
 */
object TextRecovery {

    val GB18030: Charset by lazy { Charset.forName("GB18030") }

    /** Decodes [bytes] with [declared] when it is known and decodes cleanly, else with the fallback chain. */
    fun decode(bytes: ByteArray, declared: String?): String {
        val charset = declared?.let(::charsetOrNull)
        if (charset != null) {
            strict(bytes, charset)?.let { return it }
        }
        return decodeGuessing(bytes)
    }

    /** UTF-8 when the bytes are valid UTF-8, else GB 18030, else ISO-8859-1. */
    fun decodeGuessing(bytes: ByteArray): String =
        strict(bytes, StandardCharsets.UTF_8) ?: strict(bytes, GB18030) ?: String(bytes, StandardCharsets.ISO_8859_1)

    /**
     * Repairs a header value Jakarta Mail handed over as characters: decodes RFC 2047 words
     * leniently and, when the text consists only of Latin-1 characters with 8-bit ones among
     * them (raw bytes read as ISO-8859-1), re-reads those bytes as UTF-8 or GB 18030.
     */
    fun repairHeader(value: String?): String? {
        if (value == null) return null
        val decoded = runCatching { MimeUtility.decodeText(value) }.getOrDefault(value)
        return repairLatin1(decoded)
    }

    /** Re-decodes a string whose characters are all below U+0100 with at least one 8-bit character. */
    fun repairLatin1(text: String): String {
        if (text.none { it.code in 0x80..0xFF } || text.any { it.code > 0xFF }) return text
        val bytes = text.toByteArray(StandardCharsets.ISO_8859_1)
        return strict(bytes, StandardCharsets.UTF_8) ?: strict(bytes, GB18030) ?: text
    }

    fun charsetOrNull(name: String): Charset? = runCatching {
        val javaName = MimeUtility.javaCharset(name.trim().trim('"'))
        Charset.forName(javaName)
    }.getOrNull()

    private fun strict(bytes: ByteArray, charset: Charset): String? = try {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: CharacterCodingException) {
        null
    }
}
