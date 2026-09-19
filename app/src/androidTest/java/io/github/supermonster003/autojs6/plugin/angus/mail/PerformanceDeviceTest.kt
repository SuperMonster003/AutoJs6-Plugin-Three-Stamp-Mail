package io.github.supermonster003.autojs6.plugin.angus.mail

import android.os.Build
import android.os.Debug
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.imapUid
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.FileAttachmentSource
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MailAddressSpec
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.OutgoingAttachment
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.OutgoingMessage
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageArgs
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSession
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.Random
import java.util.zip.CRC32
import kotlin.concurrent.thread

/**
 * Device rows of the P6 performance baseline against the seeded `PerfMailServer` of `mail-core`
 * (a GreenMail with 10000 messages and a 50 MiB attachment on the development machine, reached
 * through `adb reverse` or the emulator's `10.0.2.2`). Skipped without the instrumentation
 * arguments `perfHost`, `perfImapPort`, `perfSmtpPort` (`.python/run_performance_baseline.py
 * --devices <serial>` supplies them). The rows mirror `PerformanceBaselineProbe`: listing pages,
 * server and client searches, `messages.get`, the attachment download with the process PSS and
 * Java heap sampled while it runs, the raw download, and a send with a random attachment from a
 * file. Every row is logged under [TAG] and the figures are records, not thresholds; the test only
 * fails when a row's outcome is wrong (byte count, CRC, delivery).
 */
@RunWith(AndroidJUnit4::class)
class PerformanceDeviceTest {

    private val arguments get() = InstrumentationRegistry.getArguments()
    private val rows = mutableListOf<String>()
    private var failures = 0

    @Test
    fun baselineAgainstTheSeededServer() {
        val host = arguments.getString("perfHost")?.takeIf { it.isNotBlank() }
        val imapPort = arguments.getString("perfImapPort")?.toIntOrNull()
        val smtpPort = arguments.getString("perfSmtpPort")?.toIntOrNull()
        assumeTrue("no performance server arguments; skipping", host != null && imapPort != null && smtpPort != null)
        val sendMiB = arguments.getString("perfSendMiB")?.toIntOrNull() ?: 10
        val warmRuns = arguments.getString("perfWarmRuns")?.toIntOrNull() ?: 3
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val work = File(context.cacheDir, "perf").apply { mkdirs() }
        val runtime = Runtime.getRuntime()
        Log.i(TAG, "# API ${Build.VERSION.SDK_INT} (${Build.MANUFACTURER} ${Build.MODEL}), max heap ${runtime.maxMemory() / MIB} MiB, pss ${Debug.getPss()} KiB, server $host:$imapPort/$smtpPort")
        val options = JSONObject()
            .put("address", ADDRESS).put("user", LOGIN).put("receive", "imap")
            .put("imap", JSONObject().put("host", host).put("port", imapPort).put("tls", "none"))
            .put("smtp", JSONObject().put("host", host).put("port", smtpPort).put("tls", "none"))
            .put("timeout", JSONObject().put("connect", 20_000).put("read", 600_000))
        val account = MailAccountOptions.parse(options.toString(), SecretKind.PASSWORD)
        MailSession(account, MailSecret(PASSWORD.toCharArray())).use { session -> runRows(session, work, sendMiB, warmRuns) }
        work.deleteRecursively()
        Log.i(TAG, "# done: ${rows.size} rows, $failures wrong outcomes")
        assertTrue("wrong outcomes: ${rows.filter { !it.contains("| ok |") }}", failures == 0)
    }

