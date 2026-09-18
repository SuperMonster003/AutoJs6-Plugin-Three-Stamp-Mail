package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.io.OutputStream

/** The streaming copy behind `attachments.download` and `messages.raw`. */
class TransferTest {

    @Test
    fun copyReportsProgressAtTheComputedStepAndOnceAtTheEnd() {
        val total = 1_000_000L
        val data = ByteArray(total.toInt()) { it.toByte() }
        val reports = ArrayList<Pair<Long, Long?>>()
        val out = ByteArrayOutputStream()
        val copied = Transfer.copy(ByteArrayInputStream(data), out, total, { transferred, t -> reports += transferred to t })
        assertEquals(total, copied)
        assertArrayEquals(data, out.toByteArray())
        assertEquals(Transfer.BUFFER_BYTES.toLong(), Transfer.progressStep(total))
        assertEquals(total to total, reports.last())
        assertTrue("progress arrives during the copy, not only at the end: $reports", reports.size > 2)
        assertTrue(reports.zipWithNext().all { (a, b) -> a.first <= b.first })
    }

    @Test
    fun progressStepIsOnePercentTwentiethOfTheTotalWithinBounds() {
        assertEquals(Transfer.PROGRESS_MAX_STEP, Transfer.progressStep(null))
        assertEquals(Transfer.PROGRESS_MAX_STEP, Transfer.progressStep(0))
        assertEquals(Transfer.BUFFER_BYTES.toLong(), Transfer.progressStep(10))
        assertEquals(500_000L, Transfer.progressStep(10_000_000L))
        assertEquals(Transfer.PROGRESS_MAX_STEP, Transfer.progressStep(100L * 1024 * 1024))
    }

    @Test
    fun copyStopsAtTheLimit() {
        val data = ByteArray(300 * 1024)
        try {
            Transfer.copy(ByteArrayInputStream(data), ByteArrayOutputStream(), data.size.toLong(), TransferProgress.NONE, limit = 200 * 1024)
            fail("the limit must stop the transfer")
        } catch (e: MailException) {
            assertEquals(MailErrorCode.LIMIT_EXCEEDED, e.code)
        }
    }

    @Test
    fun countingSinkCountsReportsAndCaps() {
        val reports = ArrayList<Long>()
        val out = ByteArrayOutputStream()
        val counting = Transfer.counting(out, 200_000L, { transferred, _ -> reports += transferred })
        counting.write(1)
        counting.write(ByteArray(70_000), 0, 70_000)
        counting.write(ByteArray(70_000))
        assertEquals(140_001L, counting.finish())
        assertEquals(140_001, out.size())
        assertEquals(140_001L, reports.last())
        assertTrue(reports.size >= 2)
        try {
            Transfer.counting(ByteArrayOutputStream(), null, TransferProgress.NONE, limit = 10).write(ByteArray(11))
            fail("the limit must stop the transfer")
        } catch (e: MailException) {
            assertEquals(MailErrorCode.LIMIT_EXCEEDED, e.code)
        }
    }

    @Test
    fun anInterruptedThreadStopsTheCopyAtTheNextChunk() {
        val out = ByteArrayOutputStream()
        val chunks = ArrayList<Long>()
        val progress = TransferProgress { transferred, _ ->
            chunks += transferred
            // the cancel arrives while the copy is under way
            if (chunks.size == 2) Thread.currentThread().interrupt()
        }
        try {
            Transfer.copy(ByteArrayInputStream(ByteArray(1_000_000)), out, 1_000_000L, progress)
            fail("the interrupt must stop the copy")
        } catch (e: InterruptedIOException) {
            assertTrue(e.message, e.message!!.contains("cancelled"))
        } finally {
            assertTrue("the flag is left for the caller to clear", Thread.interrupted())
        }
        assertTrue("some chunks went out before the interrupt: ${out.size()}", out.size() in 1 until 1_000_000)
        try {
            Thread.currentThread().interrupt()
            Transfer.counting(ByteArrayOutputStream(), null, TransferProgress.NONE).write(ByteArray(4))
            fail("the counting sink checks the flag too")
        } catch (_: InterruptedIOException) {
        } finally {
            assertTrue(Thread.interrupted())
        }
    }

    @Test
    fun aClosedSinkSurfacesAsTheWriteFailure() {
        val closed = object : OutputStream() {
            override fun write(b: Int) = throw IOException("Broken pipe")
        }
        try {
            Transfer.copy(ByteArrayInputStream(ByteArray(10)), closed, 10, TransferProgress.NONE)
            fail("the failure must propagate for the mapper (IO_FAILED)")
        } catch (e: SinkFailedException) {
            assertEquals("Broken pipe", e.cause?.message)
            assertTrue(e.message, e.message!!.contains("destination could not be written"))
        }
        try {
            Transfer.counting(closed, 10, TransferProgress.NONE).write(ByteArray(4))
            fail("the counting sink reports the same failure")
        } catch (e: SinkFailedException) {
            assertEquals("Broken pipe", e.cause?.message)
        }
    }
}
