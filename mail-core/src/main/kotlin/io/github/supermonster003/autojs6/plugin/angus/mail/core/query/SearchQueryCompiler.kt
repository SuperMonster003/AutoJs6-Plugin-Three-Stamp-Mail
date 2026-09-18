package io.github.supermonster003.autojs6.plugin.angus.mail.core.query

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.bool
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.long
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.obj
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.rejectUnknown
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.string
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.stringValue
import jakarta.mail.Flags
import jakarta.mail.Message
import jakarta.mail.internet.MimeMessage
import jakarta.mail.search.AndTerm
import jakarta.mail.search.BodyTerm
import jakarta.mail.search.ComparisonTerm
import jakarta.mail.search.FlagTerm
import jakarta.mail.search.FromStringTerm
import jakarta.mail.search.HeaderTerm
import jakarta.mail.search.MessageIDTerm
import jakarta.mail.search.NotTerm
import jakarta.mail.search.OrTerm
import jakarta.mail.search.ReceivedDateTerm
import jakarta.mail.search.RecipientStringTerm
import jakarta.mail.search.SearchTerm
import jakarta.mail.search.SentDateTerm
import jakarta.mail.search.SizeTerm
import jakarta.mail.search.SubjectTerm
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.Date

/** A compiled `MailSearchQuery`: the Jakarta term (null when only `uid` was given) and the UID restriction. */
class CompiledQuery(
    /** What the server evaluates (`MessageIDTerm` becomes `HEADER Message-ID`). */
    val term: SearchTerm?,
    val uids: UidSet?,
    /** True when a client-side evaluation needs every header (`header`); `messageId` reads the ENVELOPE instead. */
    val needsHeaders: Boolean,
    /** True when a client-side evaluation reads the message body (`body`, `text`). */
    val needsBody: Boolean,
) {
    /** The same query for client-side evaluation: `messageId` matches the ENVELOPE's Message-ID, so no header fetch is needed. */
    val clientTerm: SearchTerm? by lazy { term?.let(::forClient) }

    private fun forClient(term: SearchTerm): SearchTerm = when (term) {
        is AndTerm -> AndTerm(term.terms.map(::forClient).toTypedArray())
        is OrTerm -> OrTerm(term.terms.map(::forClient).toTypedArray())
        is NotTerm -> NotTerm(forClient(term.term))
        is MessageIDTerm -> EnvelopeMessageIdTerm(term.pattern)
        else -> term
    }
}

/** Client-side `messageId` condition on the message id Jakarta reads from the ENVELOPE (IMAP) or the header (local). */
class EnvelopeMessageIdTerm(private val pattern: String) : SearchTerm() {
    override fun match(msg: Message): Boolean {
        val id = runCatching { (msg as? MimeMessage)?.messageID }.getOrNull() ?: return false
        return id.contains(pattern, ignoreCase = true)
    }
}

/**
 * `MailSearchQuery` (roadmap appendix A.5) -> [CompiledQuery]. String conditions are substring,
 * case-insensitive matches (IMAP `SEARCH` semantics); `text` looks at subject, body, from and
 * to; dates accept UTC milliseconds or ISO-8601 (`2026-09-01`, `2026-09-01T08:00:00Z`) and
 * compare by day like the protocol does; `uid` restricts the candidates and is only allowed at
 * the top level; `and` / `or` / `not` nest freely.
 */
object SearchQueryCompiler {

    val KNOWN: Set<String> = setOf(
        "from", "to", "cc", "bcc", "subject", "body", "text",
        "since", "before", "sentSince", "sentBefore",
        "seen", "flagged", "answered", "draft", "deleted",
        "larger", "smaller", "header", "messageId", "uid",
        "and", "or", "not",
    )

    const val MAX_DEPTH = 8

    fun compile(query: JsonObject): CompiledQuery {
        val uids = query["uid"]?.takeUnless { it is JsonNull }?.let { UidSet.parse(it, "query.uid") }
        val state = State()
        val term = term(query, "query", 0, state, allowUid = true)
        if (term == null && uids == null) throw MailException.invalidArgument("'query' needs at least one condition")
        return CompiledQuery(term, uids, state.needsHeaders, state.needsBody)
    }

    private class State {
        var needsHeaders = false
        var needsBody = false
    }

