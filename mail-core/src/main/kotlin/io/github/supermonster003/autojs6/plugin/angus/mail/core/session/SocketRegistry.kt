package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

/**
 * The live sockets of one session, so that a cancellation coming from another thread can break
 * whatever the session thread is blocked on (roadmap P2.5 `cancel`): a connect, a TLS handshake,
 * a read waiting for the server, or a transfer.
 *
 * Angus Mail asks the configured `mail.<protocol>.socketFactory` for an unconnected socket and
 * then does the connect, the timeouts and the TLS layering itself (`SocketFetcher`), for implicit
 * SSL and STARTTLS alike. Tracking that plain socket underneath every connection is therefore
 * enough: closing it makes the SSL socket, the protocol reader or the writer on top of it fail
 * at once. [Socket.close] is thread-safe, so [closeAll] may run on any thread while the session
 * thread is inside an operation; the session thread then sees the loss, drops the connection and
 * reports `CANCELLED` (see `MailSession.abort`).
 */
class SocketRegistry {

    private val sockets = LinkedHashSet<Socket>()

    /** The factory to hand to Jakarta Mail through `mail.<protocol>.socketFactory`. */
    val factory: SocketFactory = TrackingSocketFactory()

    /** Times [abort] ran (diagnostics and tests). */
    @Volatile
    var aborts: Int = 0
        private set

    /**
     * True between [abort] and [resume]: sockets registered meanwhile are closed at once, so a
     * connect that starts after the cancel (the cancel raced the socket creation) fails too.
     */
    @Volatile
    var isAborting: Boolean = false
        private set

    /** Sockets created through [factory] that are not closed yet. */
    val liveCount: Int
        get() = synchronized(sockets) {
            prune()
            sockets.size
        }

    fun register(socket: Socket): Socket = synchronized(sockets) {
        prune()
        if (isAborting) {
            runCatching { socket.close() }
        } else {
            sockets += socket
        }
        socket
    }

    /** Closes every live socket, refuses new ones until [resume], and returns how many were closed. */
    fun abort(): Int = synchronized(sockets) {
        isAborting = true
        aborts++
        closeAll()
    }

    /** Accepts new sockets again; the owner calls it before the next operation. */
    fun resume() {
        synchronized(sockets) { isAborting = false }
    }

    /** Closes every live socket and returns how many there were. */
    fun closeAll(): Int = synchronized(sockets) {
        prune()
        val closing = sockets.toList()
        sockets.clear()
        closing.forEach { socket -> runCatching { socket.close() } }
        closing.size
    }

    private fun prune() {
        sockets.removeAll { it.isClosed }
    }

    private inner class TrackingSocketFactory : SocketFactory() {
        override fun createSocket(): Socket = register(Socket())
        override fun createSocket(host: String, port: Int): Socket = register(Socket(host, port))
        override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket = register(Socket(host, port, localHost, localPort))
        override fun createSocket(host: InetAddress, port: Int): Socket = register(Socket(host, port))
        override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket = register(Socket(address, port, localAddress, localPort))
        override fun toString(): String = "SocketRegistry.factory"
    }
}
