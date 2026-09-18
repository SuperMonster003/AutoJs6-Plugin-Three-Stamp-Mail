package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import org.autojs.plugin.mail.api.IMailCallCallback
import org.autojs.plugin.mail.api.IMailSession
import org.autojs.plugin.mail.api.IMailSessionCallback
import org.autojs.plugin.mail.api.IMailWatch
import org.autojs.plugin.mail.api.IMailWatchCallback
import org.autojs.plugin.mail.api.MailContract
import org.autojs.plugin.mail.api.MailErrorCodes
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Session that honours the envelope, threading and descriptor rules of the contract without
 * touching the network (roadmap P1.3): every operation is answered from an executor thread with
 * `UNSUPPORTED_OPERATION`, received descriptors are closed, `session.close` closes the session,
 * and watches are refused. P2 replaces it with the routed session.
 */
internal class PlaceholderMailSession(private val callback: IMailSessionCallback?) : IMailSession.Stub() {

    private val closed = AtomicBoolean(false)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "angus-mail-session").apply { isDaemon = true }
    }

    override fun getStatus(): Bundle {
        return MailBundles.status(if (closed.get()) MailContract.STATE_CLOSED else MailContract.STATE_OPEN)
    }

    override fun call(request: Bundle?, descriptors: Array<ParcelFileDescriptor>?, callback: IMailCallCallback?): String {
        descriptors?.forEach { descriptor -> runCatching { descriptor.close() } }
        val requestId = MailBundles.requestId(request) ?: UUID.randomUUID().toString()
        val op = MailBundles.requestOp(request)
        if (closed.get()) {
            respondDetached(callback, closedResponse(requestId))
            return requestId
        }
        val response = when {
            op == null -> MailBundles.failure(requestId, MailBundles.error(MailErrorCodes.INVALID_ARGUMENT, "request carries no op"))
            !MailContract.isKnownOp(op) -> MailBundles.failure(requestId, MailBundles.error(MailErrorCodes.INVALID_ARGUMENT, "unknown op: $op"))
            op == MailContract.OP_SESSION_CLOSE -> MailBundles.success(requestId, true)
            else -> MailBundles.failure(requestId, MailBundles.error(MailErrorCodes.UNSUPPORTED_OPERATION, "$op is not implemented yet"))
        }
        val closeAfterResult = op == MailContract.OP_SESSION_CLOSE
        try {
            // The response leaves on the executor thread (contract B.3); `session.close` closes only
            // after its own result went out, so the executor is still alive for it.
            executor.execute {
                deliver(callback, response)
                if (closeAfterResult) close()
            }
        } catch (_: RejectedExecutionException) {
            respondDetached(callback, closedResponse(requestId))
        }
        return requestId
    }

    private fun closedResponse(requestId: String): Bundle {
        return MailBundles.failure(requestId, MailBundles.error(MailErrorCodes.SESSION_CLOSED, "session is closed", retryable = false))
    }

    private fun deliver(callback: IMailCallCallback?, response: Bundle) {
        callback ?: return
        try {
            callback.onResult(response)
        } catch (_: RemoteException) {
        }
    }

    /** After close the executor is gone; a closed session still answers, just not on the Binder thread. */
    private fun respondDetached(callback: IMailCallCallback?, response: Bundle) {
        callback ?: return
        Thread({ deliver(callback, response) }, "angus-mail-session-closed").apply { isDaemon = true }.start()
    }

    override fun cancel(requestId: String?) = Unit

    override fun watch(options: Bundle?, callback: IMailWatchCallback?): IMailWatch? = null

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            executor.shutdown()
            callback?.let { MailBundles.notifyClosed(it, null, reason = "closed") }
        }
    }
}
