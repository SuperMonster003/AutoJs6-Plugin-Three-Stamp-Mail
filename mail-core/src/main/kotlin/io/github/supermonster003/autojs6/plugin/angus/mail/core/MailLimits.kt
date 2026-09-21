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

    // Background watches and triggers (contract version 2, roadmap P8).

    /** Background watches the plugin keeps at once. */
    const val MAX_TRIGGERS = 16

    /** Live `openTrigger` subscriptions per background watch. */
    const val MAX_TRIGGER_SUBSCRIBERS = 4

    /** Trigger records (envelope summaries, no bodies) the plugin keeps per watch, newest first. */
    const val MAX_TRIGGER_RECORDS = 100

    /** Shortest spacing between two script launches of one mail-arrival task on the host. */
    const val MIN_TRIGGER_INTERVAL_MS = 3_000L

    // Watches (roadmap D17 / P5); not contract constants, the plugin owns them.

    /** First reconnect delay of a watch after a connection loss; doubles per attempt with jitter. */
    const val WATCH_BACKOFF_MIN_MS = 1_000L

    /** Longest reconnect delay of a watch. */
    const val WATCH_BACKOFF_MAX_MS = 5L * 60 * 1000

    /** Longest `pollIntervalMs` a watch accepts; larger values are clamped. */
    const val MAX_POLL_INTERVAL_MS = 60L * 60 * 1000

    /** An IDLE that ends in a connection loss sooner than this counts as a failed IDLE. */
    const val IDLE_HEALTHY_MS = 60_000L

    /** Consecutive failed IDLEs (refused, or dropped within `IDLE_HEALTHY_MS`) before a watch switches to polling. */
    const val IDLE_FAILURE_LIMIT = 3

    // Incoming mail documents (roadmap P6 hostile input, D39): one message document always fits the
    // response envelope, whatever the message carries. Not contract constants, the plugin owns them.

    /** Deepest multipart nesting the MIME tree walks; a multipart nested deeper is one downloadable leaf. */
    const val MAX_MIME_DEPTH = 32

    /** Leaves the MIME tree enumerates per message; later parts are neither listed nor downloadable. */
    const val MAX_MIME_PARTS = 256

    /** Longest subject, header value, message id or declared file name kept in a message document (UTF-16 units). */
    const val MAX_HEADER_VALUE_CHARS = 4096

    /** UTF-8 bytes of names and values the `headers` map of a message document holds; a header that does not fit is dropped. */
    const val MAX_HEADERS_BYTES = 64 * 1024

    /** Longest address, display name, content id or header name kept in a message document (UTF-16 units); the four address lists of a document share `MAX_RECIPIENTS`. */
    const val MAX_ADDRESS_CHARS = 320
}
