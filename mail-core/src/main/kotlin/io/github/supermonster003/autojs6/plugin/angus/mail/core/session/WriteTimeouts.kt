package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * The one timer behind every socket's write timeout (`mail.<protocol>.executor.writetimeout`).
 *
 * Angus Mail wraps each connection in a `WriteTimeoutSocket` that schedules a timeout task per
 * write. Without this property it creates a `ScheduledThreadPoolExecutor` of its own per socket
 * and shuts it down only when the socket is closed through that wrapper. A TLS socket on Android
 * (Conscrypt) does not close the wrapper underneath it when the plain socket below is already
 * closed, which is exactly what a cancel or a watch stop does first (`SocketRegistry.abort`), so
 * every aborted TLS connection left one `pool-N-thread-1` thread behind for the life of the
 * process (roadmap P6 lifecycle matrix). With one shared daemon timer the sockets own nothing:
 * the thread appears on the first write, serves every connection, and ends 30 s after the last
 * timeout task is gone.
 */
object WriteTimeouts {

    const val THREAD_NAME = "angus-mail-write-timeout"

    val EXECUTOR: ScheduledExecutorService = ScheduledThreadPoolExecutor(1) { runnable ->
        Thread(runnable, THREAD_NAME).apply { isDaemon = true }
    }.apply {
        // Every write schedules a task and cancels it when the write returns; cancelled tasks must
        // leave the queue at once, not at their due time.
        removeOnCancelPolicy = true
        setKeepAliveTime(30, TimeUnit.SECONDS)
        allowCoreThreadTimeOut(true)
    }
}
