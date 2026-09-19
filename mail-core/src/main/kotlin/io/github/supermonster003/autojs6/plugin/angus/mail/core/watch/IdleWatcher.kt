package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.ConnectionGuard
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.ImapMailbox
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.ProtocolTrace
import jakarta.mail.Folder
import jakarta.mail.MessagingException
import org.eclipse.angus.mail.imap.IMAPFolder

/**
 * IMAP IDLE watch (roadmap P5, D15 / D17): its own store connection, the folder kept open,
 * `IDLE` re-issued after every untagged response and broken every `IDLE_RENEW_MS` by a renew
 * thread (`DONE` + `NOOP`, RFC 2177 asks for 29 minutes at most). The account's read timeout
 * stays: Angus ignores socket timeouts while idling (`IMAPFolder.handleIdle` reads again), and
 * the renew `NOOP` detects a dead socket within that timeout. New mail is found by UID, not by
 * message-count events: after every wake-up and after every reconnect `UID FETCH lastUid+1:*`
 * reports what arrived, so nothing is duplicated and nothing is lost while the connection was
 * down. A changed `UIDVALIDITY` resets the cursor and emits `resync`. When the server does not
 * advertise IDLE, or the IDLE connection is lost `IDLE_FAILURE_LIMIT` times in a row within
 * the healthy window (`IDLE_HEALTHY_MS`, or one and a half read timeouts if that is longer: Angus
 * reads on after a tagged `NO` to IDLE until the continuation or the read timeout, so a refusal
 * surfaces as a loss at exactly the read timeout), the watch hands over to a [PollWatcher] on
 * the same cursor and emits `mode: poll`.
 */
class IdleWatcher(
    account: MailAccount,
    secret: MailSecret,
    options: WatchOptions,
    listener: WatchListener,
    config: WatchConfig = WatchConfig(),
) : AbstractWatcher(account, secret, options, listener, config, THREAD_NAME) {

    private val cursor = ImapCursor()
    private var mailbox: ImapMailbox? = null
    private var open: IMAPFolder? = null
    private var idleFailures = 0
    private val healthyMs = maxOf(config.idleHealthyMs, account.timeouts.readMillis * 3 / 2)

    @Volatile
    private var delegate: PollWatcher? = null

    /** Times the loop entered IDLE (tests). */
    @Volatile
    var idleCount: Int = 0
        private set

    /** Times the renew thread broke an IDLE (tests). */
    @Volatile
    var renewCount: Int = 0
        private set

    override val mode: WatchMode get() = if (delegate != null) WatchMode.POLL else WatchMode.IDLE

    override val isActive: Boolean get() = delegate?.isActive ?: super.isActive

    override val isClosed: Boolean get() = delegate?.isClosed ?: super.isClosed

    override fun status(): WatchStatus = delegate?.status() ?: super.status()

    override fun stop(reason: String) {
        synchronized(lifecycle) {
            super.stop(reason)
            delegate?.stop(reason)
        }
    }

    override fun reconnect(reason: String) {
        delegate?.reconnect(reason) ?: super.reconnect(reason)
    }

    // ------------------------------------------------------------------ loop

    override fun connect(): String? {
        val box = ImapMailbox.connect(account, secret, trace, sockets)
        mailbox = box
        if (!box.hasCapability("IDLE")) throw IdleUnavailable("the server does not advertise IDLE")
        val folder = box.openFolder(this.folder, Folder.READ_ONLY)
        open = folder
        return cursor.align(box, folder)
    }

    override fun run() {
        val box = mailbox ?: return
        val folder = open ?: return
        collect(box, folder)
        while (stopReason == null) {
            val entered = config.clock()
            val renewer = Renewer(folder, config.idleRenewMs, trace) { renewCount++ }
            renewer.start()
            try {
                idleCount++
                trace.timed(MailProtocol.IMAP.id, "idle ${this.folder}") { folder.idle(true) }
            } catch (e: MessagingException) {
                if (stopReason != null) return
                if (isReconnecting) throw e
                val loss = ConnectionGuard.isConnectionLoss(e)
                if (!loss || config.clock() - entered < healthyMs) idleFailures++
                if (idleFailures >= config.idleFailureLimit) {
                    throw IdleUnavailable(
                        if (loss) "the IDLE connection dropped $idleFailures times in a row" else "the server refused IDLE $idleFailures times in a row",
                        e,
                    )
                }
                if (loss) throw e
                trace.record(MailProtocol.IMAP.id, "idle refused ($idleFailures): ${e.message}")
                continue
            } finally {
                renewer.cancel()
            }
            idleFailures = 0
            collect(box, folder)
        }
    }

    override fun disconnect() {
        open?.let { folder -> runCatching { if (folder.isOpen) folder.close(false) } }
        open = null
        mailbox?.let { runCatching { it.close() } }
        mailbox = null
    }

    /** IDLE is unavailable: continue on the same cursor with polling, unless a stop won the race. */
    override fun handOff(cause: Throwable): Boolean {
        if (cause !is IdleUnavailable) return false
        synchronized(lifecycle) {
            if (stopReason != null) return false
            trace.record(MailProtocol.IMAP.id, "idle unavailable, polling: ${cause.message}")
            val poller = PollWatcher(account, secret, options, listener, config, ImapPollSource(account, secret, folder, options.fetchBody, cursor))
            delegate = poller
            emit(WatchEvent.Mode(folder, WatchMode.POLL))
            poller.start()
            return true
        }
    }

    /** Reports everything above the cursor; repeats while a message arrived during the fetch (its `EXISTS` came with that fetch's responses, not with the next IDLE). */
    private fun collect(box: ImapMailbox, folder: IMAPFolder) {
        do {
            val batch = box.messagesAfter(folder, cursor.lastUid, options.fetchBody)
            cursor.advance(batch)
            deliver(batch)
        } while (stopReason == null && box.hasMessagesAfter(folder, cursor.lastUid))
    }

    /** Breaks the IDLE with `DONE` + `NOOP` after [delayMs], so that the loop re-issues it (roadmap D17). */
    private class Renewer(
        private val folder: IMAPFolder,
        private val delayMs: Long,
        private val trace: ProtocolTrace,
        private val onRenew: () -> Unit,
    ) : Thread(RENEW_THREAD_NAME) {

        init {
            isDaemon = true
        }

        override fun run() {
            try {
                sleep(delayMs)
            } catch (_: InterruptedException) {
                return
            }
            try {
                trace.timed(MailProtocol.IMAP.id, "idle renew") {
                    // doCommand waits for the IDLE to end (it sends DONE itself) before the NOOP goes out.
                    folder.doCommand { protocol ->
                        protocol.noop()
                        null
                    }
                }
                onRenew()
            } catch (_: Exception) {
            }
        }

        fun cancel() {
            interrupt()
        }
    }

    /** Thrown inside the loop when the watch must continue by polling. */
    internal class IdleUnavailable(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

    companion object {
        const val THREAD_NAME = "angus-mail-idle"
        const val RENEW_THREAD_NAME = "angus-mail-idle-renew"
    }
}
