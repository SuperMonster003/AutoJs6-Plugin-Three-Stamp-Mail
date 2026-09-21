package io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger

import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.argsObject
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.obj
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.rejectUnknown
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.string

/**
 * The `triggerOptionsJson` document of `IMailPlugin.openTrigger` and `IMailTrigger.update`
 * (contract version 2): which background watch to subscribe to and the subscriber's own
 * [filter], applied after the watch's filter. `generation` travels in the same document but
 * belongs to the Binder envelope, like the P5 watch options.
 */
data class TriggerOptions(
    val triggerId: String,
    val filter: TriggerFilter = TriggerFilter.NONE,
) {
    companion object {
        const val FIELD_TRIGGER_ID = "triggerId"
        const val FIELD_FILTER = "filter"
        val FIELDS: Set<String> = setOf(FIELD_TRIGGER_ID, FIELD_FILTER, "generation")

        fun parse(json: String?): TriggerOptions {
            val args = argsObject(json?.takeIf { it.isNotBlank() } ?: "{}", "trigger options")
            args.rejectUnknown("trigger options", FIELDS)
            val triggerId = TriggerId.normalize(args.string(FIELD_TRIGGER_ID))
            return TriggerOptions(triggerId, TriggerFilter.parse(args.obj(FIELD_FILTER)))
        }
    }
}
