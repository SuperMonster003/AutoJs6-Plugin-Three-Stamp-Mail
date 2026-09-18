package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.toFolderJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.toJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.AttachmentSource
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.OutgoingMessageParser
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageArgs
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSession
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.TransferProgress
import org.autojs.plugin.mail.api.MailContract
import java.io.OutputStream

/**
 * The op table of one session (roadmap P2.5 `RequestRouter`, filled in from P2.1 to P2.3): each
 * handler receives the `args` object as a JSON string plus the [CallIo] of the call (attachment
 * sources, the sink descriptor of a download, the progress channel) and returns the `result` as a
 * JSON document. Every op of `MailContract.OPS` has a handler; names outside the contract answer
 * `INVALID_ARGUMENT`. The table is static so the JVM snapshot test can compare it with the
 * contract without a session.
 */
internal class RequestRouter(private val session: MailSession) {

    /** Outcome of routing an op name, before any handler runs. */
    sealed class Route {
        class Handled(val handler: Handler) : Route()
        data object Unsupported : Route()
        data object Unknown : Route()
    }

    /**
     * What a call hands to its handler besides the args. Everything is resolved lazily so that
     * routing and argument errors come first and a descriptor problem never hides them.
     */
    interface CallIo {
        /** How many descriptors the host attached; the contract rules are checked against this before the handler runs. */
        val descriptorCount: Int

        /** The descriptors as attachment sources (`mail.send`, `messages.append`). */
        fun sources(): List<AttachmentSource>

        /** The single write end the download streams into (`messages.raw`, `attachments.download`). */
        fun sink(): OutputStream

        /** Forwards transfer progress to the host (`{id, transferred, total?}` through `onProgress`). */
        fun progress(transferred: Long, total: Long?)

        companion object {
            /** No descriptors, no progress channel. */
            val NONE: CallIo = object : CallIo {
                override val descriptorCount: Int get() = 0
                override fun sources(): List<AttachmentSource> = emptyList()
                override fun sink(): OutputStream = throw MailException.invalidArgument("this call needs the write end of a pipe or file as its single descriptor (0 supplied)")
                override fun progress(transferred: Long, total: Long?) = Unit
            }
        }
    }

    fun interface Handler {
        /** Runs the operation on the session executor thread; throws [MailException] or anything the mapper understands. */
        fun handle(session: MailSession, argsJson: String, io: CallIo): String
    }

    fun route(op: String?): Route = when {
        op == null -> Route.Unknown
        !MailContract.isKnownOp(op) -> Route.Unknown
        else -> HANDLERS[op]?.let { Route.Handled(it) } ?: Route.Unsupported
    }

    /**
     * Runs [op] and returns the result document; failures come back as [MailException]. The
     * descriptor rules of the contract (limit, transfer ops only) are checked after routing, the
     * descriptors themselves are opened by the handler after its arguments parsed.
     */
    fun execute(op: String?, argsJson: String, io: CallIo = CallIo.NONE): String {
        val handler = when (val route = route(op)) {
            is Route.Handled -> route.handler
            Route.Unsupported -> throw MailException.unsupported("$op is not implemented yet")
            Route.Unknown -> throw MailException.invalidArgument(if (op == null) "request carries no op" else "unknown op: $op")
        }
        if (io.descriptorCount > MailContract.MAX_DESCRIPTORS) {
            throw MailException(MailErrorCode.LIMIT_EXCEEDED, "${io.descriptorCount} descriptors exceed the limit of ${MailContract.MAX_DESCRIPTORS}", retryable = false)
        }
        if (io.descriptorCount > 0 && !takesDescriptors(op)) throw MailException.invalidArgument("$op does not take descriptors")
        return try {
            handler.handle(session, argsJson, io)
        } catch (e: MailException) {
            throw e
        } catch (e: Throwable) {
            throw session.mapper.map(e, op)
        }
    }

