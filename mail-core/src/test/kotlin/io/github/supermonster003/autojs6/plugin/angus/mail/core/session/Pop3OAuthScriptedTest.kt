package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.AuthMethod
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailTimeouts
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.ProviderPresets
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageArgs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/**
 * A POP3 server that answers `AUTH XOAUTH2` the way Gmail does (mail roadmap P6 provider matrix):
 * a refused token comes back as a SASL continuation carrying a base64 JSON error, the client
 * has to send an empty line to get the `-ERR`, and any other command in that state is
 * `-ERR bad command`. Angus Mail 2.0.5 ignores the continuation and reports the login as
 * successful; the mail core verifies the login with a `STAT` and maps the refusal to `AUTH_FAILED`.
 *
 * The same server in `outlook` mode answers the way Outlook.com does (real account, 2026-09-21):
 * the one-line `AUTH XOAUTH2 <base64>` gets `-ERR Protocol error. Connection is closed. 10` and
 * the connection is dropped, only the bare `AUTH XOAUTH2` followed by the response after the `+`
 * continuation logs in; the `outlook` / `office365` presets and the Microsoft hosts switch Angus
 * to that form ([ProviderPreset.pop3Xoauth2TwoLine], `MailSessionProperties.pop3Xoauth2TwoLine`).
 */
class Pop3OAuthScriptedTest {

    private val servers = CopyOnWriteArrayList<ServerSocket>()
    private val sessions = CopyOnWriteArrayList<MailSession>()

    @After
    fun stop() {
        sessions.forEach { runCatching { it.close() } }
        servers.forEach { runCatching { it.close() } }
    }

    @Test
    fun aRefusedTokenSentAsASaslContinuationMapsToAuthFailed() {
        val commands = CopyOnWriteArrayList<String>()
        val port = pop3Server(commands, accept = false)
        val session = session(port)
        val report = session.test()
        assertFalse(report.ok)
        assertEquals(MailErrorCode.AUTH_FAILED, report.pop3?.error?.code)
        assertFalse("the token must not appear in the report", report.toString().contains(TOKEN))
        try {
            session.listMessages(MessageArgs.list("{}", MailProtocol.POP3))
            fail("the listing must not succeed")
        } catch (e: MailException) {
            assertEquals(MailErrorCode.AUTH_FAILED, e.code)
            assertFalse(e.message.orEmpty().contains(TOKEN))
        }
        assertTrue("the client must have sent AUTH XOAUTH2: $commands", commands.any { it.startsWith("AUTH XOAUTH2") })
        assertTrue("the login is verified with STAT: $commands", commands.any { it == "STAT" })
    }

    @Test
    fun anAcceptedTokenListsTheMailbox() {
        val commands = CopyOnWriteArrayList<String>()
        val port = pop3Server(commands, accept = true)
        val session = session(port)
        val report = session.test()
        assertTrue("${report.pop3?.error}", report.pop3?.ok == true)
        assertEquals(0, session.listMessages(MessageArgs.list("{}", MailProtocol.POP3)).size)
        assertTrue("Gmail takes the one-line form: $commands", commands.any { it == "AUTH XOAUTH2 <inline>" })
    }

    @Test
    fun theOutlookPresetSendsTheTwoLineForm() {
        val commands = CopyOnWriteArrayList<String>()
        val port = pop3Server(commands, accept = true, outlook = true)
        val session = session(port, provider = "outlook")
        val report = session.test()
        assertTrue("${report.pop3?.error}", report.pop3?.ok == true)
        assertEquals(0, session.listMessages(MessageArgs.list("{}", MailProtocol.POP3)).size)
        assertTrue("the bare command, then the response: $commands", commands.indexOf("AUTH XOAUTH2") in 0 until commands.indexOf("<sasl response>"))
        assertFalse("no one-line attempt: $commands", commands.any { it == "AUTH XOAUTH2 <inline>" })
        assertFalse("the token must not appear in the report", report.toString().contains(TOKEN))
    }

    @Test
    fun withoutThePresetOutlookRefusesTheOneLineForm() {
        val commands = CopyOnWriteArrayList<String>()
        val port = pop3Server(commands, accept = true, outlook = true)
        val session = session(port)
        val report = session.test()
        assertFalse(report.pop3?.ok == true)
        assertEquals(MailErrorCode.AUTH_FAILED, report.pop3?.error?.code)
        assertTrue("$commands", commands.any { it == "AUTH XOAUTH2 <inline>" })
        assertFalse("the token must not appear in the report", report.toString().contains(TOKEN))
    }

