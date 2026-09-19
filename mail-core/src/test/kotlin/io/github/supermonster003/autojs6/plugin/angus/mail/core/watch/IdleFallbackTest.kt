package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailTimeouts
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchTestSupport.FAST
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchTestSupport.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Roadmap P5: the switch from IDLE to polling. A scripted IMAP server either does not advertise
 * IDLE, or refuses it with a tagged `NO` (which Angus 2.0.5 reads past, so the refusal shows up
 * as a lost connection at the read timeout), or accepts it and drops the connection at once; in
 * every case the `IdleWatcher` ends up polling on a fresh connection, reports `mode: poll` once,
 * keeps its cursor, and stops cleanly. A wrong password closes the watch instead of retrying.
 */
class IdleFallbackTest {

    private val servers = ArrayList<FakeImapServer>()
    private val watchers = ArrayList<Watcher>()

    @After
    fun stop() {
        watchers.forEach { runCatching { it.stop() } }
        servers.forEach { it.close() }
    }

    private fun server(behavior: FakeImapServer.Idle): FakeImapServer = FakeImapServer(behavior).also { it.start(); servers += it }

    private fun account(server: FakeImapServer) = MailAccount(
        address = "alice@localhost",
        username = "alice",
        imap = MailEndpoint("127.0.0.1", server.port, TlsMode.NONE),
        // Angus notices a refused IDLE only at the read timeout (see IdleWatcher); keep the refusal tests short.
        timeouts = MailTimeouts.uniform(1_000),
        debug = true,
    )

    private fun watch(server: FakeImapServer, events: Events, secret: String = "secret", config: WatchConfig = FAST.copy(idleHealthyMs = 60_000)): IdleWatcher =
        IdleWatcher(account(server), MailSecret(secret), WatchOptions(pollIntervalMs = 200), events, config).also { watchers += it }

    @Test
    fun aServerWithoutIdleIsPolledFromTheStart() {
        val server = server(FakeImapServer.Idle.ABSENT)
        val events = Events()
        val watcher = watch(server, events)
        watcher.start()
        assertEquals(WatchEvent.Mode("INBOX", WatchMode.POLL), events.next())
        assertEquals(WatchMode.POLL, watcher.mode)
        assertTrue(watcher.isActive)
        await(what = "polling") { server.examines.get() >= 3 }
        assertEquals("one connection for the probe, one for the poller", 2, server.connections.get())
        assertEquals(0, server.idles.get())
        assertEquals(0, events.count { it is WatchEvent.Error })
        watcher.stop()
        assertEquals(WatchEvent.Closed("INBOX", Watcher.REASON_STOPPED), events.nextOf<WatchEvent.Closed>())
        await(what = "closed") { watcher.isClosed }
        assertFalse(watcher.isActive)
        assertEquals(1, events.count { it is WatchEvent.Closed })
        assertEquals(1, events.count { it is WatchEvent.Mode })
    }

    @Test
    fun threeRefusedIdlesSwitchToPollingAfterTheReadTimeouts() {
        val server = server(FakeImapServer.Idle.REFUSE)
        val events = Events()
        val watcher = watch(server, events)
        val started = System.currentTimeMillis()
        watcher.start()
        val mode = events.nextOf<WatchEvent.Mode>(timeoutSeconds = 20)
        assertEquals(WatchMode.POLL, mode.mode)
        val elapsed = System.currentTimeMillis() - started
        assertTrue("each refusal costs one read timeout: $elapsed ms", elapsed >= 3_000)
        assertEquals(3, server.idles.get())
        assertEquals("each refusal is a loss that reconnected", 3, watcher.connectCount)
        assertEquals(2, events.count { it is WatchEvent.Error })
        assertTrue(watcher.trace.snapshot().any { it.contains("idle INBOX failed FolderClosedException") })
        await(what = "polling") { server.examines.get() >= 5 }
        assertEquals(WatchMode.POLL, watcher.mode)
        watcher.stop()
        assertEquals(Watcher.REASON_STOPPED, events.nextOf<WatchEvent.Closed>().reason)
    }

    @Test
    fun threeDroppedIdlesInARowSwitchToPollingAfterReconnects() {
        val server = server(FakeImapServer.Idle.DROP)
        val events = Events()
        val watcher = watch(server, events)
        watcher.start()
        val mode = events.nextOf<WatchEvent.Mode>(timeoutSeconds = 20)
        assertEquals(WatchMode.POLL, mode.mode)
        assertEquals(3, server.idles.get())
        assertEquals("each drop reconnected before the switch", 3, watcher.connectCount)
        val errors = events.all.filterIsInstance<WatchEvent.Error>()
        assertEquals("the two drops before the switch were reported", 2, errors.size)
        assertTrue(errors.all { it.error.code == MailErrorCode.CONNECT_FAILED || it.error.code == MailErrorCode.IO_FAILED })
        await(what = "polling") { server.examines.get() >= 5 }
        watcher.stop()
        assertEquals(Watcher.REASON_STOPPED, events.nextOf<WatchEvent.Closed>().reason)
    }

