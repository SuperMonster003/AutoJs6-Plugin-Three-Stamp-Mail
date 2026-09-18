package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MailJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SessionTestResult
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.net.Socket

/**
 * Roadmap P2.1 session behaviour against GreenMail: `session.test` over plain and implicit TLS
 * endpoints, the error codes for a wrong password, a closed port, and a silent server, idle
 * expiry with transparent reconnects, and the redacted debug trace.
 */
class MailSessionGreenMailTest {

    private lateinit var greenMail: GreenMail
    private val sessions = ArrayList<MailSession>()

    @Before
    fun startServer() {
        greenMail = GreenMail(ServerSetupTest.ALL)
        greenMail.start()
        greenMail.setUser(BOB, BOB_LOGIN, BOB_PASSWORD)
    }

    @After
    fun stopServer() {
        sessions.forEach { it.close() }
        greenMail.stop()
    }

    @Test
    fun testProbesTheReceiveAndSmtpEndpoints() {
        val session = session(plainAccount())
        val result = session.test()
        assertTrue(json(result), result.ok)
        assertNotNull(result.imap)
        assertNull(result.pop3)
        assertNotNull(result.smtp)
        assertTrue(result.imap!!.ok)
        assertEquals(ServerSetupTest.IMAP.port, result.imap!!.port)
        assertEquals("none", result.imap!!.tls)
        assertTrue("GreenMail advertises IDLE: ${result.imap!!.capabilities}", "IDLE" in result.imap!!.capabilities)
        assertTrue(result.smtp!!.ok)
        assertTrue("AUTH" in result.smtp!!.capabilities)
        assertTrue(result.elapsedMs >= 0)
        assertEquals(BOB, result.account.address)
        assertEquals(BOB_LOGIN, result.account.user)
        assertEquals("password", result.account.auth)
        assertTrue("plain endpoints are insecure", result.account.insecure)
        assertFalse(json(result).contains(BOB_PASSWORD))
        assertEquals(listOf(MailProtocol.IMAP, MailProtocol.SMTP), session.connectedProtocols)
        assertNull(session.lastError)
    }

    @Test
    fun pop3ReceiveProbesPop3InsteadOfImap() {
        val result = session(plainAccount(receive = "pop3")).test()
        assertTrue(json(result), result.ok)
        assertNull(result.imap)
        assertTrue(result.pop3!!.ok)
        assertEquals("pop3", result.account.receive)
    }

    @Test
    fun implicitTlsEndpointsWorkWithTrustAll() {
        val result = session(tlsAccount()).test()
        assertTrue(json(result), result.ok)
        assertEquals("ssl", result.imap!!.tls)
        assertEquals(ServerSetupTest.IMAPS.port, result.imap!!.port)
        assertEquals("ssl", result.smtp!!.tls)
        assertTrue(result.account.insecure)
    }

    @Test
    fun wrongPasswordReportsAuthFailedWithoutTheSecret() {
        val session = session(plainAccount(), secret = "not-the-password")
        val result = session.test()
        assertFalse(result.ok)
        val error = result.imap!!.error!!
        assertEquals(MailErrorCode.AUTH_FAILED, error.code)
        assertFalse(error.retryable)
        assertFalse(json(result).contains("not-the-password"))
        assertEquals(MailErrorCode.AUTH_FAILED, result.smtp!!.error!!.code)
        assertEquals(MailErrorCode.AUTH_FAILED, session.lastError!!.code)
        assertTrue(session.connectedProtocols.isEmpty())
    }

    @Test
    fun closedPortReportsConnectFailed() {
        val closedPort = ServerSocket(0).use { it.localPort }
        val result = session(plainAccount(imapPort = closedPort)).test()
        assertFalse(result.ok)
        val error = result.imap!!.error!!
        assertEquals(MailErrorCode.CONNECT_FAILED, error.code)
        assertTrue(error.retryable)
        assertTrue(error.message, error.message.startsWith("imap 127.0.0.1:$closedPort/none: could not connect"))
        assertTrue("SMTP is probed independently", result.smtp!!.ok)
    }

    @Test
    fun silentServerReportsTimeout() {
        val silent = ServerSocket(0)
        val held = ArrayList<Socket>()
        val acceptor = Thread {
            try {
                while (true) held += silent.accept()
            } catch (_: Exception) {
            }
        }.apply { isDaemon = true; start() }
        try {
            val result = session(plainAccount(imapPort = silent.localPort, readTimeout = 500)).test()
            val error = result.imap!!.error!!
            assertEquals(error.message, MailErrorCode.TIMEOUT, error.code)
            assertTrue(error.retryable)
        } finally {
            silent.close()
            held.forEach { runCatching { it.close() } }
            acceptor.join(2_000)
        }
    }

