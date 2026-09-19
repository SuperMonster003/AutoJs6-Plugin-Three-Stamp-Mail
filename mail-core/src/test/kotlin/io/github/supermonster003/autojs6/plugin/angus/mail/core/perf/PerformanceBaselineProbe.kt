package io.github.supermonster003.autojs6.plugin.angus.mail.core.perf

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
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.lang.management.ManagementFactory
import java.lang.management.MemoryType
import java.util.Properties
import java.util.Random
import java.util.zip.CRC32
import kotlin.concurrent.thread

/**
 * JVM rows of the P6 performance baseline against the seeded [PerfMailServer] (out of process):
 * skipped unless `build/p6/perf.properties` (`host`, `imapPort`, `smtpPort`, optional `sendMiB`,
 * `warmRuns`) exists, which `.python/run_performance_baseline.py` writes for the run. Every row
 * goes through the same [MailSession] the Binder uses; the figures are records, not thresholds
 * (roadmap P6: "无回归阈值"). Rows: `session.test`, `folders.status`, `messages.list` (first page
 * cold and warm, a page 5000 messages deep, unseen only), server searches (subject, from,
 * since), client searches (`fallback: 'always'`, subject with a Chinese marker and a body),
 * `messages.get`, the attachment download (throughput, peak heap), the raw download, and a send
 * with a random attachment of `sendMiB` MiB from a file. The log is `build/p6/perf-jvm.log`.
 */
class PerformanceBaselineProbe {

    private class Row(val op: String, val outcome: String, val ms: Long, val note: String) {
        override fun toString(): String = "$op | $outcome | $ms ms | $note"
    }

    private lateinit var log: PrintWriter
    private val rows = mutableListOf<Row>()

