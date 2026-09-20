package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Outlook.com advertises `AUTH=XOAUTH2 LOGINDISABLED` and answers `NO Basic authentication is
 * disabled.` to `LOGIN` and `AUTHENTICATE PLAIN` (three personal accounts, 2026-09-20). With a
 * password Angus has no mechanism to try and stops before sending any credential; the session test
 * must call that `AUTH_MECHANISM_UNSUPPORTED` (the credential is of the wrong kind), not a server
 * error.
 */
class LoginDisabledTest {
    private lateinit var server: FakeImapServer

    @Before
    fun start() {
        server = FakeImapServer(advertiseId = false, loginDisabled = true).also { it.start() }
    }

    @After
    fun stop() {
        server.close()
    }

    @Test
    fun aPasswordAgainstAnXoauth2OnlyServerIsAMechanismProblem() {
        val report = MailSession(account(), MailSecret(PASSWORD)).use { it.test() }
        val imap = report.imap!!
        assertFalse(imap.ok)
        assertEquals(MailErrorCode.AUTH_MECHANISM_UNSUPPORTED, imap.error!!.code)
        val commands = server.sessions.first().commands
        assertTrue(commands.toString(), commands.none { it == "LOGIN" || it == "AUTHENTICATE" })
    }

    private fun account(): MailAccount = MailAccountOptions.parse(
        """{"address":"alice@example.org","user":"alice",
            "imap":{"host":"127.0.0.1","port":${server.port},"tls":"none"},
            "smtp":{"host":"127.0.0.1","port":${server.port},"tls":"none"},
            "clientId":{},
            "timeout":{"connect":5000,"read":10000}}""",
        SecretKind.PASSWORD,
    )

    private companion object {
        const val PASSWORD = "alice-secret"
    }
}
