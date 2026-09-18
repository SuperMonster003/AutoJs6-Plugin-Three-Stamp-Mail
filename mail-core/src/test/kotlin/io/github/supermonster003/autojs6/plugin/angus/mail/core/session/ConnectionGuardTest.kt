package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import jakarta.mail.MessagingException
import jakarta.mail.StoreClosedException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class ConnectionGuardTest {

    private class FakeConnection(val id: Int) {
        var alive = true
        var closed = false
    }

    private var now = 1_000L
    private var nextId = 0
    private val closed = ArrayList<Int>()

    private fun guard(idleTimeout: Long = 10_000L): ConnectionGuard<FakeConnection> = ConnectionGuard(
        label = "fake",
        idleTimeoutMillis = idleTimeout,
        clock = { now },
        isAlive = { it.alive },
        onClose = { it.closed = true; closed += it.id },
        connect = { FakeConnection(++nextId) },
    )

    @Test
    fun connectsLazilyAndReusesTheConnection() {
        val guard = guard()
        assertFalse(guard.isConnected)
        assertNull(guard.idleMillis)
        assertEquals(1, guard.use { it.id })
        assertEquals(1, guard.use { it.id })
        assertEquals(1, guard.connectCount)
        assertTrue(guard.isConnected)
        assertEquals(0L, guard.idleMillis)
    }

    @Test
    fun idleConnectionsExpireAndReconnectOnNextUse() {
        val guard = guard(idleTimeout = 5_000L)
        guard.use { }
        now += 4_999
        assertFalse(guard.expireIfIdle())
        now += 1
        assertTrue(guard.expireIfIdle())
        assertFalse(guard.isConnected)
        assertEquals(listOf(1), closed)
        assertEquals(2, guard.use { it.id })
        now += 6_000
        // use() itself expires an idle connection before running.
        assertEquals(3, guard.use { it.id })
        assertEquals(3, guard.connectCount)
    }

    @Test
    fun deadConnectionsAreReplacedBeforeTheBlockRuns() {
        val guard = guard()
        val first = guard.use { it }
        first.alive = false
        val second = guard.use { it }
        assertEquals(2, second.id)
        assertTrue(first.closed)
    }

    @Test
    fun lossDuringTheBlockRetriesOnceOnAFreshConnection() {
        val guard = guard()
        var attempts = 0
        val result = guard.use { connection ->
            attempts++
            if (attempts == 1) throw MessagingException("gone", StoreClosedException(null, "* BYE"))
            connection.id
        }
        assertEquals(2, attempts)
        assertEquals(2, result)
        assertEquals(listOf(1), closed)
    }

    @Test
    fun lossIsNotRetriedTwiceNorWhenTheCallerForbidsIt() {
        val guard = guard()
        var attempts = 0
        try {
            guard.use { attempts++; throw IOException("reset") }
        } catch (e: IOException) {
            assertEquals(2, attempts)
        }
        assertFalse(guard.isConnected)
        attempts = 0
        try {
            guard.use(retryOnLoss = false) { attempts++; throw IOException("reset") }
        } catch (e: IOException) {
            assertEquals(1, attempts)
        }
        assertEquals(listOf(1, 2, 3), closed)
    }

    @Test
    fun timeoutsDropTheConnectionWithoutRetrying() {
        val guard = guard()
        var attempts = 0
        try {
            guard.use { attempts++; throw MessagingException("slow", SocketTimeoutException("Read timed out")) }
            fail("expected the timeout to propagate")
        } catch (e: MessagingException) {
            assertEquals(1, attempts)
        }
        assertFalse(guard.isConnected)
    }

    @Test
    fun lossIsNotRetriedOnceTheCallerForbidsRetries() {
        var allowed = true
        val guard = ConnectionGuard<FakeConnection>(
            label = "fake",
            idleTimeoutMillis = 10_000L,
            clock = { now },
            isAlive = { it.alive },
            onClose = { it.closed = true; closed += it.id },
            connect = { FakeConnection(++nextId) },
            mayRetry = { allowed },
        )
        var attempts = 0
        try {
            guard.use { attempts++; throw IOException("reset") }
        } catch (_: IOException) {
        }
        assertEquals("retries stay on while allowed", 2, attempts)
        allowed = false
        attempts = 0
        try {
            guard.use { attempts++; throw IOException("Socket closed") }
            fail("the loss must propagate")
        } catch (_: IOException) {
        }
        assertEquals("a cancelled call is not retried on a fresh connection", 1, attempts)
        assertFalse(guard.isConnected)
    }

    @Test
    fun anInterruptedThreadIsNotRetriedByDefault() {
        val guard = guard()
        var attempts = 0
        try {
            guard.use { attempts++; Thread.currentThread().interrupt(); throw IOException("Socket closed") }
            fail("the loss must propagate")
        } catch (_: IOException) {
        } finally {
            assertTrue("the flag is left for the caller to clear", Thread.interrupted())
        }
        assertEquals(1, attempts)
        assertFalse(guard.isConnected)
    }

    @Test
    fun commandFailuresKeepTheConnection() {
        val guard = guard()
        try {
            guard.use { throw MessagingException("NO [CANNOT] Invalid mailbox name") }
        } catch (_: MessagingException) {
        }
        assertTrue(guard.isConnected)
        assertEquals(1, guard.connectCount)
    }

    @Test
    fun closeIsFinal() {
        val guard = guard()
        guard.use { }
        guard.close()
        assertTrue(guard.closed)
        assertFalse(guard.isConnected)
        try {
            guard.use { }
            fail("a closed guard must refuse work")
        } catch (_: IllegalStateException) {
        }
    }
}