    @Test
    fun idleConnectionsAreDroppedAndReconnectTransparently() {
        var now = 1_000_000L
        val session = MailSession(plainAccount(), MailSecret(BOB_PASSWORD), clock = { now }, idleTimeoutMillis = 1_000).also { sessions += it }
        assertTrue(session.imap { it.listInbox(5) }.isEmpty())
        assertEquals(1, session.connectCount(MailProtocol.IMAP))
        now += 999
        assertEquals(0, session.expireIdle())
        now += 1
        assertEquals(1, session.expireIdle())
        assertTrue(session.connectedProtocols.isEmpty())
        session.imap { it.listInbox(5) }
        assertEquals(2, session.connectCount(MailProtocol.IMAP))

        // A connection that died underneath the session is replaced before the next block runs.
        session.imap { it.close() }
        session.imap { assertTrue(it.isConnected) }
        assertEquals(3, session.connectCount(MailProtocol.IMAP))

        session.disconnect()
        assertTrue(session.connectedProtocols.isEmpty())
        session.smtp { assertTrue(it.isConnected) }
        assertEquals(1, session.connectCount(MailProtocol.SMTP))
    }

    @Test
    fun debugTraceIsRedactedAndDrained() {
        val session = session(plainAccount(debug = true))
        assertTrue(session.trace.enabled)
        session.test()
        val lines = session.trace.drain()
        assertTrue(lines.toString(), lines.any { it.contains("imap connect 127.0.0.1:${ServerSetupTest.IMAP.port}/none password ok") })
        assertTrue(lines.toString(), lines.any { it.contains("smtp connect 127.0.0.1:${ServerSetupTest.SMTP.port}/none password ok") })
        assertTrue(lines.all { it.startsWith("+") && !it.contains(BOB_PASSWORD) })
        assertTrue(session.trace.drain().isEmpty())

        val quiet = session(plainAccount())
        quiet.test()
        assertFalse(quiet.trace.enabled)
        assertTrue(quiet.trace.drain().isEmpty())
    }

    @Test
    fun sendOnlyAccountsRefuseReceiveOperations() {
        val session = session(account("""{"address":"$BOB","user":"$BOB_LOGIN","smtp":{"host":"$HOST","port":${ServerSetupTest.SMTP.port},"tls":"none"}}"""))
        val result = session.test()
        assertTrue(result.ok)
        assertNull(result.imap)
        assertNull(result.pop3)
        try {
            session.imap { it.listInbox(1) }
            fail("an account without IMAP must refuse IMAP operations")
        } catch (e: MailException) {
            assertEquals(MailErrorCode.INVALID_ARGUMENT, e.code)
            assertTrue(e.message.contains("no imap endpoint"))
        }
    }

    @Test
    fun closedSessionsRefuseEverythingAndWipeTheSecret() {
        val secret = MailSecret(BOB_PASSWORD)
        val session = MailSession(plainAccount(), secret).also { sessions += it }
        session.test()
        session.close()
        assertTrue(session.isClosed)
        assertTrue(session.connectedProtocols.isEmpty())
        assertTrue(secret.reveal().isBlank())
        try {
            session.test()
            fail("a closed session must refuse work")
        } catch (e: MailException) {
            assertEquals(MailErrorCode.SESSION_CLOSED, e.code)
            assertFalse(e.retryable)
        }
        session.close()
    }

    private fun json(result: SessionTestResult): String = MailJson.format.encodeToString(result)

    private fun session(account: MailAccount, secret: String = BOB_PASSWORD): MailSession =
        MailSession(account, MailSecret(secret)).also { sessions += it }

    private fun account(json: String): MailAccount = MailAccountOptions.parse(json, SecretKind.PASSWORD)

    private fun plainAccount(receive: String = "imap", imapPort: Int = ServerSetupTest.IMAP.port, readTimeout: Int = 10_000, debug: Boolean = false): MailAccount = account(
        """{"address":"$BOB","user":"$BOB_LOGIN","receive":"$receive","debug":$debug,
            "imap":{"host":"$HOST","port":$imapPort,"tls":"none"},
            "pop3":{"host":"$HOST","port":${ServerSetupTest.POP3.port},"tls":"none"},
            "smtp":{"host":"$HOST","port":${ServerSetupTest.SMTP.port},"tls":"none"},
            "timeout":{"connect":5000,"read":$readTimeout}}""",
    )

    private fun tlsAccount(): MailAccount = account(
        """{"address":"$BOB","user":"$BOB_LOGIN","tls":{"trustAll":true},
            "imap":{"host":"$HOST","port":${ServerSetupTest.IMAPS.port},"tls":"${TlsMode.SSL.id}"},
            "smtp":{"host":"$HOST","port":${ServerSetupTest.SMTPS.port},"tls":"${TlsMode.SSL.id}"},
            "timeout":{"connect":5000,"read":10000}}""",
    )

    private companion object {
        const val HOST = "127.0.0.1"
        const val BOB = "bob@localhost"
        const val BOB_LOGIN = "bob"
        const val BOB_PASSWORD = "bob-secret"
    }
}
