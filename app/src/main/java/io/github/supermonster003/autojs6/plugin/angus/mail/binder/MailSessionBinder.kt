package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.AttachmentSource
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSession
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.Watcher
import org.autojs.plugin.mail.api.IMailCallCallback
import org.autojs.plugin.mail.api.IMailSession
import org.autojs.plugin.mail.api.IMailSessionCallback
import org.autojs.plugin.mail.api.IMailWatch
import org.autojs.plugin.mail.api.IMailWatchCallback
import org.autojs.plugin.mail.api.MailContract
import java.io.OutputStream
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * `IMailSession` over one [MailSession] (roadmap D15 / D16 / B.3 / P2.5): every `call` returns
 * its request id at once and joins the session's queue; one worker thread runs the calls in
 * order, results and the redacted debug trace leave through the call callback from that thread,
 * and idle connections expire between calls on the same thread. The queue holds
 * `MAX_QUEUED_CALLS` behind the call in flight; a call that finds it full is refused with
 * `LIMIT_EXCEEDED` (roadmap D35: the plugin serializes the calls of a session, the host keeps
 * `MAX_CONCURRENT_CALLS` on its side). `cancel` answers a queued call with `CANCELLED` at once and
 * breaks the call in flight by interrupting the worker and aborting the session's sockets; the
 * call still completes, with `CANCELLED` unless it had already succeeded. `close` answers every
 * queued call with `SESSION_CLOSED`, aborts the one in flight the same way, then closes the mail
 * session and reports `closed` through the session callback; the host's death does the same.
 * Descriptors of a call are the plugin's copies: they are wrapped for the transfer ops on the
 * worker thread (attachment sources, or the sink a download streams into) and closed before
 * `onResult`, whatever the outcome, so the host's read end of a download pipe sees EOF before the
 * result. Every session method checks that the caller is the UID that opened the session
 * (`CallerGuard.enforceOwner`). `watch` (roadmap P5) opens a [MailWatchBinder] on the mail
 * session's own watcher, up to `MAX_WATCHES_PER_SESSION` at a time; `close` and the host's death
 * stop every watch of the session before the calls are answered.
 */
