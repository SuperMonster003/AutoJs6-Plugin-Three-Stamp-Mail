package io.github.supermonster003.autojs6.plugin.angus.mail.core

/**
 * Ceilings the mail core enforces on its own, mirrored from the host contract (`MailContract` in
 * `plugin-api/mail-api`, roadmap appendix B.5). The app module's parity test keeps every value
 * equal to the contract constant of the same name.
 */
object MailLimits {

    const val MAX_ENVELOPE_BYTES = 512 * 1024
    const val MAX_INLINE_BODY_BYTES = 256 * 1024
    const val MAX_ATTACHMENT_BYTES = 200L * 1024 * 1024
    const val MAX_ATTACHMENTS_PER_MESSAGE = 64
    const val MAX_RECIPIENTS = 500
    const val MAX_PAGE_SIZE = 1000
    const val DEFAULT_PAGE_SIZE = 50
    const val MAX_CLIENT_FILTER = 2000

    /**
     * Candidates a POP3 client search scans (roadmap P2.4). POP3 has no batch header fetch, so
     * every candidate costs one `TOP` round trip (about 0.15 s against QQ from a phone); the
     * window is a tenth of [MAX_CLIENT_FILTER] and the scan stops once `limit` messages match.
     */
    const val MAX_POP3_CLIENT_FILTER = 200
    const val MAX_WATCHES_PER_SESSION = 4
    const val MAX_WATCH_QUEUE = 256
    const val MIN_POLL_INTERVAL_MS = 15_000L
    const val DEFAULT_POLL_INTERVAL_MS = 60_000L
    const val IDLE_RENEW_MS = 24L * 60 * 1000
    const val SESSION_IDLE_TIMEOUT_MS = 10L * 60 * 1000
    const val DEFAULT_CONNECT_TIMEOUT_MS = 15_000L
    const val DEFAULT_READ_TIMEOUT_MS = 60_000L
    const val MAX_ERROR_MESSAGE_BYTES = 4 * 1024

    /** Longest socket timeout a script may request; longer values only hide a dead connection. */
    const val MAX_TIMEOUT_MS = 10L * 60 * 1000

    /** Longest single string field of the account options (display name, host, client id values). */
    const val MAX_OPTION_STRING_LENGTH = 256

    /** Entries of the IMAP `ID` command a script may supply. */
    const val MAX_CLIENT_ID_ENTRIES = 16

    /** Lines kept per session by the redacted protocol trace (roadmap D28) before the oldest are dropped. */
    const val MAX_TRACE_LINES = 200

    /** Longest redacted trace line (roadmap D28: command name, response code, first 200 characters). */
    const val MAX_TRACE_LINE_LENGTH = 200
}