    private fun runRows(session: MailSession, work: File, sendMiB: Int, warmRuns: Int) {
        op("test") {
            val report = session.test()
            listOfNotNull(report.imap, report.smtp).joinToString("; ") { e -> "${e.protocol} ${e.host}:${e.port} ${if (e.ok) "ok" else "FAIL ${e.error?.code}"} ${e.elapsedMs} ms caps=${e.capabilities}" } to report.ok
        }
        var total = 0
        op("status") {
            val status = session.folderStatus(MessageArgs.INBOX)
            total = status.messages
            "INBOX messages=${status.messages} unseen=${status.unseen} uidNext=${status.uidNext}" to (total > 0)
        }
        var newestUid = 0L
        var smallUid = 0L
        op("list-first-page") {
            val page = timed { session.listMessages(MessageArgs.list("""{"limit":50}""")) }
            newestUid = page.value.first().imapUid ?: 0
            smallUid = page.value.first().imapUid ?: 0
            "cold: ${page.value.size} messages in ${page.ms} ms, newest uid=$newestUid" to (page.value.size == 50)
        }
        op("list-first-page-warm") {
            val runs = (1..warmRuns).map { timed { session.listMessages(MessageArgs.list("""{"limit":50}""")) }.ms }
            "$warmRuns runs: ${runs.joinToString(" / ")} ms, median ${runs.sorted()[runs.size / 2]} ms" to true
        }
        op("list-deep-page") {
            val before = maxOf(2L, newestUid - 5000)
            val page = timed { session.listMessages(MessageArgs.list("""{"limit":50,"before":$before}""")) }
            "page below uid $before (about 5000 deep): ${page.value.size} messages in ${page.ms} ms" to (page.value.size == 50)
        }
        op("list-unseen-only") {
            val page = timed { session.listMessages(MessageArgs.list("""{"limit":50,"unseenOnly":true}""")) }
            "server SEARCH UNSEEN over $total then a page of ${page.value.size} in ${page.ms} ms" to (page.value.size == 50)
        }
        op("search-server-subject") {
            val result = timed { session.searchMessages(MessageArgs.search("""{"query":{"subject":"marker"},"limit":50,"fallback":"none"}""")) }
            "subject 'marker': ${result.value.messages.size} hits via ${result.value.fallback} in ${result.ms} ms" to (result.value.messages.size == 50)
        }
        op("search-server-from") {
            // GreenMail parses the FROM argument as a whole address (a fragment answers BAD), unlike the substring match of real providers.
            val result = timed { session.searchMessages(MessageArgs.search("""{"query":{"from":"sender7@example.com"},"limit":50,"fallback":"none"}""")) }
            "from 'sender7@example.com': ${result.value.messages.size} hits via ${result.value.fallback} in ${result.ms} ms" to (result.value.messages.size == 50)
        }
        op("search-server-since") {
            val result = timed { session.searchMessages(MessageArgs.search("""{"query":{"since":${System.currentTimeMillis() - 30L * 86_400_000}},"limit":50,"fallback":"none"}""")) }
            "since 30 days: ${result.value.messages.size} hits via ${result.value.fallback} in ${result.ms} ms" to (result.value.messages.size == 50)
        }
        op("search-client-subject") {
            val result = timed { session.searchMessages(MessageArgs.search("""{"query":{"subject":"$MARKER"},"limit":50,"fallback":"always"}""")) }
            "subject '$MARKER' over the newest ${MailLimits.MAX_CLIENT_FILTER} candidates: ${result.value.messages.size} hits via ${result.value.fallback} in ${result.ms} ms" to (result.value.fallback == "client" && result.value.messages.isNotEmpty())
        }
        op("search-client-body-newest") {
            val needle = "perf body ${total - 1}"
            val result = timed { session.searchMessages(MessageArgs.search("""{"query":{"body":"$needle"},"limit":1,"fallback":"always"}""")) }
            "body '$needle' (second newest message), limit 1: ${result.value.messages.size} hits via ${result.value.fallback} in ${result.ms} ms (the scan stops at the first match)" to (result.value.messages.size == 1)
        }
        op("search-client-body-50-deep") {
            val needle = "perf body ${total - 50}"
            val result = timed { session.searchMessages(MessageArgs.search("""{"query":{"body":"$needle"},"limit":1,"fallback":"always"}""")) }
            "body '$needle' (50 messages deep), limit 1: ${result.value.messages.size} hits via ${result.value.fallback} in ${result.ms} ms (about 50 candidates, two fetches each)" to (result.value.messages.size == 1)
        }
        op("get-small") {
            val doc = timed { session.getMessage(MessageArgs.get("""{"uid":$smallUid}""")) }
            "uid $smallUid in ${doc.ms} ms: textLength=${doc.value.text?.length} headers=${doc.value.headers.size}" to (doc.value.text?.startsWith("perf body") == true)
        }
        var attachmentUid = 0L
        var partId = ""
        var expectedBytes = 0L
        var expectedCrc = 0L
        op("get-attachment-envelope") {
            val summary = session.listMessages(MessageArgs.list("""{"folder":"${ATTACHMENT_FOLDER}","limit":5}""")).first { it.subject.startsWith(ATTACHMENT_SUBJECT_PREFIX) }
            attachmentUid = summary.imapUid ?: 0
            val doc = timed { session.getMessage(MessageArgs.get("""{"folder":"${ATTACHMENT_FOLDER}","uid":$attachmentUid}""")) }
            val attachment = doc.value.attachments.single()
            partId = attachment.partId
            val match = Regex("""Perf attachment (\d+) bytes crc32=([0-9a-f]{8})""").find(doc.value.subject) ?: error("unexpected subject ${doc.value.subject}")
            expectedBytes = match.groupValues[1].toLong()
            expectedCrc = match.groupValues[2].toLong(16)
            "uid $attachmentUid in ${doc.ms} ms: part ${attachment.partId} ${attachment.fileName} declared size=${attachment.size}, expected $expectedBytes bytes" to (attachment.fileName == ATTACHMENT_NAME)
        }
        op("download-attachment") {
            val target = File(work, "downloaded.bin")
            val crc = CRC32()
            var reports = 0
            val sampler = MemorySampler()
            val result = timed {
                sampler.use {
                    CrcOutputStream(FileOutputStream(target), crc).use { sink ->
                        session.downloadAttachment(MessageArgs.download("""{"folder":"${ATTACHMENT_FOLDER}","uid":$attachmentUid,"partId":"$partId"}"""), sink) { _, _ -> reports++ }
                    }
                }
            }
            val ok = target.length() == expectedBytes && crc.value == expectedCrc
            target.delete()
            "${result.value.bytes} bytes in ${result.ms} ms = ${throughput(result.value.bytes, result.ms)} MiB/s, $reports progress reports, crc ok=$ok; ${sampler.summary()}" to ok
        }
        op("download-raw") {
            val target = File(work, "raw.eml")
            val sampler = MemorySampler()
            val result = timed { sampler.use { FileOutputStream(target).use { sink -> session.downloadRaw(MessageArgs.raw("""{"folder":"${ATTACHMENT_FOLDER}","uid":$attachmentUid}"""), sink) } } }
            val length = target.length()
            target.delete()
            "${result.value.bytes} bytes in ${result.ms} ms = ${throughput(result.value.bytes, result.ms)} MiB/s; ${sampler.summary()}" to (length == result.value.bytes)
        }
        op("send-attachment") {
            val source = File(work, "outgoing.bin")
            val crc = CRC32()
            val random = Random(7L)
            val chunk = ByteArray(MIB.toInt())
            FileOutputStream(source).use { out ->
                repeat(sendMiB) {
                    random.nextBytes(chunk)
                    crc.update(chunk)
                    out.write(chunk)
                }
            }
            val subject = "Perf send $sendMiB MiB crc32=${"%08x".format(crc.value)}"
            val sampler = MemorySampler()
            val sent = timed {
                sampler.use {
                    session.send(
                        OutgoingMessage(
                            to = listOf(MailAddressSpec(ADDRESS, "Perf Inbox")),
                            subject = subject,
                            text = "A $sendMiB MiB random attachment sent by the P6 performance baseline.\n",
                            attachments = listOf(OutgoingAttachment(source.name, "application/octet-stream", source = FileAttachmentSource(source))),
                            from = MailAddressSpec(ADDRESS, "Perf Sender"),
                        ),
                        saveToSent = false,
                    )
                }
            }
            val bytes = source.length()
            source.delete()
            val delivered = session.listMessages(MessageArgs.list("""{"limit":3}""")).firstOrNull { it.subject == subject }
            val uid = delivered?.imapUid
            val part = uid?.let { session.getMessage(MessageArgs.get("""{"uid":$it}""")).attachments.singleOrNull() }
            // The declared size is the base64 body (78 bytes per 57 raw); the delivered bytes are verified by downloading them back.
            val back = CRC32()
            val returned = File(work, "returned.bin")
            val received = if (uid == null || part == null) null else timed {
                CrcOutputStream(FileOutputStream(returned), back).use { sink ->
                    session.downloadAttachment(MessageArgs.download("""{"uid":$uid,"partId":"${part.partId}"}"""), sink)
                }
            }
            returned.delete()
            val ok = received != null && received.value.bytes == bytes && back.value == crc.value
            "$bytes bytes in ${sent.ms} ms = ${throughput(bytes, sent.ms)} MiB/s (send elapsed ${sent.value.elapsedMs} ms); delivered uid=$uid size=${delivered?.size} declared part size=${part?.size}, downloaded back ${received?.value?.bytes} bytes in ${received?.ms} ms crc ok=$ok; ${sampler.summary()}" to ok
        }
        op("cleanup") {
            val page = session.listMessages(MessageArgs.list("""{"limit":5}"""))
            val ours = page.filter { it.subject.startsWith("Perf send ") }.mapNotNull { it.imapUid }
            if (ours.isEmpty()) "nothing to delete" to true
            else {
                val result = session.delete(MessageArgs.delete("""{"uids":${ours.joinToString(",", "[", "]")},"expunge":true}"""))
                "deleted ${result.uids.size} sent copies" to true
            }
        }
    }

