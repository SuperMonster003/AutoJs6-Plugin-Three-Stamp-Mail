package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailTimeouts
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageArgs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * `MailSession.abort` from another thread against servers that accept and then stay silent, the
 * shape of a stalled connection the read timeout would otherwise take a minute to notice
 * (roadmap P2.5 `cancel`).
 */
class MailSessionAbortTest {

    private lateinit var imapServer: ServerSocket
    private lateinit var smtpServer: ServerSocket
    private val accepted = ArrayList<Socket>()
    private lateinit var acceptors: List<Thread>

    @Before
    fun listen() {
        imapServer = ServerSocket(0, 4, InetAddress.getLoopbackAddress())
        smtpServer = ServerSocket(0, 4, InetAddress.getLoopbackAddress())
        acceptors = listOf(imapServer, smtpServer).map { server ->
            Thread {
                try {
                    while (true) synchronized(accepted) { accepted += server.accept() }
                } catch (_: Exception) {
                }
            }.apply { isDaemon = true; start() }
        }
    }

    @After
    fun stop() {
        imapServer.close()
        smtpServer.close()
        synchronized(accepted) { accepted.forEach { runCatching { it.close() } } }
    }

    private fun session(): MailSession = MailSession(
        MailAccount(
            address = "alice@localhost",
            imap = MailEndpoint("127.0.0.1", imapServer.localPort, TlsMode.NONE),
            smtp = MailEndpoint("127.0.0.1", smtpServer.localPort, TlsMode.NONE),
            timeouts = MailTimeouts(connectMillis = 5_000, readMillis = 60_000, writeMillis = 60_000),
            debug = true,
        ),
        MailSecret("not-a-real-secret"),
    )

    private fun awaitSocket(session: MailSession) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (session.sockets.liveCount == 0) {
            if (System.nanoTime() > deadline) fail("the session never opened a socket")
            Thread.sleep(10)
        }
        Thread.sleep(100)
    }

    @Test
    fun abortBreaksAnOperationBlockedOnTheGreetingAndReportsCancelled() {
        val session = session()
        val outcome = AtomicReference<Throwable?>()
        val done = CountDownLatch(1)
        val worker = Thread {
            try {
                session.listFolders(MessageArgs.foldersList("{}"))
            } catch (e: Throwable) {
                outcome.set(e)
            } finally {
                done.countDown()
            }
        }
        worker.start()
        awaitSocket(session)
        val began = System.nanoTime()
        assertEquals(1, session.abort())
        assertTrue(session.isAborting)
        assertTrue("the blocked operation returned", done.await(10, TimeUnit.SECONDS))
        val elapsedMs = (System.nanoTime() - began) / 1_000_000
        val error = outcome.get()
        assertTrue("expected a MailException, got $error", error is MailException)
        assertEquals(MailErrorCode.CANCELLED, (error as MailException).code)
        assertFalse(error.retryable)
        assertTrue("the read timeout is a minute, the abort took ${elapsedMs}ms", elapsedMs < 5_000)
        assertEquals(MailErrorCode.CANCELLED, session.lastError?.code)
        assertEquals("a connect cut before the greeting never counts, and nothing reconnected", 0, session.connectCount(MailProtocol.IMAP))
        assertEquals(0, session.sockets.liveCount)
        assertTrue(session.trace.snapshot().any { it.contains("abort sockets=1") })

        // until the owner clears the abort, every guarded operation is refused without a connection
        val refused = try {
            session.listFolders(MessageArgs.foldersList("{}"))
            throw AssertionError("still aborting")
        } catch (e: MailException) {
            e
        }
        assertEquals(MailErrorCode.CANCELLED, refused.code)
        assertEquals(0, session.connectCount(MailProtocol.IMAP))
        assertEquals(0, session.sockets.liveCount)
        session.clearAbort()
        assertFalse(session.isAborting)
        assertFalse(session.sockets.isAborting)
        session.close()
    }

    @Test
    fun abortDuringSessionTestSkipsTheRemainingProbes() {
        val session = session()
        val result = AtomicReference<Any?>()
        val done = CountDownLatch(1)
        Thread {
            result.set(runCatching { session.test() }.getOrElse { it })
            done.countDown()
        }.start()
        awaitSocket(session)
        session.abort()
        assertTrue(done.await(10, TimeUnit.SECONDS))
        val report = result.get()
        assertTrue("session.test reports, it does not throw: $report", report is io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SessionTestResult)
        report as io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SessionTestResult
        assertFalse(report.ok)
        assertNotNull(report.imap)
        assertEquals(MailErrorCode.CANCELLED, report.imap?.error?.code)
        assertEquals("the SMTP probe never connects once the call is cancelled", MailErrorCode.CANCELLED, report.smtp?.error?.code)
        assertEquals(0, session.connectCount(MailProtocol.SMTP))
        session.clearAbort()
        session.close()
    }

    @Test
    fun anAbortBeforeTheSocketExistsStillCancelsTheConnect() {
        val session = session()
        session.abort()
        val error = try {
            session.listFolders(MessageArgs.foldersList("{}"))
            throw AssertionError("aborting sessions refuse to connect")
        } catch (e: MailException) {
            e
        }
        assertEquals(MailErrorCode.CANCELLED, error.code)
        assertEquals(0, session.sockets.liveCount)
        session.clearAbort()
        session.close()
    }
}
