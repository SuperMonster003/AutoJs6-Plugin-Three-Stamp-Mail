package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.ExpungeResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.FolderDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.FolderStatusDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SearchResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TargetUidsResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TransferResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.UidsResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MessageMapper
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MessageSummary
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MimeTree
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.CompiledQuery
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.FlagMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageArgs
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.UidSet
import jakarta.mail.FetchProfile
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.UIDFolder
import jakarta.mail.internet.MimeMessage
import jakarta.mail.search.FlagTerm
import jakarta.mail.search.SearchTerm
import kotlinx.serialization.json.JsonPrimitive
import org.eclipse.angus.mail.iap.BadCommandException
import org.eclipse.angus.mail.imap.IMAPFolder
import org.eclipse.angus.mail.imap.IMAPMessage
import org.eclipse.angus.mail.imap.IMAPStore
import org.eclipse.angus.mail.imap.protocol.IMAPResponse
import org.eclipse.angus.mail.imap.protocol.ListInfo
import java.io.Closeable
import java.io.OutputStream

/**
 * One connected IMAP store: capabilities and the `ID` handshake (P2.1, sent on every connection
 * by [IdentifyingImapStore]), `APPEND` and the sent-folder lookup (P2.2), and the folder and
 * message operations of roadmap P2.3. Every operation opens the folder it needs and closes it
 * again (without expunging) so the store keeps no selected state between calls; UIDs are the
 * only cursors scripts see.
 */
