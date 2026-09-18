package io.github.supermonster003.autojs6.plugin.angus.mail.core.query

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.argsObject
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.bool
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.int
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.obj
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.oneOf
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.rejectUnknown
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.string
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.stringList
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.uid
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.uids
import jakarta.mail.Flags
import kotlinx.serialization.json.JsonObject

/** How `messages.setFlags` combines the given flags with the current ones. */
enum class FlagMode(val id: String) {
    ADD("add"), REMOVE("remove"), SET("set");

    companion object {
        fun fromId(id: String): FlagMode = entries.first { it.id == id }
    }
}

/**
 * The `args` of every folder and message operation of roadmap P2.3 (shapes from the host
 * protocol document `mail-plugin-protocol-v1.md`). Every parser rejects unknown fields, fills the
 * documented defaults (`folder` = `INBOX`, `limit` = `DEFAULT_PAGE_SIZE`, `order` = `desc`,
 * `peek` = true) and answers `LIMIT_EXCEEDED` for page sizes and UID lists above `MAX_PAGE_SIZE`.
 */
object MessageArgs {

    const val INBOX = "INBOX"

    class ListArgs(val folder: String, val limit: Int, val before: Long?, val after: Long?, val descending: Boolean, val unseenOnly: Boolean)

    /**
     * [clientFallback]: filter on the client when the server refuses the SEARCH (`fallback: "client"`,
     * the default); [serverSearch] false: never ask the server (`fallback: "always"`, for providers
     * whose SEARCH answers OK but misses messages, such as freshly delivered ones).
     */
    class SearchArgs(val query: CompiledQuery, val folder: String, val limit: Int, val before: Long?, val clientFallback: Boolean, val serverSearch: Boolean = true)

    class GetArgs(val folder: String, val uid: Long, val peek: Boolean, val includeRaw: Boolean)

    class RawArgs(val folder: String, val uid: Long)

    class DownloadArgs(val folder: String, val uid: Long, val partId: String)

    class FlagsArgs(val folder: String, val uids: List<Long>, val flags: Flags, val mode: FlagMode)

    class TargetArgs(val folder: String, val uids: List<Long>, val target: String)

    class DeleteArgs(val folder: String, val uids: List<Long>, val expunge: Boolean)

    class FoldersListArgs(val subscribedOnly: Boolean, val status: Boolean)

    class RenameArgs(val path: String, val newPath: String)

    fun list(json: String): ListArgs {
        val root = argsObject(json)
        root.rejectUnknown("args", setOf("folder", "limit", "before", "after", "order", "unseenOnly"))
        val before = root.uid("before")
        val after = root.uid("after")
        if (before != null && after != null && after >= before) throw MailException.invalidArgument("'after' must be below 'before'")
        return ListArgs(
            folder = folder(root, "folder"),
            limit = limit(root),
            before = before,
            after = after,
            descending = (root.oneOf("order", listOf("desc", "asc")) ?: "desc") == "desc",
            unseenOnly = root.bool("unseenOnly") ?: false,
        )
    }

    fun search(json: String): SearchArgs {
        val root = argsObject(json)
        root.rejectUnknown("args", setOf("query", "folder", "limit", "before", "fallback"))
        val query = root.obj("query") ?: throw MailException.invalidArgument("'query' is required")
        val fallback = root.oneOf("fallback", listOf("client", "none", "always")) ?: "client"
        return SearchArgs(
            query = SearchQueryCompiler.compile(query),
            folder = folder(root, "folder"),
            limit = limit(root),
            before = root.uid("before"),
            clientFallback = fallback != "none",
            serverSearch = fallback != "always",
        )
    }

    fun get(json: String): GetArgs {
        val root = argsObject(json)
        root.rejectUnknown("args", setOf("folder", "uid", "peek", "includeRaw"))
        return GetArgs(folder(root, "folder"), requiredUid(root), root.bool("peek") ?: true, root.bool("includeRaw") ?: false)
    }

    fun raw(json: String): RawArgs {
        val root = argsObject(json)
        root.rejectUnknown("args", setOf("folder", "uid"))
        return RawArgs(folder(root, "folder"), requiredUid(root))
    }

