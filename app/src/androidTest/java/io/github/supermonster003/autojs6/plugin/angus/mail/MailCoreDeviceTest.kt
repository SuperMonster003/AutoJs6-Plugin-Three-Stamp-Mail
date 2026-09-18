package io.github.supermonster003.autojs6.plugin.angus.mail

import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.AuthMethod
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.ImapMailbox
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailcapRegistry
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.OutgoingMessage
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.SmtpSender
import jakarta.activation.CommandMap
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Roadmap P0.2 on-device evidence for the mail core: the Angus Mail data content handlers resolve
 * inside the APK (also under R8 when run with `-PandroidTestRelease`), MIME composition and parsing
 * work on the device runtime, and, when a maintainer supplies an account through instrumentation
 * arguments, one message goes out through SMTP and the inbox is listed through IMAP.
 *
 * Real-account run (values never enter the repository):
 * ```
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.mailAddress=me@qq.com \
 *   -Pandroid.testInstrumentationRunnerArguments.mailSecret=authorization-code \
 *   -Pandroid.testInstrumentationRunnerArguments.mailImapHost=imap.qq.com \
 *   -Pandroid.testInstrumentationRunnerArguments.mailSmtpHost=smtp.qq.com
 * ```
 * Optional arguments: `mailUsername`, `mailImapPort` (993), `mailSmtpPort` (465), `mailTls`
 * (`ssl` / `starttls` / `none`), `mailTrustAll` (`true` accepts any certificate, roadmap D25),
 * `mailAuth` (`password` / `xoauth2`), `mailTo` (defaults to the address).
 *
 * Credential-free device run against a GreenMail server on the development machine (the same
 * server the `:mail-core` JVM tests use; `adb reverse` maps the device's localhost onto it):
 * ```
 * java -Dgreenmail.setup.test.all -Dgreenmail.hostname=0.0.0.0  *   -Dgreenmail.users=alice:alice-secret@localhost -jar greenmail-standalone-2.1.13.jar
 * adb reverse tcp:3025 tcp:3025 && adb reverse tcp:3143 tcp:3143
 * ./gradlew :app:connectedDebugAndroidTest  *   -Pandroid.testInstrumentationRunnerArguments.mailAddress=alice@localhost  *   -Pandroid.testInstrumentationRunnerArguments.mailUsername=alice  *   -Pandroid.testInstrumentationRunnerArguments.mailSecret=alice-secret  *   -Pandroid.testInstrumentationRunnerArguments.mailImapHost=127.0.0.1  *   -Pandroid.testInstrumentationRunnerArguments.mailImapPort=3143  *   -Pandroid.testInstrumentationRunnerArguments.mailSmtpHost=127.0.0.1  *   -Pandroid.testInstrumentationRunnerArguments.mailSmtpPort=3025  *   -Pandroid.testInstrumentationRunnerArguments.mailTls=none
 * ```
 */
@RunWith(AndroidJUnit4::class)
class MailCoreDeviceTest {

    private val arguments get() = InstrumentationRegistry.getArguments()

    @Test
    fun angusHandlersResolveInsideTheApk() {
        MailcapRegistry.ensureRegistered()
        val commandMap = CommandMap.getDefaultCommandMap()
        MailcapRegistry.HANDLERS.forEach { (mimeType, handler) ->
            val probe = if (mimeType.endsWith("/*")) mimeType.removeSuffix("*") + "mixed" else mimeType
            val resolved = commandMap.createDataContentHandler(probe)
            assertNotNull("no data content handler for $probe on API ${Build.VERSION.SDK_INT}", resolved)
            assertEquals(handler, resolved.javaClass.name)
        }
    }

    @Test
    fun multipartMessagesRoundTripOnTheDeviceRuntime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val attachment = File(context.cacheDir, "spike-attachment.txt").apply { writeText("attachment body") }
        try {
            val account = MailAccount("me@example.com", smtp = MailEndpoint("smtp.example.com", 465, TlsMode.SSL))
            val composed = SmtpSender(account, MailSecret("unused")).compose(
                OutgoingMessage(listOf("you@example.com"), "测试 Round trip", "text body", "<p>html body</p>", listOf(attachment)),
            )
            val bytes = ByteArrayOutputStream().also { composed.writeTo(it) }.toByteArray()
            val parsed = MimeMessage(composed.session, ByteArrayInputStream(bytes))
            assertEquals("测试 Round trip", parsed.subject)
            val mixed = parsed.content as MimeMultipart
            assertEquals(2, mixed.count)
            val alternative = mixed.getBodyPart(0).content as MimeMultipart
            assertEquals("text body", (alternative.getBodyPart(0).content as String).trim())
            val part = mixed.getBodyPart(1) as MimeBodyPart
            assertEquals("spike-attachment.txt", part.fileName)
            assertEquals("attachment body", part.inputStream.readBytes().toString(Charsets.UTF_8))
            Log.i(TAG, "MIME round trip ok on API ${Build.VERSION.SDK_INT} (${Build.MANUFACTURER} ${Build.MODEL}), ${bytes.size} bytes")
        } finally {
            attachment.delete()
        }
    }

    @Test
    fun realAccountSendsAndListsWhenProvided() {
        val address = arguments.getString("mailAddress")
        val secret = arguments.getString("mailSecret")
        val imapHost = arguments.getString("mailImapHost")
        val smtpHost = arguments.getString("mailSmtpHost")
        assumeTrue(
            "no test account supplied through instrumentation arguments; skipping the real-provider round trip",
            !address.isNullOrBlank() && !secret.isNullOrBlank() && !imapHost.isNullOrBlank() && !smtpHost.isNullOrBlank(),
        )
        val tls = when (arguments.getString("mailTls")) {
            "starttls" -> TlsMode.STARTTLS
            "none" -> TlsMode.NONE
            else -> TlsMode.SSL
        }
        val defaultSmtpPort = when (tls) {
            TlsMode.SSL -> 465
            TlsMode.STARTTLS -> 587
            TlsMode.NONE -> 25
        }
        val account = MailAccount(
            address = address!!,
            username = arguments.getString("mailUsername")?.takeIf { it.isNotBlank() } ?: address,
            auth = if (arguments.getString("mailAuth") == "xoauth2") AuthMethod.XOAUTH2 else AuthMethod.PASSWORD,
            imap = MailEndpoint(imapHost!!, arguments.getString("mailImapPort")?.toIntOrNull() ?: (if (tls == TlsMode.SSL) 993 else 143), tls),
            smtp = MailEndpoint(smtpHost!!, arguments.getString("mailSmtpPort")?.toIntOrNull() ?: defaultSmtpPort, tls),
            trustAll = arguments.getString("mailTrustAll") == "true",
        )
        val imap = requireNotNull(account.imap)
        val smtp = requireNotNull(account.smtp)
        Log.i(TAG, "account run: ${imap.host}:${imap.port} / ${smtp.host}:${smtp.port}, tls=${tls.id}, insecure=${account.insecure}")
        val to = arguments.getString("mailTo")?.takeIf { it.isNotBlank() } ?: address
        val subject = "AutoJs6 Angus Mail spike ${System.currentTimeMillis()}"

        val started = System.nanoTime()
        val messageId = SmtpSender(account, MailSecret(secret!!)).use { it.send(OutgoingMessage(listOf(to), subject, "Sent from the P0.2 spike on API ${Build.VERSION.SDK_INT}")) }
        val sendMillis = (System.nanoTime() - started) / 1_000_000
        assertTrue(messageId.isNotBlank())

        val listStarted = System.nanoTime()
        val summaries = ImapMailbox.connect(account, MailSecret(secret)).use { mailbox ->
            Log.i(TAG, "IMAP IDLE capability: ${mailbox.hasCapability("IDLE")}")
            mailbox.listInbox(5)
        }
        val listMillis = (System.nanoTime() - listStarted) / 1_000_000
        assertTrue("the inbox listing must return at least one message", summaries.isNotEmpty())
        assertTrue(summaries.size <= 5)
        Log.i(TAG, "real account ok on API ${Build.VERSION.SDK_INT}: send ${sendMillis} ms, list ${summaries.size} messages in ${listMillis} ms")
    }

    private companion object {
        const val TAG = "MailCoreDeviceTest"
    }
}
