package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailTimeouts
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.EndpointReport
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SessionTestResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.OutgoingMessage
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageArgs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.net.ssl.SSLContext

/**
 * TLS matrix of the mail core (mail roadmap P6): implicit SSL (GreenMail's 3993 / 3995 / 3465),
 * STARTTLS through [StartTlsProxy] in front of GreenMail's plain ports, plain text, the
 * self-signed certificate with and without `tls.trustAll`, a trusted certificate whose name
 * does not match the host, the wrong mode for a port, and a plain port that offers no upgrade.
 * Every row is a `session.test` report with a definite error code; the STARTTLS row also moves
 * a message through the upgraded channels. `docs/dev/p6-tls-matrix.md` records the results,
 * `TlsDeviceTest` (app androidTest) the API 24 TLS 1.2 confirmation.
 */
class TlsMatrixTest {

    private lateinit var greenMail: GreenMail
    private val sessions = ArrayList<MailSession>()
    private val proxies = ArrayList<StartTlsProxy>()
    private lateinit var defaultContext: SSLContext

    @Before
    fun startServer() {
        defaultContext = SSLContext.getDefault()
        greenMail = GreenMail(ServerSetupTest.ALL)
        greenMail.start()
        greenMail.setUser(ALICE, ALICE_LOGIN, ALICE_PASSWORD)
        greenMail.setUser(BOB, BOB_LOGIN, BOB_PASSWORD)
    }

    @After
    fun stopServer() {
        SSLContext.setDefault(defaultContext)
        sessions.forEach { it.close() }
        proxies.forEach { it.close() }
        greenMail.stop()
    }

    @Test
    fun implicitSslWorksWithTrustAllAndFailsClosedWithoutIt() {
        val trusted = test(account(ALICE, ALICE_LOGIN, ssl(), trustAll = true))
        assertOk(trusted, "SSL + trustAll")

        val untrusted = test(account(ALICE, ALICE_LOGIN, ssl(), trustAll = false))
        assertCode(untrusted, MailErrorCode.TLS_FAILED, "self-signed certificate without trustAll")
    }

    @Test
    fun aTrustedCertificateWithTheWrongNameIsRefused() {
        // The JVM trusts GreenMail's certificate now, but it names no host at all: identity check fails.
        SSLContext.setDefault(StartTlsProxy.trustingContext())
        val result = test(account(ALICE, ALICE_LOGIN, ssl(), trustAll = false))
        assertCode(result, MailErrorCode.TLS_FAILED, "trusted certificate, host name mismatch")
        result.reports().forEach { report -> assertTrue(report.error!!.message, report.error!!.message.contains("TLS handshake failed")) }
    }

    @Test
    fun starttlsUpgradesAllThreeProtocolsAndCarriesMail() {
        val imap = proxy(StartTlsProxy.Protocol.IMAP, ServerSetupTest.IMAP.port)
        val pop3 = proxy(StartTlsProxy.Protocol.POP3, ServerSetupTest.POP3.port)
        val smtp = proxy(StartTlsProxy.Protocol.SMTP, ServerSetupTest.SMTP.port)
        val endpoints = Endpoints(imap.port, pop3.port, smtp.port, TlsMode.STARTTLS)

        val probe = test(account(ALICE, ALICE_LOGIN, endpoints, trustAll = true))
        assertOk(probe, "STARTTLS + trustAll")
        assertEquals(setOf("TLSv1.3"), (imap.negotiated + pop3.negotiated + smtp.negotiated).toSet())

        val sender = MailSession(account(ALICE, ALICE_LOGIN, endpoints, trustAll = true), MailSecret(ALICE_PASSWORD)).also { sessions += it }
        val sent = sender.send(OutgoingMessage.simple(listOf(BOB), SUBJECT, "over STARTTLS"), saveToSent = false)
        assertNotNull(sent.messageId)
        assertTrue(greenMail.waitForIncomingEmail(5_000, 1))
        val receiver = MailSession(account(BOB, BOB_LOGIN, endpoints, trustAll = true), MailSecret(BOB_PASSWORD)).also { sessions += it }
        assertEquals(SUBJECT, receiver.listMessages(MessageArgs.list("{}")).single().subject)
        val popReceiver = MailSession(account(BOB, BOB_LOGIN, endpoints, trustAll = true, receive = MailProtocol.POP3), MailSecret(BOB_PASSWORD)).also { sessions += it }
        assertEquals(SUBJECT, popReceiver.listMessages(MessageArgs.list("{}", MailProtocol.POP3)).single().subject)
        assertTrue("every connection was upgraded", imap.negotiated.size >= 2 && pop3.negotiated.size >= 2 && smtp.negotiated.size >= 2)

        val untrusted = test(account(ALICE, ALICE_LOGIN, endpoints, trustAll = false))
        assertCode(untrusted, MailErrorCode.TLS_FAILED, "STARTTLS with a self-signed certificate and no trustAll")
    }

