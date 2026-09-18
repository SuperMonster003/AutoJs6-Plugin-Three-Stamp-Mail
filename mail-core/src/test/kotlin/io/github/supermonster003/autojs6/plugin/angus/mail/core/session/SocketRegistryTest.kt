package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** The tracked sockets behind `MailSession.abort` (roadmap P2.5 `cancel`). */
class SocketRegistryTest {

    @Test
    fun theFactoryRegistersEveryUnconnectedSocketAndForgetsClosedOnes() {
        val registry = SocketRegistry()
        val first = registry.factory.createSocket()
        val second = registry.factory.createSocket()
        assertFalse(first.isConnected)
        assertEquals(2, registry.liveCount)
        first.close()
        assertEquals("closed sockets are pruned on the next look", 1, registry.liveCount)
        assertEquals(1, registry.abort())
        assertTrue(second.isClosed)
        assertEquals(0, registry.liveCount)
        assertEquals(1, registry.aborts)
        assertTrue(registry.isAborting)
        val late = registry.factory.createSocket()
        assertTrue("a socket created after the abort is closed at once, so the racing connect fails too", late.isClosed)
        assertEquals(0, registry.liveCount)
        assertEquals("nothing left to close", 0, registry.abort())
        assertEquals(2, registry.aborts)
        registry.resume()
        assertFalse(registry.isAborting)
        assertFalse(registry.factory.createSocket().isClosed)
        assertEquals(1, registry.liveCount)
        assertEquals(1, registry.closeAll())
        assertEquals("SocketRegistry.factory", registry.factory.toString())
    }

    @Test
    fun closeAllFromAnotherThreadUnblocksAReadWaitingForTheServer() {
        val registry = SocketRegistry()
        ServerSocket(0, 1, InetSocketAddress("127.0.0.1", 0).address).use { server ->
            // A silent server: accepts and never writes, like a mail server that stalls before its greeting.
            val socket = registry.factory.createSocket()
            socket.connect(InetSocketAddress("127.0.0.1", server.localPort), 2_000)
            val accepted = server.accept()
            val started = CountDownLatch(1)
            var failure: Throwable? = null
            val reader = Thread {
                started.countDown()
                try {
                    socket.getInputStream().read()
                    failure = AssertionError("the read must not return normally")
                } catch (e: Throwable) {
                    failure = e
                }
            }
            reader.start()
            assertTrue(started.await(5, TimeUnit.SECONDS))
            Thread.sleep(200)
            assertTrue("the reader is blocked", reader.isAlive)
            val began = System.nanoTime()
            assertEquals(1, registry.abort())
            reader.join(5_000)
            val elapsedMs = (System.nanoTime() - began) / 1_000_000
            assertFalse("the blocked read returned within ${elapsedMs}ms", reader.isAlive)
            assertTrue("a SocketException, not a timeout: $failure", failure is SocketException || failure is IOException && failure !is java.net.SocketTimeoutException)
            accepted.close()
        }
    }

    @Test
    fun aRegisteredSocketIsTheOneAngusWouldConnect() {
        val registry = SocketRegistry()
        val socket: Socket = registry.factory.createSocket()
        try {
            socket.connect(InetSocketAddress("127.0.0.1", 1), 500)
            fail("nothing listens on port 1")
        } catch (_: IOException) {
        }
        assertEquals("a socket that failed to connect is still tracked until it is closed", 1, registry.liveCount)
        socket.close()
        assertEquals(0, registry.liveCount)
    }
}
