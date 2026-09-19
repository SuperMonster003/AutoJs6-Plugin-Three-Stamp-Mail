package io.github.supermonster003.autojs6.plugin.angus.mail.core.watch

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.argsObject
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.bool
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.long
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.oneOf
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.rejectUnknown
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.string
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageArgs
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.Pop3Mailbox

/**
 * What one watch observes (contract `watchOptionsJson`, roadmap P5): [folder], the requested
 * [mode] (null: IDLE when the server advertises it, else polling), the poll interval and whether
 * `message` events carry the body.
 */
data class WatchOptions(
    val folder: String = MessageArgs.INBOX,
    val mode: WatchMode? = null,
    val pollIntervalMs: Long = MailLimits.DEFAULT_POLL_INTERVAL_MS,
    val fetchBody: Boolean = false,
) {
    companion object {
        /** `generation` travels in the same document (protocol document "Watch options") but belongs to the Binder envelope. */
        val FIELDS: Set<String> = setOf("folder", "mode", "pollIntervalMs", "fetchBody", "generation")

        /**
         * Parses the contract's `watchOptionsJson` for an account receiving over [receive]:
         * `folder` defaults to `INBOX`, `mode` is `idle` or `poll`, `pollIntervalMs` is clamped
         * into `MIN_POLL_INTERVAL_MS..MAX_POLL_INTERVAL_MS`, `fetchBody` defaults to false and
         * unknown fields are refused. POP3 accounts have `INBOX` only and no IDLE (roadmap D3), so
         * another folder is `FOLDER_NOT_FOUND` and `mode: "idle"` is `UNSUPPORTED_OPERATION`;
         * the combination was left to the watch parser by roadmap P2.1.
         */
        fun parse(json: String?, receive: MailProtocol): WatchOptions {
            val args = argsObject(json?.takeIf { it.isNotBlank() } ?: "{}", "watch options")
            args.rejectUnknown("watch options", FIELDS)
            val folder = args.string("folder") ?: MessageArgs.INBOX
            val mode = args.oneOf("mode", WatchMode.entries.map { it.id })?.let { WatchMode.fromId(it) }
            val interval = args.long("pollIntervalMs") ?: MailLimits.DEFAULT_POLL_INTERVAL_MS
            if (interval <= 0) throw MailException.invalidArgument("'pollIntervalMs' must be positive: $interval")
            val fetchBody = args.bool("fetchBody") ?: false
            if (receive == MailProtocol.POP3) {
                Pop3Mailbox.checkFolder(folder)
                if (mode == WatchMode.IDLE) throw MailException.unsupported("POP3 accounts have no IDLE; watch with mode 'poll' (roadmap D3)")
            }
            return WatchOptions(folder, mode, interval.coerceIn(MailLimits.MIN_POLL_INTERVAL_MS, MailLimits.MAX_POLL_INTERVAL_MS), fetchBody)
        }
    }
}