    @Test
    fun theTwoLineSwitchFollowsThePresetAndTheMicrosoftHosts() {
        fun account(host: String, provider: String? = null) = MailAccount(
            address = "alice@example.org",
            auth = AuthMethod.XOAUTH2,
            receive = MailProtocol.POP3,
            pop3 = MailEndpoint(host, 995, TlsMode.SSL),
            provider = provider?.let(ProviderPresets::require),
        )
        val key = "mail.pop3s.auth.xoauth2.two.line.authentication.format"
        assertEquals("true", MailSessionProperties.build(account("outlook.office365.com"), MailProtocol.POP3).getProperty(key))
        assertEquals("true", MailSessionProperties.build(account("pop-mail.outlook.com"), MailProtocol.POP3).getProperty(key))
        assertEquals("true", MailSessionProperties.build(account("127.0.0.1", "office365"), MailProtocol.POP3).getProperty(key))
        assertEquals(null, MailSessionProperties.build(account("pop.gmail.com"), MailProtocol.POP3).getProperty(key))
        assertEquals(null, MailSessionProperties.build(account("pop.gmail.com", "gmail"), MailProtocol.POP3).getProperty(key))
        val password = MailAccount(address = "alice@example.org", auth = AuthMethod.PASSWORD, receive = MailProtocol.POP3, pop3 = MailEndpoint("outlook.office365.com", 995, TlsMode.SSL))
        assertEquals(null, MailSessionProperties.build(password, MailProtocol.POP3).getProperty(key))
    }

    private fun session(port: Int, provider: String? = null): MailSession {
        val account = MailAccount(
            address = "alice@example.org",
            auth = AuthMethod.XOAUTH2,
            receive = MailProtocol.POP3,
            pop3 = MailEndpoint("127.0.0.1", port, TlsMode.NONE),
            timeouts = MailTimeouts.uniform(5_000),
            provider = provider?.let(ProviderPresets::require),
        )
        return MailSession(account, MailSecret(TOKEN.toCharArray())).also { sessions += it }
    }

    /** Serves any number of connections; each runs the Gmail-style (or, with [outlook], the Outlook.com-style) script once. */
    private fun pop3Server(commands: MutableList<String>, accept: Boolean, outlook: Boolean = false): Int {
        val server = ServerSocket(0, 8, InetAddress.getLoopbackAddress()).also { servers += it }
        thread(isDaemon = true, name = "pop3-oauth-script") {
            while (!server.isClosed) {
                val socket = try { server.accept() } catch (_: Exception) { break }
                thread(isDaemon = true) { serve(socket, commands, accept, outlook) }
            }
        }
        return server.localPort
    }

    private fun serve(socket: Socket, commands: MutableList<String>, accept: Boolean, outlook: Boolean) {
        socket.use {
            val input = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1))
            val output = socket.getOutputStream()
            fun reply(line: String) {
                output.write("$line\r\n".toByteArray(StandardCharsets.ISO_8859_1))
                output.flush()
            }
            reply(if (outlook) "+OK The Microsoft Exchange POP3 service is ready." else "+OK Gpop ready for requests")
            var authenticated = false
            var pendingContinuation = false
            while (true) {
                val line = input.readLine() ?: return
                val words = line.trim().split(' ')
                commands += words[0].uppercase().let { verb -> if (verb == "AUTH") "AUTH " + words.getOrElse(1) { "" }.uppercase() + (if (words.size > 2) " <inline>" else "") else verb }
                if (pendingContinuation) {
                    pendingContinuation = false
                    reply("-ERR [AUTH] Invalid credentials.")
                    continue
                }
                val verb = words[0].uppercase()
                when {
                    verb == "CAPA" && outlook -> reply("+OK\r\nTOP\r\nUIDL\r\nSASL XOAUTH2\r\nUSER\r\n.")
                    verb == "CAPA" -> reply("+OK Capability list follows\r\nUSER\r\nRESP-CODES\r\nEXPIRE 0\r\nTOP\r\nUIDL\r\nSASL PLAIN XOAUTH2 OAUTHBEARER\r\n.")
                    verb == "AUTH" && outlook -> {
                        // Outlook.com: the one-line form is a protocol error and ends the connection; the bare
                        // command is answered with an empty continuation and the response line logs in.
                        if (words.size > 2) {
                            reply("-ERR Protocol error. Connection is closed. 10")
                            return
                        }
                        reply("+ ")
                        input.readLine() ?: return
                        commands += "<sasl response>"
                        authenticated = true
                        reply("+OK User successfully authenticated.")
                    }
                    verb == "AUTH" -> {
                        if (accept) {
                            authenticated = true
                            reply("+OK Welcome.")
                        } else {
                            pendingContinuation = true
                            reply("+ " + Base64.getEncoder().encodeToString("""{"status":"400","schemes":"Bearer","scope":"https://mail.google.com/"}""".toByteArray()))
                        }
                    }
                    verb == "QUIT" -> { reply("+OK Farewell."); return }
                    !authenticated -> reply("-ERR bad command")
                    verb == "STAT" -> reply("+OK 0 0")
                    verb == "UIDL" || verb == "LIST" -> reply("+OK\r\n.")
                    verb == "NOOP" -> reply("+OK")
                    else -> reply("-ERR unknown command")
                }
            }
        }
    }

    private companion object {
        const val TOKEN = "ya29.matrix-test-token-not-a-real-secret"
    }
}
