package io.github.supermonster003.autojs6.plugin.angus.mail.core.query

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MailJson
import jakarta.mail.Flags
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.UIDFolder
import jakarta.mail.internet.InternetAddress
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
import jakarta.mail.search.SentDateTerm
import jakarta.mail.search.SizeTerm
import jakarta.mail.search.SubjectTerm
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Properties

/** Roadmap appendix A.5 `MailSearchQuery` -> Jakarta terms, plus the UID set grammar. */
class SearchQueryCompilerTest {

    @Test
    fun singleStringConditionsMapToTheirTerms() {
        assertTrue(compile("""{"from": "alice"}""").term is FromStringTerm)
        assertTrue(compile("""{"subject": "报表"}""").term is SubjectTerm)
        assertTrue(compile("""{"body": "x"}""").term is BodyTerm)
        assertTrue(compile("""{"body": "x"}""").needsBody)
        assertFalse(compile("""{"subject": "x"}""").needsBody)
        assertFalse(compile("""{"subject": "x"}""").needsHeaders)
    }

    @Test
    fun textLooksAtSubjectBodyFromAndTo() {
        val term = compile("""{"text": "invoice"}""").term as OrTerm
        assertEquals(listOf(SubjectTerm::class, BodyTerm::class, FromStringTerm::class, jakarta.mail.search.RecipientStringTerm::class), term.terms.map { it::class })
    }

    @Test
    fun severalConditionsInOneObjectAreAnded() {
        val term = compile("""{"from": "alice", "subject": "hi", "seen": false}""").term as AndTerm
        assertEquals(3, term.terms.size)
        val flag = term.terms.filterIsInstance<FlagTerm>().single()
        assertTrue(flag.flags.contains(Flags.Flag.SEEN))
        assertFalse(flag.testSet)
    }

    @Test
    fun datesAcceptMillisAndIsoForms() {
        val millis = 1_756_684_800_000L
        val fromMillis = compile("""{"since": $millis}""").term as ReceivedDateTerm
        assertEquals(ComparisonTerm.GE, fromMillis.comparison)
        assertEquals(Date(millis), fromMillis.date)
        val instant = compile("""{"before": "2026-09-01T08:00:00Z"}""").term as ReceivedDateTerm
        assertEquals(ComparisonTerm.LT, instant.comparison)
        assertEquals(Date.from(java.time.Instant.parse("2026-09-01T08:00:00Z")), instant.date)
        val day = compile("""{"sentSince": "2026-09-01"}""").term as SentDateTerm
        assertEquals(Date.from(LocalDate.of(2026, 9, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()), day.date)
        assertTrue(compile("""{"sentBefore": "2026-09-01T08:00"}""").term is SentDateTerm)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure("""{"since": "yesterday"}""").code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure("""{"since": -1}""").code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure("""{"since": true}""").code)
    }