    private class Timed<T>(val value: T, val ms: Long)

    private inline fun <T> timed(block: () -> T): Timed<T> {
        val started = System.nanoTime()
        val value = block()
        return Timed(value, (System.nanoTime() - started) / 1_000_000)
    }

    private fun throughput(bytes: Long, ms: Long): String = if (ms <= 0) "-" else "%.1f".format(bytes.toDouble() / MIB / (ms / 1000.0))

    private fun op(name: String, block: () -> Pair<String, Boolean>) {
        val started = System.currentTimeMillis()
        val row = try {
            val (note, ok) = block()
            if (!ok) failures++
            "$name | ${if (ok) "ok" else "unexpected"} | ${System.currentTimeMillis() - started} ms | $note"
        } catch (e: MailException) {
            failures++
            "$name | FAIL ${e.code} | ${System.currentTimeMillis() - started} ms | ${e.message?.take(300)} ${e.details?.take(200) ?: ""}"
        } catch (e: Throwable) {
            failures++
            "$name | ERROR | ${System.currentTimeMillis() - started} ms | ${e.javaClass.simpleName}: ${e.message?.take(300)}"
        }
        rows += row
        Log.i(TAG, row)
    }

    private class CrcOutputStream(private val inner: OutputStream, private val crc: CRC32) : OutputStream() {
        override fun write(b: Int) {
            inner.write(b)
            crc.update(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            inner.write(b, off, len)
            crc.update(b, off, len)
        }

        override fun flush() = inner.flush()
        override fun close() = inner.close()
    }

