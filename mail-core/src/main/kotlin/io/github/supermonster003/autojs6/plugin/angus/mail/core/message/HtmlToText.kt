package io.github.supermonster003.autojs6.plugin.angus.mail.core.message

/**
 * Minimal HTML to text conversion for messages without a `text/plain` part (roadmap D11 / D20):
 * drops `script` / `style` / `head` content, collapses source whitespace like a browser does
 * (except inside `pre`), turns paragraphs, headings, `br`, list items and table rows into line
 * breaks, separates table cells with tabs, keeps link targets that differ from their text,
 * decodes entities and limits blank lines. It is deliberately not a parser; malformed markup
 * degrades to text.
 */
object HtmlToText {

    private val DROP = Regex("(?is)<(script|style|head|title|template)\\b[^>]*>.*?</\\1\\s*>")
    private val COMMENT = Regex("(?s)<!--.*?-->")
    private val PRE = Regex("(?is)<pre\\b[^>]*>.*?</pre\\s*>")
    private val SOURCE_SPACE = Regex("[ \\t\\r\\n\\f]+")
    private val PARAGRAPH_START = Regex("(?i)<(p|div|h[1-6]|table|ul|ol|blockquote|pre|section|article|header|footer|address|dl|form|fieldset|hr)\\b[^>]*>")
    private val PARAGRAPH_END = Regex("(?i)</(p|div|h[1-6]|table|ul|ol|blockquote|pre|section|article|header|footer|address|dl|form|fieldset)\\s*>")
    private val ROW_END = Regex("(?i)</(tr|dt|dd)\\s*>")
    private val LIST_ITEM = Regex("(?i)<li\\b[^>]*>")
    private val CELL_END = Regex("(?i)</(td|th)\\s*>")
    private val BR = Regex("(?i)<br\\s*/?>")
    private val ANCHOR = Regex("(?is)<a\\b[^>]*href\\s*=\\s*([\"'])(.*?)\\1[^>]*>(.*?)</a\\s*>")
    private val TAG = Regex("(?s)<[^>]+>")
    private val NAMED = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to "\u00a0",
        "copy" to "\u00a9", "reg" to "\u00ae", "trade" to "\u2122", "hellip" to "\u2026", "mdash" to "\u2014",
        "ndash" to "\u2013", "lsquo" to "\u2018", "rsquo" to "\u2019", "ldquo" to "\u201c", "rdquo" to "\u201d",
        "middot" to "\u00b7", "bull" to "\u2022", "euro" to "\u20ac", "yen" to "\u00a5", "pound" to "\u00a3",
    )
    private val ENTITY = Regex("&(#x[0-9a-fA-F]{1,6}|#[0-9]{1,7}|[a-zA-Z][a-zA-Z0-9]{1,15});")
    private val BLANK_LINES = Regex("\\n{3,}")
    private val SPACES = Regex("[ \\u00a0]+")
    private val AROUND_TAB = Regex("[ \\u00a0]*\\t[ \\u00a0]*")

    /** Placeholders for the angle brackets around a kept link target; they survive the tag stripping. */
    private const val OPEN = '\u0001'
    private const val CLOSE = '\u0002'

    /** A space inside `pre` that must survive the whitespace collapse. */
    private const val KEEP = '\u0003'

    fun convert(html: String): String {
        var text = COMMENT.replace(html, "")
        text = DROP.replace(text, "")
        text = PRE.replace(text) { match -> match.value.replace("\r\n", "\n").replace("\n", "<br>").replace("\t", "$KEEP$KEEP$KEEP$KEEP").replace(' ', KEEP) }
        text = SOURCE_SPACE.replace(text, " ")
        text = ANCHOR.replace(text) { match ->
            val href = match.groupValues[2].trim()
            val label = match.groupValues[3]
            val plainLabel = TAG.replace(label, "").trim()
            val same = href.isEmpty() || href.startsWith("#") || plainLabel.equals(href, ignoreCase = true) || decodeEntities(plainLabel).equals(href, ignoreCase = true)
            if (same) label else "$label $OPEN$href$CLOSE"
        }
        text = BR.replace(text, "\n")
        text = LIST_ITEM.replace(text, "\n- ")
        text = CELL_END.replace(text, "\t")
        text = ROW_END.replace(text, "\n")
        text = PARAGRAPH_START.replace(text, "\n")
        text = PARAGRAPH_END.replace(text, "\n")
        text = TAG.replace(text, "")
        text = decodeEntities(text).replace(OPEN, '<').replace(CLOSE, '>')
        return text.lines()
            .joinToString("\n") { line -> AROUND_TAB.replace(SPACES.replace(line, " "), "\t").trim(' ', '\t', '\u00a0').replace(KEEP, ' ') }
            .let { BLANK_LINES.replace(it, "\n\n") }
            .trim()
    }

    fun decodeEntities(text: String): String = ENTITY.replace(text) { match ->
        val body = match.groupValues[1]
        when {
            body.startsWith("#x") || body.startsWith("#X") -> body.substring(2).toIntOrNull(16)?.let(::codePoint) ?: match.value
            body.startsWith("#") -> body.substring(1).toIntOrNull()?.let(::codePoint) ?: match.value
            else -> NAMED[body] ?: match.value
        }
    }

    private fun codePoint(value: Int): String? = if (value in 1..0x10FFFF && value !in 0xD800..0xDFFF) String(Character.toChars(value)) else null
}
