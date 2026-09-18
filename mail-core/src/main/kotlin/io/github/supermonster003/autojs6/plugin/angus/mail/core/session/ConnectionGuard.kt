package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import jakarta.mail.FolderClosedException
import jakarta.mail.StoreClosedException
import org.eclipse.angus.mail.iap.ConnectionException
import java.io.EOFException
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException

/**
 * Owns one lazily opened connection (an IMAP or POP3 store, or an SMTP transport) on behalf of a
 * session (roadmap P2.1 `ConnectionGuard`): connects on first use, drops the connection after
 * [idleTimeoutMillis] without use, reconnects transparently when the server or the network closed
 * it, and retries an operation once when the loss surfaced during the operation and the caller
 * allows a retry. Not thread-safe by design: a session runs on one executor thread (roadmap D15).
 */
class ConnectionGuard<T : Any>(
    val label: String,
    private val idleTimeoutMillis: Long,
    private val clock: () -> Long = System::currentTimeMillis,
    private val isAlive: (T) -> Boolean,
    private val onClose: (T) -> Unit,
    private val connect: () -> T,
) {
    private var connection: T? = null
    private var lastUsedAt: Long = 0L

    /** Times [connect] ran; tests use it to prove that idle expiry and loss recovery reconnect. */
    var connectCount: Int = 0
        private set

    var closed: Boolean = false
        private set

    val isConnected: Boolean get() = connection?.let(isAlive) == true

    /** Milliseconds since the last completed use, or null without a connection. */
    val idleMillis: Long? get() = connection?.let { clock() - lastUsedAt }

    /**
     * Runs [block] against a live connection. A connection loss during [block] drops the
     * connection; when [retryOnLoss] is true and the failure was not a timeout, the block runs
     * once more on a fresh connection, otherwise the failure propagates.
     */
    fun <R> use(retryOnLoss: Boolean = true, block: (T) -> R): R {
        check(!closed) { "$label guard is closed" }
        expireIfIdle()
        var retried = false
        while (true) {
            val current = acquire()
            try {
                val result = block(current)
                lastUsedAt = clock()
                return result
            } catch (e: Exception) {
                val lost = !isAlive(current) || isConnectionLoss(e)
                if (lost) drop()
                if (lost && retryOnLoss && !retried && !isTimeout(e)) {
                    retried = true
                    continue
                }
                throw e
            }
        }
    }

    private fun acquire(): T {
        connection?.let { existing ->
            if (isAlive(existing)) return existing
            drop()
        }
        val fresh = connect()
        connection = fresh
        connectCount++
        lastUsedAt = clock()
        return fresh
    }

    /** Drops the connection when it has been idle for [idleTimeoutMillis]; returns true when it did. */
    fun expireIfIdle(): Boolean {
        if (connection == null) return false
        if (clock() - lastUsedAt < idleTimeoutMillis) return false
        drop()
        return true
    }

    /** Closes the current connection, if any; the next [use] reconnects. */
    fun drop() {
        val current = connection ?: return
        connection = null
        try {
            onClose(current)
        } catch (_: Exception) {
        }
    }

    fun close() {
        closed = true
        drop()
    }

    companion object {
        /** True for failures that mean the connection is unusable rather than the command being wrong. */
        fun isConnectionLoss(e: Throwable): Boolean {
            var current: Throwable? = e
            var depth = 0
            while (current != null && depth++ < 12) {
                when (current) {
                    // the host's pipe or file failed, not the server connection
                    is SinkFailedException -> return false
                    is FolderClosedException, is StoreClosedException, is ConnectionException -> return true
                    is SocketException, is EOFException, is SocketTimeoutException -> return true
                    is IOException -> return true
                }
                current = current.cause
            }
            return false
        }

        fun isTimeout(e: Throwable): Boolean {
            var current: Throwable? = e
            var depth = 0
            while (current != null && depth++ < 12) {
                if (current is SocketTimeoutException) return true
                current = current.cause
            }
            return false
        }
    }
}
