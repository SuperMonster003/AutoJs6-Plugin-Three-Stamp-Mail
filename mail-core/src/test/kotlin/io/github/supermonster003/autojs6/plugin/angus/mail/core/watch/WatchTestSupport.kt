package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.GreenMailUtil
import com.icegreen.greenmail.util.ServerSetupTest
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailTimeouts
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Collects the events of one watch; the tests wait for them with a timeout. */
internal class Events : WatchListener {
    private val queue = LinkedBlockingQueue<WatchEvent>()
    val all = ArrayList<WatchEvent>()

    override fun onEvent(event: WatchEvent) {
        synchronized(all) { all += event }
        queue.add(event)
    }

    fun next(timeoutSeconds: Long = 10): WatchEvent =
        queue.poll(timeoutSeconds, TimeUnit.SECONDS) ?: fail("no watch event within $timeoutSeconds s; seen so far: ${snapshot()}").let { throw AssertionError() }

    /** The next event of type [T], skipping others (which are recorded in [all]). */
    inline fun <reified T : WatchEvent> nextOf(timeoutSeconds: Long = 10): T {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
        while (true) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0) fail("no ${T::class.simpleName} event within $timeoutSeconds s; seen so far: ${snapshot()}")
            val event = pollNanos(remaining) ?: continue
            if (event is T) return event
        }
    }

    fun pollNanos(nanos: Long): WatchEvent? = queue.poll(nanos, TimeUnit.NANOSECONDS)

    /** True when no event arrives within [millis]. */
    fun quietFor(millis: Long): Boolean = queue.poll(millis, TimeUnit.MILLISECONDS) == null

    fun snapshot(): List<String> = synchronized(all) { all.map { describe(it) } }

    fun messages(): List<WatchEvent.Message> = synchronized(all) { all.filterIsInstance<WatchEvent.Message>() }

    fun count(predicate: (WatchEvent) -> Boolean): Int = synchronized(all) { all.count(predicate) }

    companion object {
        fun describe(event: WatchEvent): String = when (event) {
            is WatchEvent.Message -> "message(uid=${event.message.uid.content}, subject=${event.message.subject})"
            is WatchEvent.Mode -> "mode(${event.mode.id})"
            is WatchEvent.Resync -> "resync(${event.reason})"
            is WatchEvent.Error -> "error(${event.error.code}: ${event.error.message})"
            is WatchEvent.Closed -> "closed(${event.reason})"
        }
    }
}

internal object WatchTestSupport {
    const val HOST = "127.0.0.1"
    const val ALICE = "alice@localhost"
    const val BOB = "bob@localhost"
    const val BOB_LOGIN = "bob"
    const val BOB_PASSWORD = "bob-secret"

    /** Short delays so that reconnects and polls finish within a test. */
    val FAST = WatchConfig(backoffMinMs = 100, backoffMaxMs = 500, idleHealthyMs = 0)

    fun bob(receive: MailProtocol = MailProtocol.IMAP, readTimeoutMs: Long = 10_000): MailAccount = MailAccount(
        address = BOB,
        username = BOB_LOGIN,
        receive = receive,
        imap = MailEndpoint(HOST, ServerSetupTest.IMAP.port, TlsMode.NONE),
        pop3 = MailEndpoint(HOST, ServerSetupTest.POP3.port, TlsMode.NONE),
        smtp = MailEndpoint(HOST, ServerSetupTest.SMTP.port, TlsMode.NONE),
        timeouts = MailTimeouts.uniform(readTimeoutMs),
        debug = true,
    )

    fun newServer(): GreenMail = GreenMail(ServerSetupTest.ALL).apply {
        start()
        setUser(BOB, BOB_LOGIN, BOB_PASSWORD)
    }

    /** Delivers a plain-text message to Bob and waits until GreenMail stored it (the [expectedTotal]-th message). */
    fun deliver(greenMail: GreenMail, subject: String, expectedTotal: Int, body: String = "plain text") {
        GreenMailUtil.sendTextEmail(BOB, ALICE, subject, body, ServerSetupTest.SMTP)
        assertTrue("GreenMail did not store '$subject'", greenMail.waitForIncomingEmail(5_000, expectedTotal))
    }

    /** Waits until [condition] holds, polling every 20 ms. */
    fun await(timeoutMs: Long = 10_000, what: String = "condition", condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (!condition()) {
            if (System.nanoTime() > deadline) fail("$what did not hold within $timeoutMs ms")
            Thread.sleep(20)
        }
    }
}
