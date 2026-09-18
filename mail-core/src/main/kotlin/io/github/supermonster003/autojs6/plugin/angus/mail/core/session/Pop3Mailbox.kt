package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.FolderDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SearchResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TransferResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.UidsResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MessageMapper
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MessageSummary
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MimeTree
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageArgs
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageUid
import jakarta.mail.FetchProfile
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.UIDFolder
import kotlinx.serialization.json.JsonPrimitive
import org.eclipse.angus.mail.pop3.POP3Folder
import org.eclipse.angus.mail.pop3.POP3Store
import java.io.Closeable
import java.io.OutputStream

/**
 * One connected POP3 store: the degraded receive path of roadmap D3 / P2.4. POP3 has a single
 * mailbox (`INBOX`), no flags, no folders, no server-side search and no stable numbering besides
 * the `UIDL` string of each message, so:
 *
 * - `folders.list` answers `INBOX` alone (with its message count when asked for status);
 * - `messages.list` pages by message number (newest last on the wire) and reports the UIDL as a
 *   string `uid`; the envelope comes from `TOP n 0` (headers only), `hasAttachments` is a
 *   Content-Type heuristic;
 * - `messages.search` filters headers on the client, newest first, one `TOP n 0` per candidate
 *   over at most `MAX_POP3_CLIENT_FILTER` messages, stopping once `limit` match; `body` / `text`
 *   / `uid` conditions are refused rather than downloading every candidate;
 * - `messages.get`, `messages.raw` and `attachments.download` download the whole message (`RETR`);
 * - `messages.delete` marks with `DELE` and commits when the folder closes at the end of the
 *   call, whatever `expunge` says, because POP3 has no "marked but kept" state;
 * - everything else (`folders.status` / `create` / `delete` / `rename`, `setFlags`, `move`,
 *   `copy`, `expunge`, `append`) is `UNSUPPORTED_OPERATION`, raised by [MailSession] and the
 *   argument parsers before any connection.
 *
 * Every operation opens `INBOX` and closes it again; a read-only open never issues `DELE`.
 * Angus Mail fetches POP3 headers and sizes one message at a time (`TOP n 0` + `LIST n`, two
 * round trips per envelope), so a page of `limit` envelopes costs `2 * limit` round trips: POP3
 * callers should keep `limit` small and page with `before`.
 */
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

    /** Newest [limit] messages, newest first (P0 surface, kept for the device tests). */
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

    fun <T> withInbox(mode: Int, block: (POP3Folder) -> T): T {
        val folder = store.getFolder(INBOX) as POP3Folder
        trace.timed(MailProtocol.POP3.id, "open INBOX ${if (mode == Folder.READ_WRITE) "rw" else "ro"}") { folder.open(mode) }
        try {
            return block(folder)
        } finally {
            // POP3 applies DELE on close(true) only; a read-only open never expunges.
            if (folder.isOpen) trace.timed(MailProtocol.POP3.id, "close INBOX${if (mode == Folder.READ_WRITE) " commit" else ""}") { folder.close(mode == Folder.READ_WRITE) }
        }
    }

    // ------------------------------------------------------------------ folders (P2.4)

    /** `folders.list`: the single POP3 mailbox; [status] adds its message count (`STAT`), `unseen` stays unknown. */
    fun listFolders(status: Boolean): List<FolderDocument> {
        val messages = if (status) withInbox(Folder.READ_ONLY) { trace.timed(MailProtocol.POP3.id, "stat") { it.messageCount } } else null
        return listOf(FolderDocument(name = INBOX, path = INBOX, delimiter = "/", specialUse = SpecialUse.INBOX.id, selectable = true, messages = messages, unseen = null))
    }

    // ------------------------------------------------------------------ messages (P2.4)

    /**
     * `messages.list`: one page of envelopes. Cursors are UIDLs; they map to the message numbers
     * of the current connection through one `UIDL` command for the whole mailbox, and a cursor
     * that no longer exists is an error because POP3 has no ordering to fall back on.
     */
    fun listMessages(args: MessageArgs.ListArgs): List<MessageDocument> = withInbox(Folder.READ_ONLY) { folder ->
        checkList(args)
        val total = folder.messageCount
        if (total == 0) return@withInbox emptyList()
        val uidls = uidls(folder)
        val upper = args.before?.let { position(uidls, it) - 1 } ?: total
        val lower = args.after?.let { position(uidls, it) + 1 } ?: 1
        if (upper < lower || upper < 1) return@withInbox emptyList()
        val numbers = (lower..upper).toList()
        val page = if (args.descending) numbers.takeLast(args.limit).asReversed() else numbers.take(args.limit)
        envelopes(folder, folder.getMessages(page.toIntArray()), uidls)
    }

    /**
     * `messages.search`: the compiled query evaluated on the client, newest first, over the
     * headers of at most `MAX_POP3_CLIENT_FILTER` messages (older than `before` when given).
     * POP3 has no batch header fetch, so every candidate costs one `TOP n 0` round trip; the
     * scan stops as soon as `limit` messages match, which keeps "find what just arrived" cheap
     * while a miss is bounded by the window. Conditions that need the body or IMAP UIDs are
     * refused instead of downloading every candidate.
     */
    fun search(args: MessageArgs.SearchArgs): SearchResult = withInbox(Folder.READ_ONLY) { folder ->
        checkSearch(args)
        val total = folder.messageCount
        if (total == 0) return@withInbox SearchResult(emptyList(), SearchResult.FALLBACK_CLIENT)
        val uidls = uidls(folder)
        val upper = args.before?.let { position(uidls, it) - 1 } ?: total
        if (upper < 1) return@withInbox SearchResult(emptyList(), SearchResult.FALLBACK_CLIENT)
        val lower = maxOf(1, upper - MailLimits.MAX_POP3_CLIENT_FILTER + 1)
        val term = args.query.clientTerm
        val matched = ArrayList<Message>(args.limit)
        val scanned = trace.timed(MailProtocol.POP3.id, "client filter window ${upper - lower + 1} limit ${args.limit}") {
            var scanned = 0
            for (number in upper downTo lower) {
                if (matched.size >= args.limit) break
                val message = folder.getMessage(number)
                // TOP n 0 (Angus loads the headers on first access); a lost connection surfaces here instead of as "no match"
                message.allHeaders
                scanned++
                if (term == null || term.match(message)) matched += message
            }
            scanned
        }
        trace.record(MailProtocol.POP3.id, "client filter scanned $scanned matched ${matched.size}")
        // headers are cached now; the fetch adds the sizes (LIST n) of the matched page only
        SearchResult(envelopes(folder, matched.toTypedArray(), uidls), SearchResult.FALLBACK_CLIENT)
    }

    /** `messages.get`: the full document after a `RETR`; `peek` is meaningless on POP3 and ignored. */
    fun getMessage(uid: MessageUid, includeRaw: Boolean = false): MessageDocument = withInbox(Folder.READ_ONLY) { folder ->
        val message = byUidl(folder, uid)
        trace.timed(MailProtocol.POP3.id, "retr ${message.messageNumber}") { MessageMapper.full(message, INBOX, JsonPrimitive(uid.text), includeRaw) }
    }

    /** `attachments.download`: `RETR`, then the decoded part [partId] streamed into [sink]. */
    fun downloadPart(uid: MessageUid, partId: String, sink: OutputStream, progress: TransferProgress): TransferResult = withInbox(Folder.READ_ONLY) { folder ->
        val message = byUidl(folder, uid)
        val leaf = MimeTree.find(message, partId) ?: throw MailException(MailErrorCode.ATTACHMENT_NOT_FOUND, "message ${uid.text} has no part $partId", retryable = false)
        val document = MessageMapper.attachment(leaf)
        val bytes = trace.timed(MailProtocol.POP3.id, "download ${message.messageNumber} part $partId") {
            leaf.part.inputStream.use { input -> Transfer.copy(input, sink, total = null, progress = progress) }
        }
        TransferResult(bytes, document.fileName, document.mimeType)
    }

    /** `messages.raw`: the RFC 822 source streamed from `RETR` into [sink]. */
    fun downloadRaw(uid: MessageUid, sink: OutputStream, progress: TransferProgress): TransferResult = withInbox(Folder.READ_ONLY) { folder ->
        val message = byUidl(folder, uid)
        val total = runCatching { message.size.toLong() }.getOrDefault(-1L).takeIf { it > 0 }
        val bytes = trace.timed(MailProtocol.POP3.id, "raw ${message.messageNumber}") {
            // POP3Message.writeTo streams RETR straight into the sink unless the content was loaded before.
            val counting = Transfer.counting(sink, total, progress)
            message.writeTo(counting)
            counting.finish()
        }
        TransferResult(bytes, "${uid.text}.eml", "message/rfc822")
    }

    /**
     * `messages.delete`: `DELE` for every UIDL that exists, committed by the `QUIT` when the
     * folder closes at the end of this call. Returns the UIDLs that were found.
     */
    fun delete(uids: List<MessageUid>): UidsResult = withInbox(Folder.READ_WRITE) { folder ->
        val uidls = uidls(folder)
        val found = uids.filter { it.text in uidls }
        if (found.isEmpty()) throw MailException(MailErrorCode.MESSAGE_NOT_FOUND, "none of the ${uids.size} uids exist in INBOX", retryable = false)
        trace.timed(MailProtocol.POP3.id, "dele ${found.size}") {
            found.forEach { uid -> folder.getMessage(uidls.getValue(uid.text)).setFlag(Flags.Flag.DELETED, true) }
        }
        UidsResult.pop3(found.map { it.text })
    }

    // ------------------------------------------------------------------ helpers

    /** UIDL -> message number of the open [folder], from one `UIDL` command; the server must support it. */
    private fun uidls(folder: POP3Folder): Map<String, Int> {
        val messages = folder.messages
        trace.timed(MailProtocol.POP3.id, "uidl ${messages.size}") { folder.fetch(messages, UID_PROFILE) }
        val result = LinkedHashMap<String, Int>(messages.size * 2)
        messages.forEach { message ->
            val uidl = folder.getUID(message) ?: throw MailException.unsupported("the POP3 server does not support UIDL; messages cannot be addressed")
            result[uidl] = message.messageNumber
        }
        return result
    }

    private fun position(uidls: Map<String, Int>, uid: MessageUid): Int =
        uidls[uid.text] ?: throw MailException(MailErrorCode.MESSAGE_NOT_FOUND, "cursor uid '${uid.text.take(40)}' no longer exists in INBOX", retryable = false)

    private fun byUidl(folder: POP3Folder, uid: MessageUid): Message {
        val number = uidls(folder)[uid.text] ?: throw MailException(MailErrorCode.MESSAGE_NOT_FOUND, "no message with uid '${uid.text.take(40)}' in INBOX", retryable = false)
        return folder.getMessage(number)
    }

    private fun envelopes(folder: POP3Folder, messages: Array<Message>, uidls: Map<String, Int>, fetch: Boolean = true): List<MessageDocument> {
        if (messages.isEmpty()) return emptyList()
        if (fetch) trace.timed(MailProtocol.POP3.id, "fetch headers ${messages.size}") { folder.fetch(messages, HEADERS_PROFILE) }
        val byNumber = uidls.entries.associate { (uidl, number) -> number to uidl }
        return messages.map { message -> MessageMapper.envelopeFromHeaders(message, INBOX, JsonPrimitive(byNumber.getValue(message.messageNumber))) }
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
        const val INBOX = ImapMailbox.INBOX

        /** One `UIDL` for the whole mailbox (Angus fetches every UID at once). */
        val UID_PROFILE: FetchProfile = FetchProfile().apply { add(UIDFolder.FetchProfileItem.UID) }

        /** Headers (`TOP n 0`) and sizes (`LIST n`) of the given messages. */
        val HEADERS_PROFILE: FetchProfile = FetchProfile().apply {
            add(FetchProfile.Item.ENVELOPE)
            add(FetchProfile.Item.SIZE)
        }

        fun connect(account: MailAccount, secret: MailSecret, trace: ProtocolTrace = ProtocolTrace.disabled(), sockets: SocketRegistry? = null): Pop3Mailbox =
            Pop3Mailbox(MailSessionFactory.connectStore(account, MailProtocol.POP3, secret, trace, sockets) as POP3Store, trace)

        // The checks below need no connection; MailSession runs them before the guard connects.

        /** POP3 has one mailbox: any other `folder` is `FOLDER_NOT_FOUND`. */
        fun checkFolder(folder: String) {
            if (!folder.equals(INBOX, ignoreCase = true)) {
                throw MailException(MailErrorCode.FOLDER_NOT_FOUND, "POP3 accounts have INBOX only, no folder '$folder'", retryable = false)
            }
        }

        fun checkList(args: MessageArgs.ListArgs) {
            checkFolder(args.folder)
            if (args.unseenOnly) throw MailException.unsupported("'unseenOnly' needs an IMAP account: POP3 keeps no seen flag")
        }

        fun checkSearch(args: MessageArgs.SearchArgs) {
            checkFolder(args.folder)
            if (args.query.needsBody) throw MailException.unsupported("'body' and 'text' conditions need an IMAP account: POP3 search matches headers only")
            if (args.query.uids != null) throw MailException.unsupported("'uid' conditions need an IMAP account")
        }
    }
}
