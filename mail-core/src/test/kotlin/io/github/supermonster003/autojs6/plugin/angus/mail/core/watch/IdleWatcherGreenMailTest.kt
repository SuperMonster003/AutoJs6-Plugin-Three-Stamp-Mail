package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

import com.icegreen.greenmail.util.GreenMail
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
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
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Roadmap P5 `IdleWatcher` against GreenMail (which advertises and pushes IDLE): new mail
 * arrives as `message` events in order and once, the renew thread re-issues IDLE without losing
 * mail, a dropped connection reconnects with the cursor intact, a server restart (new
 * `UIDVALIDITY`) resets it with a `resync`, `stop` and the session's `close` end the watch with
 * exactly one `closed` event, and the session refuses the fifth watch.
 */
class IdleWatcherGreenMailTest {

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

    private fun watch(events: Events, options: WatchOptions = WatchOptions(), config: WatchConfig = FAST): IdleWatcher =
        IdleWatcher(bob(), MailSecret(BOB_PASSWORD), options, events, config).also { watchers += it }

    private fun awaitIdle(watcher: IdleWatcher, count: Int = 1) = await(what = "IDLE entered $count times") { watcher.idleCount >= count }

    @Test
    fun newMailArrivesThroughIdleInOrderAndOnlyOnce() {
        deliver(greenMail, "before the watch", 1)
        val events = Events()
        val watcher = watch(events)
        assertFalse(watcher.isActive)
        assertEquals(WatchMode.IDLE, watcher.mode)
        watcher.start()
        assertTrue(watcher.isActive)
        awaitIdle(watcher)
        assertTrue("the backlog is not reported", events.quietFor(300))

        deliver(greenMail, "first", 2)
        val first = events.nextOf<WatchEvent.Message>()
        assertEquals("first", first.message.subject)
        assertEquals("INBOX", first.folder)
        assertEquals("2", first.message.uid.content)
        assertFalse(first.message.bodyLoaded)

        deliver(greenMail, "second", 3)
        deliver(greenMail, "third", 4)
        assertEquals("second", events.nextOf<WatchEvent.Message>().message.subject)
        assertEquals("third", events.nextOf<WatchEvent.Message>().message.subject)
        assertTrue("no duplicate: ${events.snapshot()}", events.quietFor(500))
        assertEquals(listOf("first", "second", "third"), events.messages().map { it.message.subject })
        assertEquals(1, watcher.connectCount)

        watcher.stop()
        val closed = events.nextOf<WatchEvent.Closed>()
        assertEquals(Watcher.REASON_STOPPED, closed.reason)
        await(what = "watcher closed") { watcher.isClosed }
        assertFalse(watcher.isActive)
        assertEquals(WatchStatus(false, WatchMode.IDLE, Watcher.REASON_STOPPED, null), watcher.status())
        assertTrue("nothing after closed", events.quietFor(300))
        assertEquals(1, events.count { it is WatchEvent.Closed })
        assertTrue(watcher.trace.snapshot().none { it.contains(BOB_PASSWORD) })
    }

    @Test
    fun theBodyComesAlongWhenAsked() {
        val events = Events()
        val watcher = watch(events, WatchOptions(fetchBody = true))
        watcher.start()
        awaitIdle(watcher)
        deliver(greenMail, "with body", 1, body = "the body text")
        val message = events.nextOf<WatchEvent.Message>().message
        assertTrue(message.bodyLoaded)
        assertEquals("the body text", message.text?.trim())
    }

    @Test
    fun theRenewThreadReissuesIdleWithoutLosingMail() {
        val events = Events()
        val watcher = watch(events, config = FAST.copy(idleRenewMs = 400))
        watcher.start()
        await(timeoutMs = 10_000, what = "two renews") { watcher.renewCount >= 2 }
        assertTrue("IDLE was re-entered after the renews", watcher.idleCount >= 3)
        assertEquals("the renew keeps the connection", 1, watcher.connectCount)
        deliver(greenMail, "after renew", 1)
        assertEquals("after renew", events.nextOf<WatchEvent.Message>().message.subject)
        assertTrue(events.count { it is WatchEvent.Error } == 0)
    }