    fun download(json: String): DownloadArgs {
        val root = argsObject(json)
        root.rejectUnknown("args", setOf("folder", "uid", "partId"))
        val partId = root.string("partId") ?: throw MailException.invalidArgument("'partId' is required")
        if (!PART_ID.matches(partId)) throw MailException.invalidArgument("'partId' is not a part id: '${partId.take(40)}'")
        return DownloadArgs(folder(root, "folder"), requiredUid(root), partId)
    }

    fun flags(json: String): FlagsArgs {
        val root = argsObject(json)
        root.rejectUnknown("args", setOf("folder", "uids", "flags", "mode"))
        val names = root.stringList("flags")
        val mode = FlagMode.fromId(root.oneOf("mode", FlagMode.entries.map { it.id }) ?: "add")
        if (names.isEmpty() && mode != FlagMode.SET) throw MailException.invalidArgument("'flags' must name at least one flag")
        return FlagsArgs(folder(root, "folder"), requiredUids(root), FlagMapper.toFlags(names), mode)
    }

    fun target(json: String): TargetArgs {
        val root = argsObject(json)
        root.rejectUnknown("args", setOf("folder", "uids", "target"))
        val target = root.string("target") ?: throw MailException.invalidArgument("'target' is required")
        return TargetArgs(folder(root, "folder"), requiredUids(root), target)
    }

    fun delete(json: String): DeleteArgs {
        val root = argsObject(json)
        root.rejectUnknown("args", setOf("folder", "uids", "expunge"))
        return DeleteArgs(folder(root, "folder"), requiredUids(root), root.bool("expunge") ?: false)
    }

    /** `messages.expunge` and `folders.status`: an optional or required folder. */
    fun folderOnly(json: String, field: String = "folder", required: Boolean = false): String {
        val root = argsObject(json)
        root.rejectUnknown("args", setOf(field))
        val value = root.string(field)
        if (value == null && required) throw MailException.invalidArgument("'$field' is required")
        return value ?: INBOX
    }

    fun foldersList(json: String): FoldersListArgs {
        val root = argsObject(json)
        root.rejectUnknown("args", setOf("subscribedOnly", "status"))
        return FoldersListArgs(root.bool("subscribedOnly") ?: false, root.bool("status") ?: false)
    }

    fun path(json: String): String {
        val root = argsObject(json)
        root.rejectUnknown("args", setOf("path"))
        return root.string("path") ?: throw MailException.invalidArgument("'path' is required")
    }

    fun rename(json: String): RenameArgs {
        val root = argsObject(json)
        root.rejectUnknown("args", setOf("path", "newPath"))
        val path = root.string("path") ?: throw MailException.invalidArgument("'path' is required")
        val newPath = root.string("newPath") ?: throw MailException.invalidArgument("'newPath' is required")
        if (path == newPath) throw MailException.invalidArgument("'newPath' equals 'path'")
        return RenameArgs(path, newPath)
    }

    private fun folder(root: JsonObject, field: String): String = root.string(field) ?: INBOX

    private fun limit(root: JsonObject): Int {
        val limit = root.int("limit") ?: return MailLimits.DEFAULT_PAGE_SIZE
        if (limit < 1) throw MailException.invalidArgument("'limit' must be at least 1")
        if (limit > MailLimits.MAX_PAGE_SIZE) throw MailException(MailErrorCode.LIMIT_EXCEEDED, "'limit' $limit exceeds MAX_PAGE_SIZE ${MailLimits.MAX_PAGE_SIZE}", retryable = false)
        return limit
    }

    private fun requiredUid(root: JsonObject): Long = root.uid("uid") ?: throw MailException.invalidArgument("'uid' is required")

    private fun requiredUids(root: JsonObject): List<Long> {
        val uids = root.uids("uids")
        if (uids.isEmpty()) throw MailException.invalidArgument("'uids' must hold at least one UID")
        if (uids.size > MailLimits.MAX_PAGE_SIZE) throw MailException(MailErrorCode.LIMIT_EXCEEDED, "${uids.size} UIDs exceed MAX_PAGE_SIZE ${MailLimits.MAX_PAGE_SIZE}", retryable = false)
        return uids
    }

    private val PART_ID = Regex("[1-9][0-9]*(\\.[1-9][0-9]*)*")
}
