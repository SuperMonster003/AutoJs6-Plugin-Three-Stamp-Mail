package io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MailJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.query.MessageArgs
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchOptions
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * One background watch as the plugin keeps it (roadmap P8): a saved account [alias] (never
 * inline credentials), the [folder], the requested [mode] (null: IDLE when the server pushes,
 * else polling, as `Watchers.open` decides), the poll interval, the [filter] every reported
 * message must pass, and whether the watch is [enabled]. [since] is the creation time in UTC
 * milliseconds. The settings page edits it, `listTriggers` renders it, the watch keeper runs it.
 */
@Serializable
data class TriggerConfig(
    val triggerId: String,
    val alias: String,
    val enabled: Boolean = true,
    val folder: String = MessageArgs.INBOX,
    val mode: String? = null,
    val pollIntervalMs: Long = MailLimits.DEFAULT_POLL_INTERVAL_MS,
    val filter: TriggerFilter = TriggerFilter.NONE,
    val since: Long = 0L,
) {
    val watchMode: WatchMode? get() = mode?.let(WatchMode::fromId)

    /** The P5 watch options of this watch; the account's protocol decides at start whether they are usable. */
    fun watchOptions(receive: MailProtocol): WatchOptions =
        WatchOptions.parse(MailJson.format.encodeToString(WatchOptionsShape(folder, mode, pollIntervalMs)), receive)

    /**
     * The canonical form: trigger id and alias normalized, the folder trimmed, the interval
     * clamped into the contract range, the filter normalized; refuses an unknown mode, a blank
     * folder or a filter beyond its ceilings with `INVALID_ARGUMENT`.
     */
    fun validated(): TriggerConfig {
        val id = TriggerId.normalize(triggerId)
        val canonicalAlias = TriggerId.normalize(alias, "alias")
        val canonicalFolder = folder.trim()
        if (canonicalFolder.isEmpty()) throw MailException.invalidArgument("'folder' must not be blank")
        if (canonicalFolder.length > MailLimits.MAX_OPTION_STRING_LENGTH) {
            throw MailException.invalidArgument("'folder' exceeds ${MailLimits.MAX_OPTION_STRING_LENGTH} characters")
        }
        if (mode != null && WatchMode.fromId(mode) == null) {
            throw MailException.invalidArgument("'mode' must be one of ${WatchMode.entries.joinToString(", ") { it.id }}: '$mode'")
        }
        if (pollIntervalMs <= 0) throw MailException.invalidArgument("'pollIntervalMs' must be positive: $pollIntervalMs")
        return copy(
            triggerId = id,
            alias = canonicalAlias,
            folder = canonicalFolder,
            pollIntervalMs = pollIntervalMs.coerceIn(MailLimits.MIN_POLL_INTERVAL_MS, MailLimits.MAX_POLL_INTERVAL_MS),
            filter = filter.normalized(),
        )
    }

    fun toJson(): String = MailJson.format.encodeToString(this)

    companion object {
        /** Parses a stored or submitted document leniently (unknown fields ignored) and validates it. */
        fun parse(json: String): TriggerConfig = try {
            MailJson.format.decodeFromString<TriggerConfig>(json)
        } catch (e: IllegalArgumentException) {
            throw MailException.invalidArgument("trigger document is not valid", e.message?.take(200))
        }.validated()
    }

    /** The subset of the P5 watch options document a background watch fixes (`fetchBody` stays false). */
    @Serializable
    private data class WatchOptionsShape(val folder: String, val mode: String? = null, val pollIntervalMs: Long)
}

/** The stored list of background watches (`mail-triggers/triggers.json`), at most `MAX_TRIGGERS`. */
@Serializable
data class TriggerConfigsDocument(val triggers: List<TriggerConfig> = emptyList()) {

    fun toJson(): String = MailJson.format.encodeToString(this)

    companion object {
        fun parse(json: String): TriggerConfigsDocument = try {
            MailJson.format.decodeFromString<TriggerConfigsDocument>(json)
        } catch (e: IllegalArgumentException) {
            throw MailException.invalidArgument("trigger list document is not valid", e.message?.take(200))
        }

        /**
         * Reads a stored document entry by entry: entries that do not decode or do not validate are
         * left out instead of failing the whole list, so one damaged watch does not hide the others.
         * A document that is not an object with a `triggers` array yields an empty list.
         */
        fun parseLenient(json: String): TriggerConfigsDocument {
            val root = runCatching { MailJson.format.parseToJsonElement(json) }.getOrNull() as? JsonObject ?: return TriggerConfigsDocument()
            val entries = root[FIELD_TRIGGERS] as? JsonArray ?: return TriggerConfigsDocument()
            return TriggerConfigsDocument(entries.mapNotNull { entry -> runCatching { MailJson.format.decodeFromJsonElement<TriggerConfig>(entry).validated() }.getOrNull() })
        }

        const val FIELD_TRIGGERS = "triggers"
    }
}