    @Test
    fun aDroppedConnectionReconnectsAndReportsWhatArrivedMeanwhileOnce() {
        val events = Events()
        val watcher = watch(events)
        watcher.start()
        awaitIdle(watcher)
        deliver(greenMail, "before the drop", 1)
        assertEquals("before the drop", events.nextOf<WatchEvent.Message>().message.subject)

        val idles = watcher.idleCount
        watcher.reconnect("test")
        await(what = "reconnect") { watcher.connectCount >= 2 }
        awaitIdle(watcher, idles + 1)
        assertTrue("a requested reconnect is not an error: ${events.snapshot()}", events.count { it is WatchEvent.Error } == 0)
        deliver(greenMail, "after the drop", 2)
        assertEquals("after the drop", events.nextOf<WatchEvent.Message>().message.subject)
        assertTrue("no duplicate after the reconnect: ${events.snapshot()}", events.quietFor(500))
        assertEquals(2, events.messages().size)
    }

    @Test
    fun aServerOutageIsReportedThenRecoveredWithAResyncForTheNewUidValidity() {
        val events = Events()
        val watcher = watch(events)
        watcher.start()
        awaitIdle(watcher)
        deliver(greenMail, "old", 1)
        assertEquals("old", events.nextOf<WatchEvent.Message>().message.subject)

        greenMail.stop()
        val error = events.nextOf<WatchEvent.Error>()
        assertTrue("a loss is retried: ${error.error}", error.error.code in setOf(MailErrorCode.CONNECT_FAILED, MailErrorCode.IO_FAILED, MailErrorCode.TIMEOUT))
        assertTrue(watcher.isActive)
        // GreenMail derives UIDVALIDITY from the folder's creation second: the restarted server has a new one.
        Thread.sleep(1_100)
        greenMail = newServer()
        val resync = events.nextOf<WatchEvent.Resync>(timeoutSeconds = 20)
        assertEquals(Watcher.RESYNC_UIDVALIDITY, resync.reason)
        assertTrue(watcher.connectCount >= 2)
        awaitIdle(watcher, watcher.idleCount + 1)
        deliver(greenMail, "new", 1)
        val message = events.nextOf<WatchEvent.Message>()
        assertEquals("new", message.message.subject)
        // the fresh mailbox numbers from 1 again; without the resync the cursor would have hidden it
        assertEquals("1", message.message.uid.content)
        assertTrue(events.quietFor(300))
    }

    @Test
    fun theSessionCapsWatchesAndClosesThemWithItself() {
        val session = MailSession(bob(), MailSecret(BOB_PASSWORD))
        val events = (1..4).map { Events() }
        val opened = events.map { session.watch(WatchOptions(), it, FAST).also { watcher -> watcher.start() } }
        assertEquals(4, session.watchCount)
        try {
            session.watch(WatchOptions(), Events(), FAST)
            fail("the fifth watch must be refused")
        } catch (e: io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException) {
            assertEquals(MailErrorCode.LIMIT_EXCEEDED, e.code)
            assertEquals(e, session.lastError)
        }
        opened.forEach { (it as IdleWatcher).let { watcher -> awaitIdle(watcher) } }
        opened.first().stop()
        assertEquals(Watcher.REASON_STOPPED, events.first().nextOf<WatchEvent.Closed>().reason)
        await(what = "watch count drops") { session.watchCount == 3 }
        // a slot is free again
        val fifth = session.watch(WatchOptions(), Events(), FAST)
        assertEquals(4, session.watchCount)
        assertFalse(fifth.isActive)

        session.close()
        events.drop(1).forEach { assertEquals(Watcher.REASON_SESSION_CLOSED, it.nextOf<WatchEvent.Closed>().reason) }
        await(what = "all watches closed") { opened.all { it.isClosed } && fifth.isClosed }
        assertEquals(0, session.watchCount)
        try {
            session.watch(WatchOptions(), Events(), FAST)
            fail("a closed session opens no watch")
        } catch (e: io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException) {
            assertEquals(MailErrorCode.SESSION_CLOSED, e.code)
        }
    }

    @Test
    fun stoppingBeforeStartingClosesAtOnce() {
        val events = Events()
        val watcher = watch(events)
        watcher.stop("never started")
        assertEquals(WatchEvent.Closed("INBOX", "never started"), events.next())
        assertTrue(watcher.isClosed)
        watcher.start()
        assertFalse(watcher.isActive)
        assertTrue(events.quietFor(200))
    }
}
