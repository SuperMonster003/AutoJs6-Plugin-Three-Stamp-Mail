package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackoffTest {

    @Test
    fun ceilingsDoubleFromTheMinimumAndStopAtTheMaximum() {
        val backoff = Backoff(1_000, 300_000) { 0 }
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 32_000L, 64_000L, 128_000L, 256_000L, 300_000L, 300_000L), (0..10).map(backoff::ceilingFor))
        assertEquals(300_000L, backoff.ceilingFor(30))
        assertEquals(300_000L, backoff.ceilingFor(62))
    }

    @Test
    fun delaysStayInTheUpperHalfOfTheCeilingAndResetRestarts() {
        val draws = ArrayList<Long>()
        val backoff = Backoff(1_000, 300_000) { bound -> (bound - 1).also { draws += bound } }
        assertEquals(1_000L, backoff.next())
        assertEquals(2_000L, backoff.next())
        assertEquals(4_000L, backoff.next())
        assertEquals(3, backoff.attempts)
        // the draw bounds cover exactly the upper half, inclusive
        assertEquals(listOf(501L, 1_001L, 2_001L), draws)
        backoff.reset()
        assertEquals(0, backoff.attempts)
        val lowest = Backoff(1_000, 300_000) { 0 }
        assertEquals(500L, lowest.next())
        assertEquals(1_000L, lowest.next())
    }

    @Test
    fun theRealJitterNeverLeavesTheRange() {
        val backoff = Backoff(1_000, 5_000, WatchConfig().random)
        repeat(50) { attempt ->
            val ceiling = backoff.ceilingFor(attempt)
            val delay = backoff.next()
            assertTrue("attempt $attempt: $delay within ${ceiling / 2}..$ceiling", delay in (ceiling / 2)..ceiling)
        }
    }
}
