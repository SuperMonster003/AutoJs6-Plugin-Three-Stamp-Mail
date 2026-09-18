package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSession
import org.autojs.plugin.mail.api.IMailCallCallback
import org.autojs.plugin.mail.api.IMailSession
import org.autojs.plugin.mail.api.IMailSessionCallback
import org.autojs.plugin.mail.api.IMailWatch
import org.autojs.plugin.mail.api.IMailWatchCallback
import org.autojs.plugin.mail.api.MailContract
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * `IMailSession` over one [MailSession] (roadmap D15 / D16 / B.3): every `call` returns its
 * request id at once and runs on the session's single executor thread, results and the redacted
 * debug trace leave through the call callback from that thread, idle connections expire on a
 * timer of the same thread, and the session closes when the host asks, after `session.close`,
 * or when the host process dies. Watches and cancellation arrive with P5 and P2.5.
 */
internal class MailSessionBinder(
    private val session: MailSession,
    private val callback: IMailSessionCallback?,
) : IMailSession.Stub() {

    private val closed = AtomicBoolean(false)
    private val finalized = AtomicBoolean(false)
    private val router = RequestRouter(session)
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, THREAD_NAME).apply { isDaemon = true }
    }
    private val hostDeath = IBinder.DeathRecipient { close() }

    init {
        executor.scheduleWithFixedDelay({ runCatching { session.expireIdle() } }, IDLE_CHECK_MS, IDLE_CHECK_MS, TimeUnit.MILLISECONDS)
        try {
            callback?.asBinder()?.linkToDeath(hostDeath, 0)
        } catch (_: RemoteException) {
            close()
        }
    }

    override fun getStatus(): Bundle {
        val state = if (closed.get()) MailContract.STATE_CLOSED else MailContract.STATE_OPEN
        return MailBundles.status(state, lastError = session.lastError?.let(MailBundles::error), connected = session.connectedProtocols.map { it.id })
    }

    override fun call(request: Bundle?, descriptors: Array<ParcelFileDescriptor>?, callback: IMailCallCallback?): String {
        // Descriptors are consumed by the transfer ops of P2.3; until then every copy is released here.
        descriptors?.forEach { descriptor -> runCatching { descriptor.close() } }
        val requestId = MailBundles.requestId(request) ?: UUID.randomUUID().toString()
        val op = MailBundles.requestOp(request)
        val args = MailBundles.requestArgs(request)
        if (closed.get()) {
            respondDetached(callback, closedResponse(requestId))
            return requestId
        }
        try {
            executor.execute { run(requestId, op, args, callback) }
        } catch (_: RejectedExecutionException) {
            respondDetached(callback, closedResponse(requestId))
        }
        return requestId
    }

    private fun run(requestId: String, op: String?, args: String?, callback: IMailCallCallback?) {
        val response = if (closed.get()) {
            closedResponse(requestId)
        } else if (args == null) {
            MailBundles.failure(requestId, MailBundles.error(MailErrorCode.INVALID_ARGUMENT, "'args' must be an object"))
        } else {
            try {
                MailBundles.successJson(requestId, router.execute(op, args))
            } catch (e: MailException) {
                MailBundles.failure(requestId, MailBundles.error(e))
            } catch (e: Throwable) {
                MailBundles.failure(requestId, MailBundles.error(session.mapper.map(e, op)))
            }
        }
        if (session.trace.enabled) {
            val lines = session.trace.drain()
            if (lines.isNotEmpty()) MailBundles.progress(callback, MailBundles.debugProgress(requestId, lines))
        }
        MailBundles.deliver(callback, response)
        if (op == MailContract.OP_SESSION_CLOSE) closeNow("closed")
    }

    private fun closedResponse(requestId: String): Bundle =
        MailBundles.failure(requestId, MailBundles.error(MailErrorCode.SESSION_CLOSED, "session is closed", retryable = false))

    /** After close the executor is gone; a closed session still answers, just not on the Binder thread. */
    private fun respondDetached(callback: IMailCallCallback?, response: Bundle) {
        callback ?: return
        Thread({ MailBundles.deliver(callback, response) }, "$THREAD_NAME-closed").apply { isDaemon = true }.start()
    }

    override fun cancel(requestId: String?) = Unit

    override fun watch(options: Bundle?, callback: IMailWatchCallback?): IMailWatch? = null

    /** Closes after the in-flight operation, on the session thread, so the mail core is never touched concurrently. */
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        try {
            executor.execute { closeNow("closed") }
        } catch (_: RejectedExecutionException) {
            closeNow("closed")
        }
        executor.shutdown()
    }

    private fun closeNow(reason: String) {
        closed.set(true)
        if (!finalized.compareAndSet(false, true)) return
        runCatching { callback?.asBinder()?.unlinkToDeath(hostDeath, 0) }
        runCatching { session.close() }
        callback?.let { MailBundles.notifyClosed(it, session.lastError?.let(MailBundles::error), reason) }
        executor.shutdown()
    }

    companion object {
        const val THREAD_NAME = "angus-mail-session"

        /** How often the session thread looks for idle connections to drop (roadmap D15). */
        const val IDLE_CHECK_MS = 60_000L
    }
}
