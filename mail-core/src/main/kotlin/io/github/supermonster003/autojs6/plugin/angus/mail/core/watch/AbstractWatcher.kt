package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.ExceptionMapper
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.Redactor
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.ProtocolTrace
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.SocketRegistry
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The reconnect loop shared by [IdleWatcher] and [PollWatcher] (roadmap D17): connect, run until
 * the connection is lost, report the loss as an `error` event, wait for the [Backoff] delay and
 * connect again; a fatal failure (wrong password, missing folder, refused options) closes the
 * watch instead. [stop] and [reconnect] break whatever the thread is blocked on by closing the
 * watch's sockets; the thread then unwinds and, for a stop, sends the single `closed` event.
 */
abstract class AbstractWatcher(
    protected val account: MailAccount,
    protected val secret: MailSecret,
    protected val options: WatchOptions,
    protected val listener: WatchListener,
    protected val config: WatchConfig,
    threadName: String,
) : Watcher {

    final override val folder: String = options.folder

    protected val redactor = Redactor(account.username, secret)
    protected val mapper = ExceptionMapper(redactor)

    /** The redacted trace of this watch's own connection (roadmap D28); tests read it. */
    val trace = ProtocolTrace(account.debug, redactor, config.clock)

    /** The sockets of this watch's connection, so [stop] and [reconnect] can break a blocked IDLE or read. */
    protected val sockets = SocketRegistry()
    protected val backoff = Backoff(config.backoffMinMs, config.backoffMaxMs, config.random)

    /** Guards the start / stop / hand-off transitions. */
    protected val lifecycle = Any()
    private val thread = Thread(::loop, threadName).apply { isDaemon = true }
    private val closedSent = AtomicBoolean(false)

    @Volatile
    private var started = false

    @Volatile
    private var finished = false

    /** The stop reason once [stop] ran (or a fatal error ended the loop). */
    @Volatile
    protected var stopReason: String? = null
        private set

    @Volatile
    private var reconnectReason: String? = null

    /** True between [reconnect] and the next connection attempt. */
    protected val isReconnecting: Boolean get() = reconnectReason != null

    /** The last failure the loop reported (`error` event). */
    @Volatile
    var lastError: MailException? = null
        protected set

    /** Times the loop connected; the tests use it to prove reconnects. */
    @Volatile
    var connectCount: Int = 0
        private set

    override val isActive: Boolean get() = started && !finished

    override val isClosed: Boolean get() = finished

    override fun status(): WatchStatus = WatchStatus(isActive, mode, if (finished) stopReason ?: Watcher.REASON_ERROR else null, lastError)

    override fun start() {
        synchronized(lifecycle) {
            if (started) return
            started = true
            val reason = stopReason
            if (reason != null) {
                finish(reason)
                return
            }
            thread.start()
        }
    }

    override fun stop(reason: String) {
        synchronized(lifecycle) {
            if (stopReason != null) return
            stopReason = reason
            if (!started || finished) {
                finish(reason)
                return
            }
        }
        thread.interrupt()
        sockets.abort()
    }

    override fun reconnect(reason: String) {
        if (stopReason != null || finished) return
        reconnectReason = reason
        trace.record("watch", "reconnect: $reason")
        thread.interrupt()
        sockets.abort()
    }

    // ------------------------------------------------------------------ subclass contract

    /**
     * Connects and aligns the cursor with the folder; returns a `resync` reason when the messages
     * that arrived since the previous connection cannot be determined (roadmap D17).
     */
    protected abstract fun connect(): String?

    /** Runs on the connection: returns once [stopReason] is set, throws when the connection is lost. */
    protected abstract fun run()

    /** Releases the connection; runs after every loss, on stop, and on hand-off. Must not block. */
    protected abstract fun disconnect()

    /**
     * Asked when [run] threw [cause] and the watch is neither stopping nor reconnecting: true when
     * the subclass handed the watch over to another watcher (which now owns the `closed` event).
     */
    protected open fun handOff(cause: Throwable): Boolean = false

    protected fun emit(event: WatchEvent) {
        try {
            listener.onEvent(event)
        } catch (_: Throwable) {
        }
    }

    protected fun deliver(messages: List<MessageDocument>) {
        messages.forEach { emit(WatchEvent.Message(folder, it)) }
    }

    /** Sleeps [millis] unless [stop] or [reconnect] interrupts. */
    protected fun pause(millis: Long) {
        if (stopReason != null || reconnectReason != null) return
        try {
            Thread.sleep(millis)
        } catch (_: InterruptedException) {
        }
    }

    // ------------------------------------------------------------------ loop

    private fun loop() {
        var handedOff = false
        try {
            while (stopReason == null) {
                try {
                    Thread.interrupted()
                    reconnectReason = null
                    sockets.resume()
                    val resync = connect()
                    connectCount++
                    backoff.reset()
                    config.onConnected?.let { hook -> runCatching { hook(mode) } }
                    resync?.let { emit(WatchEvent.Resync(folder, it)) }
                    run()
                } catch (e: Throwable) {
                    sockets.closeAll()
                    runCatching { disconnect() }
                    if (stopReason != null) break
                    val kicked = reconnectReason
                    if (kicked != null) {
                        backoff.reset()
                        continue
                    }
                    if (handOff(e)) {
                        handedOff = true
                        break
                    }
                    if (stopReason != null) break
                    val mapped = mapper.map(e, "watch $folder")
                    lastError = mapped
                    emit(WatchEvent.Error(folder, mapped))
                    if (mapped.code in FATAL_CODES) {
                        synchronized(lifecycle) { if (stopReason == null) stopReason = Watcher.REASON_ERROR }
                        break
                    }
                    pause(backoff.next())
                }
            }
        } finally {
            sockets.closeAll()
            runCatching { disconnect() }
            if (!handedOff) synchronized(lifecycle) { finish(stopReason ?: Watcher.REASON_ERROR) }
        }
    }

    private fun finish(reason: String) {
        finished = true
        if (closedSent.compareAndSet(false, true)) emit(WatchEvent.Closed(folder, reason))
    }

    companion object {
        /** Failures a reconnect cannot fix: the watch closes after reporting them. */
        val FATAL_CODES: Set<String> = setOf(
            MailErrorCode.AUTH_FAILED,
            MailErrorCode.AUTH_MECHANISM_UNSUPPORTED,
            MailErrorCode.FOLDER_NOT_FOUND,
            MailErrorCode.INVALID_ARGUMENT,
            MailErrorCode.UNSUPPORTED_OPERATION,
            MailErrorCode.LIMIT_EXCEEDED,
            MailErrorCode.PROVIDER_UNKNOWN,
            MailErrorCode.ACCOUNT_NOT_FOUND,
            MailErrorCode.NO_DEFAULT_ACCOUNT,
            MailErrorCode.SESSION_CLOSED,
            MailErrorCode.WATCH_CLOSED,
            MailErrorCode.CANCELLED,
        )
    }
}
