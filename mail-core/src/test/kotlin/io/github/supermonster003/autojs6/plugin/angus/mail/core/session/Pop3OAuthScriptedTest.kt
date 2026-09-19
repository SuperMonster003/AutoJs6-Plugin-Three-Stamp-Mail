package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.AuthMethod
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
    }

    private fun session(port: Int): MailSession {
        val account = MailAccount(
            address = "alice@example.org",
            auth = AuthMethod.XOAUTH2,
            receive = MailProtocol.POP3,
            pop3 = MailEndpoint("127.0.0.1", port, TlsMode.NONE),
            timeouts = MailTimeouts.uniform(5_000),
        )
        return MailSession(account, MailSecret(TOKEN.toCharArray())).also { sessions += it }
    }

    /** Serves any number of connections; each runs the Gmail-style script once. */
    private fun pop3Server(commands: MutableList<String>, accept: Boolean): Int {
        val server = ServerSocket(0, 8, InetAddress.getLoopbackAddress()).also { servers += it }
        thread(isDaemon = true, name = "pop3-oauth-script") {
            while (!server.isClosed) {
                val socket = try { server.accept() } catch (_: Exception) { break }
                thread(isDaemon = true) { serve(socket, commands, accept) }
            }
        }
        return server.localPort
    }

    private fun serve(socket: Socket, commands: MutableList<String>, accept: Boolean) {
        socket.use {
            val input = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1))
            val output = socket.getOutputStream()
            fun reply(line: String) {
                output.write("$line\r\n".toByteArray(StandardCharsets.ISO_8859_1))
                output.flush()
            }
            reply("+OK Gpop ready for requests")
            var authenticated = false
            var pendingContinuation = false
            while (true) {
                val line = input.readLine() ?: return
                commands += line.substringBefore(' ').uppercase().let { verb -> if (verb == "AUTH") "AUTH " + line.substringAfter(' ').substringBefore(' ').uppercase() else verb }
                if (pendingContinuation) {
                    pendingContinuation = false
                    reply("-ERR [AUTH] Invalid credentials.")
                    continue
                }
                val verb = line.substringBefore(' ').uppercase()
                when {
                    verb == "CAPA" -> reply("+OK Capability list follows\r\nUSER\r\nRESP-CODES\r\nEXPIRE 0\r\nTOP\r\nUIDL\r\nSASL PLAIN XOAUTH2 OAUTHBEARER\r\n.")
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
