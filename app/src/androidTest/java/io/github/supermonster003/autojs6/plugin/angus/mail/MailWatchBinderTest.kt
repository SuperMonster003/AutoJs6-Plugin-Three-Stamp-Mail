package io.github.supermonster003.autojs6.plugin.angus.mail

import android.content.Context
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.CallerGuard
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.MailPluginBinder
import org.autojs.plugin.mail.api.IMailSession
import org.autojs.plugin.mail.api.IMailSessionCallback
import org.autojs.plugin.mail.api.IMailWatch
import org.autojs.plugin.mail.api.IMailWatchCallback
import org.autojs.plugin.mail.api.MailContract
import org.autojs.plugin.mail.api.MailErrorCodes
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Roadmap P5 `IMailSession.watch` over the Binder inside the plugin process, against a scripted
 * IMAP server that pushes `EXISTS` while the watch idles: events carry the host's generation and
 * a `seq` increasing from 1, `message` events hold the envelope, `stop` ends with one `closed`
 * event, the fifth watch of a session is refused with the reason in the session status, unusable
 * options are refused the same way, and closing the session closes its watches.
 */
@RunWith(AndroidJUnit4::class)
class MailWatchBinderTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var imap: ScriptedIdleImapServer
    private val sessions = ArrayList<IMailSession>()

    @Before
    fun start() {
        imap = ScriptedIdleImapServer()
    }

    @After
    fun stop() {
        sessions.forEach { runCatching { it.close() } }
        imap.close()
    }

    private class Events {
        val events = LinkedBlockingQueue<Triple<Long, Long, JSONObject>>()
        val threads = LinkedBlockingQueue<String>()
        val callback = object : IMailWatchCallback.Stub() {
            override fun onEvent(generation: Long, seq: Long, event: Bundle?) {
                threads.add(Thread.currentThread().name)
                events.add(Triple(generation, seq, JSONObject(event?.getString(MailContract.KEY_EVENT_JSON).orEmpty())))
            }
        }

        fun next(timeoutSeconds: Long = 10): Triple<Long, Long, JSONObject> =
            requireNotNull(events.poll(timeoutSeconds, TimeUnit.SECONDS)) { "no watch event within $timeoutSeconds s" }

        fun quietFor(millis: Long): Boolean = events.poll(millis, TimeUnit.MILLISECONDS) == null
    }

    private class Statuses {
        val statuses = LinkedBlockingQueue<JSONObject>()
        val callback = object : IMailSessionCallback.Stub() {
            override fun onStatus(status: Bundle?) {
                statuses.add(JSONObject(status?.getString(MailContract.KEY_STATUS_JSON).orEmpty()))
            }
        }
    }

    private fun openSession(): IMailSession {
        val account = Bundle().apply {
            putInt(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
            putLong(MailContract.KEY_HOST_VERSION_CODE, AngusMailPlugin.REQUIRED_HOST_VERSION)
            putString(
                MailContract.KEY_ACCOUNT_JSON,
                """{"address":"alice@example.org","user":"alice","debug":true,
                    "imap":{"host":"127.0.0.1","port":${imap.port},"tls":"none"},
                    "smtp":{"host":"127.0.0.1","port":${imap.port},"tls":"none"},
                    "timeout":{"connect":2000,"read":30000}}""",
            )
            putString(MailContract.KEY_SECRET_PASSWORD, SECRET)
        }
        return requireNotNull(MailPluginBinder(context, CallerGuard.trusting()).openSession(account, Statuses().callback)) { "the loopback account must open" }
            .also { sessions += it }
    }

    private fun options(generation: Long, json: String = """{"folder":"INBOX"}"""): Bundle = Bundle().apply {
        putInt(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
        putLong(MailContract.KEY_GENERATION, generation)
        putString(MailContract.KEY_WATCH_OPTIONS_JSON, json)
    }

    private fun status(watch: IMailWatch): JSONObject = JSONObject(requireNotNull(watch.status.getString(MailContract.KEY_STATUS_JSON)))

    private fun status(session: IMailSession): JSONObject = JSONObject(requireNotNull(session.status.getString(MailContract.KEY_STATUS_JSON)))

    private fun await(timeoutMs: Long = 10_000, what: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (!condition()) {
            assertTrue("$what did not hold within $timeoutMs ms", System.nanoTime() < deadline)
            Thread.sleep(20)
        }
    }

    @Test
    fun eventsCarryTheGenerationAndAnIncreasingSeqAndStopEndsWithClosed() {
        val session = openSession()
        val events = Events()
        val watch = requireNotNull(session.watch(options(7), events.callback)) { "the watch must open: ${status(session)}" }
        val open = status(watch)
        assertEquals(MailContract.STATE_OPEN, open.getString(MailContract.FIELD_STATE))
        assertEquals(MailContract.WATCH_MODE_IDLE, open.getString(MailContract.FIELD_MODE))
        assertFalse(open.has(MailContract.FIELD_REASON))
        imap.awaitIdling(1)
        assertEquals(1, status(session).getInt("watches"))
        assertTrue("nothing before a push", events.quietFor(300))

        imap.push("hello")
        val (generation, seq, event) = events.next()
        assertEquals(7L, generation)
        assertEquals(1L, seq)
        assertEquals(MailContract.EVENT_MESSAGE, event.getString(MailContract.FIELD_TYPE))
        assertEquals(1L, event.getLong(MailContract.FIELD_SEQ))
        assertEquals(7L, event.getLong(MailContract.FIELD_GENERATION))
        assertEquals("INBOX", event.getString(MailContract.FIELD_FOLDER))
        val message = event.getJSONObject(MailContract.FIELD_MESSAGE)
        assertEquals("hello", message.getString("subject"))
        assertEquals(1L, message.getLong("uid"))
        assertEquals("INBOX", message.getString("folder"))
        assertFalse(message.getBoolean("bodyLoaded"))
        assertFalse(events.threads.poll()!!.contains("Binder"))

        imap.push("second")
        val second = events.next()
        assertEquals(2L, second.second)
        assertEquals("second", second.third.getJSONObject(MailContract.FIELD_MESSAGE).getString("subject"))
        assertEquals(2L, second.third.getJSONObject(MailContract.FIELD_MESSAGE).getLong("uid"))

        watch.stop()
        val closed = events.next()
        assertEquals(3L, closed.second)
        assertEquals(MailContract.EVENT_CLOSED, closed.third.getString(MailContract.FIELD_TYPE))
        assertEquals("stopped", closed.third.getString(MailContract.FIELD_REASON))
        assertTrue("nothing after closed", events.quietFor(300))
        val after = status(watch)
        assertEquals(MailContract.STATE_CLOSED, after.getString(MailContract.FIELD_STATE))
        assertEquals("stopped", after.getString(MailContract.FIELD_REASON))
        await(what = "session watch count") { status(session).getInt("watches") == 0 }
        // stop is idempotent
        watch.stop()
        assertTrue(events.quietFor(200))
        assertFalse(event.toString().contains(SECRET))
    }

    @Test
    fun theFifthWatchIsRefusedAndClosingTheSessionClosesTheWatches() {
        val session = openSession()
        val events = (1..4).map { Events() }
        val watches = events.mapIndexed { index, it -> requireNotNull(session.watch(options(index + 1L), it.callback)) { "watch $index must open: ${status(session)}" } }
        imap.awaitIdling(4)
        assertNull(session.watch(options(5), Events().callback))
        val refused = status(session)
        assertEquals(MailErrorCodes.LIMIT_EXCEEDED, refused.getJSONObject(MailContract.FIELD_LAST_ERROR).getString(MailContract.FIELD_ERROR_CODE))
        assertEquals(4, refused.getInt("watches"))

        session.close()
        events.forEachIndexed { index, it ->
            val (generation, seq, event) = it.next()
            assertEquals(index + 1L, generation)
            assertEquals(1L, seq)
            assertEquals(MailContract.EVENT_CLOSED, event.getString(MailContract.FIELD_TYPE))
            assertEquals("closed", event.getString(MailContract.FIELD_REASON))
        }
        watches.forEach { assertEquals(MailContract.STATE_CLOSED, status(it).getString(MailContract.FIELD_STATE)) }
        assertNull("a closed session opens no watch", session.watch(options(6), Events().callback))
    }

    @Test
    fun unusableOptionsAreRefusedWithTheReasonInTheSessionStatus() {
        val session = openSession()
        assertNull(session.watch(options(1, """{"interval":5}"""), Events().callback))
        assertEquals(MailErrorCodes.INVALID_ARGUMENT, status(session).getJSONObject(MailContract.FIELD_LAST_ERROR).getString(MailContract.FIELD_ERROR_CODE))
        assertNull(session.watch(options(1, """{"mode":"push"}"""), Events().callback))
        assertNull(session.watch(options(1), null))
        assertEquals(0, status(session).getInt("watches"))
        // the poll mode is honoured without IDLE
        val events = Events()
        val watch = requireNotNull(session.watch(options(2, """{"mode":"poll","pollIntervalMs":15000}"""), events.callback)) { "the poll watch must open: ${status(session)}" }
        assertEquals(MailContract.WATCH_MODE_POLL, status(watch).getString(MailContract.FIELD_MODE))
        await(what = "the poller logged in") { imap.logins.get() >= 1 }
        assertEquals(0, imap.idles.get())
        watch.stop()
        assertEquals(MailContract.EVENT_CLOSED, events.next().third.getString(MailContract.FIELD_TYPE))
    }

    private companion object {
        const val SECRET = "watch-secret"
    }
}
