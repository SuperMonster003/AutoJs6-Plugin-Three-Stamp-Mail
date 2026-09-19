package io.github.supermonster003.autojs6.plugin.angus.mail.core.message

import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Properties

/**
 * Charset matrix (mail roadmap P6): GB18030, GBK, GB2312, Big5, ISO-2022-JP, EUC-KR and UTF-8
 * in the subject, the display name, the body and both file-name forms (RFC 2231 `filename*=`
 * and an RFC 2047 word in `name=`), each declared, undeclared (raw bytes on the wire) and
 * mis-declared (a charset that cannot represent the bytes). The fixtures are built here from
 * [Sample] texts, so the expected strings are the source of truth. `docs/dev/p6-charset-matrix.md`
 * records the outcome per cell; `CharsetDeviceTest` (app androidTest) checks the same charsets
 * exist on the device runtime.
 */
class CharsetMatrixTest {

    private val session: Session = Session.getInstance(MimeLeniency.apply(Properties()))

    @Test
    fun declaredCharsetsDecodeEverywhere() {
        Sample.ALL.forEach { sample ->
            val doc = full(Fixtures.declared(sample))
            assertEquals("${sample.charset} subject", sample.text, doc.subject)
            assertEquals("${sample.charset} display name", sample.text, doc.from?.name)
            assertEquals("${sample.charset} body", sample.text, doc.text)
            assertEquals("${sample.charset} file names", listOf("${sample.text}.txt", "${sample.text}.txt"), doc.attachments.map { it.fileName })
        }
    }

    @Test
    fun undeclaredBytesAreRecoveredForUtf8TheGbFamilyAndIso2022Jp() {
        Sample.ALL.forEach { sample ->
            val doc = full(Fixtures.undeclared(sample))
            if (sample.recoverableUndeclared) {
                assertEquals("${sample.charset} raw subject", sample.text, doc.subject)
                assertEquals("${sample.charset} raw body", sample.text, doc.text)
                if (sample === Sample.ISO2022JP) {
                    // Raw ISO-2022-JP survives only unstructured headers and bodies: in an address its `ESC ( B` reads as an
                    // RFC 822 comment, and in a quoted parameter the ESC control character ends the parameter (derived name).
                    assertEquals("part-2", doc.attachments.single().fileName)
                } else {
                    assertEquals("${sample.charset} raw display name", sample.text, doc.from?.name)
                    assertEquals("${sample.charset} raw file name", "${sample.text}.txt", doc.attachments.single().fileName)
                }
            } else {
                // Big5 and EUC-KR pairs are valid GB 18030 pairs: without a declaration they read as other characters, never as an error.
                assertNotEquals("${sample.charset} raw subject is mojibake", sample.text, doc.subject)
                assertNotEquals("${sample.charset} raw body is mojibake", sample.text, doc.text)
                assertTrue(doc.subject.isNotEmpty() && doc.text!!.isNotEmpty())
            }
        }
    }

    @Test
    fun misdeclaredCharsetsFallBackToTheGuessingChain() {
        // 163 / QQ habit: gb2312 declared, GBK-only (traditional) characters sent.
        val gbk = Sample.GBK
        val gb2312Lie = full(Fixtures.declared(gbk, declaredAs = "gb2312"))
        assertEquals(gbk.text, gb2312Lie.text)
        assertEquals(gbk.text, gb2312Lie.subject)
        // us-ascii declared on UTF-8 bytes.
        val utf8 = Sample.UTF8
        val asciiLie = full(Fixtures.declared(utf8, declaredAs = "us-ascii"))
        assertEquals(utf8.text, asciiLie.text)
        // an unknown charset name on GB 18030 bytes
        val gb = Sample.GB18030
        assertEquals(gb.text, full(Fixtures.declared(gb, declaredAs = "x-mail-cn")).text)
        // a Big5 body declared as GB 2312 cannot be repaired: it decodes (as GB 18030) to other characters
        val big5 = Sample.BIG5
        val big5Lie = full(Fixtures.declared(big5, declaredAs = "gb2312"))
        assertNotEquals(big5.text, big5Lie.text)
        assertTrue(big5Lie.text!!.isNotEmpty())
    }

