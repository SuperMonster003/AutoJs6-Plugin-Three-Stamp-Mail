package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailTimeouts
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The write timeouts of every socket run on the one shared timer of [WriteTimeouts], not on a
 * `ScheduledThreadPoolExecutor` Angus Mail creates per `WriteTimeoutSocket` (roadmap P6 lifecycle
 * matrix: on Android one such pool thread stayed behind for every TLS connection that a cancel or a
 * watch stop had aborted underneath the TLS layer). Angus names its own pools with the default
 * thread factory (`pool-N-thread-1`), so a connection over GreenMail's implicit SSL must not add
 * such a thread while it is open, and the shared daemon thread must be the only timer.
 */
class WriteTimeoutExecutorTest {

    private lateinit var greenMail: GreenMail

    @Before
    fun start() {
        greenMail = GreenMail(ServerSetupTest.IMAPS)
        greenMail.start()
        greenMail.setUser(ADDRESS, LOGIN, PASSWORD)
    }

    @After
    fun stop() {
        greenMail.stop()
    }

    @Test
    fun writeTimeoutsRunOnTheSharedTimerNotOnAPoolPerSocket() {
        val before = poolThreads()
        val first = SocketRegistry()
        ImapMailbox.connect(account(), MailSecret(PASSWORD), sockets = first).use { mailbox ->
            assertTrue(mailbox.listFolders().isNotEmpty())
            assertEquals("Angus created a write-timeout pool of its own", emptySet<String>(), poolThreads() - before)
            val timers = timerThreads()
            assertEquals("one shared write-timeout thread", 1, timers.size)
            assertTrue("the timer is a daemon thread", timers.single().isDaemon)

            // A second connection, aborted underneath the TLS layer like a cancel or a watch stop,
            // then closed on top of the dead socket: still no pool of its own, still one timer.
            val second = SocketRegistry()
            val aborted = ImapMailbox.connect(account(), MailSecret(PASSWORD), sockets = second)
            assertTrue(aborted.listFolders().isNotEmpty())
            assertEquals(1, second.abort())
            runCatching { aborted.close() }
            assertEquals("Angus created a write-timeout pool of its own", emptySet<String>(), poolThreads() - before)
            assertEquals("one shared write-timeout thread", 1, timerThreads().size)
        }
    }

    private fun account() = MailAccount(
        address = ADDRESS,
        username = LOGIN,
        receive = MailProtocol.IMAP,
        imap = MailEndpoint(HOST, ServerSetupTest.IMAPS.port, TlsMode.SSL),
        pop3 = MailEndpoint(HOST, ServerSetupTest.POP3S.port, TlsMode.SSL),
        smtp = MailEndpoint(HOST, ServerSetupTest.SMTPS.port, TlsMode.SSL),
        trustAll = true,
        timeouts = MailTimeouts.uniform(10_000),
    )

    private fun poolThreads(): Set<String> = Thread.getAllStackTraces().keys.filter { it.isAlive && it.name.startsWith("pool-") }.map { it.name }.toSet()

    private fun timerThreads(): List<Thread> = Thread.getAllStackTraces().keys.filter { it.isAlive && it.name == WriteTimeouts.THREAD_NAME }

    private companion object {
        const val HOST = "127.0.0.1"
        const val ADDRESS = "alice@localhost"
        const val LOGIN = "alice"
        const val PASSWORD = "alice-secret"
    }
}
