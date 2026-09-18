package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.OutgoingMessage
import jakarta.activation.CommandMap
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MailcapRegistryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun everyAngusHandlerResolvesAfterRegistration() {
        MailcapRegistry.ensureRegistered()
        MailcapRegistry.ensureRegistered()
        val commandMap = CommandMap.getDefaultCommandMap()
        MailcapRegistry.HANDLERS.forEach { (mimeType, handler) ->
            val probe = if (mimeType.endsWith("/*")) mimeType.removeSuffix("*") + "mixed" else mimeType
            val resolved = commandMap.createDataContentHandler(probe)
            assertNotNull("no data content handler for $probe", resolved)
            assertEquals(handler, resolved.javaClass.name)
        }
    }

    @Test
    fun multipartMessagesSurviveAWriteAndParseRoundTrip() {
        val account = MailAccount("me@example.com", smtp = MailEndpoint("smtp.example.com", 465, TlsMode.SSL))
        val attachment = temporaryFolder.newFile("附件.txt").apply { writeText("attachment body") }
        val composed = SmtpSender(account, MailSecret("unused")).compose(
            OutgoingMessage.simple(listOf("you@example.com"), "Round trip", "text body", "<p>html body</p>", listOf(attachment)),
        )
        val bytes = ByteArrayOutputStream().also { composed.writeTo(it) }.toByteArray()

        val parsed = MimeMessage(composed.session, ByteArrayInputStream(bytes))
        assertEquals("Round trip", parsed.subject)
        assertEquals("me@example.com", parsed.from.single().toString())
        val mixed = parsed.content as MimeMultipart
        assertEquals(2, mixed.count)
        val alternative = mixed.getBodyPart(0).content as MimeMultipart
        assertEquals("text body", (alternative.getBodyPart(0).content as String).trim())
        assertEquals("<p>html body</p>", (alternative.getBodyPart(1).content as String).trim())
        val part = mixed.getBodyPart(1) as MimeBodyPart
        assertEquals("附件.txt", part.fileName)
        assertEquals("attachment body", part.inputStream.readBytes().toString(Charsets.UTF_8))
    }
}