    @Test
    fun theRecoveryChainIsDeterministic() {
        Sample.ALL.forEach { sample ->
            val bytes = sample.text.toByteArray(sample.javaCharset)
            assertEquals("${sample.charset} declared", sample.text, TextRecovery.decode(bytes, sample.charset))
            val guessed = TextRecovery.decodeGuessing(bytes)
            if (sample.recoverableUndeclared) assertEquals("${sample.charset} guessed", sample.text, guessed) else assertNotEquals(sample.text, guessed)
        }
        assertEquals("ISO-2022-JP header without an encoded word", Sample.ISO2022JP.text, TextRecovery.repairHeader(String(Sample.ISO2022JP.text.toByteArray(Sample.ISO2022JP.javaCharset), StandardCharsets.ISO_8859_1)))
    }

    private fun full(raw: ByteArray): MessageDocument = MessageMapper.full(MimeMessage(session, ByteArrayInputStream(raw)), "INBOX", JsonPrimitive(1))
}

/** One charset with a text it can represent (`recoverableUndeclared`: the guessing chain gets it back without a declaration). */
class Sample(val charset: String, val text: String, val recoverableUndeclared: Boolean) {
    val javaCharset: Charset get() = Charset.forName(charset)

    companion object {
        val UTF8 = Sample("UTF-8", "你好 こんにちは 안녕 café", true)
        val GB18030 = Sample("GB18030", "你好，𠮷野家的报表。", true)
        val GBK = Sample("GBK", "你好，這是一封測試郵件。", true)
        val GB2312 = Sample("GB2312", "你好，这是一封测试邮件。", true)
        val BIG5 = Sample("Big5", "測試郵件，你好。", false)
        val ISO2022JP = Sample("ISO-2022-JP", "こんにちは、テストメールです。", true)
        val EUCKR = Sample("EUC-KR", "안녕하세요, 테스트 메일입니다.", false)
        val ALL = listOf(UTF8, GB18030, GBK, GB2312, BIG5, ISO2022JP, EUCKR)
    }
}

/** Raw messages for the matrix; every string part is ASCII, the charset bytes are spliced in as bytes. */
object Fixtures {

    fun declared(sample: Sample, declaredAs: String = sample.charset): ByteArray {
        val bytes = sample.text.toByteArray(sample.javaCharset)
        val word = "=?$declaredAs?B?${Base64.getEncoder().encodeToString(bytes)}?="
        val fileWord = "=?$declaredAs?B?${Base64.getEncoder().encodeToString(sample.text.toByteArray(sample.javaCharset) + ".txt".toByteArray())}?="
        val rfc2231 = "$declaredAs''" + percent(bytes) + ".txt"
        val out = ByteArrayOutputStream()
        out.ascii("From: $word <a@example.org>\r\nTo: b@example.org\r\nSubject: $word\r\nContent-Type: multipart/mixed; boundary=\"m\"\r\n\r\n")
        out.ascii("--m\r\nContent-Type: text/plain; charset=$declaredAs\r\nContent-Transfer-Encoding: 8bit\r\n\r\n")
        out.write(bytes)
        out.ascii("\r\n--m\r\nContent-Type: application/octet-stream\r\nContent-Disposition: attachment; filename*=$rfc2231\r\n\r\nx\r\n")
        out.ascii("--m\r\nContent-Type: application/octet-stream; name=\"$fileWord\"\r\nContent-Disposition: attachment\r\n\r\nx\r\n--m--\r\n")
        return out.toByteArray()
    }

    fun undeclared(sample: Sample): ByteArray {
        val bytes = sample.text.toByteArray(sample.javaCharset)
        val out = ByteArrayOutputStream()
        out.ascii("From: \""); out.write(bytes); out.ascii("\" <a@example.org>\r\nTo: b@example.org\r\nSubject: "); out.write(bytes)
        out.ascii("\r\nContent-Type: multipart/mixed; boundary=\"m\"\r\n\r\n--m\r\nContent-Type: text/plain\r\nContent-Transfer-Encoding: 8bit\r\n\r\n")
        out.write(bytes)
        out.ascii("\r\n--m\r\nContent-Type: application/octet-stream\r\nContent-Disposition: attachment; filename=\""); out.write(bytes); out.ascii(".txt\"\r\n\r\nx\r\n--m--\r\n")
        return out.toByteArray()
    }

    private fun percent(bytes: ByteArray): String = bytes.joinToString("") { "%%%02X".format(it.toInt() and 0xff) }

    private fun ByteArrayOutputStream.ascii(text: String) = write(text.toByteArray(StandardCharsets.US_ASCII))
}
