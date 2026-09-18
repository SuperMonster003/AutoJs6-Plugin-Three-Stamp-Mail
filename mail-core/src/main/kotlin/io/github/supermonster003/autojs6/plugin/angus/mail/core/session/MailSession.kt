package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.ExceptionMapper
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.Redactor
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.AccountDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.AppendResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.EndpointReport
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.ExpungeResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.FolderDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.FolderStatusDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MessageDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SearchResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SendResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SessionTestResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TargetUidsResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TransferResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.UidsResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.toDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.OutgoingMessage
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageArgs
import jakarta.mail.Flags
import java.io.Closeable
import java.io.OutputStream

/**
 * One account's session (roadmap D15): the IMAP or POP3 store and the SMTP transport behind
 * [ConnectionGuard]s that connect lazily, expire when idle, and reconnect after a loss; the
 * redacted [trace]; and the exception mapping every operation goes through. Not thread-safe: the
 * app runs each session on one executor thread and calls [expireIdle] from a timer on that thread.
 */
class MailSession(
    val account: MailAccount,
    private val secret: MailSecret,
    private val clock: () -> Long = System::currentTimeMillis,
    idleTimeoutMillis: Long = MailLimits.SESSION_IDLE_TIMEOUT_MS,
) : Closeable {

    val redactor = Redactor(account.username, secret)
    val mapper = ExceptionMapper(redactor)
    val trace = ProtocolTrace(account.debug, redactor, clock)

    private val imapGuard = ConnectionGuard<ImapMailbox>(
        label = MailProtocol.IMAP.id,
        idleTimeoutMillis = idleTimeoutMillis,
        clock = clock,
        isAlive = { it.isConnected },
        onClose = { it.close() },
        connect = { ImapMailbox.connect(account, secret, trace) },
    )
    private val pop3Guard = ConnectionGuard<Pop3Mailbox>(
        label = MailProtocol.POP3.id,
        idleTimeoutMillis = idleTimeoutMillis,
        clock = clock,
        isAlive = { it.isConnected },
        onClose = { it.close() },
        connect = { Pop3Mailbox.connect(account, secret, trace) },
    )
    private val smtpGuard = ConnectionGuard<SmtpSender>(
        label = MailProtocol.SMTP.id,
        idleTimeoutMillis = idleTimeoutMillis,
        clock = clock,
        isAlive = { it.isConnected },
        onClose = { it.close() },
        connect = { SmtpSender(account, secret, trace).connect() },
    )

    var isClosed: Boolean = false
        private set

    /** The last failure any operation reported, for `getStatus`. */
    var lastError: MailException? = null
        private set

    /** Protocols with a live connection right now. */
    val connectedProtocols: List<MailProtocol>
        get() = listOfNotNull(
            MailProtocol.IMAP.takeIf { imapGuard.isConnected },
            MailProtocol.POP3.takeIf { pop3Guard.isConnected },
            MailProtocol.SMTP.takeIf { smtpGuard.isConnected },
        )

    /** Times each protocol connected; tests use it to prove reconnects. */
    fun connectCount(protocol: MailProtocol): Int = guard(protocol).connectCount

    fun <R> imap(retryOnLoss: Boolean = true, block: (ImapMailbox) -> R): R = guarded(MailProtocol.IMAP, imapGuard, retryOnLoss, block)

    fun <R> pop3(retryOnLoss: Boolean = true, block: (Pop3Mailbox) -> R): R = guarded(MailProtocol.POP3, pop3Guard, retryOnLoss, block)

    /** SMTP operations never retry by default: a send interrupted mid-way must not go out twice. */
    fun <R> smtp(retryOnLoss: Boolean = false, block: (SmtpSender) -> R): R = guarded(MailProtocol.SMTP, smtpGuard, retryOnLoss, block)

    /**
     * Probes the receive endpoint and the SMTP endpoint (roadmap P2.1 `session.test`): connects
     * each, lists its capabilities, and reports the round-trip time. Endpoint failures are part of
     * the result, not exceptions; only a closed session throws.
     */
    fun test(): SessionTestResult {
        ensureOpen()
        val started = clock()
        val imap = account.imap?.takeIf { account.receive == MailProtocol.IMAP }?.let { endpoint ->
            probe(MailProtocol.IMAP, endpoint) { imap(retryOnLoss = false) { it.capabilities() } }
        }
        val pop3 = account.pop3?.takeIf { account.receive == MailProtocol.POP3 }?.let { endpoint ->
            probe(MailProtocol.POP3, endpoint) { pop3(retryOnLoss = false) { it.capabilities() } }
        }
        val smtp = account.smtp?.let { endpoint ->
            probe(MailProtocol.SMTP, endpoint) { smtp { it.extensions() } }
        }
        val reports = listOfNotNull(imap, pop3, smtp)
        return SessionTestResult(
            ok = reports.all { it.ok },
            account = AccountDocument.of(account),
            imap = imap,
            pop3 = pop3,
            smtp = smtp,
            elapsedMs = clock() - started,
        )
    }

    /**
     * `mail.send` (roadmap P2.2): sends through SMTP, then files a copy in the sent folder when
     * [saveToSent] is true, or when it is null and the provider preset says the server does not
     * do it by itself. The copy needs an IMAP receive endpoint; a failure to file it does not undo
     * the send and is reported in the result instead of thrown.
     */
    fun send(message: OutgoingMessage, saveToSent: Boolean? = null): SendResult {
        ensureOpen()
        val started = clock()
        val sent = smtp { it.send(message) }
        // A provider that files its own copy never gets a second one: Gmail would show a duplicate
        // and QQ refuses the APPEND outright ("NO Mail has saved by smtp!", verified 2026-09-18).
        val serverKeepsCopy = account.provider?.autoSavesSent == true
        val wantsCopy = when (saveToSent) {
            true -> !serverKeepsCopy
            false -> false
            null -> account.provider?.autoSavesSent == false
        }
        val canCopy = account.receive == MailProtocol.IMAP && account.imap != null
        var savedToSent = false
        var sentFolder: String? = null
        var saveError: MailException? = null
        if (wantsCopy && canCopy) {
            try {
                imap { mailbox ->
                    sentFolder = resolveSentFolder(mailbox)
                    val folder = sentFolder
                    if (folder != null) {
                        mailbox.append(folder, sent.mime, Flags(Flags.Flag.SEEN))
                        savedToSent = true
                    }
                }
            } catch (e: MailException) {
                saveError = e
            }
        }
        return SendResult(
            messageId = sent.messageId,
            accepted = sent.accepted,
            savedToSent = savedToSent,
            sentCopy = when {
                savedToSent -> SendResult.SENT_COPY_APPENDED
                saveError != null -> SendResult.SENT_COPY_FAILED
                serverKeepsCopy -> SendResult.SENT_COPY_SERVER
                else -> SendResult.SENT_COPY_NONE
            },
            sentFolder = sentFolder,
            saveError = saveError?.toDocument(),
            elapsedMs = clock() - started,
        )
    }

    /** `messages.append`: stores a composed message in [folder] (drafts, archived copies). */
    fun append(folder: String, message: OutgoingMessage, flags: Flags): AppendResult {
        val uid = imapOnly("messages.append") { mailbox ->
            val mime = smtpComposer().compose(message)
            mailbox.append(folder, mime, flags)
        }
        return AppendResult(folder, uid)
    }

    // ------------------------------------------------------------------ P2.3 folders and messages (IMAP), P2.4 POP3 subset

    /** The protocol every receive operation runs on; the argument parsers take it (roadmap P2.4). */
    val receiveProtocol: MailProtocol get() = account.receive

    fun listFolders(args: MessageArgs.FoldersListArgs): List<FolderDocument> =
        receive({ it.listFolders(args.subscribedOnly, args.status) }, { it.listFolders(args.status) })

    fun folderStatus(path: String): FolderStatusDocument = imapOnly("folders.status") { it.folderStatus(path) }

    fun createFolder(path: String): FolderDocument = imapOnly("folders.create") { it.createFolder(path) }

    fun deleteFolder(path: String): Boolean = imapOnly("folders.delete", retryOnLoss = false) { it.deleteFolder(path) }

    fun renameFolder(args: MessageArgs.RenameArgs): FolderDocument = imapOnly("folders.rename", retryOnLoss = false) { it.renameFolder(args.path, args.newPath) }

    fun listMessages(args: MessageArgs.ListArgs): List<MessageDocument> =
        receive({ it.listMessages(args) }, { it.listMessages(args) }, precheck = { Pop3Mailbox.checkList(args) })

    fun searchMessages(args: MessageArgs.SearchArgs): SearchResult =
        receive({ it.search(args) }, { it.search(args) }, precheck = { Pop3Mailbox.checkSearch(args) })

    fun getMessage(args: MessageArgs.GetArgs): MessageDocument =
        receive({ it.getMessage(args.folder, args.uid.imap, args.peek, args.includeRaw) }, { it.getMessage(args.uid, args.includeRaw) }, precheck = { Pop3Mailbox.checkFolder(args.folder) })

    /** Streams into [sink]; never retried, because the sink already holds whatever went out before a loss. */
    fun downloadAttachment(args: MessageArgs.DownloadArgs, sink: OutputStream, progress: TransferProgress = TransferProgress.NONE): TransferResult =
        receive(
            retryOnLoss = false,
            imap = { it.downloadPart(args.folder, args.uid.imap, args.partId, sink, progress) },
            pop3 = { it.downloadPart(args.uid, args.partId, sink, progress) },
            precheck = { Pop3Mailbox.checkFolder(args.folder) },
        )

    fun downloadRaw(args: MessageArgs.RawArgs, sink: OutputStream, progress: TransferProgress = TransferProgress.NONE): TransferResult =
        receive(
            retryOnLoss = false,
            imap = { it.downloadRaw(args.folder, args.uid.imap, sink, progress) },
            pop3 = { it.downloadRaw(args.uid, sink, progress) },
            precheck = { Pop3Mailbox.checkFolder(args.folder) },
        )

    fun setFlags(args: MessageArgs.FlagsArgs): UidsResult = imapOnly("messages.setFlags", retryOnLoss = false) { it.setFlags(args.folder, args.uids, args.flags, args.mode) }

    fun move(args: MessageArgs.TargetArgs): TargetUidsResult = imapOnly("messages.move", retryOnLoss = false) { it.move(args.folder, args.uids, args.target) }

    fun copy(args: MessageArgs.TargetArgs): TargetUidsResult = imapOnly("messages.copy", retryOnLoss = false) { it.copy(args.folder, args.uids, args.target) }

    /** POP3 deletes commit when the folder closes at the end of the call, whatever [MessageArgs.DeleteArgs.expunge] says (roadmap P2.4). */
    fun delete(args: MessageArgs.DeleteArgs): UidsResult =
        receive(
            retryOnLoss = false,
            imap = { it.delete(args.folder, args.uids.map { uid -> uid.imap }, args.expunge) },
            pop3 = { it.delete(args.uids) },
            precheck = { Pop3Mailbox.checkFolder(args.folder) },
        )

    fun expunge(folder: String): ExpungeResult = imapOnly("messages.expunge", retryOnLoss = false) { it.expunge(folder) }

    /**
     * Runs the receive operation on the account's protocol: [imap] for IMAP accounts, [pop3] for
     * POP3 accounts after [precheck] (what POP3 cannot do is refused before any connection).
     */
    private fun <R> receive(imap: (ImapMailbox) -> R, pop3: (Pop3Mailbox) -> R, retryOnLoss: Boolean = true, precheck: () -> Unit = {}): R {
        ensureOpen()
        return when (account.receive) {
            MailProtocol.POP3 -> {
                try {
                    precheck()
                } catch (e: MailException) {
                    lastError = e
                    throw e
                }
                pop3(retryOnLoss, pop3)
            }
            else -> imap(retryOnLoss, imap)
        }
    }

    /** IMAP-only operations: POP3 accounts get `UNSUPPORTED_OPERATION` without connecting (roadmap D3 / P2.4). */
    private fun <R> imapOnly(op: String, retryOnLoss: Boolean = true, block: (ImapMailbox) -> R): R {
        ensureOpen()
        if (account.receive != MailProtocol.IMAP) {
            throw MailException.unsupported("$op needs an IMAP account; this account receives over ${account.receive.id} (roadmap D3)").also { lastError = it }
        }
        return imap(retryOnLoss, block)
    }


    /**
     * The sent folder: the preset's name when that folder exists, else the `\Sent` special-use
     * folder, else the first existing conventional name; null when nothing matches (the plugin
     * never creates folders on its own).
     */
    fun resolveSentFolder(mailbox: ImapMailbox): String? {
        account.provider?.sentFolder?.let { preset -> if (mailbox.folderExists(preset)) return preset }
        mailbox.findSpecialUse("\\Sent")?.let { return it }
        return ImapMailbox.SENT_FOLDER_CANDIDATES.firstOrNull { mailbox.folderExists(it) }
    }

    /** A sender used for composing only; it never connects. */
    private fun smtpComposer(): SmtpSender = SmtpSender(account, secret, trace)

    private fun probe(protocol: MailProtocol, endpoint: MailEndpoint, capabilities: () -> List<String>): EndpointReport {
        val started = clock()
        return try {
            val advertised = capabilities()
            EndpointReport(protocol.id, endpoint.host, endpoint.port, endpoint.tls.id, ok = true, elapsedMs = clock() - started, capabilities = advertised)
        } catch (e: MailException) {
            EndpointReport(protocol.id, endpoint.host, endpoint.port, endpoint.tls.id, ok = false, elapsedMs = clock() - started, error = e.toDocument())
        }
    }

    /** Drops connections idle for longer than the session idle timeout; returns how many were dropped. */
    fun expireIdle(): Int = listOf(imapGuard, pop3Guard, smtpGuard).count { it.expireIfIdle() }

    /** Drops every connection but keeps the session usable; the next operation reconnects. */
    fun disconnect() {
        listOf(imapGuard, pop3Guard, smtpGuard).forEach { it.drop() }
    }

    override fun close() {
        if (isClosed) return
        isClosed = true
        listOf(imapGuard, pop3Guard, smtpGuard).forEach { runCatching { it.close() } }
        secret.clear()
    }

    private fun guard(protocol: MailProtocol): ConnectionGuard<*> = when (protocol) {
        MailProtocol.IMAP -> imapGuard
        MailProtocol.POP3 -> pop3Guard
        MailProtocol.SMTP -> smtpGuard
    }

    private fun <T : Any, R> guarded(protocol: MailProtocol, guard: ConnectionGuard<T>, retryOnLoss: Boolean, block: (T) -> R): R {
        ensureOpen()
        if (account.endpointOrNull(protocol) == null) {
            throw MailException.invalidArgument("account ${account.address} has no ${protocol.id} endpoint").also { lastError = it }
        }
        try {
            return guard.use(retryOnLoss, block)
        } catch (e: MailException) {
            lastError = e
            throw e
        } catch (e: Exception) {
            val mapped = mapper.map(e, "${protocol.id} ${endpointLabel(protocol)}")
            lastError = mapped
            throw mapped
        }
    }

    private fun endpointLabel(protocol: MailProtocol): String = account.endpointOrNull(protocol)?.toString() ?: protocol.id

    private fun ensureOpen() {
        if (isClosed) throw MailException(MailErrorCode.SESSION_CLOSED, "session is closed", retryable = false)
    }
}