    @Test
    fun starttlsRequiredIsRefusedWhereTheServerOffersNoUpgrade() {
        val result = test(account(ALICE, ALICE_LOGIN, plain(TlsMode.STARTTLS), trustAll = true))
        assertCode(result, MailErrorCode.TLS_FAILED, "STARTTLS required on a plain port")
        result.reports().forEach { report -> assertTrue(report.error!!.message, report.error!!.message.contains("STARTTLS")) }
        assertEquals("nothing reached the server in the clear", 0, greenMail.receivedMessages.size)
    }

    @Test
    fun plainTextWorksOnlyWhereItIsConfigured() {
        assertOk(test(account(ALICE, ALICE_LOGIN, plain(TlsMode.NONE), trustAll = false)), "tls none on plain ports")
        val sslOnPlain = test(account(ALICE, ALICE_LOGIN, plain(TlsMode.SSL), trustAll = true))
        assertCode(sslOnPlain, MailErrorCode.TLS_FAILED, "SSL against a plain port")
        val plainOnSsl = test(account(ALICE, ALICE_LOGIN, ssl().copy(tls = TlsMode.NONE), trustAll = false, timeouts = MailTimeouts.uniform(2_000)))
        assertCode(plainOnSsl, MailErrorCode.TIMEOUT, "tls none against an SSL port (the server waits for a ClientHello)")
        plainOnSsl.reports().forEach { assertTrue(it.error!!.retryable) }
    }

    private class Endpoints(val imap: Int, val pop3: Int, val smtp: Int, val tls: TlsMode) {
        fun copy(tls: TlsMode) = Endpoints(imap, pop3, smtp, tls)
    }

    private fun ssl() = Endpoints(ServerSetupTest.IMAPS.port, ServerSetupTest.POP3S.port, ServerSetupTest.SMTPS.port, TlsMode.SSL)

    private fun plain(tls: TlsMode) = Endpoints(ServerSetupTest.IMAP.port, ServerSetupTest.POP3.port, ServerSetupTest.SMTP.port, tls)

    private fun proxy(protocol: StartTlsProxy.Protocol, upstream: Int): StartTlsProxy = StartTlsProxy(protocol, upstream).also { proxies += it }

    private fun account(address: String, login: String, endpoints: Endpoints, trustAll: Boolean, receive: MailProtocol = MailProtocol.IMAP, timeouts: MailTimeouts = MailTimeouts.uniform(10_000)): MailAccount =
        MailAccount(
            address = address,
            username = login,
            receive = receive,
            imap = MailEndpoint(HOST, endpoints.imap, endpoints.tls),
            pop3 = MailEndpoint(HOST, endpoints.pop3, endpoints.tls),
            smtp = MailEndpoint(HOST, endpoints.smtp, endpoints.tls),
            trustAll = trustAll,
            timeouts = timeouts,
        )

    /** `session.test` for the receive protocol and SMTP, then again with POP3 as the receive protocol, so all three endpoints report. */
    private fun test(account: MailAccount): SessionTestResult {
        val imapSide = MailSession(account, MailSecret(ALICE_PASSWORD)).also { sessions += it }.test()
        val pop3Side = MailSession(account.copy(receive = MailProtocol.POP3), MailSecret(ALICE_PASSWORD)).also { sessions += it }.test()
        return imapSide.copy(pop3 = pop3Side.pop3, ok = imapSide.ok && pop3Side.ok)
    }

    private fun SessionTestResult.reports(): List<EndpointReport> = listOfNotNull(imap, pop3, smtp)

    private fun assertOk(result: SessionTestResult, label: String) {
        assertEquals(3, result.reports().size)
        result.reports().forEach { report -> assertTrue("$label ${report.protocol}: ${report.error}", report.ok) }
        assertTrue(result.ok)
    }

    private fun assertCode(result: SessionTestResult, code: String, label: String) {
        assertEquals(3, result.reports().size)
        assertFalse(result.ok)
        result.reports().forEach { report ->
            assertFalse("$label ${report.protocol} must fail", report.ok)
            assertEquals("$label ${report.protocol}: ${report.error}", code, report.error!!.code)
        }
    }

    private companion object {
        const val HOST = "127.0.0.1"
        const val ALICE = "alice@localhost"
        const val ALICE_LOGIN = "alice"
        const val ALICE_PASSWORD = "alice-secret"
        const val BOB = "bob@localhost"
        const val BOB_LOGIN = "bob"
        const val BOB_PASSWORD = "bob-secret"
        const val SUBJECT = "tls matrix"
    }
}
