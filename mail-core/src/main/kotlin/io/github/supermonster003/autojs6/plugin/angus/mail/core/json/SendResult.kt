package io.github.supermonster003.autojs6.plugin.angus.mail.core.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/** Result document of `mail.send` (protocol document: `{messageId, accepted, rejected}` plus the sent-copy outcome). */
@Serializable
data class SendResult(
    val messageId: String,
    /** Recipients the server accepted; with the all-or-nothing policy of the plugin this is every recipient. */
    val accepted: List<String>,
    /** Always empty on success: a rejected recipient fails the whole send with `SEND_REJECTED`. */
    val rejected: List<String> = emptyList(),
    /** True when the plugin appended a copy to the sent folder. */
    val savedToSent: Boolean,
    /**
     * Where the sent copy is: [SENT_COPY_SERVER] when the provider files it itself (the plugin never
     * appends a duplicate, QQ even refuses one with `NO Mail has saved by smtp!`), [SENT_COPY_APPENDED]
     * when the plugin appended it to [sentFolder], [SENT_COPY_FAILED] when the append failed (see
     * [saveError]) and [SENT_COPY_NONE] when no copy was wanted or no sent folder exists.
     */
    val sentCopy: String,
    /** The folder the copy went to, or the folder that was looked for. */
    val sentFolder: String? = null,
    /** Set when the message went out but the sent copy could not be appended. */
    val saveError: ErrorDocument? = null,
    val elapsedMs: Long,
) {
    companion object {
        const val SENT_COPY_SERVER = "server"
        const val SENT_COPY_APPENDED = "appended"
        const val SENT_COPY_FAILED = "failed"
        const val SENT_COPY_NONE = "none"
    }
}

/** Result document of `messages.append`. */
@Serializable
data class AppendResult(
    val folder: String,
    /** UID of the appended message when the server supports `UIDPLUS`, otherwise null. */
    val uid: Long? = null,
)

fun SendResult.toJson(): String = MailJson.format.encodeToString(this)

fun AppendResult.toJson(): String = MailJson.format.encodeToString(this)
