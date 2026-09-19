package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.concurrent.LinkedBlockingQueue
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory

/**
 * A STARTTLS front for GreenMail's plain ports (mail roadmap P6 TLS matrix): GreenMail 2.1.13
 * has no STARTTLS, so this proxy advertises the upgrade (`STARTTLS` in the IMAP capabilities,
 * `STLS` in the POP3 `CAPA` list, `250-STARTTLS` in the SMTP `EHLO` reply), answers the upgrade
 * command itself, completes a real TLS handshake on the client connection with GreenMail's own
 * self-signed certificate (`greenmail.p12` from the GreenMail jar) and relays everything after
 * it in the clear to GreenMail. The protocol version of every completed handshake is recorded.
 */
internal class StartTlsProxy(private val protocol: Protocol, private val upstreamPort: Int) : AutoCloseable {

    enum class Protocol { IMAP, POP3, SMTP }

    private val server = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
    private val sockets = ArrayList<Socket>()

    /** TLS protocol names of the handshakes completed so far. */
    val negotiated = LinkedBlockingQueue<String>()

    val port: Int get() = server.localPort

    init {
        Thread {
            try {
                while (true) {
                    val client = server.accept()
                    synchronized(sockets) { sockets += client }
                    Thread { relay(client) }.apply { isDaemon = true; start() }
                }
            } catch (_: Exception) {
            }
        }.apply { isDaemon = true; start() }
    }

    override fun close() {
        server.close()
        synchronized(sockets) { sockets.forEach { runCatching { it.close() } } }
    }

    private inner class Link(private val client: Socket, private val upstream: Socket) {
        @Volatile
        var clientIn: InputStream = client.getInputStream()

        @Volatile
        var clientOut: OutputStream = client.getOutputStream()

        @Volatile
        var tls = false

        @Volatile
        var capaPending = false

        fun upgrade() {
            val ssl = serverContext().socketFactory.createSocket(client, "127.0.0.1", client.port, true) as SSLSocket
            ssl.useClientMode = false
            ssl.startHandshake()
            negotiated.add(ssl.session.protocol)
            clientIn = ssl.inputStream
            clientOut = ssl.outputStream
            tls = true
        }

        fun sendToClient(line: String) {
            clientOut.write((line + "\r\n").toByteArray(StandardCharsets.ISO_8859_1))
            clientOut.flush()
        }
    }

    private fun relay(client: Socket) {
        val upstream = Socket("127.0.0.1", upstreamPort)
        synchronized(sockets) { sockets += upstream }
        val link = Link(client, upstream)
        val downstream = Thread { runCatching { downstream(link, upstream) }; runCatching { client.close() } }.apply { isDaemon = true; start() }
        try {
            val up = upstream.getOutputStream()
            while (true) {
                if (link.tls) {
                    pump(link.clientIn, up)
                    return
                }
                val line = readLine(link.clientIn) ?: return
                val upper = line.uppercase()
                val upgrade = when (protocol) {
                    Protocol.IMAP -> upper.substringAfter(' ', "").trim() == "STARTTLS"
                    Protocol.POP3 -> upper.trim() == "STLS"
                    Protocol.SMTP -> upper.trim() == "STARTTLS"
                }
                if (upgrade) {
                    link.sendToClient(
                        when (protocol) {
                            Protocol.IMAP -> "${line.substringBefore(' ')} OK Begin TLS negotiation now"
                            Protocol.POP3 -> "+OK Begin TLS negotiation"
                            Protocol.SMTP -> "220 Ready to start TLS"
                        },
                    )
                    link.upgrade()
                    continue
                }
                if (protocol == Protocol.POP3 && upper.trim() == "CAPA") link.capaPending = true
                up.write((line + "\r\n").toByteArray(StandardCharsets.ISO_8859_1))
                up.flush()
            }
        } catch (_: Exception) {
        } finally {
            runCatching { upstream.close() }
            runCatching { client.close() }
            downstream.interrupt()
        }
    }

    private fun downstream(link: Link, upstream: Socket) {
        val input = upstream.getInputStream()
        while (true) {
            if (link.tls) {
                pump(input, link.clientOut)
                return
            }
            val line = readLine(input) ?: return
            when (protocol) {
                Protocol.IMAP -> when {
                    line.startsWith("* CAPABILITY") -> link.sendToClient("$line STARTTLS")
                    line.startsWith("* OK [CAPABILITY") -> link.sendToClient(line.replaceFirst("]", " STARTTLS]"))
                    else -> link.sendToClient(line)
                }
                Protocol.POP3 -> {
                    if (link.capaPending && line == ".") {
                        link.sendToClient("STLS")
                        link.capaPending = false
                    }
                    if (link.capaPending && line.startsWith("-ERR")) link.capaPending = false
                    link.sendToClient(line)
                }
                Protocol.SMTP -> {
                    if (line.startsWith("250 ")) link.sendToClient("250-STARTTLS")
                    link.sendToClient(line)
                }
            }
        }
    }

    /** One CRLF-terminated line read byte by byte, so nothing beyond it is consumed before an upgrade. */
    private fun readLine(input: InputStream): String? {
        val bytes = java.io.ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b < 0) return if (bytes.size() == 0) null else String(bytes.toByteArray(), StandardCharsets.ISO_8859_1)
            if (b == '\n'.code) {
                val text = String(bytes.toByteArray(), StandardCharsets.ISO_8859_1)
                return text.removeSuffix("\r")
            }
            bytes.write(b)
        }
    }

    private fun pump(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) return
            output.write(buffer, 0, read)
            output.flush()
        }
    }

    companion object {
        private const val STORE_PASSWORD = "changeit"

        /** GreenMail's own key store (self-signed, CN "GreenMail selfsigned Test Certificate", no subject alternative name). */
        fun greenMailKeyStore(): KeyStore = KeyStore.getInstance("PKCS12").apply {
            val stream = checkNotNull(StartTlsProxy::class.java.getResourceAsStream("/greenmail.p12")) { "greenmail.p12 is missing from the test classpath" }
            stream.use { load(it, STORE_PASSWORD.toCharArray()) }
        }

        /** A server context presenting GreenMail's certificate. */
        fun serverContext(): SSLContext {
            val keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply { init(greenMailKeyStore(), STORE_PASSWORD.toCharArray()) }
            return SSLContext.getInstance("TLS").apply { init(keyManagers.keyManagers, null, null) }
        }

        /** A client context that trusts GreenMail's certificate (and nothing else), for the host-name mismatch case. */
        fun trustingContext(): SSLContext {
            val trusted = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
            val source = greenMailKeyStore()
            source.aliases().asSequence().forEach { alias -> trusted.setCertificateEntry(alias, source.getCertificate(alias)) }
            val trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(trusted) }
            return SSLContext.getInstance("TLS").apply { init(null, trustManagers.trustManagers, null) }
        }
    }
}
