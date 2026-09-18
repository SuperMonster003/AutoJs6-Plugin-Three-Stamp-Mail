package io.github.supermonster003.autojs6.plugin.angus.mail

import android.content.Context
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.CallerGuard
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.MailPluginBinder
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.MailSessionBinder
import org.autojs.plugin.mail.api.IMailCallCallback
import org.autojs.plugin.mail.api.IMailSession
import org.autojs.plugin.mail.api.IMailSessionCallback
import org.autojs.plugin.mail.api.MailContract
import org.autojs.plugin.mail.api.MailErrorCodes
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * The queue, cancel, close and envelope ceilings of `MailSessionBinder` (roadmap P2.5) against a
 * loopback server that accepts and then stays silent: the first call blocks in the IMAP greeting
 * read (the read timeout is 30 s, so only an abort can end it early), later calls queue behind it.
 * The binder is in-process with a trusting guard because this instrumentation is not the host.
 */
@RunWith(AndroidJUnit4::class)
class MailSessionBinderTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var server: ServerSocket
    private val accepted = ArrayList<Socket>()
    private lateinit var acceptor: Thread

    @Before
    fun listen() {
        server = ServerSocket(0, 64, InetAddress.getByName("127.0.0.1"))
        acceptor = Thread {
            try {
                while (true) {
                    val socket = server.accept()
                    synchronized(accepted) { accepted += socket }
                }
            } catch (_: Exception) {
            }
        }.apply { isDaemon = true; start() }
    }

    @After
    fun stop() {
        server.close()
        synchronized(accepted) { accepted.forEach { runCatching { it.close() } } }
    }

    private class Results {
        val results = LinkedBlockingQueue<Triple<String, String, JSONObject>>()
        val callback = object : IMailCallCallback.Stub() {
            override fun onProgress(progress: Bundle?) = Unit
            override fun onResult(response: Bundle?) {
                val json = JSONObject(response?.getString(MailContract.KEY_RESPONSE_JSON).orEmpty())
                results.add(Triple(json.getString(MailContract.FIELD_ID), Thread.currentThread().name, json))
            }
        }

        fun next(timeoutSeconds: Long = 10): Triple<String, String, JSONObject> = requireNotNull(results.poll(timeoutSeconds, TimeUnit.SECONDS)) { "no onResult within ${timeoutSeconds}s" }
    }

    private class Statuses {
        val statuses = LinkedBlockingQueue<JSONObject>()
        val callback = object : IMailSessionCallback.Stub() {
            override fun onStatus(status: Bundle?) {
                statuses.add(JSONObject(status?.getString(MailContract.KEY_STATUS_JSON).orEmpty()))
            }
        }
    }

    private fun openSession(statuses: Statuses): IMailSession {
        val account = Bundle().apply {
            putInt(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
            putLong(MailContract.KEY_HOST_VERSION_CODE, AngusMailPlugin.REQUIRED_HOST_VERSION)
            putString(
                MailContract.KEY_ACCOUNT_JSON,
                """{"address":"alice@localhost","user":"alice","debug":true,
                    "imap":{"host":"127.0.0.1","port":${server.localPort},"tls":"none"},
                    "smtp":{"host":"127.0.0.1","port":${server.localPort},"tls":"none"},
                    "timeout":{"connect":2000,"read":30000}}""",
            )
            putString(MailContract.KEY_SECRET_PASSWORD, FAKE_SECRET)
        }
        return requireNotNull(MailPluginBinder(context, CallerGuard.trusting()).openSession(account, statuses.callback)) { "the loopback account must open" }
    }

    private fun submit(session: IMailSession, results: Results, id: String, op: String, args: String = "{}", descriptors: Array<ParcelFileDescriptor>? = null): String {
        val request = Bundle().apply {
            putInt(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
            putString(MailContract.KEY_REQUEST_JSON, """{"id":"$id","op":"$op","args":$args}""")
        }
        assertEquals(id, session.call(request, descriptors, results.callback))
        return id
    }

    private fun status(session: IMailSession): JSONObject = JSONObject(requireNotNull(session.status.getString(MailContract.KEY_STATUS_JSON)))

    private fun error(response: JSONObject): JSONObject = response.getJSONObject(MailContract.FIELD_ERROR)

    /** Waits until the first call is the one in flight and the silent server has accepted its connection. */
    private fun awaitBlocked(session: IMailSession, id: String) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (true) {
            val current = status(session)
            val connections = synchronized(accepted) { accepted.size }
            if (current.optString("active") == id && connections >= 1) break
            assertTrue("call $id never reached the greeting read: $current, connections=$connections", System.nanoTime() < deadline)
            Thread.sleep(20)
        }
        Thread.sleep(200)
    }

    @Test
    fun cancelBreaksTheCallInFlightAndTheSessionStaysUsable() {
        val statuses = Statuses()
        val session = openSession(statuses)
        val results = Results()
        submit(session, results, "c1", MailContract.OP_FOLDERS_LIST)
        awaitBlocked(session, "c1")
        assertTrue(results.results.isEmpty())

        val began = System.nanoTime()
        session.cancel("c1")
        val (id, thread, response) = results.next()
        val elapsedMs = (System.nanoTime() - began) / 1_000_000
        assertEquals("c1", id)
        assertEquals(MailSessionBinder.THREAD_NAME, thread)
        assertFalse(response.getBoolean(MailContract.FIELD_OK))
        assertEquals(response.toString(), MailErrorCodes.CANCELLED, error(response).getString(MailContract.FIELD_ERROR_CODE))
        assertFalse(error(response).getBoolean(MailContract.FIELD_ERROR_RETRYABLE))
        assertTrue("the read timeout is 30 s; the abort took ${elapsedMs}ms", elapsedMs < 5_000)
        assertFalse(response.toString().contains(FAKE_SECRET))

        val after = status(session)
        assertEquals(MailContract.STATE_OPEN, after.getString(MailContract.FIELD_STATE))
        assertEquals(MailErrorCodes.CANCELLED, after.getJSONObject(MailContract.FIELD_LAST_ERROR).getString(MailContract.FIELD_ERROR_CODE))
        assertEquals(0, after.getInt("queued"))
        assertFalse(after.has("active"))
        assertEquals("the aborted connection is gone", 0, after.getJSONArray("connected").length())

        // finished and unknown ids are ignored; the session still answers, without a second result for c1
        session.cancel("c1")
        session.cancel("never-submitted")
        session.cancel(null)
        submit(session, results, "c2", MailContract.OP_FOLDERS_STATUS)
        val (id2, _, response2) = results.next()
        assertEquals("c2", id2)
        assertEquals(MailErrorCodes.INVALID_ARGUMENT, error(response2).getString(MailContract.FIELD_ERROR_CODE))
        assertNull(results.results.poll(300, TimeUnit.MILLISECONDS))

        session.close()
        val closed = requireNotNull(statuses.statuses.poll(10, TimeUnit.SECONDS))
        assertEquals(MailContract.STATE_CLOSED, closed.getString(MailContract.FIELD_STATE))
        assertEquals("closed", closed.getString(MailContract.FIELD_REASON))
    }

    @Test
    fun theQueueHoldsMaxQueuedCallsAndCloseAnswersThemAll() {
        val statuses = Statuses()
        val session = openSession(statuses)
        val results = Results()
        submit(session, results, "c1", MailContract.OP_FOLDERS_LIST)
        awaitBlocked(session, "c1")

        val queuedIds = (1..MailContract.MAX_QUEUED_CALLS).map { submit(session, results, "q$it", MailContract.OP_FOLDERS_STATUS) }
        val full = status(session)
        assertEquals(MailContract.MAX_QUEUED_CALLS, full.getInt("queued"))
        assertEquals("c1", full.getString("active"))
        assertNull("queued calls are not answered while c1 blocks", results.results.poll(300, TimeUnit.MILLISECONDS))

        val pipe = ParcelFileDescriptor.createPipe()
        submit(session, results, "overflow", MailContract.OP_MESSAGES_RAW, """{"uid": 1}""", arrayOf(pipe[1]))
        val (overflowId, overflowThread, overflow) = results.next()
        assertEquals("overflow", overflowId)
        assertNotEquals("refusals never come from the Binder thread", Thread.currentThread().name, overflowThread)
        assertEquals(overflow.toString(), MailErrorCodes.LIMIT_EXCEEDED, error(overflow).getString(MailContract.FIELD_ERROR_CODE))
        assertFalse(error(overflow).getBoolean(MailContract.FIELD_ERROR_RETRYABLE))
        assertTrue(error(overflow).getString(MailContract.FIELD_ERROR_MESSAGE).contains("MAX_QUEUED_CALLS"))
        assertFalse("the plugin's copy of a refused call's descriptor is closed", pipe[1].fileDescriptor.valid())
        pipe[0].close()
        assertEquals(MailErrorCodes.LIMIT_EXCEEDED, status(session).getJSONObject(MailContract.FIELD_LAST_ERROR).getString(MailContract.FIELD_ERROR_CODE))

        session.cancel("q5")
        val (cancelledId, _, cancelled) = results.next()
        assertEquals("q5", cancelledId)
        assertEquals(MailErrorCodes.CANCELLED, error(cancelled).getString(MailContract.FIELD_ERROR_CODE))
        assertEquals(MailContract.MAX_QUEUED_CALLS - 1, status(session).getInt("queued"))

        // close: the queue answers SESSION_CLOSED, the call in flight is aborted and answers SESSION_CLOSED, then the status closes
        session.close()
        val answered = HashMap<String, String>()
        val expected = queuedIds.size - 1 + 1
        repeat(expected) {
            val (id, _, response) = results.next()
            assertNull("exactly one answer per id: $id", answered.put(id, error(response).getString(MailContract.FIELD_ERROR_CODE)))
        }
        assertEquals(expected, answered.size)
        assertTrue(answered.keys.containsAll(queuedIds - "q5"))
        assertEquals(MailErrorCodes.SESSION_CLOSED, answered["c1"])
        assertTrue(answered.toString(), answered.values.all { it == MailErrorCodes.SESSION_CLOSED })
        val closed = requireNotNull(statuses.statuses.poll(10, TimeUnit.SECONDS))
        assertEquals(MailContract.STATE_CLOSED, closed.getString(MailContract.FIELD_STATE))
        assertEquals("closed", closed.getString(MailContract.FIELD_REASON))
        assertEquals(MailContract.STATE_CLOSED, status(session).getString(MailContract.FIELD_STATE))

        submit(session, results, "late", MailContract.OP_SESSION_TEST)
        val (lateId, _, late) = results.next()
        assertEquals("late", lateId)
        assertEquals(MailErrorCodes.SESSION_CLOSED, error(late).getString(MailContract.FIELD_ERROR_CODE))
        assertNull("no further answers", results.results.poll(500, TimeUnit.MILLISECONDS))
        assertNull("no further status", statuses.statuses.poll(300, TimeUnit.MILLISECONDS))
    }

    @Test
    fun anOversizedRequestEnvelopeIsRefusedBeforeItIsParsed() {
        val statuses = Statuses()
        val session = openSession(statuses)
        val results = Results()
        val pipe = ParcelFileDescriptor.createPipe()
        val padding = "x".repeat(MailContract.MAX_ENVELOPE_BYTES)
        submit(session, results, "big", MailContract.OP_MESSAGES_RAW, """{"uid": 1, "note": "$padding"}""", arrayOf(pipe[1]))
        val (id, thread, response) = results.next()
        assertEquals("big", id)
        assertNotEquals(Thread.currentThread().name, thread)
        assertEquals(response.toString(), MailErrorCodes.LIMIT_EXCEEDED, error(response).getString(MailContract.FIELD_ERROR_CODE))
        assertFalse(error(response).getBoolean(MailContract.FIELD_ERROR_RETRYABLE))
        assertTrue(error(response).getString(MailContract.FIELD_ERROR_MESSAGE).startsWith("request envelope of "))
        assertFalse(pipe[1].fileDescriptor.valid())
        pipe[0].close()
        val after = status(session)
        assertEquals(MailErrorCodes.LIMIT_EXCEEDED, after.getJSONObject(MailContract.FIELD_LAST_ERROR).getString(MailContract.FIELD_ERROR_CODE))
        assertEquals(0, after.getInt("queued"))
        assertEquals("nothing was connected for a refused call", 0, synchronized(accepted) { accepted.size })
        session.close()
        assertEquals(MailContract.STATE_CLOSED, requireNotNull(statuses.statuses.poll(10, TimeUnit.SECONDS)).getString(MailContract.FIELD_STATE))
    }

    private companion object {
        const val FAKE_SECRET = "not-a-real-secret"
    }
}
