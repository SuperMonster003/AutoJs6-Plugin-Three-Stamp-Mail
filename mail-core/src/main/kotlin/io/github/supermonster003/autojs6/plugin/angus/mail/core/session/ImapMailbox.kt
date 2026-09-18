package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MessageSummary
import jakarta.mail.FetchProfile
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.MessagingException
import jakarta.mail.UIDFolder
import jakarta.mail.internet.MimeMessage
import org.eclipse.angus.mail.imap.IMAPFolder
import org.eclipse.angus.mail.imap.IMAPStore
import java.io.Closeable

/**
 * One connected IMAP store. P2.1 surface: capabilities, the `ID` handshake, folder access, and
 * the inbox listing of the P0 spike; P2.2 adds `APPEND` and the sent-folder lookup; roadmap P2.3
 * adds the full operation set on top.
 */
class ImapMailbox private constructor(
    private val store: IMAPStore,
    private val trace: ProtocolTrace,
) : Closeable {

    val isConnected: Boolean get() = store.isConnected

    /** What the server answered to the `ID` command, or null when it was not sent or not supported. */
    var serverId: Map<String, String>? = null
        private set

    fun hasCapability(name: String): Boolean = store.hasCapability(name)

    /** The advertised capabilities the plugin cares about, in [KNOWN_CAPABILITIES] order. */
    fun capabilities(): List<String> = KNOWN_CAPABILITIES.filter { store.hasCapability(it) }

    /** Newest [limit] messages of INBOX, newest first, without marking them as read. */
    fun listInbox(limit: Int): List<MessageSummary> = withFolder(INBOX, Folder.READ_ONLY) { folder ->
        val total = folder.messageCount
        if (total == 0 || limit <= 0) return@withFolder emptyList()
        val messages = folder.getMessages(maxOf(1, total - limit + 1), total)
        folder.fetch(messages, FetchProfile().apply {
            add(FetchProfile.Item.ENVELOPE)
            add(FetchProfile.Item.FLAGS)
            add(FetchProfile.Item.SIZE)
            add(UIDFolder.FetchProfileItem.UID)
        })
        messages.map { MessageSummary.of(it) }.asReversed()
    }

    fun <T> withFolder(name: String, mode: Int, block: (IMAPFolder) -> T): T {
        val folder = store.getFolder(name) as IMAPFolder
        trace.timed(MailProtocol.IMAP.id, "open $name ${if (mode == Folder.READ_WRITE) "rw" else "ro"}") { folder.open(mode) }
        try {
            return block(folder)
        } finally {
            if (folder.isOpen) folder.close(false)
        }
    }

    fun folderExists(name: String): Boolean = trace.timed(MailProtocol.IMAP.id, "exists $name") { store.getFolder(name).exists() }

    /**
     * The first folder that carries the special-use attribute [attribute] (RFC 6154, e.g. `\Sent`)
     * in the server's `LIST` reply, or null. Servers without SPECIAL-USE return no attributes.
     */
    fun findSpecialUse(attribute: String): String? = trace.timed(MailProtocol.IMAP.id, "list special-use $attribute") {
        store.defaultFolder.list("*")
            .filterIsInstance<IMAPFolder>()
            .firstOrNull { folder -> runCatching { folder.attributes.any { it.equals(attribute, ignoreCase = true) } }.getOrDefault(false) }
            ?.fullName
    }

    /**
     * Appends [mime] to [folder] with [flags] and returns its UID when the server supports
     * `UIDPLUS`, otherwise null. The folder must exist; a missing one surfaces as `FOLDER_NOT_FOUND`.
     */
    fun append(folder: String, mime: MimeMessage, flags: Flags): Long? {
        mime.setFlags(flags, true)
        return withFolder(folder, Folder.READ_WRITE) { target ->
            trace.timed(MailProtocol.IMAP.id, "append $folder") {
                if (store.hasCapability("UIDPLUS")) {
                    target.appendUIDMessages(arrayOf(mime)).firstOrNull()?.uid?.takeIf { it >= 0 }
                } else {
                    target.appendMessages(arrayOf(mime))
                    null
                }
            }
        }
    }

    /**
     * Sends the `ID` command (RFC 2971) with [clientId]. Servers that do not advertise `ID` are
     * skipped; a server that rejects the command does not fail the connection either, because the
     * providers that require it (163 / 126) refuse the *next* command instead, which then surfaces
     * as the real error.
     */
    private fun identify(clientId: Map<String, String>) {
        if (clientId.isEmpty() || !store.hasCapability("ID")) return
        try {
            serverId = trace.timed(MailProtocol.IMAP.id, "ID ${clientId.keys.joinToString(",")}") { store.id(clientId) }
        } catch (_: MessagingException) {
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
        const val INBOX = "INBOX"

        /** Capabilities reported by `session.test`; the plugin's behaviour depends on the first block. */
        val KNOWN_CAPABILITIES: List<String> = listOf(
            "IDLE", "UIDPLUS", "MOVE", "CONDSTORE", "QRESYNC", "ID", "ENABLE", "NAMESPACE",
            "SPECIAL-USE", "LIST-EXTENDED", "LIST-STATUS", "UNSELECT", "CHILDREN", "XLIST",
            "ESEARCH", "SORT", "THREAD=REFERENCES", "WITHIN", "LITERAL+", "LITERAL-", "BINARY",
            "COMPRESS=DEFLATE", "UTF8=ACCEPT", "AUTH=PLAIN", "AUTH=XOAUTH2",
        )

        /** Names tried for the sent folder when neither the preset nor a special-use attribute names it. */
        val SENT_FOLDER_CANDIDATES: List<String> = listOf("Sent", "Sent Messages", "Sent Items", "INBOX.Sent", "INBOX/Sent", "已发送")

        fun connect(account: MailAccount, secret: MailSecret, trace: ProtocolTrace = ProtocolTrace.disabled()): ImapMailbox {
            val store = MailSessionFactory.connectStore(account, MailProtocol.IMAP, secret, trace) as IMAPStore
            return ImapMailbox(store, trace).also { it.identify(account.clientId) }
        }
    }
}
