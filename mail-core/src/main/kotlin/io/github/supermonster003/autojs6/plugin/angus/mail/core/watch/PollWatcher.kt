package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.imapUid
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.ImapMailbox
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.ProtocolTrace
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.SocketRegistry
import jakarta.mail.Folder
import org.eclipse.angus.mail.imap.IMAPFolder

/** What a [PollWatcher] polls: an IMAP folder by UID (the POP3 mailbox by UIDL follows, roadmap D3 / P5). */
interface PollSource {
    /** One poll: a `resync` reason when the increment could not be determined, and the new messages, oldest first. */
    class Poll(val resync: String?, val messages: List<MessageDocument>)

    /** Connects and takes (or re-aligns) the snapshot; returns a `resync` reason when the increment since the previous connection is unknown. */
    fun connect(sockets: SocketRegistry, trace: ProtocolTrace): String?

    fun poll(): Poll

    /** Releases the connection; must not block on a dead socket (the caller closed the sockets already). */
    fun disconnect()
}

/**
 * Polling watch (roadmap P5): every `pollIntervalMs` the [PollSource] diffs the folder against
 * what it reported last (IMAP by UID). Losses and reconnects follow [AbstractWatcher]; the cursor
 * survives them, so nothing is reported twice. The [IdleWatcher] hands over to this watcher when
 * IDLE is unavailable.
 */
class PollWatcher(
    account: MailAccount,
    secret: MailSecret,
    options: WatchOptions,
    listener: WatchListener,
    config: WatchConfig = WatchConfig(),
    source: PollSource? = null,
) : AbstractWatcher(account, secret, options, listener, config, THREAD_NAME) {

    private val source: PollSource = source ?: ImapPollSource(account, secret, options.folder, options.fetchBody)

    /** Polls completed (tests). */
    @Volatile
    var pollCount: Int = 0
        private set

    override val mode: WatchMode get() = WatchMode.POLL

    override fun connect(): String? = source.connect(sockets, trace)

    override fun run() {
        while (stopReason == null) {
            val poll = source.poll()
            pollCount++
            poll.resync?.let { emit(WatchEvent.Resync(folder, it)) }
            deliver(poll.messages)
            pause(options.pollIntervalMs)
        }
    }

    override fun disconnect() {
        source.disconnect()
    }

    companion object {
        const val THREAD_NAME = "angus-mail-poll"
    }
}

/**
 * What an IMAP watch knows about its folder: the `UIDVALIDITY` it saw and the highest UID it
 * reported. Shared between an [IdleWatcher] and the [PollWatcher] it hands over to.
 */
internal class ImapCursor {
    @Volatile
    var uidValidity: Long = -1

    /** -1 until the first alignment. */
    @Volatile
    var lastUid: Long = -1

    /**
     * Aligns with the open [folder]: the first time, the watch starts after the newest message;
     * after a `UIDVALIDITY` change it starts over and returns the `resync` reason; otherwise the
     * cursor stands and the next fetch reports what arrived meanwhile.
     */
    fun align(mailbox: ImapMailbox, folder: IMAPFolder): String? {
        val validity = mailbox.uidValidity(folder)
        return when {
            lastUid < 0 -> {
                uidValidity = validity
                lastUid = mailbox.highestUid(folder)
                null
            }
            validity >= 0 && uidValidity >= 0 && validity != uidValidity -> {
                uidValidity = validity
                lastUid = mailbox.highestUid(folder)
                Watcher.RESYNC_UIDVALIDITY
            }
            else -> {
                if (validity >= 0) uidValidity = validity
                null
            }
        }
    }

    fun advance(messages: List<MessageDocument>) {
        messages.forEach { document -> document.imapUid?.let { if (it > lastUid) lastUid = it } }
    }
}

/** IMAP polling: `SELECT` + `UID FETCH lastUid+1:*` per poll on a connection kept between polls. */
class ImapPollSource internal constructor(
    private val account: MailAccount,
    private val secret: MailSecret,
    private val folder: String,
    private val fetchBody: Boolean,
    private val cursor: ImapCursor,
) : PollSource {

    constructor(account: MailAccount, secret: MailSecret, folder: String, fetchBody: Boolean) : this(account, secret, folder, fetchBody, ImapCursor())

    private var mailbox: ImapMailbox? = null

    override fun connect(sockets: SocketRegistry, trace: ProtocolTrace): String? {
        val box = ImapMailbox.connect(account, secret, trace, sockets)
        mailbox = box
        return box.withFolder(folder, Folder.READ_ONLY) { open -> cursor.align(box, open) }
    }

    override fun poll(): PollSource.Poll {
        val box = mailbox ?: throw IllegalStateException("the poll source is not connected")
        return box.withFolder(folder, Folder.READ_ONLY) { open ->
            val resync = cursor.align(box, open)
            PollSource.Poll(resync, box.messagesAfter(open, cursor.lastUid, fetchBody).also { cursor.advance(it) })
        }
    }

    override fun disconnect() {
        mailbox?.let { runCatching { it.close() } }
        mailbox = null
    }
}