internal class MailSessionBinder(
    private val session: MailSession,
    private val callback: IMailSessionCallback?,
    private val guard: CallerGuard = CallerGuard.trusting(),
    private val ownerUid: Int = android.os.Binder.getCallingUid(),
    private val network: WatchNetworkMonitor? = null,
) : IMailSession.Stub() {

    /** One submitted call, from `call` until its single answer. */
    private class Call(
        val requestId: String,
        val op: String?,
        val args: String?,
        val descriptors: List<ParcelFileDescriptor?>,
        val callback: IMailCallCallback?,
    ) {
        /** Set under the lock by `cancel` or `close`; the worker maps a failure of a cancelled call to this code. */
        @Volatile
        var cancelCode: String? = null
        val answered = AtomicBoolean(false)
    }

    private val router = RequestRouter(session)
    private val lock = ReentrantLock()
    private val available = lock.newCondition()
    private val queue = ArrayDeque<Call>()
    private var active: Call? = null
    private val watches = ArrayList<MailWatchBinder>()
    private val closed = AtomicBoolean(false)
    private val finalized = AtomicBoolean(false)

    @Volatile
    private var closeReason = "closed"
    private val hostDeath = IBinder.DeathRecipient { shutdown("host-died") }
    private val worker = Thread(::loop, THREAD_NAME).apply { isDaemon = true }

    init {
        worker.start()
        try {
            callback?.asBinder()?.linkToDeath(hostDeath, 0)
        } catch (_: RemoteException) {
            shutdown("host-died")
        }
    }

    // ------------------------------------------------------------------ AIDL

    override fun getStatus(): Bundle {
        guard.enforceOwner(ownerUid)
        val (queued, current) = lock.withLock { queue.size to active?.requestId }
        val state = if (closed.get()) MailContract.STATE_CLOSED else MailContract.STATE_OPEN
        return MailBundles.status(
            state,
            reason = if (closed.get()) closeReason else null,
            lastError = session.lastError?.let(MailBundles::error),
            connected = session.connectedProtocols.map { it.id },
            queued = queued,
            active = current,
            watches = lock.withLock { watches.count { it.isActive } },
        )
    }

    override fun call(request: Bundle?, descriptors: Array<ParcelFileDescriptor>?, callback: IMailCallCallback?): String {
        guard.enforceOwner(ownerUid)
        val requestId = MailBundles.requestId(request) ?: UUID.randomUUID().toString()
        val copies: List<ParcelFileDescriptor?> = descriptors?.toList() ?: emptyList()
        // The envelope ceiling comes first: an oversized document is not even parsed for its op.
        Limits.checkRequest(request?.getString(MailContract.KEY_REQUEST_JSON))?.let { refusal ->
            refuse(requestId, copies, callback, refusal)
            return requestId
        }
        val call = Call(requestId, MailBundles.requestOp(request), MailBundles.requestArgs(request), copies, callback)
        val refusal = lock.withLock {
            when {
                closed.get() -> sessionClosed()
                queue.size >= Limits.QUEUE_CAPACITY -> Limits.queueFull()
                else -> {
                    queue.addLast(call)
                    available.signal()
                    null
                }
            }
        }
        refusal?.let { refuse(requestId, copies, callback, it) }
        return requestId
    }

    /**
     * Synchronous (contract B.3): a queued call is answered `CANCELLED` before this returns, the
     * call in flight is broken (interrupt plus socket abort) and answers when it unwinds. Unknown
     * and already finished ids are ignored.
     */
    override fun cancel(requestId: String?) {
        guard.enforceOwner(ownerUid)
        requestId ?: return
        val queued = lock.withLock {
            val current = active
            if (current != null && current.requestId == requestId) {
                abort(current, MailErrorCode.CANCELLED)
                null
            } else {
                queue.firstOrNull { it.requestId == requestId }?.also { queue.remove(it) }
            }
        }
        queued?.let { answerDetached(it, MailBundles.failure(it.requestId, MailBundles.error(cancelled()))) }
    }

    /**
     * Synchronous (contract B.3): null refuses the watch (closed session, `MAX_WATCHES_PER_SESSION`,
     * unusable options, a host whose callback is already dead) and the reason is in the session's
     * `lastError`; otherwise the events start after this returns. The watcher connects on its own
     * thread and store, so nothing here touches the network.
     */
    override fun watch(options: Bundle?, callback: IMailWatchCallback?): IMailWatch? {
        guard.enforceOwner(ownerUid)
        callback ?: return null
        val generation = options?.getLong(MailContract.KEY_GENERATION, 0L) ?: 0L
        val parsed = try {
            WatchOptions.parse(options?.getString(MailContract.KEY_WATCH_OPTIONS_JSON), session.receiveProtocol)
        } catch (e: MailException) {
            session.record(e)
            return null
        }
        val binder = MailWatchBinder(generation, callback, guard, ownerUid, network) { finished -> lock.withLock { watches.remove(finished) } }
        val watcher = lock.withLock {
            if (closed.get()) {
                session.record(sessionClosed())
                return null
            }
            val created = try {
                session.watch(parsed, binder)
            } catch (e: MailException) {
                session.record(e)
                return null
            }
            watches += binder
            created
        }
        binder.attach(watcher)
        if (!binder.start()) {
            lock.withLock { watches.remove(binder) }
            session.record(MailException(MailErrorCode.WATCH_CLOSED, "the host's watch callback is dead", retryable = false))
            return null
        }
        return binder
    }

    override fun close() {
        guard.enforceOwner(ownerUid)
        shutdown("closed")
    }

    // ------------------------------------------------------------------ lifecycle

    /**
     * Stops accepting calls, answers the queue with `SESSION_CLOSED`, aborts the call in flight
     * and lets the worker close the mail session once it is free. Idempotent; not guarded, so the
     * death recipient and the `session.close` op can use it.
     */
    private fun shutdown(reason: String) {
        val drained: List<Call>
        val stopping: List<MailWatchBinder>
        lock.withLock {
            if (!closed.compareAndSet(false, true)) return
            closeReason = reason
            drained = queue.toList()
            queue.clear()
            stopping = watches.toList()
            active?.let { abort(it, MailErrorCode.SESSION_CLOSED) }
            available.signalAll()
        }
        stopping.forEach { it.shutdown(if (reason == "host-died") Watcher.REASON_HOST_DIED else reason) }
        drained.forEach { answerDetached(it, MailBundles.failure(it.requestId, MailBundles.error(sessionClosed()))) }
    }

    /** Under the lock: marks the call in flight and breaks whatever it is blocked on. */
    private fun abort(call: Call, code: String) {
        if (call.cancelCode != null) return
        call.cancelCode = code
        worker.interrupt()
        session.abort()
    }

    private fun loop() {
        try {
            while (true) {
                val next = takeNext() ?: break
                run(next)
            }
        } finally {
            closeNow(closeReason)
        }
    }

    /** The next queued call, or null once the session is closed; between calls, idle connections expire. */
    private fun takeNext(): Call? {
        while (true) {
            var idle = false
            lock.withLock {
                if (closed.get()) return null
                queue.pollFirst()?.let { call ->
                    // The abort state of a previous call must not leak into this one (both are set under the lock).
                    Thread.interrupted()
                    session.clearAbort()
                    active = call
                    return call
                }
                idle = try {
                    !available.await(IDLE_CHECK_MS, TimeUnit.MILLISECONDS)
                } catch (_: InterruptedException) {
                    false
                }
            }
            if (idle) runCatching { session.expireIdle() }
        }
    }

    private fun run(call: Call) {
        val io = BinderCallIo(call.requestId, call.descriptors, call.callback)
        var response = try {
            if (call.args == null) {
                throw MailException.invalidArgument("'args' must be an object")
            }
            val result = MailBundles.successJson(call.requestId, router.execute(call.op, call.args, io))
            Limits.checkResponse(result.getString(MailContract.KEY_RESPONSE_JSON).orEmpty())?.let { throw it }
            result
        } catch (e: MailException) {
            session.record(e)
            MailBundles.failure(call.requestId, MailBundles.error(e))
        } catch (e: Throwable) {
            val mapped = session.mapper.map(e, call.op)
            session.record(mapped)
            MailBundles.failure(call.requestId, MailBundles.error(mapped))
        } finally {
            // Contract B.3: the sink is flushed and the plugin's copies are gone before onResult, so the host may release its own.
            io.finish()
            closeAll(call.descriptors)
        }
        val cancelCode = lock.withLock {
            active = null
            Thread.interrupted()
            session.clearAbort()
            call.cancelCode
        }
        if (cancelCode != null && !MailBundles.isSuccess(response)) {
            // A call that failed after its cancel reports the cancel, whatever broke first; one that
            // finished before the abort took effect keeps its result (a sent message is reported sent).
            val cause = if (cancelCode == MailErrorCode.SESSION_CLOSED) sessionClosed() else cancelled()
            session.record(cause)
            response = MailBundles.failure(call.requestId, MailBundles.error(cause))
        }
        if (session.trace.enabled) {
            val lines = session.trace.drain()
            if (lines.isNotEmpty()) MailBundles.progress(call.callback, MailBundles.debugProgress(call.requestId, lines))
        }
        answer(call, response)
        if (call.op == MailContract.OP_SESSION_CLOSE && cancelCode == null) shutdown("closed")
    }

    /** Runs once, on the worker thread, after the loop ended. */
    private fun closeNow(reason: String) {
        if (!finalized.compareAndSet(false, true)) return
        runCatching { callback?.asBinder()?.unlinkToDeath(hostDeath, 0) }
        runCatching { session.close() }
        callback?.let { MailBundles.notifyClosed(it, session.lastError?.let(MailBundles::error), reason) }
    }

    // ------------------------------------------------------------------ answers

    private fun answer(call: Call, response: Bundle) {
        if (!call.answered.compareAndSet(false, true)) return
        MailBundles.deliver(call.callback, response)
    }

    /** Answers a call that never reached the worker; never from the Binder thread (contract B.3). */
    private fun answerDetached(call: Call, response: Bundle) {
        closeAll(call.descriptors)
        if (!call.answered.compareAndSet(false, true)) return
        RESPONDER.execute { MailBundles.deliver(call.callback, response) }
    }

    private fun refuse(requestId: String, descriptors: List<ParcelFileDescriptor?>, callback: IMailCallCallback?, error: MailException) {
        closeAll(descriptors)
        session.record(error)
        RESPONDER.execute { MailBundles.deliver(callback, MailBundles.failure(requestId, MailBundles.error(error))) }
    }

    private fun closeAll(descriptors: List<ParcelFileDescriptor?>) {
        descriptors.forEach { descriptor -> runCatching { descriptor?.close() } }
    }

    private fun sessionClosed(): MailException = MailException(MailErrorCode.SESSION_CLOSED, "session is closed", retryable = false)

    private fun cancelled(): MailException = MailException(MailErrorCode.CANCELLED, "the call was cancelled", retryable = false)

    /**
     * The descriptors and the progress channel of one call. The router has already applied the
     * contract rules (limit, transfer ops only) when a handler asks for [sources] or [sink].
     */
    private class BinderCallIo(
        private val requestId: String,
        private val descriptors: List<ParcelFileDescriptor?>,
        private val callback: IMailCallCallback?,
    ) : RequestRouter.CallIo {

        private var sink: OutputStream? = null

        override val descriptorCount: Int get() = descriptors.size

        override fun sources(): List<AttachmentSource> = if (descriptors.isEmpty()) emptyList() else DescriptorSource.wrap(descriptors)

        override fun sink(): OutputStream {
            if (descriptors.size != 1) {
                throw MailException.invalidArgument("a download needs the write end of a pipe or file as its single descriptor (${descriptors.size} supplied)")
            }
            val descriptor = descriptors[0]
            if (descriptor == null || descriptor.fileDescriptor?.valid() != true) throw MailException.invalidArgument("descriptor 0 is not open")
            return ParcelFileDescriptor.AutoCloseOutputStream(descriptor).also { sink = it }
        }

        override fun progress(transferred: Long, total: Long?) {
            MailBundles.progress(callback, MailBundles.transferProgress(requestId, transferred, total))
        }

        /** Flushes and closes the sink (and with it the descriptor) so the host's read end sees EOF before `onResult`. */
        fun finish() {
            val stream = sink ?: return
            sink = null
            runCatching { stream.flush() }
            runCatching { stream.close() }
        }
    }

    companion object {
        const val THREAD_NAME = "angus-mail-session"

        /** How long the worker waits for a call before it looks for idle connections to drop (roadmap D15). */
        const val IDLE_CHECK_MS = 60_000L

        /** Delivers the answers of calls that never reached a worker (refusals, cancels, close), off the Binder thread. */
        private val RESPONDER = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "$THREAD_NAME-responder").apply { isDaemon = true } }
    }
}
