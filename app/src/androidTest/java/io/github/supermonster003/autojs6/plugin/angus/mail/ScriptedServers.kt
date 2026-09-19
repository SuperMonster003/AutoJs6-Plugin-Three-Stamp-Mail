package io.github.supermonster003.autojs6.plugin.angus.mail

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.LinkedBlockingQueue

/**
 * Loopback servers that speak just enough protocol for `session.test`: the device tests of the
 * store and the settings screens save an account against them and observe the secret arriving
 * on the wire (`tls: none`), which is what proves the decrypt path. Bound to an explicit
 * `127.0.0.1` because the API 24 AVD's loopback address is not reachable otherwise.
 */
internal abstract class ScriptedServer : AutoCloseable {
    private val server = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
    private val sockets = ArrayList<Socket>()
    val port: Int get() = server.localPort

    init {
        Thread {
            try {
                while (true) {
                    val socket = server.accept()
                    synchronized(sockets) { sockets += socket }
                    Thread { serveSafely(socket) }.apply { isDaemon = true; start() }
                }
            } catch (_: Exception) {
            }
        }.apply { isDaemon = true; start() }
    }

    private fun serveSafely(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1))
            val writer = OutputStreamWriter(socket.getOutputStream(), StandardCharsets.ISO_8859_1)
            serve(reader) { text ->
                writer.write(text + "\r\n")
                writer.flush()
            }
        } catch (_: Exception) {
        } finally {
            runCatching { socket.close() }
        }
    }

    protected abstract fun serve(reader: BufferedReader, send: (String) -> Unit)

    override fun close() {
        server.close()
        synchronized(sockets) { sockets.forEach { runCatching { it.close() } } }
    }
}

/** Greeting, CAPABILITY (IMAP4rev1 only, so Angus uses LOGIN), LOGIN (recorded), NOOP, LOGOUT. */
internal class ScriptedImapServer : ScriptedServer() {
    val logins = LinkedBlockingQueue<String>()

    override fun serve(reader: BufferedReader, send: (String) -> Unit) {
        send("* OK scripted IMAP ready")
        while (true) {
            val line = reader.readLine() ?: return
            val parts = line.split(' ', limit = 3)
            val tag = parts.getOrNull(0) ?: return
            when (parts.getOrNull(1)?.uppercase()) {
                "CAPABILITY" -> {
                    send("* CAPABILITY IMAP4rev1")
                    send("$tag OK done")
                }
                "LOGIN" -> {
                    logins.add(line)
                    send("$tag OK logged in")
                }
                "NOOP" -> send("$tag OK noop")
                "LOGOUT" -> {
                    send("* BYE")
                    send("$tag OK bye")
                    return
                }
                else -> send("$tag NO unsupported")
            }
        }
    }
}

/** Greeting, EHLO advertising AUTH PLAIN / LOGIN, AUTH (recorded, always accepted), NOOP, RSET, QUIT. */
internal class ScriptedSmtpServer : ScriptedServer() {
    val auths = LinkedBlockingQueue<String>()

    override fun serve(reader: BufferedReader, send: (String) -> Unit) {
        send("220 scripted SMTP ready")
        var loginSteps = 0
        while (true) {
            val line = reader.readLine() ?: return
            if (loginSteps > 0) {
                auths.add(line)
                loginSteps--
                send(if (loginSteps == 0) "235 2.7.0 accepted" else "334 UGFzc3dvcmQ6")
                continue
            }
            val verb = line.substringBefore(' ').uppercase()
            when (verb) {
                "EHLO", "HELO" -> {
                    send("250-scripted")
                    send("250-AUTH PLAIN LOGIN")
                    send("250 8BITMIME")
                }
                "AUTH" -> {
                    auths.add(line)
                    if (line.uppercase().startsWith("AUTH LOGIN") && line.split(' ').size == 2) {
                        loginSteps = 2
                        send("334 VXNlcm5hbWU6")
                    } else if (line.uppercase().startsWith("AUTH LOGIN")) {
                        loginSteps = 1
                        send("334 UGFzc3dvcmQ6")
                    } else {
                        send("235 2.7.0 accepted")
                    }
                }
                "NOOP", "RSET" -> send("250 ok")
                "QUIT" -> {
                    send("221 bye")
                    return
                }
                else -> send("502 unsupported")
            }
        }
    }
}
