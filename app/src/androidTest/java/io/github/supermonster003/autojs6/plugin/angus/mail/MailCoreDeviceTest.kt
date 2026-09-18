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
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.FolderDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SendResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.imapUid
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.OutgoingMessage
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageArgs
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSession
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailcapRegistry
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.SmtpSender
import jakarta.activation.CommandMap
import jakarta.mail.Flags
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * folder such as `Drafts` receives one draft through `messages.append`, reporting the UID),
 * `mailCleanup` (`true` deletes and expunges the round-trip message at the end). A second account
 * (`mailPeerAddress`, `mailPeerSecret`, `mailPeerProvider`, `mailPeerAuth`, optional `mailPeerImapHost`) turns the run into a
 * two-account round trip: the message with its attachment goes to the peer and the test polls the
 * peer inbox until it arrives. Wherever the message lands (the peer, or the account itself when it
 * is its own recipient), the roadmap P2.3 operations run against it: the folder tree, the inbox
 * page, `messages.search` by Message-ID, `messages.get`, `attachments.download` (byte-compared with
 * what was sent), `messages.raw`, `messages.setFlags` and, with `mailCleanup`, `messages.delete`.
 *
 * Credential-free device run against a GreenMail server on the development machine (the same
 * server the `:mail-core` JVM tests use; `adb reverse` maps the device's localhost onto it):
 * ```
 * java -Dgreenmail.setup.test.all -Dgreenmail.hostname=0.0.0.0 \
 *   -Dgreenmail.users=alice:alice-secret@localhost -jar greenmail-standalone-2.1.13.jar
 * adb reverse tcp:3025 tcp:3025 && adb reverse tcp:3143 tcp:3143
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.mailAddress=alice@localhost \
 *   -Pandroid.testInstrumentationRunnerArguments.mailUsername=alice \
 *   -Pandroid.testInstrumentationRunnerArguments.mailSecret=alice-secret \
 *   -Pandroid.testInstrumentationRunnerArguments.mailImapHost=127.0.0.1 \
 *   -Pandroid.testInstrumentationRunnerArguments.mailImapPort=3143 \
 *   -Pandroid.testInstrumentationRunnerArguments.mailSmtpHost=127.0.0.1 \
 *   -Pandroid.testInstrumentationRunnerArguments.mailSmtpPort=3025 \
 *   -Pandroid.testInstrumentationRunnerArguments.mailTls=none
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
        val account = MailAccountOptions.parse(options.toString(), secretKind, clientIdDefaults())
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
            val attachmentBytes = "attachment sent from API ${Build.VERSION.SDK_INT}\n".toByteArray()
            val attachment = File(context.cacheDir, "round-trip-attachment.txt").apply { writeBytes(attachmentBytes) }
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
            val summaries = session.listMessages(MessageArgs.list("""{"limit": 5}"""))
            val listMillis = (System.nanoTime() - listStarted) / 1_000_000
            assertTrue("the inbox listing must return at least one message", summaries.isNotEmpty())
            assertTrue(summaries.size <= 5)
            Log.i(TAG, "real account ok on API ${Build.VERSION.SDK_INT}: test ${report.elapsedMs} ms, send $sendMillis ms, list ${summaries.size} messages in $listMillis ms, connected=${session.connectedProtocols.map { it.id }}")
            logTrace(session)

            val cleanup = arguments.getString("mailCleanup") == "true"
            if (peerAddress != null && peerSecret != null) {
                val peerOptions = JSONObject().put("address", peerAddress).put("timeout", JSONObject().put("connect", 20_000).put("read", 60_000))
                arguments.getString("mailPeerProvider")?.takeIf { it.isNotBlank() }?.let { peerOptions.put("provider", it) }
                arguments.getString("mailPeerImapHost")?.takeIf { it.isNotBlank() }?.let { peerOptions.put("imap", JSONObject().put("host", it)) }
                if (arguments.getString("mailDebug") == "true") peerOptions.put("debug", true)
                val peerKind = if (arguments.getString("mailPeerAuth") == "xoauth2") SecretKind.ACCESS_TOKEN else SecretKind.PASSWORD
                val peer = MailAccountOptions.parse(peerOptions.toString(), peerKind, clientIdDefaults())
                MailSession(peer, MailSecret(peerSecret)).use { peerSession ->
                    val delivered = awaitDelivery(peerSession, subject, "peer")
                    verifyReceivedMessage(peerSession, delivered, subject, sent.messageId, attachmentBytes, cleanup)
                }
            } else if (to == account.address) {
                val delivered = awaitDelivery(session, subject, "own")
                verifyReceivedMessage(session, delivered, subject, sent.messageId, attachmentBytes, cleanup)
            } else {
                Log.i(TAG, "recipient is a third party: the P2.3 read operations run on the newest inbox message instead")
                verifyReadOperations(session, summaries.first())
            }
            saveFailure?.let { fail(it) }
        }
    }

    /** Polls the INBOX of [receiver] (`messages.list`, newest 10) until the round-trip message shows up. */
    private fun awaitDelivery(receiver: MailSession, subject: String, label: String): MessageDocument {
        val pollStarted = System.nanoTime()
        val deadline = System.currentTimeMillis() + PEER_WAIT_MS
        var delivered: MessageDocument? = null
        var polls = 0
        while (delivered == null && System.currentTimeMillis() < deadline) {
            polls++
            delivered = receiver.listMessages(MessageArgs.list("""{"limit": 10}""")).firstOrNull { it.subject == subject }
            if (delivered == null) Thread.sleep(PEER_POLL_MS)
        }
        val waited = (System.nanoTime() - pollStarted) / 1_000_000
        assertNotNull("the $label inbox (${receiver.account.provider?.id ?: receiver.account.imap}) did not receive the message within ${PEER_WAIT_MS / 1000} s ($polls polls)", delivered)
        Log.i(TAG, "$label inbox received the message after $waited ms ($polls polls): uid=${delivered!!.imapUid} size=${delivered.size} bytes seen=${delivered.seen} hasAttachments=${delivered.hasAttachments}")
        logTrace(receiver)
        return delivered
    }

    /**
     * Roadmap P2.3 against the message that just arrived: folders, status, search by Message-ID,
     * `messages.get` (peek), the attachment bytes, the raw source, flags and the optional cleanup.
     */
    private fun verifyReceivedMessage(receiver: MailSession, delivered: MessageDocument, subject: String, messageId: String, attachmentBytes: ByteArray, cleanup: Boolean) {
        val uid = requireNotNull(delivered.imapUid)
        logFolders(receiver)

        // Some providers replace the Message-ID of outgoing mail; search for the id the receiver actually holds.
        val storedId = delivered.messageId ?: messageId
        Log.i(TAG, "Message-ID kept by the receiving server: ${storedId == messageId}")
        // Provider search indexes lag behind delivery by seconds; poll the server a few times, then prove the client path.
        val searchStarted = System.nanoTime()
        var found = receiver.searchMessages(MessageArgs.search("""{"query": {"messageId": ${JSONObject.quote(storedId)}}, "limit": 5}"""))
        var attempts = 1
        while (found.messages.isEmpty() && attempts < SEARCH_ATTEMPTS) {
            Thread.sleep(PEER_POLL_MS)
            attempts++
            found = receiver.searchMessages(MessageArgs.search("""{"query": {"messageId": ${JSONObject.quote(storedId)}}, "limit": 5}"""))
        }
        Log.i(TAG, "messages.search by Message-ID: ${found.messages.size} hit(s) via ${found.fallback} after $attempts attempt(s), ${(System.nanoTime() - searchStarted) / 1_000_000} ms")
        if (found.messages.isEmpty()) {
            val clientStarted = System.nanoTime()
            found = receiver.searchMessages(MessageArgs.search("""{"query": {"messageId": ${JSONObject.quote(storedId)}}, "limit": 5, "fallback": "always"}"""))
            Log.i(TAG, "messages.search fallback=always: ${found.messages.size} hit(s) via ${found.fallback} in ${(System.nanoTime() - clientStarted) / 1_000_000} ms (the server index did not have the message yet)")
        }
        assertEquals("the search must find the round-trip message", listOf(uid), found.messages.map { it.imapUid })
        val bySubject = receiver.searchMessages(MessageArgs.search("""{"query": {"subject": "Angus Mail round trip"}, "limit": 5, "fallback": "always"}"""))
        Log.i(TAG, "messages.search by subject (client): ${bySubject.messages.size} hit(s), contains ours=${bySubject.messages.any { it.imapUid == uid }}")
        val unseenOnly = receiver.listMessages(MessageArgs.list("""{"limit": 5, "unseenOnly": true}"""))
        Log.i(TAG, "messages.list unseenOnly: ${unseenOnly.size} message(s), contains ours=${unseenOnly.any { it.imapUid == uid }}")

        val getStarted = System.nanoTime()
        val message = receiver.getMessage(MessageArgs.get("""{"uid": $uid}"""))
        Log.i(TAG, "messages.get in ${(System.nanoTime() - getStarted) / 1_000_000} ms: textLength=${message.text?.length} html=${message.html != null} headers=${message.headers.size} attachments=${message.attachments.map { "${it.partId}:${it.mimeType}:${it.size}" }} bodyTruncated=${message.bodyTruncated} seen=${message.seen}")
        assertEquals(subject, message.subject)
        assertTrue(message.bodyLoaded)
        assertTrue("the text body must be present", message.text?.contains("Sent from the AutoJs6 Angus Mail plugin") == true)
        assertTrue(message.hasAttachments)
        assertEquals(1, message.attachments.size)
        assertEquals("round-trip-attachment.txt", message.attachments.single().fileName)
        assertEquals(storedId, message.messageId)
        assertFalse("peek must not mark the message read", message.seen)

        val part = message.attachments.single()
        val downloadStarted = System.nanoTime()
        val bytes = ByteArrayOutputStream()
        var reports = 0
        val download = receiver.downloadAttachment(MessageArgs.download("""{"uid": $uid, "partId": "${part.partId}"}"""), bytes) { _, _ -> reports++ }
        Log.i(TAG, "attachments.download part ${part.partId}: ${download.bytes} bytes (${download.mimeType}) in ${(System.nanoTime() - downloadStarted) / 1_000_000} ms, $reports progress report(s)")
        assertArrayEquals("the attachment bytes must round trip unchanged", attachmentBytes, bytes.toByteArray())
        assertEquals(attachmentBytes.size.toLong(), download.bytes)

        val rawStarted = System.nanoTime()
        val raw = ByteArrayOutputStream()
        val rawResult = receiver.downloadRaw(MessageArgs.raw("""{"uid": $uid}"""), raw)
        Log.i(TAG, "messages.raw: ${rawResult.bytes} bytes in ${(System.nanoTime() - rawStarted) / 1_000_000} ms")
        assertEquals(raw.size().toLong(), rawResult.bytes)
        assertEquals(subject, MimeMessage(null as jakarta.mail.Session?, ByteArrayInputStream(raw.toByteArray())).subject)

        val flagged = receiver.setFlags(MessageArgs.flags("""{"uids": [$uid], "flags": ["seen", "flagged"]}"""))
        assertEquals(listOf(uid), flagged.uids)
        val afterFlags = receiver.getMessage(MessageArgs.get("""{"uid": $uid}"""))
        assertTrue(afterFlags.seen && afterFlags.flagged)
        receiver.setFlags(MessageArgs.flags("""{"uids": [$uid], "flags": ["seen", "flagged"], "mode": "remove"}"""))
        assertFalse(receiver.getMessage(MessageArgs.get("""{"uid": $uid}""")).flagged)
        Log.i(TAG, "messages.setFlags add / remove ok")
        logTrace(receiver)

        if (cleanup) {
            val deleted = receiver.delete(MessageArgs.delete("""{"uids": [$uid], "expunge": true}"""))
            assertEquals(listOf(uid), deleted.uids)
            try {
                receiver.getMessage(MessageArgs.get("""{"uid": $uid}"""))
                fail("the message must be gone after delete + expunge")
            } catch (e: MailException) {
                assertEquals(MailErrorCode.MESSAGE_NOT_FOUND, e.code)
            }
            Log.i(TAG, "messages.delete expunge=true ok, uid $uid gone")
            logTrace(receiver)
        }
    }

    /** Read-only P2.3 operations on an existing message when the round-trip message went to a third party. */
    private fun verifyReadOperations(session: MailSession, newest: MessageDocument) {
        logFolders(session)
        val uid = requireNotNull(newest.imapUid)
        val message = session.getMessage(MessageArgs.get("""{"uid": $uid}"""))
        Log.i(TAG, "messages.get uid $uid: textLength=${message.text?.length} attachments=${message.attachments.size} bodyTruncated=${message.bodyTruncated}")
        assertTrue(message.bodyLoaded)
        val raw = ByteArrayOutputStream()
        Log.i(TAG, "messages.raw uid $uid: ${session.downloadRaw(MessageArgs.raw("""{"uid": $uid}"""), raw).bytes} bytes")
        logTrace(session)
    }

    /** Logs the folder tree (paths, special-use roles, counts) and the INBOX status; never message contents. */
    private fun logFolders(session: MailSession) {
        val started = System.nanoTime()
        val folders = session.listFolders(MessageArgs.foldersList("""{"status": true}"""))
        val millis = (System.nanoTime() - started) / 1_000_000
        fun walk(folder: FolderDocument, depth: Int) {
            Log.i(TAG, "folder ${"  ".repeat(depth)}${folder.path} specialUse=${folder.specialUse} selectable=${folder.selectable} messages=${folder.messages} unseen=${folder.unseen}")
            folder.children.forEach { walk(it, depth + 1) }
        }
        folders.forEach { walk(it, 0) }
        val flat = ArrayList<FolderDocument>().also { fun collect(f: FolderDocument) { it += f; f.children.forEach(::collect) }; folders.forEach(::collect) }
        Log.i(TAG, "folders.list: ${flat.size} folder(s) in $millis ms, roles=${flat.mapNotNull { it.specialUse }.sorted()}")
        assertTrue("INBOX must carry the inbox role", flat.any { it.specialUse == "inbox" })
        val status = session.folderStatus("INBOX")
        Log.i(TAG, "folders.status INBOX: messages=${status.messages} unseen=${status.unseen} uidNext=${status.uidNext} uidValidity=${status.uidValidity}")
        logTrace(session)
    }

    /** The same IMAP `ID` payload the Binder sends (the four fields NetEase documents for the command). */
    private fun clientIdDefaults(): MailAccountOptions.Defaults {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
        return MailAccountOptions.Defaults(
            clientId = mapOf(
                "name" to "AutoJs6-Plugin-Angus-Mail",
                "version" to versionName,
                "vendor" to AngusMailPlugin.AUTHOR,
                "support-email" to AngusMailPlugin.SUPPORT_EMAIL,
            ),
        )
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
        const val SEARCH_ATTEMPTS = 4
    }
}
