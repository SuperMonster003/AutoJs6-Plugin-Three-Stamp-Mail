package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import java.util.ArrayDeque
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The events of one watch waiting for delivery to the host (roadmap D17 / B.3: the plugin never
 * blocks on a slow host). The watcher thread [offer]s, one delivery thread [take]s. When
 * [capacity] events are pending, everything pending collapses into a single `resync`
 * (`queue-overflow`) and the message that did not fit is dropped: the script fetches on its own.
 * Events other than messages (`mode`, `error`, `resync`) are kept after the collapse because
 * they are few and carry state. A `closed` event ends the queue: nothing is accepted after it and
 * [take] answers null once it went out.
 */
class WatchEventQueue(
    private val folder: String,
    private val capacity: Int = MailLimits.MAX_WATCH_QUEUE,
) {
    init {
        require(capacity >= 2) { "the queue must hold at least a resync and a closed event: $capacity" }
    }

    private val lock = ReentrantLock()
    private val available = lock.newCondition()
    private val events = ArrayDeque<WatchEvent>()
    private var closing = false
    private var drained = false

    /** Times the queue overflowed. */
    var overflows: Long = 0
        private set

    /** Events dropped by overflows (the collapsed ones plus the messages that did not fit). */
    var dropped: Long = 0
        private set

    val size: Int get() = lock.withLock { events.size }

    val isClosed: Boolean get() = lock.withLock { closing }

    /** Adds [event]; false once a `closed` event is queued. */
    fun offer(event: WatchEvent): Boolean = lock.withLock {
        if (closing) return false
        if (event is WatchEvent.Closed) {
            closing = true
            events.addLast(event)
            available.signalAll()
            return true
        }
        if (events.size >= capacity) {
            overflows++
            dropped += events.size
            events.clear()
            events.addLast(WatchEvent.Resync(folder, Watcher.RESYNC_QUEUE_OVERFLOW))
            if (event is WatchEvent.Message) {
                dropped++
                available.signalAll()
                return true
            }
        }
        events.addLast(event)
        available.signalAll()
        true
    }

    /** The next event, waiting for one; null once the `closed` event has been taken (or after [abandon]). */
    fun take(): WatchEvent? {
        lock.lock()
        try {
            while (true) {
                if (drained) return null
                val next = events.pollFirst()
                if (next != null) {
                    if (next is WatchEvent.Closed) drained = true
                    return next
                }
                available.await()
            }
        } finally {
            lock.unlock()
        }
    }

    /** Drops everything and wakes the taker with null (the host is gone; nobody would read the events). */
    fun abandon() = lock.withLock {
        events.clear()
        closing = true
        drained = true
        available.signalAll()
    }
}
