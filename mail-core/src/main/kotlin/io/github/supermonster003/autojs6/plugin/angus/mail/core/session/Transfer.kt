package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * The host's destination (pipe or file) refused a write, typically because the read end was
 * closed. Kept apart from every other [IOException] so the connection guard does not drop the
 * server connection and the mapper answers `IO_FAILED` instead of `CONNECT_FAILED`.
 */
class SinkFailedException(cause: IOException) : IOException("the download destination could not be written: ${cause.message}", cause)

/** Progress sink of a transfer: bytes written so far and the expected total when known. */
fun interface TransferProgress {
    fun report(transferred: Long, total: Long?)

    companion object {
        val NONE = TransferProgress { _, _ -> }
    }
}

/**
 * Streams a decoded part or a raw message into the host's descriptor (roadmap P2.3): 64 KiB
 * buffer, a progress report every 1 MiB or 5 percent of the known total (whichever is smaller,
 * never below one buffer), and a hard stop with `LIMIT_EXCEEDED` above `MAX_ATTACHMENT_BYTES`.
 * A closed read end surfaces as the `IOException` of the write, which the mapper turns into
 * `IO_FAILED`.
 */
object Transfer {

    const val BUFFER_BYTES = 64 * 1024
    const val PROGRESS_MAX_STEP = 1024L * 1024

    fun copy(input: InputStream, output: OutputStream, total: Long?, progress: TransferProgress, limit: Long = MailLimits.MAX_ATTACHMENT_BYTES): Long {
        val step = progressStep(total)
        val buffer = ByteArray(BUFFER_BYTES)
        var transferred = 0L
        var nextReport = step
        var reported = -1L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            transferred += read
            if (transferred > limit) {
                throw MailException(MailErrorCode.LIMIT_EXCEEDED, "the part exceeds the transfer limit of $limit bytes", retryable = false)
            }
            sink { output.write(buffer, 0, read) }
            if (transferred >= nextReport) {
                progress.report(transferred, total)
                reported = transferred
                nextReport = transferred + step
            }
        }
        sink { output.flush() }
        if (reported != transferred) progress.report(transferred, total ?: transferred)
        return transferred
    }

    /** Runs one write to the destination and rethrows its failure as [SinkFailedException]. */
    inline fun sink(block: () -> Unit) {
        try {
            block()
        } catch (e: SinkFailedException) {
            throw e
        } catch (e: IOException) {
            throw SinkFailedException(e)
        }
    }

    fun progressStep(total: Long?): Long {
        if (total == null || total <= 0) return PROGRESS_MAX_STEP
        return minOf(PROGRESS_MAX_STEP, maxOf(BUFFER_BYTES.toLong(), total / 20))
    }

    /** Wraps [output] so that whatever is written through it is counted, reported and capped like [copy]. */
    fun counting(output: OutputStream, total: Long?, progress: TransferProgress, limit: Long = MailLimits.MAX_ATTACHMENT_BYTES): CountingOutputStream =
        CountingOutputStream(output, total, progress, limit)

    class CountingOutputStream(
        private val output: OutputStream,
        private val total: Long?,
        private val progress: TransferProgress,
        private val limit: Long,
    ) : OutputStream() {
        var transferred: Long = 0
            private set
        private val step = progressStep(total)
        private var nextReport = step
        private var reported = -1L

        override fun write(b: Int) {
            account(1)
            sink { output.write(b) }
            report()
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            if (len <= 0) return
            account(len.toLong())
            sink { output.write(b, off, len) }
            report()
        }

        override fun flush() = sink { output.flush() }

        /** Flushes, reports the final count and leaves [output] open for the owner to close. */
        fun finish(): Long {
            sink { output.flush() }
            if (reported != transferred) progress.report(transferred, total ?: transferred)
            return transferred
        }

        private fun account(bytes: Long) {
            transferred += bytes
            if (transferred > limit) throw MailException(MailErrorCode.LIMIT_EXCEEDED, "the message exceeds the transfer limit of $limit bytes", retryable = false)
        }

        private fun report() {
            if (transferred >= nextReport) {
                progress.report(transferred, total)
                reported = transferred
                nextReport = transferred + step
            }
        }
    }
}
