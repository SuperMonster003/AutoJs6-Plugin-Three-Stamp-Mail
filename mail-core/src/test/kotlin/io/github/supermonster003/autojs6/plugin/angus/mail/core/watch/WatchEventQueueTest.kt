package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WatchEventQueueTest {

    private fun message(uid: Long) = WatchEvent.Message("INBOX", MessageDocument(uid = JsonPrimitive(uid), folder = "INBOX", subject = "m$uid", size = 1))

    @Test
    fun eventsComeOutInOrderAndTheClosedEventEndsTheQueue() {
        val queue = WatchEventQueue("INBOX", capacity = 8)
        assertTrue(queue.offer(message(1)))
        assertTrue(queue.offer(WatchEvent.Mode("INBOX", WatchMode.POLL)))
        assertTrue(queue.offer(WatchEvent.Closed("INBOX", "stopped")))
        assertFalse("nothing after closed", queue.offer(message(2)))
        assertTrue(queue.isClosed)
        assertEquals(message(1), queue.take())
        assertEquals(WatchEvent.Mode("INBOX", WatchMode.POLL), queue.take())
        assertEquals(WatchEvent.Closed("INBOX", "stopped"), queue.take())
        assertNull(queue.take())
        assertNull(queue.take())
    }

    @Test
    fun anOverflowCollapsesThePendingEventsIntoOneResync() {
        val queue = WatchEventQueue("INBOX", capacity = 4)
        (1L..4L).forEach { assertTrue(queue.offer(message(it))) }
        assertEquals(4, queue.size)
        // the fifth message does not fit: everything pending becomes one resync, the message itself is dropped
        assertTrue(queue.offer(message(5)))
        assertEquals(1, queue.size)
        assertEquals(1L, queue.overflows)
        assertEquals(5L, queue.dropped)
        assertEquals(WatchEvent.Resync("INBOX", Watcher.RESYNC_QUEUE_OVERFLOW), queue.take())
        // after the collapse, messages queue normally again
        assertTrue(queue.offer(message(6)))
        assertEquals(message(6), queue.take())
    }

    @Test
    fun stateEventsSurviveAnOverflow() {
        val queue = WatchEventQueue("INBOX", capacity = 2)
        assertTrue(queue.offer(message(1)))
        assertTrue(queue.offer(message(2)))
        val error = WatchEvent.Error("INBOX", MailException(MailErrorCode.CONNECT_FAILED, "lost"))
        assertTrue(queue.offer(error))
        assertEquals(listOf(WatchEvent.Resync("INBOX", Watcher.RESYNC_QUEUE_OVERFLOW), error), listOf(queue.take(), queue.take()))
        assertEquals(2L, queue.dropped)
        // a closed event always fits
        (1L..2L).forEach { queue.offer(message(it)) }
        assertTrue(queue.offer(WatchEvent.Closed("INBOX", "stopped")))
        assertEquals(3, queue.size)
    }

    @Test
    fun takeBlocksUntilAnEventArrivesAndAbandonReleasesIt() {
        val queue = WatchEventQueue("INBOX", capacity = 4)
        val taken = ArrayList<WatchEvent?>()
        val done = CountDownLatch(1)
        val taker = Thread {
            taken += queue.take()
            taken += queue.take()
            done.countDown()
        }.apply { start() }
        Thread.sleep(100)
        assertTrue(taker.isAlive)
        queue.offer(message(1))
        Thread.sleep(100)
        queue.abandon()
        assertTrue(done.await(5, TimeUnit.SECONDS))
        assertEquals(listOf(message(1), null), taken)
        assertFalse("nothing is accepted after abandon", queue.offer(message(2)))
    }
}