class ImapMailbox private constructor(
    private val store: IMAPStore,
    private val trace: ProtocolTrace,
) : Closeable {

    val isConnected: Boolean get() = store.isConnected

    /** What the server answered to the first `ID` command, or null when none was sent or accepted (see [IdentifyingImapStore]). */
    val serverId: Map<String, String>? get() = (store as? IdentifyingImapStore)?.serverId

    fun hasCapability(name: String): Boolean = store.hasCapability(name)

    /** The advertised capabilities the plugin cares about, in [KNOWN_CAPABILITIES] order. */
    fun capabilities(): List<String> = KNOWN_CAPABILITIES.filter { store.hasCapability(it) }

    /** Newest [limit] messages of INBOX, newest first, without marking them as read (P0 surface, kept for the device tests). */
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

    /** Opens [name] for [block] and closes it afterwards without expunging. */
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

    // ------------------------------------------------------------------ watches (P5)

    /** Opens [name] and leaves it open for a watch's IDLE loop (roadmap P5); the caller closes it with `close(false)`. */
    fun openFolder(name: String, mode: Int): IMAPFolder {
        val folder = store.getFolder(name) as IMAPFolder
        trace.timed(MailProtocol.IMAP.id, "open $name ${if (mode == Folder.READ_WRITE) "rw" else "ro"}") { folder.open(mode) }
        return folder
    }

    /** `UIDVALIDITY` of the open [folder], or -1 when the server did not report it. */
    fun uidValidity(folder: IMAPFolder): Long = runCatching { folder.uidValidity }.getOrDefault(-1L)

    /**
     * The UID a watch starts after: `UIDNEXT - 1` when the server reports `UIDNEXT`, else the UID
     * of the last message, or 0 for an empty folder. Everything above it arrived later.
     */
    fun highestUid(folder: IMAPFolder): Long {
        val next = runCatching { folder.uidNext }.getOrDefault(-1L)
        if (next > 0) return next - 1
        val count = folder.messageCount
        return if (count > 0) folder.getUID(folder.getMessage(count)) else 0L
    }

    /**
     * True when the newest message of the open [folder] has a UID above [uid]. Angus keeps the
     * count from the untagged `EXISTS` responses of any command (and sends a `NOOP` when the
     * connection was quiet for a second), so a watch calls this after every fetch to close the
     * window in which a message arrives while the previous batch is being fetched.
     */
    fun hasMessagesAfter(folder: IMAPFolder, uid: Long): Boolean {
        val count = folder.messageCount
        return count > 0 && folder.getUID(folder.getMessage(count)) > uid
    }

    /**
     * Messages of the open [folder] with a UID above [afterUid] (`UID FETCH afterUid+1:*`),
     * oldest first, as envelopes or, with [fetchBody], as the full documents of `messages.get`
     * (bodies are peeked, so nothing is marked read). A watch calls it after every IDLE wake-up
     * and after a reconnect, so the same message is never reported twice (roadmap D17).
     */
    fun messagesAfter(folder: IMAPFolder, afterUid: Long, fetchBody: Boolean): List<MessageDocument> {
        // `n:*` also returns the last message when every UID is below n (RFC 3501), hence the filter.
        val found = trace.timed(MailProtocol.IMAP.id, "uids after $afterUid") {
            folder.getMessagesByUID(afterUid + 1, UIDFolder.LASTUID).filter { folder.getUID(it) > afterUid }.sortedBy { folder.getUID(it) }
        }
        if (found.isEmpty()) return emptyList()
        val messages = found.toTypedArray()
        if (!fetchBody) return envelopes(folder, messages)
        messages.forEach { (it as? IMAPMessage)?.setPeek(true) }
        trace.timed(MailProtocol.IMAP.id, "fetch full ${messages.size}") { folder.fetch(messages, FULL_PROFILE) }
        return messages.map { MessageMapper.full(it, folder.fullName, JsonPrimitive(folder.getUID(it))) }
    }

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

    // ------------------------------------------------------------------ folders (P2.3)

    /**
     * `folders.list`: the folder tree with special-use roles. Roles come from the `LIST`
     * attributes (RFC 6154, Gmail), else from `XLIST` (QQ, 163), else from conventional names;
     * counts are fetched with `STATUS` only when [status] is requested.
     */
    fun listFolders(subscribedOnly: Boolean = false, status: Boolean = false): List<FolderDocument> = trace.timed(MailProtocol.IMAP.id, "list folders") {
        val root = store.defaultFolder
        val folders = (if (subscribedOnly) root.listSubscribed("*") else root.list("*")).filterIsInstance<IMAPFolder>()
        val attributes = folders.associate { it.fullName to runCatching { it.attributes.toSet() }.getOrDefault(emptySet()) }.toMutableMap()
        if (attributes.values.none { SpecialUse.fromAttributes(it) != null } && hasCapability("XLIST") && !hasCapability("SPECIAL-USE")) {
            xlistAttributes().forEach { (name, attrs) -> attributes[name] = attributes[name].orEmpty() + attrs }
        }
        val serverRoles = attributes.values.any { SpecialUse.fromAttributes(it) != null }
        val documents = folders.map { folder ->
            val attrs = attributes[folder.fullName].orEmpty()
            val selectable = attrs.none { it.equals("\\Noselect", ignoreCase = true) || it.equals("\\NonExistent", ignoreCase = true) } &&
                runCatching { folder.type and Folder.HOLDS_MESSAGES != 0 }.getOrDefault(true)
            describe(folder, attrs, serverRoles, selectable, subscribed = if (subscribedOnly) true else null, status = status && selectable)
        }
        tree(documents)
    }

    /** `folders.status`: counts and UID markers of one folder without selecting it. */
    fun folderStatus(path: String): FolderStatusDocument = trace.timed(MailProtocol.IMAP.id, "status $path") {
        val folder = existing(path)
        FolderStatusDocument(
            name = folder.name,
            path = folder.fullName,
            messages = folder.messageCount,
            unseen = folder.unreadMessageCount,
            recent = runCatching { folder.newMessageCount }.getOrNull()?.takeIf { it >= 0 },
            uidNext = runCatching { folder.uidNext }.getOrNull()?.takeIf { it >= 0 },
            uidValidity = runCatching { folder.uidValidity }.getOrNull()?.takeIf { it >= 0 },
        )
    }

    fun createFolder(path: String): FolderDocument = trace.timed(MailProtocol.IMAP.id, "create $path") {
        val folder = store.getFolder(path) as IMAPFolder
        if (folder.exists()) throw MailException.invalidArgument("folder already exists: $path")
        if (!folder.create(Folder.HOLDS_MESSAGES or Folder.HOLDS_FOLDERS)) {
            throw MailException(MailErrorCode.SERVER_ERROR, "the server did not create the folder: $path", retryable = false)
        }
        describe(folder)
    }

    fun deleteFolder(path: String): Boolean = trace.timed(MailProtocol.IMAP.id, "delete folder $path") {
        if (path.equals(INBOX, ignoreCase = true)) throw MailException.invalidArgument("INBOX cannot be deleted")
        val folder = existing(path)
        if (!folder.delete(true)) throw MailException(MailErrorCode.SERVER_ERROR, "the server did not delete the folder: $path", retryable = false)
        true
    }

    fun renameFolder(path: String, newPath: String): FolderDocument = trace.timed(MailProtocol.IMAP.id, "rename $path") {
        if (path.equals(INBOX, ignoreCase = true)) throw MailException.invalidArgument("INBOX cannot be renamed")
        val folder = existing(path)
        val target = store.getFolder(newPath) as IMAPFolder
        if (target.exists()) throw MailException.invalidArgument("folder already exists: $newPath")
        if (!folder.renameTo(target)) throw MailException(MailErrorCode.SERVER_ERROR, "the server did not rename the folder: $path", retryable = false)
        describe(store.getFolder(newPath) as IMAPFolder)
    }

    // ------------------------------------------------------------------ messages (P2.3)

    /**
     * `messages.list`: one page of envelopes. Cursors are UIDs (`before` = strictly older,
     * `after` = strictly newer); they are turned into sequence positions with one `UID FETCH`
     * each, so a page costs the envelope fetch of its own messages only. `unseenOnly` runs
     * `SEARCH UNSEEN` on the server and pages through its result.
     */
    fun listMessages(args: MessageArgs.ListArgs): List<MessageDocument> = withFolder(args.folder, Folder.READ_ONLY) { folder ->
        val total = folder.messageCount
        if (total == 0) return@withFolder emptyList()
        val upper = args.before?.let { positionBefore(folder, it.imap) } ?: total
        val lower = args.after?.let { positionAfter(folder, it.imap) } ?: 1
        if (upper < lower || upper < 1) return@withFolder emptyList()
        val numbers: List<Int> = if (args.unseenOnly) {
            trace.timed(MailProtocol.IMAP.id, "search unseen") { folder.search(FlagTerm(Flags(Flags.Flag.SEEN), false)) }
                .map { it.messageNumber }
                .filter { it in lower..upper }
                .sorted()
        } else {
            (lower..upper).toList()
        }
        val page = if (args.descending) numbers.takeLast(args.limit).asReversed() else numbers.take(args.limit)
        envelopes(folder, folder.getMessages(page.toIntArray()))
    }

    /**
     * `messages.search`: the server evaluates the compiled term (restricted to the `uid` set when
     * given); when it refuses (typically non-ASCII text on QQ / 163) and [args] allow it, the
     * newest `MAX_CLIENT_FILTER` candidates are matched on the client and the result says so.
     * [serverSearch] false (`fallback: "always"`) skips the server altogether.
     */
    fun search(args: MessageArgs.SearchArgs, serverSearch: Boolean = args.serverSearch): SearchResult = withFolder(args.folder, Folder.READ_ONLY) { folder ->
        val total = folder.messageCount
        if (total == 0) return@withFolder SearchResult(emptyList(), SearchResult.FALLBACK_SERVER)
        val upper = args.before?.let { positionBefore(folder, it.imap) } ?: total
        if (upper < 1) return@withFolder SearchResult(emptyList(), SearchResult.FALLBACK_SERVER)
        val candidates: Array<Message>? = args.query.uids?.let { byUidSet(folder, it) }
        if (candidates != null && candidates.isEmpty()) return@withFolder SearchResult(emptyList(), SearchResult.FALLBACK_SERVER)
        val term = args.query.term
        var fallback = SearchResult.FALLBACK_SERVER
        val matched: List<Message> = when {
            term == null -> candidates!!.toList()
            else -> {
                val server = if (serverSearch) serverSearch(folder, term, candidates, args.clientFallback) else null
                if (server != null) {
                    server
                } else {
                    fallback = SearchResult.FALLBACK_CLIENT
                    clientSearch(folder, args.query, candidates, upper, args.limit)
                }
            }
        }
        val page = matched.filter { it.messageNumber <= upper }.sortedByDescending { it.messageNumber }.take(args.limit)
        SearchResult(envelopes(folder, page.toTypedArray()), fallback)
    }

    /**
     * `messages.get`: the full document of one message. Bodies are always fetched with `PEEK`;
     * `peek = false` stores `\Seen` explicitly afterwards, so the outcome does not depend on
     * whether the server marks a partially fetched message read (and needs a writable folder).
     */
    fun getMessage(folder: String, uid: Long, peek: Boolean = true, includeRaw: Boolean = false): MessageDocument =
        withFolder(folder, if (peek) Folder.READ_ONLY else Folder.READ_WRITE) { f ->
            val message = byUid(f, uid)
            (message as? IMAPMessage)?.setPeek(true)
            f.fetch(arrayOf(message), FULL_PROFILE)
            if (!peek && !message.isSet(Flags.Flag.SEEN)) {
                trace.timed(MailProtocol.IMAP.id, "store seen $uid") { f.setFlags(arrayOf(message), Flags(Flags.Flag.SEEN), true) }
            }
            trace.timed(MailProtocol.IMAP.id, "get $folder $uid") { MessageMapper.full(message, folder, JsonPrimitive(uid), includeRaw) }
        }

    /** `attachments.download`: streams the decoded part [partId] of message [uid] into [sink]. */
    fun downloadPart(folder: String, uid: Long, partId: String, sink: OutputStream, progress: TransferProgress): TransferResult = withFolder(folder, Folder.READ_ONLY) { f ->
        val message = byUid(f, uid)
        (message as? IMAPMessage)?.setPeek(true)
        f.fetch(arrayOf(message), STRUCTURE_PROFILE)
        val leaf = MimeTree.find(message, partId) ?: throw MailException(MailErrorCode.ATTACHMENT_NOT_FOUND, "message $uid has no part $partId", retryable = false)
        val document = MessageMapper.attachment(leaf)
        val bytes = trace.timed(MailProtocol.IMAP.id, "download $folder $uid part $partId") {
            leaf.part.inputStream.use { input -> Transfer.copy(input, sink, total = null, progress = progress) }
        }
        TransferResult(bytes, document.fileName, document.mimeType)
    }

    /** `messages.raw`: streams the RFC 822 source of message [uid] into [sink]. */
    fun downloadRaw(folder: String, uid: Long, sink: OutputStream, progress: TransferProgress): TransferResult = withFolder(folder, Folder.READ_ONLY) { f ->
        val message = byUid(f, uid)
        (message as? IMAPMessage)?.setPeek(true)
        f.fetch(arrayOf(message), STRUCTURE_PROFILE)
        val total = message.size.toLong().takeIf { it > 0 }
        val bytes = trace.timed(MailProtocol.IMAP.id, "raw $folder $uid") {
            // IMAPMessage.writeTo streams BODY[] in fetchsize chunks (mail.imap.partialfetch), so only counting is needed here.
            val counting = Transfer.counting(sink, total, progress)
            message.writeTo(counting)
            counting.finish()
        }
        TransferResult(bytes, "$uid.eml", "message/rfc822")
    }

    /** `messages.setFlags`: adds, removes or replaces flags; returns the UIDs that exist. */
    fun setFlags(folder: String, uids: List<Long>, flags: Flags, mode: FlagMode): UidsResult = withFolder(folder, Folder.READ_WRITE) { f ->
        val messages = byUids(f, uids)
        trace.timed(MailProtocol.IMAP.id, "store ${mode.id} ${uids.size} uids") {
            when (mode) {
                FlagMode.ADD -> f.setFlags(messages, flags, true)
                FlagMode.REMOVE -> f.setFlags(messages, flags, false)
                FlagMode.SET -> {
                    val current = Flags()
                    messages.forEach { current.add(it.flags) }
                    current.remove(Flags.Flag.RECENT)
                    current.remove(flags)
                    if (current.systemFlags.isNotEmpty() || current.userFlags.isNotEmpty()) f.setFlags(messages, current, false)
                    if (flags.systemFlags.isNotEmpty() || flags.userFlags.isNotEmpty()) f.setFlags(messages, flags, true)
                }
            }
        }
        UidsResult.imap(messages.map { f.getUID(it) })
    }

    /**
     * `messages.move`: `MOVE` when the server has it, else `COPY` + `\Deleted` + [expungeDeleted].
     * Returns the target UIDs when the server reports them (UIDPLUS `COPYUID`).
     */
    fun move(folder: String, uids: List<Long>, target: String, useMove: Boolean = hasCapability("MOVE")): TargetUidsResult = withFolder(folder, Folder.READ_WRITE) { f ->
        val messages = byUids(f, uids)
        val destination = existing(target)
        val uidplus = hasCapability("UIDPLUS")
        trace.timed(MailProtocol.IMAP.id, "move ${uids.size} to $target") {
            if (useMove) {
                if (uidplus) {
                    TargetUidsResult(f.moveUIDMessages(messages, destination)?.map { it.uid })
                } else {
                    f.moveMessages(messages, destination)
                    TargetUidsResult(null)
                }
            } else {
                val copied = if (uidplus) f.copyUIDMessages(messages, destination)?.map { it.uid } else { f.copyMessages(messages, destination); null }
                f.setFlags(messages, Flags(Flags.Flag.DELETED), true)
                expungeDeleted(f, messages)
                TargetUidsResult(copied)
            }
        }
    }

    fun copy(folder: String, uids: List<Long>, target: String): TargetUidsResult = withFolder(folder, Folder.READ_ONLY) { f ->
        val messages = byUids(f, uids)
        val destination = existing(target)
        trace.timed(MailProtocol.IMAP.id, "copy ${uids.size} to $target") {
            if (hasCapability("UIDPLUS")) {
                TargetUidsResult(f.copyUIDMessages(messages, destination)?.map { it.uid })
            } else {
                f.copyMessages(messages, destination)
                TargetUidsResult(null)
            }
        }
    }

    /** `messages.delete`: marks `\Deleted` and, with [expunge], removes the messages at once. */
    fun delete(folder: String, uids: List<Long>, expunge: Boolean): UidsResult = withFolder(folder, Folder.READ_WRITE) { f ->
        val messages = byUids(f, uids)
        val found = messages.map { f.getUID(it) }
        trace.timed(MailProtocol.IMAP.id, "delete ${uids.size}${if (expunge) " expunge" else ""}") {
            f.setFlags(messages, Flags(Flags.Flag.DELETED), true)
            if (expunge) expungeDeleted(f, messages)
        }
        UidsResult.imap(found)
    }

    /** True once the server answered `BAD` to `UID EXPUNGE` (NetEase advertises UIDPLUS but cannot parse it); the folder is expunged as a whole from then on. */
    var uidExpungeRefused: Boolean = false
        private set

    /**
     * Removes [messages], which carry `\Deleted` already: `UID EXPUNGE` with UIDPLUS so that no
     * other `\Deleted` message of the folder goes with them, a plain `EXPUNGE` (RFC 3501, every
     * `\Deleted` message of the folder) without UIDPLUS or once the server has refused the
     * UIDPLUS form with `BAD`.
     */
    private fun expungeDeleted(f: IMAPFolder, messages: Array<Message>): Array<Message> {
        if (hasCapability("UIDPLUS") && !uidExpungeRefused) {
            try {
                return f.expunge(messages)
            } catch (e: MessagingException) {
                if (!refusedAsBad(e)) throw e
                uidExpungeRefused = true
                trace.record(MailProtocol.IMAP.id, "UID EXPUNGE refused with BAD, expunging the folder instead")
            }
        }
        return f.expunge()
    }

    private fun refusedAsBad(e: MessagingException): Boolean {
        var current: Throwable? = e
        var depth = 0
        while (current != null && depth++ < 8) {
            if (current is BadCommandException) return true
            current = current.cause
        }
        return false
    }

    /** `messages.expunge`: removes every `\Deleted` message of the folder. */
    fun expunge(folder: String): ExpungeResult = withFolder(folder, Folder.READ_WRITE) { f ->
        ExpungeResult(trace.timed(MailProtocol.IMAP.id, "expunge $folder") { f.expunge().size })
    }

    // ------------------------------------------------------------------ helpers

    private fun existing(path: String): IMAPFolder {
        val folder = store.getFolder(path) as IMAPFolder
        if (!folder.exists()) throw MailException(MailErrorCode.FOLDER_NOT_FOUND, "no such folder: $path", retryable = false)
        return folder
    }

    private fun describe(folder: IMAPFolder, attrs: Set<String> = runCatching { folder.attributes.toSet() }.getOrDefault(emptySet()), serverRoles: Boolean = true, selectable: Boolean = true, subscribed: Boolean? = null, status: Boolean = false): FolderDocument {
        val role = SpecialUse.fromAttributes(attrs)
            ?: SpecialUse.INBOX.takeIf { folder.fullName.equals(INBOX, ignoreCase = true) }
            ?: SpecialUse.fromName(folder.name).takeIf { !serverRoles }
        return FolderDocument(
            name = folder.name,
            path = folder.fullName,
            delimiter = runCatching { folder.separator.toString() }.getOrDefault("/"),
            specialUse = role?.id,
            selectable = selectable,
            subscribed = subscribed,
            messages = if (status) runCatching { folder.messageCount }.getOrNull() else null,
            unseen = if (status) runCatching { folder.unreadMessageCount }.getOrNull() else null,
        )
    }

    /** Nests the flat LIST result by path; a folder whose parent was not listed becomes a root. */
    private fun tree(flat: List<FolderDocument>): List<FolderDocument> {
        val byPath = flat.associateBy { it.path }
        val children = HashMap<String, MutableList<FolderDocument>>()
        val roots = ArrayList<FolderDocument>()
        flat.sortedBy { it.path }.forEach { doc ->
            val parent = doc.path.substringBeforeLast(doc.delimiter, "")
            if (parent.isNotEmpty() && parent != doc.path && byPath.containsKey(parent)) children.getOrPut(parent) { ArrayList() }.add(doc) else roots.add(doc)
        }
        fun build(doc: FolderDocument): FolderDocument = doc.copy(children = children[doc.path].orEmpty().map(::build))
        return roots.map(::build)
    }

    /** `XLIST "" "*"` for servers that mark special folders only there (QQ, 163). */
    private fun xlistAttributes(): Map<String, Set<String>> {
        val result = HashMap<String, Set<String>>()
        val inbox = store.getFolder(INBOX) as IMAPFolder
        runCatching {
            inbox.doCommand { protocol ->
                val responses = protocol.command("XLIST \"\" \"*\"", null)
                responses.forEach { response ->
                    if (response is IMAPResponse && response.keyEquals("XLIST")) {
                        val info = ListInfo(response)
                        result[info.name] = info.attrs.orEmpty().toSet()
                    }
                }
                protocol.notifyResponseHandlers(responses)
                protocol.handleResult(responses.last())
                null
            }
        }
        return result
    }

    /** Messages with number <= the position of [uid] are older than the cursor. */
    private fun positionBefore(folder: IMAPFolder, uid: Long): Int {
        folder.getMessageByUID(uid)?.let { return it.messageNumber - 1 }
        if (uid <= 1) return 0
        return folder.getMessagesByUID(1, uid - 1).size
    }

    private fun positionAfter(folder: IMAPFolder, uid: Long): Int {
        folder.getMessageByUID(uid)?.let { return it.messageNumber + 1 }
        return folder.getMessagesByUID(1, uid).size + 1
    }

    private fun byUid(folder: IMAPFolder, uid: Long): Message =
        folder.getMessageByUID(uid) ?: throw MailException(MailErrorCode.MESSAGE_NOT_FOUND, "no message with uid $uid in ${folder.fullName}", retryable = false)

    private fun byUids(folder: IMAPFolder, uids: List<Long>): Array<Message> {
        val found = folder.getMessagesByUID(uids.toLongArray()).filterNotNull()
        if (found.isEmpty()) throw MailException(MailErrorCode.MESSAGE_NOT_FOUND, "none of the ${uids.size} uids exist in ${folder.fullName}", retryable = false)
        return found.toTypedArray()
    }

    private fun byUidSet(folder: IMAPFolder, uids: UidSet): Array<Message> {
        val seen = LinkedHashMap<Int, Message>()
        uids.ranges.forEach { range ->
            val messages: List<Message> = when {
                // `*` alone: the message with the highest UID
                range.first == UidSet.STAR -> if (folder.messageCount > 0) listOf(folder.getMessage(folder.messageCount)) else emptyList()
                range.first == range.last -> listOfNotNull(folder.getMessageByUID(range.first))
                // `n:*` also returns the last message when every UID is below n (RFC 3501), hence the filter
                else -> folder.getMessagesByUID(range.first, UidSet.wire(range.last)).filter { message -> folder.getUID(message) >= range.first }
            }
            messages.forEach { seen[it.messageNumber] = it }
        }
        return seen.values.toTypedArray()
    }

    private fun serverSearch(folder: IMAPFolder, term: SearchTerm, candidates: Array<Message>?, clientFallback: Boolean): List<Message>? = try {
        trace.timed(MailProtocol.IMAP.id, "search") {
            (if (candidates != null) folder.search(term, candidates) else folder.search(term)).toList()
        }
    } catch (e: MessagingException) {
        if (!clientFallback) throw e
        trace.record(MailProtocol.IMAP.id, "search rejected by the server, filtering on the client")
        null
    }

    /**
     * The client-side filter over the newest `MAX_CLIENT_FILTER` candidates: envelopes (and headers
     * when the term needs them) come in one batch fetch; a `body` / `text` term then costs the
     * server one or two fetches per candidate, so the scan runs newest first and stops once
     * [limit] messages match, which is exactly the page [search] keeps (roadmap P6 baseline).
     */
    private fun clientSearch(folder: IMAPFolder, query: CompiledQuery, candidates: Array<Message>?, upper: Int, limit: Int): List<Message> {
        val window = candidates?.filter { it.messageNumber <= upper }?.sortedBy { it.messageNumber }?.takeLast(MailLimits.MAX_CLIENT_FILTER)
            ?: folder.getMessages(maxOf(1, upper - MailLimits.MAX_CLIENT_FILTER + 1), upper).toList()
        if (window.isEmpty()) return emptyList()
        val profile = FetchProfile().apply {
            add(FetchProfile.Item.ENVELOPE)
            add(FetchProfile.Item.FLAGS)
            add(FetchProfile.Item.SIZE)
            add(UIDFolder.FetchProfileItem.UID)
            if (query.needsHeaders) add(IMAPFolder.FetchProfileItem.HEADERS)
        }
        folder.fetch(window.toTypedArray(), profile)
        val term = query.clientTerm ?: return window
        val hits = ArrayList<Message>()
        var scanned = 0
        trace.timed(MailProtocol.IMAP.id, "client filter ${window.size}") {
            for (message in window.asReversed()) {
                if (hits.size >= limit) break
                scanned++
                if (runCatching { term.match(message) }.getOrDefault(false)) hits += message
            }
        }
        if (scanned < window.size) trace.record(MailProtocol.IMAP.id, "client filter stopped after $scanned of ${window.size} candidates: $limit matched")
        return hits
    }

    private fun envelopes(folder: IMAPFolder, messages: Array<Message>): List<MessageDocument> {
        if (messages.isEmpty()) return emptyList()
        trace.timed(MailProtocol.IMAP.id, "fetch envelopes ${messages.size}") { folder.fetch(messages, ENVELOPE_PROFILE) }
        return messages.map { MessageMapper.envelope(it, folder.fullName, JsonPrimitive(folder.getUID(it))) }
    }

    /**
     * Sends the `ID` command (RFC 2971) with [clientId]. Servers that do not advertise `ID` are
     * skipped; a server that rejects the command does not fail the connection either, because the
     * providers that require it (163 / 126) refuse the *next* command instead, which then surfaces
     * as the real error.
     */
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

        /** Envelope listings: everything `MessageMapper.envelope` reads, including BODYSTRUCTURE for `hasAttachments`. */
        val ENVELOPE_PROFILE: FetchProfile = FetchProfile().apply {
            add(FetchProfile.Item.ENVELOPE)
            add(FetchProfile.Item.FLAGS)
            add(FetchProfile.Item.SIZE)
            add(FetchProfile.Item.CONTENT_INFO)
            add(UIDFolder.FetchProfileItem.UID)
        }

        /** `messages.get`: the envelope profile plus every header. */
        val FULL_PROFILE: FetchProfile = FetchProfile().apply {
            add(FetchProfile.Item.ENVELOPE)
            add(FetchProfile.Item.FLAGS)
            add(FetchProfile.Item.SIZE)
            add(FetchProfile.Item.CONTENT_INFO)
            add(UIDFolder.FetchProfileItem.UID)
            add(IMAPFolder.FetchProfileItem.HEADERS)
        }

        /** Downloads: the structure and size only. */
        val STRUCTURE_PROFILE: FetchProfile = FetchProfile().apply {
            add(FetchProfile.Item.SIZE)
            add(FetchProfile.Item.CONTENT_INFO)
            add(UIDFolder.FetchProfileItem.UID)
        }

        fun connect(account: MailAccount, secret: MailSecret, trace: ProtocolTrace = ProtocolTrace.disabled(), sockets: SocketRegistry? = null): ImapMailbox {
            val store = MailSessionFactory.connectStore(account, MailProtocol.IMAP, secret, trace, sockets) as IMAPStore
            return ImapMailbox(store, trace)
        }
    }
}