    private fun term(query: JsonObject, label: String, depth: Int, state: State, allowUid: Boolean): SearchTerm? {
        if (depth > MAX_DEPTH) throw MailException.invalidArgument("'$label' nests deeper than $MAX_DEPTH levels")
        query.rejectUnknown(label, KNOWN)
        if (!allowUid && query.containsKey("uid")) throw MailException.invalidArgument("'$label.uid' is only allowed at the top level of the query")
        val terms = ArrayList<SearchTerm>()
        query.string("$label.from")?.let { terms += FromStringTerm(it) }
        query.string("$label.to")?.let { terms += RecipientStringTerm(Message.RecipientType.TO, it) }
        query.string("$label.cc")?.let { terms += RecipientStringTerm(Message.RecipientType.CC, it) }
        query.string("$label.bcc")?.let { terms += RecipientStringTerm(Message.RecipientType.BCC, it) }
        query.string("$label.subject")?.let { terms += SubjectTerm(it) }
        query.string("$label.body")?.let { terms += BodyTerm(it); state.needsBody = true }
        query.string("$label.text")?.let {
            terms += OrTerm(arrayOf(SubjectTerm(it), BodyTerm(it), FromStringTerm(it), RecipientStringTerm(Message.RecipientType.TO, it)))
            state.needsBody = true
        }
        date(query, "$label.since")?.let { terms += ReceivedDateTerm(ComparisonTerm.GE, it) }
        date(query, "$label.before")?.let { terms += ReceivedDateTerm(ComparisonTerm.LT, it) }
        date(query, "$label.sentSince")?.let { terms += SentDateTerm(ComparisonTerm.GE, it) }
        date(query, "$label.sentBefore")?.let { terms += SentDateTerm(ComparisonTerm.LT, it) }
        FlagMapper.SYSTEM.forEach { (name, flag) ->
            query.bool("$label.$name")?.let { terms += FlagTerm(Flags(flag), it) }
        }
        query.long("$label.larger")?.let { terms += SizeTerm(ComparisonTerm.GT, size(it, "$label.larger")) }
        query.long("$label.smaller")?.let { terms += SizeTerm(ComparisonTerm.LT, size(it, "$label.smaller")) }
        query.obj("header")?.let { headers ->
            if (headers.isEmpty()) throw MailException.invalidArgument("'$label.header' must name at least one header")
            headers.forEach { (name, value) ->
                if (!HEADER_NAME.matches(name)) throw MailException.invalidArgument("'$label.header.$name' is not a header name")
                val text = (value as? JsonPrimitive)?.stringValue("$label.header.$name") ?: throw MailException.invalidArgument("'$label.header.$name' must be a string")
                terms += HeaderTerm(name, text)
            }
            state.needsHeaders = true
        }
        query.string("$label.messageId")?.let { terms += MessageIDTerm(it.trim()) }
        query["and"]?.takeUnless { it is JsonNull }?.let { element ->
            val parts = subQueries(element, "$label.and", depth, state)
            if (parts.isNotEmpty()) terms += if (parts.size == 1) parts[0] else AndTerm(parts.toTypedArray())
        }
        query["or"]?.takeUnless { it is JsonNull }?.let { element ->
            val parts = subQueries(element, "$label.or", depth, state)
            if (parts.size < 2) throw MailException.invalidArgument("'$label.or' needs at least two conditions")
            terms += OrTerm(parts.toTypedArray())
        }
        query["not"]?.takeUnless { it is JsonNull }?.let { element ->
            val inner = element as? JsonObject ?: throw MailException.invalidArgument("'$label.not' must be an object")
            val negated = term(inner, "$label.not", depth + 1, state, allowUid = false) ?: throw MailException.invalidArgument("'$label.not' needs a condition")
            terms += NotTerm(negated)
        }
        return when (terms.size) {
            0 -> null
            1 -> terms[0]
            else -> AndTerm(terms.toTypedArray())
        }
    }

    private fun subQueries(element: kotlinx.serialization.json.JsonElement, label: String, depth: Int, state: State): List<SearchTerm> {
        val array = element as? JsonArray ?: throw MailException.invalidArgument("'$label' must be an array of conditions")
        return array.mapIndexedNotNull { index, item ->
            val obj = item as? JsonObject ?: throw MailException.invalidArgument("'$label[$index]' must be an object")
            term(obj, "$label[$index]", depth + 1, state, allowUid = false)
        }
    }

    private fun size(value: Long, path: String): Int {
        if (value < 0 || value > Int.MAX_VALUE) throw MailException.invalidArgument("'$path' must be a byte count")
        return value.toInt()
    }

    private fun date(query: JsonObject, path: String): Date? {
        val element = query[path.substringAfterLast('.')] ?: return null
        if (element is JsonNull) return null
        val primitive = element as? JsonPrimitive ?: throw MailException.invalidArgument("'$path' must be a date (UTC milliseconds or ISO-8601)")
        if (!primitive.isString) {
            val millis = query.long(path) ?: throw MailException.invalidArgument("'$path' must be a date (UTC milliseconds or ISO-8601)")
            if (millis < 0) throw MailException.invalidArgument("'$path' must be UTC milliseconds")
            return Date(millis)
        }
        return parseIsoDate(primitive.content.trim()) ?: throw MailException.invalidArgument("'$path' is not an ISO-8601 date: '${primitive.content.take(40)}'")
    }

    /** `2026-09-01`, `2026-09-01T08:00`, `2026-09-01T08:00:00Z` or with an offset; local dates are taken in the device zone. */
    fun parseIsoDate(text: String): Date? {
        val zone = ZoneId.systemDefault()
        runCatching { return Date.from(Instant.parse(text)) }
        runCatching { return Date.from(OffsetDateTime.parse(text).toInstant()) }
        runCatching { return Date.from(LocalDateTime.parse(text).atZone(zone).toInstant()) }
        return try {
            Date.from(LocalDate.parse(text).atStartOfDay(zone).toInstant())
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private val HEADER_NAME = Regex("[!-9;-~]{1,64}")
}
