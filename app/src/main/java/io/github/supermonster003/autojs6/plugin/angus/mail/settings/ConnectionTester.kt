package io.github.supermonster003.autojs6.plugin.angus.mail.settings

import android.os.Handler
import android.os.Looper
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SessionTestResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSession
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Runs `session.test` of the mail core for the settings page (roadmap P4.2): one probe at a time on
 * a background thread, the outcome delivered on the main thread, the session closed either way.
 * The secret handed to [start] is copied into the session and wiped immediately; nothing is
 * written to disk.
 */
internal class ConnectionTester {

    sealed class Outcome {
        class Done(val result: SessionTestResult) : Outcome()
        class Failed(val error: MailException) : Outcome()
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mail-settings-probe").apply { isDaemon = true }
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    private var active: MailSession? = null
    private var generation = 0

    val isRunning: Boolean get() = active != null

    /** Starts a probe; must be called on the main thread while no probe runs. */
    fun start(account: MailAccount, secret: CharArray, onOutcome: (Outcome) -> Unit) {
        check(active == null) { "a connection test is already running" }
        val session = MailSession(account, MailSecret(secret))
        secret.fill(' ')
        active = session
        val ticket = ++generation
        executor.execute {
            val outcome = try {
                Outcome.Done(session.test())
            } catch (e: MailException) {
                Outcome.Failed(e)
            } catch (e: RuntimeException) {
                Outcome.Failed(MailException(MailErrorCode.INTERNAL, "the connection test failed unexpectedly", e.javaClass.simpleName, retryable = false))
            } finally {
                runCatching { session.close() }
            }
            mainHandler.post {
                if (ticket != generation) return@post
                active = null
                onOutcome(outcome)
            }
        }
    }

    /** Breaks the sockets of the running probe; its outcome is dropped. Main thread only. */
    fun cancel() {
        val session = active ?: return
        generation++
        active = null
        session.abort()
    }

    fun shutdown() {
        cancel()
        executor.shutdown()
    }
}