/** Special-use roles of folders (RFC 6154 attributes, XLIST attributes, or conventional names). */
enum class SpecialUse(val id: String, private val attributes: Set<String>, private val names: Set<String>) {
    INBOX("inbox", setOf("\\Inbox"), setOf("inbox")),
    SENT("sent", setOf("\\Sent"), setOf("sent", "sent messages", "sent items", "sent mail", "已发送")),
    DRAFTS("drafts", setOf("\\Drafts"), setOf("drafts", "draft", "草稿箱")),
    TRASH("trash", setOf("\\Trash"), setOf("trash", "deleted messages", "deleted items", "deleted", "bin", "已删除")),
    JUNK("junk", setOf("\\Junk", "\\Spam"), setOf("junk", "spam", "junk e-mail", "junk email", "bulk mail", "垃圾邮件")),
    ARCHIVE("archive", setOf("\\Archive"), setOf("archive", "archives")),
    ALL("all", setOf("\\All", "\\AllMail"), setOf("all mail")),
    FLAGGED("flagged", setOf("\\Flagged", "\\Starred"), setOf("starred")),
    IMPORTANT("important", setOf("\\Important"), setOf("important"));

    companion object {
        fun fromAttributes(attrs: Collection<String>): SpecialUse? =
            entries.firstOrNull { role -> attrs.any { attr -> role.attributes.any { it.equals(attr, ignoreCase = true) } } }

        fun fromName(name: String): SpecialUse? = entries.firstOrNull { role -> name.lowercase() in role.names }
    }
}