    @Test
    fun sizesHeadersAndMessageIdsCompile() {
        val larger = compile("""{"larger": 1024}""").term as SizeTerm
        assertEquals(ComparisonTerm.GT, larger.comparison)
        assertEquals(1024, larger.number)
        assertTrue(compile("""{"smaller": 5}""").term is SizeTerm)
        val header = compile("""{"header": {"X-Campaign": "spring"}}""")
        assertTrue(header.term is HeaderTerm)
        assertTrue(header.needsHeaders)
        val id = compile("""{"messageId": " <abc@example.org> "}""")
        assertEquals("<abc@example.org>", (id.term as MessageIDTerm).pattern)
        assertFalse("the client side reads the ENVELOPE, not the headers", id.needsHeaders)
        assertTrue(id.clientTerm is EnvelopeMessageIdTerm)
        val nested = compile("""{"or": [{"messageId": "x"}, {"and": [{"not": {"messageId": "y"}}, {"from": "a"}]}]}""")
        val clientOr = nested.clientTerm as OrTerm
        assertTrue(clientOr.terms[0] is EnvelopeMessageIdTerm)
        assertTrue(((clientOr.terms[1] as AndTerm).terms[0] as NotTerm).term is EnvelopeMessageIdTerm)
        assertTrue((nested.term as OrTerm).terms[0] is MessageIDTerm)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure("""{"larger": -5}""").code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure("""{"header": {}}""").code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure("""{"header": {"bad name": "x"}}""").code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure("""{"header": {"X": 1}}""").code)
    }

    @Test
    fun booleanCombinatorsNest() {
        val query = compile("""{"or": [{"from": "a"}, {"and": [{"subject": "b"}, {"not": {"seen": true}}]}]}""")
        val or = query.term as OrTerm
        assertEquals(2, or.terms.size)
        assertTrue(or.terms[0] is FromStringTerm)
        val and = or.terms[1] as AndTerm
        assertTrue(and.terms[0] is SubjectTerm)
        assertTrue(and.terms[1] is NotTerm)
        assertTrue(compile("""{"and": [{"from": "a"}]}""").term is FromStringTerm)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure("""{"or": [{"from": "a"}]}""").code)
        assertTrue(failure("""{"or": [{"from": "a"}]}""").message.contains("at least two"))
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure("""{"not": {}}""").code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure("""{"and": {"from": "a"}}""").code)
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure("""{"or": [{"from": "a"}, "b"]}""").code)
    }

    @Test
    fun depthIsBounded() {
        var json = """{"from": "a"}"""
        repeat(SearchQueryCompiler.MAX_DEPTH) { json = """{"not": $json}""" }
        assertNotNull(compile(json).term)
        val tooDeep = failure("""{"not": $json}""")
        assertTrue(tooDeep.message, tooDeep.message.contains("deeper than"))
    }

    @Test
    fun emptyAndUnknownConditionsAreRejected() {
        assertTrue(failure("{}").message.contains("at least one condition"))
        val unknown = failure("""{"sender": "a"}""")
        assertEquals(MailErrorCode.INVALID_ARGUMENT, unknown.code)
        assertTrue(unknown.message, unknown.message.contains("query.sender"))
        assertTrue(failure("""{"from": ""}""").message.contains("blank"))
        assertTrue(failure("""{"from": 1}""").message.contains("must be a string"))
        assertEquals(MailErrorCode.INVALID_ARGUMENT, failure("""{"seen": "yes"}""").code)
    }

    @Test
    fun uidRestrictionsLiveAtTheTopLevelOnly() {
        val query = compile("""{"uid": "100:*", "seen": false}""")
        assertEquals("100:*", query.uids.toString())
        assertTrue(query.term is FlagTerm)
        val only = compile("""{"uid": [1, "5:3", "9,12"]}""")
        assertNull(only.term)
        assertEquals("1,3:5,9,12", only.uids.toString())
        val nested = failure("""{"or": [{"uid": 1}, {"from": "a"}]}""")
        assertTrue(nested.message, nested.message.contains("only allowed at the top level"))
        assertTrue(failure("""{"not": {"uid": "1:2"}}""").message.contains("top level"))
    }

    @Test
    fun uidSetGrammar() {
        val set = UidSet.parse(JsonPrimitive("1,5,9:12,20:*"))
        assertTrue(set.contains(1L))
        assertFalse(set.contains(2L))
        assertTrue(set.contains(10L))
        assertTrue(set.contains(9_999_999L))
        assertEquals(listOf(1L..1L, 5L..5L, 9L..12L, 20L..UidSet.STAR), set.ranges)
        assertEquals("7", UidSet.parse(JsonPrimitive(7)).toString())
        assertEquals("3,4,5", UidSet.of(listOf(5L, 3L, 4L)).toString())
        assertEquals("*", UidSet.parse(JsonPrimitive("*")).toString())
        assertEquals(UIDFolder.LASTUID, UidSet.wire(UidSet.STAR))
        assertEquals(42L, UidSet.wire(42L))
        assertEquals("5:*", UidSet.parse(JsonPrimitive("*:5")).toString())
        listOf("\"0\"", "\"a:b\"", "\"1:2:3\"", "\"\"", "\" , \"", "true", "0", "-4", "[]", "[[1]]", "{\"x\":1}").forEach { bad ->
            try {
                UidSet.parse(MailJson.format.parseToJsonElement(bad), "query.uid")
                fail("$bad must be rejected")
            } catch (e: MailException) {
                assertEquals(bad, MailErrorCode.INVALID_ARGUMENT, e.code)
                assertTrue(e.message, e.message.contains("query.uid"))
            }
        }
    }

    @Test
    fun compiledTermsMatchLocalMessagesLikeTheClientFallback() {
        val message = MimeMessage(Session.getInstance(Properties())).apply {
            setFrom(InternetAddress("alice@example.org", "Alice"))
            setRecipients(Message.RecipientType.TO, "bob@example.org")
            subject = "Quarterly 报表"
            setText("the body mentions invoices")
            setHeader("X-Campaign", "spring")
            saveChanges()
        }
        assertTrue(compile("""{"from": "alice", "subject": "报表"}""").term!!.match(message))
        assertTrue(compile("""{"text": "invoices"}""").term!!.match(message))
        assertTrue(compile("""{"header": {"X-Campaign": "spr"}}""").term!!.match(message))
        assertTrue(compile("""{"messageId": "${message.messageID.trim('<', '>')}"}""").clientTerm!!.match(message))
        assertFalse(compile("""{"messageId": "nope@example.org"}""").clientTerm!!.match(message))
        assertFalse(compile("""{"to": "carol"}""").term!!.match(message))
        assertTrue(compile("""{"not": {"to": "carol"}}""").term!!.match(message))
        assertTrue(compile("""{"or": [{"to": "carol"}, {"subject": "quarterly"}]}""").term!!.match(message))
    }

    private fun compile(json: String): CompiledQuery = SearchQueryCompiler.compile(MailJson.format.parseToJsonElement(json) as JsonObject)

    private fun failure(json: String): MailException {
        try {
            compile(json)
        } catch (e: MailException) {
            return e
        }
        fail("expected a MailException for $json")
        throw AssertionError()
    }
}