    @Test
    fun aWrongPasswordClosesTheWatchWithoutRetrying() {
        val server = server(FakeImapServer.Idle.ACCEPT)
        val events = Events()
        val watcher = watch(server, events, secret = "wrong")
        watcher.start()
        val error = events.nextOf<WatchEvent.Error>()
        assertEquals(MailErrorCode.AUTH_FAILED, error.error.code)
        assertFalse(error.error.message.contains("wrong"))
        assertEquals(WatchEvent.Closed("INBOX", Watcher.REASON_ERROR), events.nextOf<WatchEvent.Closed>())
        await(what = "closed") { watcher.isClosed }
        // Angus itself retries a refused LOGIN once on a fresh connection with the authenticator's password; the watch adds none.
        val connections = server.connections.get()
        assertTrue("Angus's own login retry: $connections", connections <= 2)
        Thread.sleep(400)
        assertEquals("no reconnect after a fatal failure", connections, server.connections.get())
        assertEquals(WatchStatus(false, WatchMode.IDLE, Watcher.REASON_ERROR, error.error), watcher.status())
    }
}

/**
 * Just enough IMAP for the watch loop: greeting, CAPABILITY, LOGIN (`secret` only), EXAMINE /
 * SELECT of an empty INBOX, `UID FETCH`, NOOP, CLOSE, LOGOUT, and IDLE according to [idle].
 */
internal class FakeImapServer(private val idle: Idle) : Closeable {

    enum class Idle { ABSENT, REFUSE, DROP, ACCEPT }

    private val server = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
    private val sockets = CopyOnWriteArrayList<Socket>()
    val port: Int get() = server.localPort
    val connections = AtomicInteger()
    val logins = AtomicInteger()
    val examines = AtomicInteger()
    val idles = AtomicInteger()

    fun start() {
        Thread {
            try {
                while (true) {
                    val socket = server.accept()
                    connections.incrementAndGet()
                    sockets += socket
                    Thread { serve(socket) }.apply { isDaemon = true; start() }
                }
            } catch (_: Exception) {
            }
        }.apply { isDaemon = true; start() }
    }

    private fun serve(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1))
            val writer = OutputStreamWriter(socket.getOutputStream(), StandardCharsets.ISO_8859_1)
            fun send(line: String) {
                writer.write(line + "\r\n")
                writer.flush()
            }
            send("* OK fake IMAP ready")
            while (true) {
                val line = reader.readLine() ?: return
                val parts = line.split(' ', limit = 3)
                val tag = parts[0]
                val command = parts.getOrNull(1)?.uppercase() ?: continue
                val rest = parts.getOrNull(2).orEmpty()
                when (command) {
                    "CAPABILITY" -> {
                        send("* CAPABILITY IMAP4rev1 UIDPLUS" + if (idle == Idle.ABSENT) "" else " IDLE")
                        send("$tag OK done")
                    }
                    "LOGIN" -> {
                        logins.incrementAndGet()
                        if (rest.trimEnd().endsWith("\"secret\"") || rest.trimEnd().endsWith(" secret")) send("$tag OK logged in") else send("$tag NO [AUTHENTICATIONFAILED] wrong password")
                    }
                    "EXAMINE", "SELECT" -> {
                        examines.incrementAndGet()
                        send("* 0 EXISTS")
                        send("* 0 RECENT")
                        send("* FLAGS (\\Seen)")
                        send("* OK [UIDVALIDITY 1] ok")
                        send("* OK [UIDNEXT 1] ok")
                        send("$tag OK [READ-ONLY] done")
                    }
                    "UID" -> send("$tag OK done")
                    "NOOP" -> send("$tag OK noop")
                    "CLOSE" -> send("$tag OK closed")
                    "IDLE" -> {
                        idles.incrementAndGet()
                        when (idle) {
                            Idle.REFUSE -> send("$tag NO IDLE is disabled here")
                            Idle.DROP -> return
                            else -> {
                                send("+ idling")
                                while (true) {
                                    val next = reader.readLine() ?: return
                                    if (next.trim().equals("DONE", ignoreCase = true)) break
                                }
                                send("$tag OK idle done")
                            }
                        }
                    }
                    "LOGOUT" -> {
                        send("* BYE")
                        send("$tag OK bye")
                        return
                    }
                    else -> send("$tag NO unsupported")
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { socket.close() }
        }
    }

    override fun close() {
        runCatching { server.close() }
        sockets.forEach { runCatching { it.close() } }
    }
}
