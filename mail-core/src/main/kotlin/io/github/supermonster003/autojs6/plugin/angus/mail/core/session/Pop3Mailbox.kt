package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MessageSummary
import jakarta.mail.FetchProfile
import jakarta.mail.Folder
import jakarta.mail.MessagingException
import org.eclipse.angus.mail.pop3.POP3Store
import java.io.Closeable

/** One connected POP3 store. POP3 exposes a single read-only INBOX; roadmap P2.4 adds the degraded operation set. */
class Pop3Mailbox private constructor(
    private val store: POP3Store,
    private val trace: ProtocolTrace,
) : Closeable {

    val isConnected: Boolean get() = store.isConnected

    /** Keys of the server's `CAPA` reply (RFC 2449), empty when the server does not support `CAPA`. */
    fun capabilities(): List<String> = try {
        store.capabilities()?.keys?.sorted().orEmpty()
    } catch (_: MessagingException) {
        emptyList()
    }

    /** Newest [limit] messages, newest first. POP3 has no IMAP-style UIDs, so `uid` is null. */
    fun listInbox(limit: Int): List<MessageSummary> = withInbox(Folder.READ_ONLY) { folder ->
        val total = folder.messageCount
        if (total == 0 || limit <= 0) return@withInbox emptyList()
        val messages = folder.getMessages(maxOf(1, total - limit + 1), total)
        folder.fetch(messages, FetchProfile().apply {
            add(FetchProfile.Item.ENVELOPE)
            add(FetchProfile.Item.SIZE)
        })
        messages.map { MessageSummary.of(it) }.asReversed()
    }

    fun <T> withInbox(mode: Int, block: (Folder) -> T): T {
        val folder = store.getFolder(ImapMailbox.INBOX)
        trace.timed(MailProtocol.POP3.id, "open INBOX ${if (mode == Folder.READ_WRITE) "rw" else "ro"}") { folder.open(mode) }
        try {
            return block(folder)
        } finally {
            // POP3 applies DELE on close(true) only; a read-only open never expunges.
            if (folder.isOpen) folder.close(mode == Folder.READ_WRITE)
        }
    }

    override fun close() {
        if (store.isConnected) {
            try {
                store.close()
            } catch (_: MessagingException) {
            }
        }
    }

    companion object {
        fun connect(account: MailAccount, secret: MailSecret, trace: ProtocolTrace = ProtocolTrace.disabled()): Pop3Mailbox =
            Pop3Mailbox(MailSessionFactory.connectStore(account, MailProtocol.POP3, secret, trace) as POP3Store, trace)
    }
}
