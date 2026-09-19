package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import android.os.Bundle
import android.os.DeadObjectException
import android.os.IBinder
import android.os.RemoteException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.WatchEventDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.toDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.toJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchEvent
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchEventQueue
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchListener
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.Watcher
import org.autojs.plugin.mail.api.IMailWatch
import org.autojs.plugin.mail.api.IMailWatchCallback
import org.autojs.plugin.mail.api.MailContract
import java.util.concurrent.atomic.AtomicBoolean

/**
 * `IMailWatch` over one [Watcher] (roadmap P5, D17 / B.3): the watcher's events go through a
 * [WatchEventQueue] to a delivery thread, which numbers them (`seq` from 1) and hands them to the
 * host's `oneway` callback with the host's `generation`; the watcher thread therefore never waits
 * for the host, and a host that stops draining sees one `resync` instead of a backlog. `stop`,
 * the session's close and the host's death (`linkToDeath` on the callback) all stop the watcher,
 * whose single `closed` event is the last delivery. An event whose envelope the Binder cannot
 * carry is delivered again without the body, then as a `resync`. Every method checks the owner
 * UID like the session does.
 */
internal class MailWatchBinder(
    private val generation: Long,
    private val callback: IMailWatchCallback,
    private val guard: CallerGuard,
    private val ownerUid: Int,
    private val network: WatchNetworkMonitor?,
    private val onFinished: (MailWatchBinder) -> Unit,
) : IMailWatch.Stub(), WatchListener {

    private lateinit var watcher: Watcher
    private lateinit var queue: WatchEventQueue
    private val deliverer = Thread(::deliver, THREAD_NAME).apply { isDaemon = true }
    private val hostDeath = IBinder.DeathRecipient { hostDied() }
    private val linked = AtomicBoolean(false)
    private val finished = AtomicBoolean(false)

    /** Events handed to the host so far (the `seq` of the last one). */
    @Volatile
    var delivered: Long = 0
        private set

    val folder: String get() = watcher.folder
    val isActive: Boolean get() = watcher.isActive

    /** Binds the watcher created with this binder as its listener; must run before [start]. */
    fun attach(watcher: Watcher) {
        this.watcher = watcher
        queue = WatchEventQueue(watcher.folder)
    }

    /** Links the host's death, starts the delivery thread and the watcher; false when the host is already gone. */
    fun start(): Boolean {
        try {
            callback.asBinder().linkToDeath(hostDeath, 0)
            linked.set(true)
        } catch (_: RemoteException) {
            watcher.stop(Watcher.REASON_HOST_DIED)
            finish()
            return false
        }
        network?.attach(watcher)
        deliverer.start()
        watcher.start()
        return true
    }

    /** From the watcher thread: queue only, never the Binder. */
    override fun onEvent(event: WatchEvent) {
        queue.offer(event)
    }

    // ------------------------------------------------------------------ AIDL

    override fun getStatus(): Bundle {
        guard.enforceOwner(ownerUid)
        return MailBundles.watchStatus(watcher.status())
    }

    override fun stop() {
        guard.enforceOwner(ownerUid)
        watcher.stop(Watcher.REASON_STOPPED)
    }

    /** The session closes or the host died: the watcher's `closed` event still goes out (when the host is alive). */
    fun shutdown(reason: String) {
        watcher.stop(reason)
    }

    // ------------------------------------------------------------------ delivery

    private fun hostDied() {
        watcher.stop(Watcher.REASON_HOST_DIED)
        queue.abandon()
    }

    private fun deliver() {
        try {
            while (true) {
                val event = queue.take() ?: break
                val seq = ++delivered
                if (!send(event, seq)) break
                if (event is WatchEvent.Closed) break
            }
        } finally {
            finish()
        }
    }

    /** Hands one event to the host; false once the host is dead. */
    private fun send(event: WatchEvent, seq: Long): Boolean {
        val documents = sequenceOf(
            { event.toDocument(seq, generation) },
            { (event as? WatchEvent.Message)?.let { MailBundles.withoutBody(it).toDocument(seq, generation) } },
            { WatchEvent.Resync(event.folder, RESYNC_ENVELOPE).toDocument(seq, generation) },
        )
        for (document in documents) {
            val candidate = document() ?: continue
            val json = candidate.toJson()
            if (Limits.utf8Length(json) > MailContract.MAX_ENVELOPE_BYTES) continue
            try {
                callback.onEvent(generation, seq, MailBundles.json(MailContract.KEY_EVENT_JSON, json))
                return true
            } catch (_: DeadObjectException) {
                hostDied()
                return false
            } catch (_: RemoteException) {
                // the transaction itself failed (too large for the async buffer): try the smaller form
            }
        }
        return true
    }

    private fun finish() {
        if (!finished.compareAndSet(false, true)) return
        if (linked.compareAndSet(true, false)) runCatching { callback.asBinder().unlinkToDeath(hostDeath, 0) }
        network?.detach(watcher)
        onFinished(this)
    }

    companion object {
        const val THREAD_NAME = "angus-mail-watch"

        /** `resync` reason when even the body-less form of an event does not fit the envelope ceiling. */
        const val RESYNC_ENVELOPE = "envelope-too-large"

        /** Mirrors [WatchEventDocument.TYPES] for the contract parity test. */
        val EVENT_TYPES: Set<String> get() = WatchEventDocument.TYPES
    }
}
