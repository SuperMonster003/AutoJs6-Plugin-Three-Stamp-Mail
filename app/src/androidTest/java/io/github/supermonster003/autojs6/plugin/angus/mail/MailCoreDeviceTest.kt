package io.github.supermonster003.autojs6.plugin.angus.mail

import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SendResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MessageSummary
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.OutgoingMessage
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSession
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailcapRegistry
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.SmtpSender
import jakarta.activation.CommandMap
import jakarta.mail.Flags
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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
 * `mailProvider=qq` selects a built-in preset instead of the host arguments (still overridable).
 * Optional arguments: `mailUsername`, `mailImapPort` (993), `mailSmtpPort` (465), `mailTls`
 * (`ssl` / `starttls` / `none`), `mailTrustAll` (`true` accepts any certificate, roadmap D25),
 * `mailAuth` (`password` / `xoauth2`), `mailTo` (defaults to the peer address, then the address),
 * `mailSaveToSent` (`true` / `false` forces the sent copy; absent follows the preset), `mailDebug`
 * (`true` logs the redacted protocol trace of the session, roadmap D28), `mailAppendFolder` (a
 * folder such as `Drafts` receives one draft through `messages.append`, reporting the UID). A second
 * account (`mailPeerAddress`, `mailPeerSecret`, `mailPeerProvider`, `mailPeerAuth`) turns the run
 * into a two-account round trip: the message with its attachment goes to the peer and the test
 * polls the peer inbox until it arrives.
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
                OutgoingMessage.simple(listOf("you@example.com"), "测试 Round trip", "text body", "<p>html body</p>", listOf(attachment)),
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

    /**
     * Real-provider round trip (roadmap P0.2 item 2 and the P2 acceptance): the account JSON goes
     * through `MailAccountOptions` exactly as the Binder would build it (`mailProvider` selects a
     * preset, host / port / tls arguments override it), `session.test` probes the receive and SMTP
     * endpoints and reports their capabilities, one message with an attachment goes out through
     * `MailSession.send` (SMTP plus the optional sent copy over IMAP `APPEND`), the inbox is listed
     * through IMAP and, with a peer account, the peer inbox is polled for the message. Logs carry
     * host names, folder names, capabilities and durations only.
     */
    @Test
    fun realAccountSendsAndListsWhenProvided() {
        val address = arguments.getString("mailAddress")
        val secret = arguments.getString("mailSecret")
        val provider = arguments.getString("mailProvider")?.takeIf { it.isNotBlank() }
        val imapHost = arguments.getString("mailImapHost")?.takeIf { it.isNotBlank() }
        val smtpHost = arguments.getString("mailSmtpHost")?.takeIf { it.isNotBlank() }
        assumeTrue(
            "no test account supplied through instrumentation arguments; skipping the real-provider round trip",
            !address.isNullOrBlank() && !secret.isNullOrBlank() && (provider != null || (imapHost != null && smtpHost != null)),
        )
        val tls = arguments.getString("mailTls")?.takeIf { it.isNotBlank() }
        val options = JSONObject().put("address", address)
        provider?.let { options.put("provider", it) }
        arguments.getString("mailUsername")?.takeIf { it.isNotBlank() }?.let { options.put("user", it) }
        fun endpoint(host: String?, portKey: String): JSONObject? {
            val port = arguments.getString(portKey)?.toIntOrNull()
            if (host == null && port == null && tls == null) return null
            return JSONObject().apply {
                host?.let { put("host", it) }
                port?.let { put("port", it) }
                tls?.let { put("tls", it) }
            }
        }
        endpoint(imapHost, "mailImapPort")?.let { options.put("imap", it) }
        endpoint(smtpHost, "mailSmtpPort")?.let { options.put("smtp", it) }
        if (arguments.getString("mailTrustAll") == "true") options.put("tls", JSONObject().put("trustAll", true))
        if (arguments.getString("mailDebug") == "true") options.put("debug", true)
        options.put("timeout", JSONObject().put("connect", 20_000).put("read", 60_000))
        val secretKind = if (arguments.getString("mailAuth") == "xoauth2") SecretKind.ACCESS_TOKEN else SecretKind.PASSWORD
        val account = MailAccountOptions.parse(options.toString(), secretKind)
        val imap = requireNotNull(account.imap) { "the account needs an IMAP endpoint" }
        val smtp = requireNotNull(account.smtp) { "the account needs an SMTP endpoint" }
        Log.i(TAG, "account run: provider=${account.provider?.id} auth=${account.auth.id} imap=$imap smtp=$smtp insecure=${account.insecure}")

        MailSession(account, MailSecret(secret!!)).use { session ->
            val report = session.test()
            listOfNotNull(report.imap, report.pop3, report.smtp).forEach { endpoint ->
                Log.i(TAG, "session.test ${endpoint.protocol} ${endpoint.host}:${endpoint.port}/${endpoint.tls} ok=${endpoint.ok} ${endpoint.elapsedMs} ms capabilities=${endpoint.capabilities} error=${endpoint.error?.code} ${endpoint.error?.message ?: ""}")
            }
            assertTrue("session.test failed: imap=${report.imap?.error?.code} smtp=${report.smtp?.error?.code}", report.ok)

            val peerAddress = arguments.getString("mailPeerAddress")?.takeIf { it.isNotBlank() }
            val peerSecret = arguments.getString("mailPeerSecret")?.takeIf { it.isNotBlank() }
            val to = arguments.getString("mailTo")?.takeIf { it.isNotBlank() } ?: peerAddress ?: account.address
            val subject = "AutoJs6 Angus Mail round trip ${System.currentTimeMillis()}"
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val attachment = File(context.cacheDir, "round-trip-attachment.txt").apply { writeText("attachment sent from API ${Build.VERSION.SDK_INT}\n") }
            val saveToSent = arguments.getString("mailSaveToSent")?.toBooleanStrictOrNull()
            val sent = try {
                session.send(
                    OutgoingMessage.simple(listOf(to), subject, "Sent from the AutoJs6 Angus Mail plugin on API ${Build.VERSION.SDK_INT}", attachments = listOf(attachment)),
                    saveToSent,
                )
            } finally {
                attachment.delete()
            }
            val sendMillis = sent.elapsedMs
            Log.i(TAG, "mail.send ok in $sendMillis ms: accepted=${sent.accepted.size} rejected=${sent.rejected.size} sentCopy=${sent.sentCopy} savedToSent=${sent.savedToSent} sentFolder=${sent.sentFolder} saveError=${sent.saveError?.code} ${sent.saveError?.message ?: ""} ${sent.saveError?.details ?: ""}")
            logTrace(session)
            assertTrue(sent.messageId.isNotBlank())
            assertEquals(1, sent.accepted.size)
            val saveFailure = if (saveToSent == true && sent.sentCopy != SendResult.SENT_COPY_APPENDED && sent.sentCopy != SendResult.SENT_COPY_SERVER) {
                "saveToSent=true must leave a copy: ${sent.sentCopy} ${sent.saveError?.code} ${sent.sentFolder} ${sent.saveError?.details ?: ""}"
            } else {
                null
            }

            arguments.getString("mailAppendFolder")?.takeIf { it.isNotBlank() }?.let { folder ->
                val appendStarted = System.nanoTime()
                val appended = session.append(
                    folder,
                    OutgoingMessage.simple(listOf(to), "AutoJs6 Angus Mail draft ${System.currentTimeMillis()}", "Unsent draft appended from API ${Build.VERSION.SDK_INT}"),
                    Flags(Flags.Flag.DRAFT).also { it.add(Flags.Flag.SEEN) },
                )
                Log.i(TAG, "messages.append ok in ${(System.nanoTime() - appendStarted) / 1_000_000} ms: folder=${appended.folder} uid=${appended.uid}")
                logTrace(session)
                assertEquals(folder, appended.folder)
            }

            val listStarted = System.nanoTime()
            val summaries = session.imap { mailbox -> mailbox.listInbox(5) }
            val listMillis = (System.nanoTime() - listStarted) / 1_000_000
            assertTrue("the inbox listing must return at least one message", summaries.isNotEmpty())
            assertTrue(summaries.size <= 5)
            Log.i(TAG, "real account ok on API ${Build.VERSION.SDK_INT}: test ${report.elapsedMs} ms, send $sendMillis ms, list ${summaries.size} messages in $listMillis ms, connected=${session.connectedProtocols.map { it.id }}")
            logTrace(session)

            if (peerAddress != null && peerSecret != null) {
                val peerOptions = JSONObject().put("address", peerAddress).put("timeout", JSONObject().put("connect", 20_000).put("read", 60_000))
                arguments.getString("mailPeerProvider")?.takeIf { it.isNotBlank() }?.let { peerOptions.put("provider", it) }
                val peerKind = if (arguments.getString("mailPeerAuth") == "xoauth2") SecretKind.ACCESS_TOKEN else SecretKind.PASSWORD
                val peer = MailAccountOptions.parse(peerOptions.toString(), peerKind)
                MailSession(peer, MailSecret(peerSecret)).use { peerSession ->
                    val pollStarted = System.nanoTime()
                    val deadline = System.currentTimeMillis() + PEER_WAIT_MS
                    var delivered: MessageSummary? = null
                    var polls = 0
                    while (delivered == null && System.currentTimeMillis() < deadline) {
                        polls++
                        delivered = peerSession.imap { mailbox -> mailbox.listInbox(10) }.firstOrNull { it.subject == subject }
                        if (delivered == null) Thread.sleep(PEER_POLL_MS)
                    }
                    val waited = (System.nanoTime() - pollStarted) / 1_000_000
                    assertNotNull("the peer inbox (${peer.provider?.id ?: peer.imap}) did not receive the message within ${PEER_WAIT_MS / 1000} s ($polls polls)", delivered)
                    Log.i(TAG, "peer received the message after $waited ms ($polls polls): uid=${delivered!!.uid} size=${delivered.size} bytes seen=${delivered.seen}")
                    logTrace(peerSession)
                }
            }
            saveFailure?.let { fail(it) }
        }
    }

    /** The redacted protocol trace (`debug: true`): command names, response codes and durations, never credentials. */
    private fun logTrace(session: MailSession) {
        if (!session.trace.enabled) return
        session.trace.drain().forEach { Log.i(TAG, "trace ${session.account.provider?.id ?: session.account.address.substringAfter('@')}: $it") }
    }

    private companion object {
        const val TAG = "MailCoreDeviceTest"
        const val PEER_WAIT_MS = 120_000L
        const val PEER_POLL_MS = 5_000L
    }
}