    /** Samples the process PSS (every 100 ms), the Java heap and the native heap (every 10 ms) and keeps their peaks. */
    private class MemorySampler : AutoCloseable {
        private val runtime = Runtime.getRuntime()

        @Volatile
        private var stopped = false
        private val startPss = Debug.getPss()
        private val startHeap: Long
        private val startNative = Debug.getNativeHeapAllocatedSize()

        @Volatile
        private var peakPss = startPss

        @Volatile
        private var peakHeap: Long

        @Volatile
        private var peakNative = startNative
        private val thread: Thread

        init {
            System.gc()
            startHeap = runtime.totalMemory() - runtime.freeMemory()
            peakHeap = startHeap
            thread = thread(isDaemon = true, name = "memory-sampler") {
                var ticks = 0
                while (!stopped) {
                    val heap = runtime.totalMemory() - runtime.freeMemory()
                    if (heap > peakHeap) peakHeap = heap
                    val native = Debug.getNativeHeapAllocatedSize()
                    if (native > peakNative) peakNative = native
                    if (ticks++ % 10 == 0) {
                        val pss = Debug.getPss()
                        if (pss > peakPss) peakPss = pss
                    }
                    Thread.sleep(10)
                }
            }
        }

        override fun close() {
            stopped = true
            thread.join(1000)
            val pss = Debug.getPss()
            if (pss > peakPss) peakPss = pss
        }

        fun summary(): String = "pss before ${startPss / 1024} MiB peak ${peakPss / 1024} MiB (+${(peakPss - startPss) / 1024} MiB), java heap before ${startHeap / MIB} MiB peak ${peakHeap / MIB} MiB (+${(peakHeap - startHeap) / MIB} MiB), native heap before ${startNative / MIB} MiB peak ${peakNative / MIB} MiB"
    }

    private companion object {
        const val TAG = "PerfDevice"
        const val MIB = 1024L * 1024
        const val ADDRESS = "perf@localhost"
        const val LOGIN = "perf"
        const val PASSWORD = "perf-secret"
        const val ATTACHMENT_NAME = "perf-random.bin"
        const val ATTACHMENT_FOLDER = "Perf"
        const val MARKER = "标记"
        const val ATTACHMENT_SUBJECT_PREFIX = "Perf attachment "
    }
}
