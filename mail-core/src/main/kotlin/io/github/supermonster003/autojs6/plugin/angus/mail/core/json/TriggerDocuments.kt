package io.github.supermonster003.autojs6.plugin.angus.mail.core.json

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerConfig
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerFilter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive

/**
 * The status of one background watch (contract version 2: `IMailTrigger.getStatus`,
 * `IMailTriggerCallback.onStatus`, the `status` of a `listTriggers` entry; protocol document
 * "Trigger status"). [state] is one of [STATES]; [mode] is the running mode while connected;
 * [reason] explains a `stopped` state; [lastError] is the failure that ended or interrupted the
 * watch; [since] is when the state was entered; [lastMailAt] the last reported message.
 */
@Serializable
data class TriggerStatusDocument(
    val triggerId: String,
    val state: String,
    val mode: String? = null,
    val reason: String? = null,
    val lastError: ErrorDocument? = null,
    val since: Long,
    val lastMailAt: Long? = null,
) {
    fun toJson(): String = MailJson.format.encodeToString(this)

    companion object {
        const val STATE_STOPPED = "stopped"
        const val STATE_CONNECTING = "connecting"
        const val STATE_CONNECTED = "connected"
        const val STATE_FAILED = "failed"

        /** Mirrors the contract's `TRIGGER_STATES` (the app module's parity test compares them). */
        val STATES: Set<String> = setOf(STATE_STOPPED, STATE_CONNECTING, STATE_CONNECTED, STATE_FAILED)

        /** `reason` of a stopped watch that is switched off, missing from the store, or whose account is gone. */
        const val REASON_DISABLED = "disabled"
        const val REASON_REMOVED = "removed"
        const val REASON_SERVICE_STOPPED = "service-stopped"
    }
}

/** One entry of `listTriggers` (protocol document "Trigger list"): the configuration plus the live [status]. */
@Serializable
data class TriggerEntryDocument(
    val triggerId: String,
    val alias: String,
    /** The account's address, or null when the saved account no longer exists. */
    val address: String? = null,
    val enabled: Boolean,
    val folder: String,
    val mode: String? = null,
    val pollIntervalMs: Long,
    val filter: TriggerFilter,
    val since: Long,
    /** Trigger records kept for this watch (at most `MAX_TRIGGER_RECORDS`). */
    val records: Int = 0,
    val status: TriggerStatusDocument,
) {
    companion object {
        fun of(config: TriggerConfig, address: String?, records: Int, status: TriggerStatusDocument): TriggerEntryDocument = TriggerEntryDocument(
            triggerId = config.triggerId,
            alias = config.alias,
            address = address,
            enabled = config.enabled,
            folder = config.folder,
            mode = config.mode,
            pollIntervalMs = config.pollIntervalMs,
            filter = config.filter,
            since = config.since,
            records = records,
            status = status,
        )
    }
}

@Serializable
data class TriggerListDocument(val triggers: List<TriggerEntryDocument> = emptyList()) {
    fun toJson(): String = MailJson.format.encodeToString(this)
}

/**
 * The `eventJson` of `IMailTriggerCallback.onMail` and of the host wake-up intent (protocol
 * document "Trigger event"): one message that passed the watch's filter, as the envelope without
 * a body, with the watch and account it belongs to and when the plugin saw it.
 */
@Serializable
data class TriggerEventDocument(
    val type: String = TYPE_MAIL,
    val triggerId: String,
    val alias: String,
    val address: String,
    val folder: String,
    val message: MessageDocument,
    val receivedAt: Long,
) {
    fun toJson(): String = MailJson.format.encodeToString(this)

    companion object {
        /** Mirrors the contract's `TRIGGER_EVENT_MAIL`. */
        const val TYPE_MAIL = "mail"

        fun parse(json: String): TriggerEventDocument = try {
            MailJson.format.decodeFromString<TriggerEventDocument>(json)
        } catch (e: IllegalArgumentException) {
            throw MailException.invalidArgument("trigger event is not valid", e.message?.take(200))
        }
    }
}

/**
 * One line of a watch's trigger records (roadmap P8: the last `MAX_TRIGGER_RECORDS` arrivals,
 * envelope summary only, never a body): kept in `noBackupFilesDir` for the settings page.
 */
@Serializable
data class TriggerRecord(
    val receivedAt: Long,
    val folder: String,
    val uid: JsonPrimitive,
    val subject: String,
    val from: AddressDocument? = null,
    val date: Long? = null,
    val messageId: String? = null,
    val size: Long = 0,
) {
    companion object {
        /** Longest subject kept in a record (UTF-16 units); the message document itself allows `MAX_HEADER_VALUE_CHARS`. */
        const val MAX_SUBJECT_CHARS = 256

        fun of(message: MessageDocument, receivedAt: Long): TriggerRecord = TriggerRecord(
            receivedAt = receivedAt,
            folder = message.folder,
            uid = message.uid,
            subject = message.subject.take(MAX_SUBJECT_CHARS),
            from = message.from ?: message.sender,
            date = message.date,
            messageId = message.messageId?.take(MailLimits.MAX_ADDRESS_CHARS),
            size = message.size,
        )
    }
}

/** The stored records of one watch, newest first, at most `MAX_TRIGGER_RECORDS`. */
@Serializable
data class TriggerRecordsDocument(val records: List<TriggerRecord> = emptyList()) {

    fun toJson(): String = MailJson.format.encodeToString(this)

    /** The document with [record] in front and the oldest lines beyond [capacity] dropped. */
    fun prepend(record: TriggerRecord, capacity: Int = MailLimits.MAX_TRIGGER_RECORDS): TriggerRecordsDocument =
        TriggerRecordsDocument((listOf(record) + records).take(capacity))

    companion object {
        fun parse(json: String): TriggerRecordsDocument = try {
            MailJson.format.decodeFromString<TriggerRecordsDocument>(json)
        } catch (e: IllegalArgumentException) {
            throw MailException.invalidArgument("trigger records document is not valid", e.message?.take(200))
        }
    }
}
