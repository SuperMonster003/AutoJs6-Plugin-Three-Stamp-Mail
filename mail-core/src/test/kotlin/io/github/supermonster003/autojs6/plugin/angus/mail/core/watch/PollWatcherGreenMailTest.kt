package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

import com.icegreen.greenmail.util.GreenMail
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.ProviderPresets
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageArgs
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSession
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchTestSupport.BOB_PASSWORD
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchTestSupport.FAST
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchTestSupport.await
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchTestSupport.bob
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchTestSupport.deliver
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchTestSupport.newServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Roadmap P5 `PollWatcher` against GreenMail: IMAP polls diff by UID, POP3 polls diff by UIDL
 * (string uids, additions only, the maildrop is free between polls), a requested reconnect keeps
 * the cursor, and `Watchers.open` picks the poller for POP3 accounts, for `mode: "poll"` and for
 * presets whose server does not push through IDLE (`idlePush = false`) unless `idle` is asked for.
 */
class PollWatcherGreenMailTest {

    private lateinit var greenMail: GreenMail
    private val watchers = ArrayList<Watcher>()

    @Before
    fun startServer() {
        greenMail = newServer()
    }

    @After
    fun stopServer() {
        watchers.forEach { runCatching { it.stop() } }
        greenMail.stop()
    }

    private fun poll(events: Events, receive: MailProtocol, options: WatchOptions = WatchOptions(mode = WatchMode.POLL, pollIntervalMs = 300)): PollWatcher =
        (Watchers.open(bob(receive), MailSecret(BOB_PASSWORD), options, events, FAST) as PollWatcher).also { watchers += it }

    @Test
    fun imapPollingReportsNewMailByUidOnce() {
        deliver(greenMail, "before", 1)
        val events = Events()
        val watcher = poll(events, MailProtocol.IMAP)
        assertEquals(WatchMode.POLL, watcher.mode)
        watcher.start()
        await(what = "first poll") { watcher.pollCount >= 1 }
        assertTrue("the backlog is not reported", events.quietFor(400))

        deliver(greenMail, "first", 2)
        deliver(greenMail, "second", 3)
        assertEquals("first", events.nextOf<WatchEvent.Message>().message.subject)
        assertEquals("second", events.nextOf<WatchEvent.Message>().message.subject)
        val polls = watcher.pollCount
        await(what = "three more polls") { watcher.pollCount >= polls + 3 }
        assertEquals("no duplicates over later polls: ${events.snapshot()}", 2, events.messages().size)
        assertEquals(listOf("2", "3"), events.messages().map { it.message.uid.content })

        watcher.reconnect("test")
        await(what = "reconnect") { watcher.connectCount >= 2 }
        deliver(greenMail, "third", 4)
        assertEquals("third", events.nextOf<WatchEvent.Message>().message.subject)
        assertEquals(0, events.count { it is WatchEvent.Error })
        watcher.stop()
        assertEquals(Watcher.REASON_STOPPED, events.nextOf<WatchEvent.Closed>().reason)
    }

    @Test
    fun pop3PollingReportsAdditionsByUidlAndNeverDeletions() {
        deliver(greenMail, "before", 1)
        val events = Events()
        val watcher = poll(events, MailProtocol.POP3)
        watcher.start()
        await(what = "first poll") { watcher.pollCount >= 1 }
        assertTrue(events.quietFor(400))

        deliver(greenMail, "first", 2)
        val first = events.nextOf<WatchEvent.Message>().message
        assertEquals("first", first.subject)
        assertTrue("POP3 uids are UIDL strings: ${first.uid}", first.uid.isString)
        assertFalse(first.bodyLoaded)

        // delete the older message through the session API (POP3 delete commits on close), then add another
        MailSession(bob(MailProtocol.POP3), MailSecret(BOB_PASSWORD)).use { session ->
            val listed = session.listMessages(MessageArgs.list("""{"limit":10}""", MailProtocol.POP3))
            val older = listed.first { it.subject == "before" }
            session.delete(MessageArgs.delete("""{"uids":["${older.uid.content}"],"expunge":true}""", MailProtocol.POP3))
        }
        deliver(greenMail, "second", 2)
        val second = events.nextOf<WatchEvent.Message>().message
        assertEquals("second", second.subject)
        val polls = watcher.pollCount
        await(what = "two more polls") { watcher.pollCount >= polls + 2 }
        assertEquals("the deletion produced nothing: ${events.snapshot()}", 2, events.messages().size)
        assertEquals(0, events.count { it is WatchEvent.Error })

        // between polls the maildrop is not locked: another POP3 login succeeds while the watch runs
        MailSession(bob(MailProtocol.POP3), MailSecret(BOB_PASSWORD)).use { session ->
            assertEquals(2, session.listMessages(MessageArgs.list("""{"limit":10}""", MailProtocol.POP3)).size)
        }
        watcher.stop()
        assertEquals(Watcher.REASON_STOPPED, events.nextOf<WatchEvent.Closed>().reason)
    }

    @Test
    fun pop3PollingCanFetchTheBody() {
        val events = Events()
        val watcher = poll(events, MailProtocol.POP3, WatchOptions(mode = WatchMode.POLL, pollIntervalMs = 300, fetchBody = true))
        watcher.start()
        await(what = "first poll") { watcher.pollCount >= 1 }
        deliver(greenMail, "with body", 1, body = "pop body")
        val message = events.nextOf<WatchEvent.Message>().message
        assertTrue(message.bodyLoaded)
        assertEquals("pop body", message.text?.trim())
    }

    @Test
    fun theFactoryChoosesByProtocolAndMode() {
        val secret = MailSecret(BOB_PASSWORD)
        assertTrue(Watchers.open(bob(), secret, WatchOptions(), Events(), FAST) is IdleWatcher)
        assertTrue(Watchers.open(bob(), secret, WatchOptions(mode = WatchMode.IDLE), Events(), FAST) is IdleWatcher)
        assertTrue(Watchers.open(bob(), secret, WatchOptions(mode = WatchMode.POLL), Events(), FAST) is PollWatcher)
        assertTrue(Watchers.open(bob(MailProtocol.POP3), secret, WatchOptions(), Events(), FAST) is PollWatcher)
        val silent = bob().copy(provider = ProviderPresets.require("qq"))
        assertFalse(silent.provider!!.idlePush)
        assertTrue(Watchers.open(silent, secret, WatchOptions(), Events(), FAST) is PollWatcher)
        assertTrue(Watchers.open(silent, secret, WatchOptions(mode = WatchMode.IDLE), Events(), FAST) is IdleWatcher)
        assertTrue(Watchers.open(bob().copy(provider = ProviderPresets.require("gmail")), secret, WatchOptions(), Events(), FAST) is IdleWatcher)
        try {
            Watchers.open(bob(MailProtocol.POP3), secret, WatchOptions(mode = WatchMode.IDLE), Events(), FAST)
            org.junit.Assert.fail("POP3 has no IDLE")
        } catch (e: io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException) {
            assertEquals(io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode.UNSUPPORTED_OPERATION, e.code)
        }
    }
}
