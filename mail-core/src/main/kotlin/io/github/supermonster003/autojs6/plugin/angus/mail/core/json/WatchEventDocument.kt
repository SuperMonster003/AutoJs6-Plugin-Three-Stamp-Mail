package io.github.supermonster003.autojs6.plugin.angus.mail.core.json

import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * The `eventJson` document of `IMailWatchCallback.onEvent` (protocol document "Event", roadmap
 * D17): [type] is one of [WatchEventDocument.TYPES], [seq] increases by one per delivered event
 * of a watch and [generation] echoes the watch options.
 */
@Serializable
data class WatchEventDocument(
    val type: String,
    val seq: Long,
    val generation: Long,
    val folder: String,
    val message: MessageDocument? = null,
    val mode: String? = null,
    val reason: String? = null,
    val error: ErrorDocument? = null,
) {
    companion object {
        const val TYPE_MESSAGE = "message"
        const val TYPE_MODE = "mode"
        const val TYPE_RESYNC = "resync"
        const val TYPE_ERROR = "error"
        const val TYPE_CLOSED = "closed"

        /** Mirrors the contract's `EVENT_TYPES` (the app module's parity test compares them). */
        val TYPES: Set<String> = setOf(TYPE_MESSAGE, TYPE_ERROR, TYPE_CLOSED, TYPE_MODE, TYPE_RESYNC)
    }
}

fun WatchEvent.toDocument(seq: Long, generation: Long): WatchEventDocument = when (this) {
    is WatchEvent.Message -> WatchEventDocument(WatchEventDocument.TYPE_MESSAGE, seq, generation, folder, message = message)
    is WatchEvent.Mode -> WatchEventDocument(WatchEventDocument.TYPE_MODE, seq, generation, folder, mode = mode.id)
    is WatchEvent.Resync -> WatchEventDocument(WatchEventDocument.TYPE_RESYNC, seq, generation, folder, reason = reason)
    is WatchEvent.Error -> WatchEventDocument(WatchEventDocument.TYPE_ERROR, seq, generation, folder, error = error.toDocument())
    is WatchEvent.Closed -> WatchEventDocument(WatchEventDocument.TYPE_CLOSED, seq, generation, folder, reason = reason)
}

fun WatchEventDocument.toJson(): String = MailJson.format.encodeToString(this)