    companion object {
        val HANDLERS: Map<String, Handler> = linkedMapOf(
            MailContract.OP_SESSION_TEST to Handler { session, _, _ -> session.test().toJson() },
            // The Binder closes the session after this result went out (contract B.3 ordering).
            MailContract.OP_SESSION_CLOSE to Handler { _, _, _ -> "true" },

            MailContract.OP_FOLDERS_LIST to Handler { session, args, _ -> session.listFolders(MessageArgs.foldersList(args)).toFolderJson() },
            MailContract.OP_FOLDERS_STATUS to Handler { session, args, _ -> session.folderStatus(MessageArgs.folderOnly(args, "folder", required = true)).toJson() },
            MailContract.OP_FOLDERS_CREATE to Handler { session, args, _ -> session.createFolder(MessageArgs.path(args)).toJson() },
            MailContract.OP_FOLDERS_DELETE to Handler { session, args, _ -> session.deleteFolder(MessageArgs.path(args)).toString() },
            MailContract.OP_FOLDERS_RENAME to Handler { session, args, _ -> session.renameFolder(MessageArgs.rename(args)).toJson() },

            // The parsers take the account's receive protocol (roadmap P2.4): POP3 UIDs are UIDL
            // strings, and the IMAP-only ops answer UNSUPPORTED_OPERATION for POP3 accounts before any connection.
            MailContract.OP_MESSAGES_LIST to Handler { session, args, _ -> session.listMessages(MessageArgs.list(args, session.receiveProtocol)).toJson() },
            MailContract.OP_MESSAGES_SEARCH to Handler { session, args, _ -> session.searchMessages(MessageArgs.search(args, session.receiveProtocol)).toJson() },
            MailContract.OP_MESSAGES_GET to Handler { session, args, _ -> session.getMessage(MessageArgs.get(args, session.receiveProtocol)).toJson() },
            MailContract.OP_MESSAGES_RAW to Handler { session, args, io ->
                val raw = MessageArgs.raw(args, session.receiveProtocol)
                session.downloadRaw(raw, io.sink(), TransferProgress { transferred, total -> io.progress(transferred, total) }).toJson()
            },
            MailContract.OP_ATTACHMENTS_DOWNLOAD to Handler { session, args, io ->
                val download = MessageArgs.download(args, session.receiveProtocol)
                session.downloadAttachment(download, io.sink(), TransferProgress { transferred, total -> io.progress(transferred, total) }).toJson()
            },
            MailContract.OP_MESSAGES_SET_FLAGS to Handler { session, args, _ -> session.setFlags(MessageArgs.flags(args, session.receiveProtocol)).toJson() },
            MailContract.OP_MESSAGES_MOVE to Handler { session, args, _ -> session.move(MessageArgs.target(args, session.receiveProtocol, MailContract.OP_MESSAGES_MOVE)).toJson() },
            MailContract.OP_MESSAGES_COPY to Handler { session, args, _ -> session.copy(MessageArgs.target(args, session.receiveProtocol, MailContract.OP_MESSAGES_COPY)).toJson() },
            MailContract.OP_MESSAGES_DELETE to Handler { session, args, _ -> session.delete(MessageArgs.delete(args, session.receiveProtocol)).toJson() },
            MailContract.OP_MESSAGES_EXPUNGE to Handler { session, args, _ -> session.expunge(MessageArgs.folderOnly(args)).toJson() },
            MailContract.OP_MESSAGES_APPEND to Handler { session, args, io ->
                val append = OutgoingMessageParser.parseAppendArgs(args, io.sources())
                session.append(append.folder, append.message, append.flags).toJson()
            },

            MailContract.OP_MAIL_SEND to Handler { session, args, io ->
                val send = OutgoingMessageParser.parseSendArgs(args, io.sources())
                session.send(send.message, send.saveToSent).toJson()
            },
        )

        /** Ops whose `call` may carry descriptors: attachment sources (send, append) or the sink of a download. */
        fun takesDescriptors(op: String?): Boolean = op != null && (op in MailContract.OPS_WITH_SOURCES || op in MailContract.OPS_WITH_SINK)

        val SUPPORTED_OPS: Set<String> get() = HANDLERS.keys

        /** Contract ops without a handler; empty since P2.3 (P2.4 changed what POP3 accounts answer, not the table). */
        val PENDING_OPS: List<String> get() = MailContract.OPS.filter { it !in HANDLERS }

        fun errorCodeFor(route: Route): String? = when (route) {
            is Route.Handled -> null
            Route.Unsupported -> MailErrorCode.UNSUPPORTED_OPERATION
            Route.Unknown -> MailErrorCode.INVALID_ARGUMENT
        }
    }
}
