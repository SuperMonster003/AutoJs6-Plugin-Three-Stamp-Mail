package io.github.supermonster003.autojs6.plugin.angus.mail

import android.content.Context
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.CallerGuard
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.MailPluginBinder
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerConfig
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStores
import io.github.supermonster003.autojs6.plugin.angus.mail.trigger.TriggerStores
import io.github.supermonster003.autojs6.plugin.angus.mail.trigger.WatchKeeper
import org.autojs.plugin.mail.api.IMailTrigger
import org.autojs.plugin.mail.api.IMailTriggerCallback
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
 * Roadmap P8 `IMailPlugin.openTrigger` / `listTriggers` over the Binder inside the plugin
 * process: a background watch configured in the plugin's trigger store on a saved loopback
 * account is run by the watch keeper against a scripted IMAP server; a subscriber receives the
 * current status at once, `mail` events with the host's generation and a `seq` increasing from
 * 1 for each pushed message, a subscriber's filter narrows its events, `listTriggers` shows the
 * watch with its record count, a disabled or unknown watch is refused with a `stopped` status
 * whose reason is `refused`, and `stop` ends the subscription without stopping the watch.
 */
@RunWith(AndroidJUnit4::class)
class MailTriggerBinderTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var imap: ScriptedIdleImapServer
    private val subscriptions = ArrayList<IMailTrigger>()

    @Before
    fun start() {
        imap = ScriptedIdleImapServer()
        AccountStores.of(context).put(
            ALIAS,
            """{"address":"alice@example.org","user":"alice",
                "imap":{"host":"127.0.0.1","port":${imap.port},"tls":"none"},
                "smtp":{"host":"127.0.0.1","port":${imap.port},"tls":"none"},
                "timeout":{"connect":2000,"read":30000}}""",
            SecretKind.PASSWORD,
            SECRET.toCharArray(),
        )
        TriggerStores.of(context).put(TriggerConfig(TRIGGER, ALIAS, mode = MailContract.WATCH_MODE_IDLE))
        TriggerStores.of(context).put(TriggerConfig(DISABLED, ALIAS, enabled = false))
    }

    @After
    fun stop() {
        subscriptions.forEach { runCatching { it.stop() } }
        runCatching { TriggerStores.of(context).remove(TRIGGER) }
        runCatching { TriggerStores.of(context).remove(DISABLED) }
        runCatching { WatchKeeper.of(context).sync() }
        runCatching { AccountStores.of(context).remove(ALIAS) }
        imap.close()
    }

    private class Events {
        val mails = LinkedBlockingQueue<Triple<Long, Long, JSONObject>>()
        val statuses = LinkedBlockingQueue<JSONObject>()
        val callback = object : IMailTriggerCallback.Stub() {
            override fun onMail(generation: Long, seq: Long, event: Bundle?) {
                mails.add(Triple(generation, seq, JSONObject(event?.getString(MailContract.KEY_EVENT_JSON).orEmpty())))
            }

            override fun onStatus(status: Bundle?) {
                statuses.add(JSONObject(status?.getString(MailContract.KEY_STATUS_JSON).orEmpty()))
            }
        }

        fun nextMail(timeoutSeconds: Long = 15): Triple<Long, Long, JSONObject> =
            requireNotNull(mails.poll(timeoutSeconds, TimeUnit.SECONDS)) { "no mail event within $timeoutSeconds s" }

        fun nextStatus(timeoutSeconds: Long = 15): JSONObject =
            requireNotNull(statuses.poll(timeoutSeconds, TimeUnit.SECONDS)) { "no status within $timeoutSeconds s" }

        fun quietFor(millis: Long): Boolean = mails.poll(millis, TimeUnit.MILLISECONDS) == null
    }

    private fun options(generation: Long, json: String = """{"triggerId":"$TRIGGER"}"""): Bundle = Bundle().apply {
        putInt(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
        putLong(MailContract.KEY_GENERATION, generation)
        putString(MailContract.KEY_TRIGGER_OPTIONS_JSON, json)
    }

    private fun binder() = MailPluginBinder(context, CallerGuard.trusting())

    private fun open(events: Events, generation: Long, json: String = """{"triggerId":"$TRIGGER"}"""): IMailTrigger =
        requireNotNull(binder().openTrigger(options(generation, json), events.callback)) { "the subscription must open: ${events.statuses.peek()}" }
            .also { subscriptions += it }

    private fun status(trigger: IMailTrigger): JSONObject = JSONObject(requireNotNull(trigger.status.getString(MailContract.KEY_STATUS_JSON)))

    private fun awaitConnected(events: Events): JSONObject {
        while (true) {
            val status = events.nextStatus()
            if (status.getString(MailContract.FIELD_STATE) == MailContract.TRIGGER_STATE_CONNECTED) return status
            assertTrue("the watch must not fail: $status", status.getString(MailContract.FIELD_STATE) != MailContract.TRIGGER_STATE_FAILED)
        }
    }

    @Test
    fun aSubscriberSeesTheStatusThenEachPushedMessageWithTheGenerationAndAnIncreasingSeq() {
        val events = Events()
        val trigger = open(events, 7)
        val connected = awaitConnected(events)
        assertEquals(TRIGGER, connected.getString(MailContract.FIELD_TRIGGER_ID))
        assertEquals(MailContract.WATCH_MODE_IDLE, connected.getString(MailContract.FIELD_MODE))
        imap.awaitIdling(1)
        assertTrue("nothing before a push", events.quietFor(300))

        imap.push("hello")
        val (generation, seq, event) = events.nextMail()
        assertEquals(7L, generation)
        assertEquals(1L, seq)
        assertEquals(MailContract.TRIGGER_EVENT_MAIL, event.getString(MailContract.FIELD_TYPE))
        assertEquals(TRIGGER, event.getString(MailContract.FIELD_TRIGGER_ID))
        assertEquals(ALIAS, event.getString(MailContract.FIELD_ALIAS))
        assertEquals("alice@example.org", event.getString(MailContract.FIELD_ADDRESS))
        assertEquals("INBOX", event.getString(MailContract.FIELD_FOLDER))
        assertTrue(event.getLong(MailContract.FIELD_RECEIVED_AT) > 0)
        val message = event.getJSONObject(MailContract.FIELD_MESSAGE)
        assertEquals("hello", message.getString("subject"))
        assertEquals(1L, message.getLong("uid"))
        assertFalse(event.toString().contains(SECRET))

        imap.push("second")
        val second = events.nextMail()
        assertEquals(2L, second.second)
        assertEquals("second", second.third.getJSONObject(MailContract.FIELD_MESSAGE).getString("subject"))

        val listed = JSONObject(requireNotNull(binder().listTriggers().getString(MailContract.KEY_TRIGGERS_JSON)))
        val entries = listed.getJSONArray(MailContract.FIELD_TRIGGERS)
        val entry = (0 until entries.length()).map { entries.getJSONObject(it) }.single { it.getString(MailContract.FIELD_TRIGGER_ID) == TRIGGER }
        assertEquals(ALIAS, entry.getString(MailContract.FIELD_ALIAS))
        assertEquals("alice@example.org", entry.getString(MailContract.FIELD_ADDRESS))
        assertTrue(entry.getBoolean(MailContract.FIELD_ENABLED))
        assertEquals(2, entry.getInt("records"))
        assertEquals(MailContract.TRIGGER_STATE_CONNECTED, entry.getJSONObject("status").getString(MailContract.FIELD_STATE))
        assertEquals(MailContract.TRIGGER_STATE_CONNECTED, status(trigger).getString(MailContract.FIELD_STATE))
        assertEquals(1, WatchKeeper.of(context).subscribers(TRIGGER))

        trigger.stop()
        assertEquals(0, WatchKeeper.of(context).subscribers(TRIGGER))
        imap.push("after stop")
        assertTrue("nothing after stop", events.quietFor(1_500))
        assertEquals("the watch keeps running for the store", MailContract.TRIGGER_STATE_CONNECTED, status(trigger).getString(MailContract.FIELD_STATE))
        trigger.stop()
    }

    @Test
    fun aSubscriberFilterNarrowsItsEventsAndUpdateReplacesIt() {
        val events = Events()
        val trigger = open(events, 3, """{"triggerId":"$TRIGGER","filter":{"subject":["invoice"]}}""")
        awaitConnected(events)
        imap.awaitIdling(1)
        imap.push("hello")
        assertTrue("a message outside the filter is not delivered", events.quietFor(1_500))
        imap.push("Invoice 42")
        assertEquals("Invoice 42", events.nextMail().third.getJSONObject(MailContract.FIELD_MESSAGE).getString("subject"))

        trigger.update(options(3, """{"triggerId":"$TRIGGER"}"""))
        imap.push("plain")
        assertEquals("plain", events.nextMail().third.getJSONObject(MailContract.FIELD_MESSAGE).getString("subject"))
        try {
            trigger.update(options(3, """{"triggerId":"other"}"""))
            throw AssertionError("another watch must be refused")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty(), e.message.orEmpty().contains(TRIGGER))
        }
    }

    @Test
    fun aDisabledOrUnknownWatchAndBadOptionsAreRefusedWithAStoppedStatus() {
        val disabled = Events()
        assertNull(binder().openTrigger(options(1, """{"triggerId":"$DISABLED"}"""), disabled.callback))
        val refused = disabled.nextStatus()
        assertEquals(MailContract.TRIGGER_STATE_STOPPED, refused.getString(MailContract.FIELD_STATE))
        assertEquals("refused", refused.getString(MailContract.FIELD_REASON))
        assertEquals(DISABLED, refused.getString(MailContract.FIELD_TRIGGER_ID))
        assertEquals(MailErrorCodes.WATCH_CLOSED, refused.getJSONObject(MailContract.FIELD_LAST_ERROR).getString(MailContract.FIELD_ERROR_CODE))

        val unknown = Events()
        assertNull(binder().openTrigger(options(1, """{"triggerId":"nobody"}"""), unknown.callback))
        assertEquals(MailErrorCodes.INVALID_ARGUMENT, unknown.nextStatus().getJSONObject(MailContract.FIELD_LAST_ERROR).getString(MailContract.FIELD_ERROR_CODE))

        val bad = Events()
        assertNull(binder().openTrigger(options(1, """{"triggerId":"$TRIGGER","interval":5}"""), bad.callback))
        assertEquals(MailErrorCodes.INVALID_ARGUMENT, bad.nextStatus().getJSONObject(MailContract.FIELD_LAST_ERROR).getString(MailContract.FIELD_ERROR_CODE))
        assertNull(binder().openTrigger(options(1), null))
    }

    private companion object {
        const val ALIAS = "trigger-binder"
        const val TRIGGER = "binder-watch"
        const val DISABLED = "binder-disabled"
        const val SECRET = "trigger-secret"
    }
}
