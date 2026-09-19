package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import java.io.Closeable

/** How a watch learns about new mail (contract `WATCH_MODE_*`, roadmap D7 / D17). */
enum class WatchMode(val id: String) {
    IDLE("idle"),
    POLL("poll");

    companion object {
        fun fromId(id: String): WatchMode? = entries.firstOrNull { it.id == id }
    }
}

/**
 * One event of a watch (roadmap D17), produced on the watcher's thread in the order the app must
 * deliver it. [Closed] is always the last event of a watch and arrives exactly once.
 */
sealed class WatchEvent {
    abstract val folder: String

    /** A message that arrived after the watch started, as an envelope or, with `fetchBody`, as a full document. */
    data class Message(override val folder: String, val message: MessageDocument) : WatchEvent()

    /** The watch now runs in [mode] (IDLE was unavailable or kept failing, roadmap P5). */
    data class Mode(override val folder: String, val mode: WatchMode) : WatchEvent()

    /** The watcher cannot tell which messages are new; the script should fetch on its own. */
    data class Resync(override val folder: String, val reason: String) : WatchEvent()

    /** A failure; the watch keeps reconnecting after a recoverable one, a [Closed] follows a fatal one. */
    data class Error(override val folder: String, val error: MailException) : WatchEvent()

    /** The last event of a watch. */
    data class Closed(override val folder: String, val reason: String) : WatchEvent()
}

/** Receives the events of one watch on the watcher's thread; must return quickly and never throw. */
fun interface WatchListener {
    fun onEvent(event: WatchEvent)
}

/** The facts `IMailWatch.getStatus` reports: [active] until the `closed` event, then [reason]. */
data class WatchStatus(val active: Boolean, val mode: WatchMode, val reason: String?, val lastError: MailException?)

/**
 * A new-mail watch on one folder (roadmap P5): its own thread and its own store connection
 * (D15), so a blocked IDLE never delays the session's calls. [start] begins the thread, [stop]
 * ends it (the listener receives `closed` once, whatever happened before), [reconnect] drops the
 * connection so that the loop reconnects at once with the backoff reset (the app calls it when
 * the network changes). Thread-safe.
 */
interface Watcher : Closeable {
    val folder: String
    val mode: WatchMode

    /** True from [start] until the `closed` event. */
    val isActive: Boolean

    /** True once the `closed` event went out (or [stop] ran before [start]). */
    val isClosed: Boolean

    fun start()
    fun stop(reason: String = REASON_STOPPED)
    fun reconnect(reason: String)
    fun status(): WatchStatus
    override fun close() = stop()

    companion object {
        const val REASON_STOPPED = "stopped"
        const val REASON_ERROR = "error"
        const val REASON_SESSION_CLOSED = "session-closed"
        const val REASON_HOST_DIED = "host-died"

        /** `resync` reasons (roadmap D17). */
        const val RESYNC_UIDVALIDITY = "uidvalidity-changed"
        const val RESYNC_QUEUE_OVERFLOW = "queue-overflow"
    }
}