    @Test
    fun runTheBaseline() {
        val settings = File("../build/p6/perf.properties")
        assumeTrue("no performance server settings", settings.isFile)
        val config = Properties().apply { settings.reader(Charsets.UTF_8).use { load(it) } }
        val host = config.getProperty("host", "127.0.0.1")
        val imapPort = config.getProperty("imapPort", "3143").toInt()
        val smtpPort = config.getProperty("smtpPort", "3025").toInt()
        val sendMiB = config.getProperty("sendMiB", "10").toInt()
        val warmRuns = config.getProperty("warmRuns", "3").toInt()
        val work = File("../build/p6/perf-work").apply { mkdirs() }
        log = File("../build/p6/perf-jvm.log").printWriter()
        try {
            val runtime = Runtime.getRuntime()
            out("# JVM ${System.getProperty("java.vm.name")} ${System.getProperty("java.version")}, max heap ${runtime.maxMemory() / MIB} MiB, ${runtime.availableProcessors()} cpus, server $host:$imapPort/$smtpPort")
            val account = MailAccountOptions.parse(
                """{"address":"${PerfFixtures.ADDRESS}","user":"${PerfFixtures.LOGIN}","receive":"imap","imap":{"host":"$host","port":$imapPort,"tls":"none"},"smtp":{"host":"$host","port":$smtpPort,"tls":"none"},"timeout":{"connect":20000,"read":600000}}""",
                SecretKind.PASSWORD,
            )
            val session = MailSession(account, MailSecret(PerfFixtures.PASSWORD.toCharArray()))
            try {
                runRows(session, work, sendMiB, warmRuns)
            } finally {
                session.close()
            }
        } finally {
            log.close()
            rows.forEach { println("perf: $it") }
        }
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
            "cold: ${page.value.size} messages in ${page.ms} ms, newest uid=$newestUid subject=${page.value.first().subject.take(40)}" to (page.value.size == 50)
        }
        op("list-first-page-warm") {
            val runs = (1..warmRuns).map { timed { session.listMessages(MessageArgs.list("""{"limit":50}""")) }.ms }
            "$warmRuns runs: ${runs.joinToString(" / ")} ms, median ${runs.sorted()[runs.size / 2]} ms" to true
        }
        op("list-deep-page") {
            val before = maxOf(2L, newestUid - 5000)
            val page = timed { session.listMessages(MessageArgs.list("""{"limit":50,"before":$before}""")) }
            "page below uid $before (about 5000 deep): ${page.value.size} messages in ${page.ms} ms, uids ${page.value.first().imapUid}..${page.value.last().imapUid}" to (page.value.size == 50)
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
            val result = timed { session.searchMessages(MessageArgs.search("""{"query":{"subject":"${PerfFixtures.MARKER}"},"limit":50,"fallback":"always"}""")) }
            "subject '${PerfFixtures.MARKER}' over the newest ${MailLimits.MAX_CLIENT_FILTER} candidates: ${result.value.messages.size} hits via ${result.value.fallback} in ${result.ms} ms" to (result.value.fallback == "client" && result.value.messages.isNotEmpty())
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
            "uid $smallUid in ${doc.ms} ms: textLength=${doc.value.text?.length} headers=${doc.value.headers.size} seen=${doc.value.seen}" to (doc.value.text?.startsWith("perf body") == true)
        }
        var attachmentUid = 0L
        var partId = ""
        var expectedBytes = 0L
        var expectedCrc = 0L
        op("get-attachment-envelope") {
            val summary = session.listMessages(MessageArgs.list("""{"folder":"${PerfFixtures.ATTACHMENT_FOLDER}","limit":5}""")).first { it.subject.startsWith(PerfFixtures.ATTACHMENT_SUBJECT_PREFIX) }
            attachmentUid = summary.imapUid ?: 0
            val doc = timed { session.getMessage(MessageArgs.get("""{"folder":"${PerfFixtures.ATTACHMENT_FOLDER}","uid":$attachmentUid}""")) }
            val attachment = doc.value.attachments.single()
            partId = attachment.partId
            val (bytes, crc) = PerfFixtures.parseAttachmentSubject(doc.value.subject) ?: error("unexpected subject ${doc.value.subject}")
            expectedBytes = bytes
            expectedCrc = crc
            "uid $attachmentUid in ${doc.ms} ms: part ${attachment.partId} ${attachment.fileName} ${attachment.mimeType} declared size=${attachment.size}, expected $bytes bytes crc32=${"%08x".format(crc)}" to (attachment.fileName == PerfFixtures.ATTACHMENT_NAME)
        }
        op("download-attachment") {
            val target = File(work, "downloaded.bin")
            val crc = CRC32()
            var reports = 0
            val sampler = HeapSampler()
            val result = timed {
                sampler.use {
                    CrcOutputStream(FileOutputStream(target), crc).use { sink ->
                        session.downloadAttachment(MessageArgs.download("""{"folder":"${PerfFixtures.ATTACHMENT_FOLDER}","uid":$attachmentUid,"partId":"$partId"}"""), sink) { _, _ -> reports++ }
                    }
                }
            }
            val ok = target.length() == expectedBytes && crc.value == expectedCrc
            target.delete()
            "${result.value.bytes} bytes in ${result.ms} ms = ${throughput(result.value.bytes, result.ms)} MiB/s, $reports progress reports, crc ok=$ok; heap used before ${sampler.startUsed / MIB} MiB, peak ${sampler.peakUsed / MIB} MiB (+${(sampler.peakUsed - sampler.startUsed) / MIB} MiB), pool peak ${sampler.poolPeak / MIB} MiB" to ok
        }
        op("download-raw") {
            val target = File(work, "raw.eml")
            val sampler = HeapSampler()
            val result = timed { sampler.use { FileOutputStream(target).use { sink -> session.downloadRaw(MessageArgs.raw("""{"folder":"${PerfFixtures.ATTACHMENT_FOLDER}","uid":$attachmentUid}"""), sink) } } }
            val length = target.length()
            target.delete()
            "${result.value.bytes} bytes (${length} on disk) in ${result.ms} ms = ${throughput(result.value.bytes, result.ms)} MiB/s; heap peak ${sampler.peakUsed / MIB} MiB (+${(sampler.peakUsed - sampler.startUsed) / MIB} MiB)" to (length == result.value.bytes)
        }
        op("send-attachment") {
            val source = File(work, "outgoing.bin")
            val bytes = ByteArray(sendMiB * MIB.toInt())
            Random(7L).nextBytes(bytes)
            val crc = CRC32().apply { update(bytes) }.value
            source.writeBytes(bytes)
            val subject = "Perf send $sendMiB MiB crc32=${"%08x".format(crc)}"
            val sampler = HeapSampler()
            val sent = timed {
                sampler.use {
                    session.send(
                        OutgoingMessage(
                            to = listOf(MailAddressSpec(PerfFixtures.ADDRESS, "Perf Inbox")),
                            subject = subject,
                            text = "A $sendMiB MiB random attachment sent by the P6 performance baseline.\n",
                            attachments = listOf(OutgoingAttachment(source.name, "application/octet-stream", source = FileAttachmentSource(source))),
                            from = MailAddressSpec(PerfFixtures.ADDRESS, "Perf Sender"),
                        ),
                        saveToSent = false,
                    )
                }
            }
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
            val ok = received != null && received.value.bytes == bytes.size.toLong() && back.value == crc
            "${bytes.size} bytes in ${sent.ms} ms = ${throughput(bytes.size.toLong(), sent.ms)} MiB/s (send elapsed ${sent.value.elapsedMs} ms, accepted=${sent.value.accepted.size}); delivered uid=$uid size=${delivered?.size} declared part size=${part?.size}, downloaded back ${received?.value?.bytes} bytes in ${received?.ms} ms crc ok=$ok; heap peak ${sampler.peakUsed / MIB} MiB (+${(sampler.peakUsed - sampler.startUsed) / MIB} MiB)" to ok
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
            Row(name, if (ok) "ok" else "unexpected", System.currentTimeMillis() - started, note)
        } catch (e: MailException) {
            Row(name, "FAIL ${e.code}", System.currentTimeMillis() - started, "${e.message?.take(300)} ${e.details?.take(200) ?: ""}")
        } catch (e: Throwable) {
            Row(name, "ERROR", System.currentTimeMillis() - started, "${e.javaClass.simpleName}: ${e.message?.take(300)}")
        }
        rows += row
        out(row.toString())
    }

    private fun out(line: String) {
        log.println(line)
        log.flush()
    }

    private class CrcOutputStream(private val inner: FileOutputStream, private val crc: CRC32) : java.io.OutputStream() {
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

    /** Samples the used heap every 10 ms and records its peak; also resets and reads the heap pools' own peak counters. */
    private class HeapSampler : AutoCloseable {
        private val runtime = Runtime.getRuntime()
        private val pools = ManagementFactory.getMemoryPoolMXBeans().filter { it.type == MemoryType.HEAP && it.isValid }

        @Volatile
        private var stopped = false
        val startUsed: Long
        @Volatile
        var peakUsed: Long
        var poolPeak: Long = 0
            private set
        private val thread: Thread

        init {
            System.gc()
            Thread.sleep(50)
            pools.forEach { it.resetPeakUsage() }
            startUsed = runtime.totalMemory() - runtime.freeMemory()
            peakUsed = startUsed
            thread = thread(isDaemon = true, name = "heap-sampler") {
                while (!stopped) {
                    val used = runtime.totalMemory() - runtime.freeMemory()
                    if (used > peakUsed) peakUsed = used
                    Thread.sleep(10)
                }
            }
        }

        override fun close() {
            stopped = true
            thread.join(1000)
            poolPeak = pools.sumOf { it.peakUsage?.used ?: 0L }
        }
    }

    private companion object {
        const val MIB = 1024L * 1024
    }
}
