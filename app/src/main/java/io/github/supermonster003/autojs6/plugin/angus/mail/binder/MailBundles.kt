package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import android.os.Bundle
import android.os.RemoteException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchEvent
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchStatus
import org.autojs.plugin.mail.api.IMailCallCallback
import org.autojs.plugin.mail.api.IMailSessionCallback
import org.autojs.plugin.mail.api.MailContract
import org.autojs.plugin.mail.api.MailErrorCodes
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/** Bundle and envelope helpers of the plugin side; every document is built with `org.json`. */
internal object MailBundles {

    /** Key under which `onProgress` carries the redacted protocol trace lines (roadmap D28). */
    const val FIELD_DEBUG = "debug"

    /** Extra `getStatus` field: protocols with a live connection. */
    const val FIELD_CONNECTED = "connected"

    /** Extra `getStatus` fields (roadmap P2.5): calls waiting behind the one in flight, and the id of the one in flight. */
    const val FIELD_QUEUED = "queued"
    const val FIELD_ACTIVE = "active"

    /** Extra `getStatus` field (roadmap P5): watches of the session that have not closed. */
    const val FIELD_WATCHES = "watches"

    fun json(key: String, document: String): Bundle = Bundle().apply {
        putInt(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
        putString(key, document)
    }

    fun status(
        state: String,
        reason: String? = null,
        lastError: JSONObject? = null,
        connected: List<String> = emptyList(),
        queued: Int = 0,
        active: String? = null,
        watches: Int = 0,
    ): Bundle {
        val document = JSONObject().put(MailContract.FIELD_STATE, state)
        reason?.let { document.put(MailContract.FIELD_REASON, it) }
        lastError?.let { document.put(MailContract.FIELD_LAST_ERROR, it) }
        document.put(FIELD_CONNECTED, JSONArray(connected))
        document.put(FIELD_QUEUED, queued)
        active?.let { document.put(FIELD_ACTIVE, it) }
        document.put(FIELD_WATCHES, watches)
        return json(MailContract.KEY_STATUS_JSON, document.toString())
    }

    /** `IMailWatch.getStatus` (protocol document "Status"): state, mode, and after the close its reason and last error. */
    fun watchStatus(status: WatchStatus): Bundle {
        val document = JSONObject()
            .put(MailContract.FIELD_STATE, if (status.active) MailContract.STATE_OPEN else MailContract.STATE_CLOSED)
            .put(MailContract.FIELD_MODE, status.mode.id)
        status.reason?.let { document.put(MailContract.FIELD_REASON, it) }
        status.lastError?.let { document.put(MailContract.FIELD_LAST_ERROR, error(it)) }
        return json(MailContract.KEY_STATUS_JSON, document.toString())
    }

    /** The envelope form of a `message` event whose full form does not fit the envelope ceiling. */
    fun withoutBody(event: WatchEvent.Message): WatchEvent.Message = event.copy(
        message = event.message.copy(
            bodyLoaded = false,
            text = null,
            html = null,
            headers = emptyMap(),
            bodyTruncated = true,
            bodyParts = emptyList(),
            raw = null,
            rawTruncated = false,
        ),
    )

    /** An error document; the message is clamped to `MAX_ERROR_MESSAGE_BYTES` like the host does on its side. */
    fun error(code: String, message: String, retryable: Boolean = MailErrorCodes.isRetryableByDefault(code), details: String? = null): JSONObject {
        val document = JSONObject()
            .put(MailContract.FIELD_ERROR_CODE, code)
            .put(MailContract.FIELD_ERROR_MESSAGE, Limits.clampErrorMessage(message))
            .put(MailContract.FIELD_ERROR_RETRYABLE, retryable)
        details?.let { document.put(MailContract.FIELD_ERROR_DETAILS, it) }
        return document
    }

    /** True for a response bundle built by [success] / [successJson]. */
    fun isSuccess(response: Bundle): Boolean {
        val json = response.getString(MailContract.KEY_RESPONSE_JSON) ?: return false
        return runCatching { JSONObject(json).optBoolean(MailContract.FIELD_OK, false) }.getOrDefault(false)
    }

    fun error(exception: MailException): JSONObject = error(exception.code, exception.message, exception.retryable, exception.details)

    fun failure(requestId: String?, error: JSONObject): Bundle {
        val document = JSONObject()
            .put(MailContract.FIELD_ID, requestId ?: JSONObject.NULL)
            .put(MailContract.FIELD_OK, false)
            .put(MailContract.FIELD_ERROR, error)
        return json(MailContract.KEY_RESPONSE_JSON, document.toString())
    }

    fun success(requestId: String, result: Any?): Bundle {
        val document = JSONObject()
            .put(MailContract.FIELD_ID, requestId)
            .put(MailContract.FIELD_OK, true)
            .put(MailContract.FIELD_RESULT, result ?: JSONObject.NULL)
        return json(MailContract.KEY_RESPONSE_JSON, document.toString())
    }

    /** Wraps a result that is already a JSON document (object, array, string, number, boolean or null). */
    fun successJson(requestId: String, resultJson: String): Bundle = success(requestId, JSONTokener(resultJson).nextValue())

    /** The `onProgress` document carrying redacted trace lines; the host prints them to `console.verbose`. */
    fun debugProgress(requestId: String, lines: List<String>): Bundle {
        val document = JSONObject()
            .put(MailContract.FIELD_ID, requestId)
            .put(FIELD_DEBUG, JSONArray(lines))
        return json(MailContract.KEY_PROGRESS_JSON, document.toString())
    }

    /** The `onProgress` document of a transfer (`messages.raw`, `attachments.download`): `{id, transferred, total?}`. */
    fun transferProgress(requestId: String, transferred: Long, total: Long?): Bundle {
        val document = JSONObject()
            .put(MailContract.FIELD_ID, requestId)
            .put(MailContract.FIELD_TRANSFERRED, transferred)
        total?.let { document.put(MailContract.FIELD_TOTAL, it) }
        return json(MailContract.KEY_PROGRESS_JSON, document.toString())
    }

    /** Returns the error document when the open-session bundle is unusable, or null when it is fine. */
    fun validateAccount(account: Bundle?): JSONObject? {
        account ?: return error(MailErrorCodes.INVALID_ARGUMENT, "account bundle is missing")
        val version = account.getInt(MailContract.KEY_CONTRACT_VERSION, 0)
        if (!MailContract.supportsContractVersion(version)) {
            return error(MailErrorCodes.INVALID_ARGUMENT, "unsupported contract version $version")
        }
        val alias = account.getString(MailContract.KEY_ACCOUNT_ALIAS)
        val json = account.getString(MailContract.KEY_ACCOUNT_JSON)
        if (alias == null && json == null) {
            return error(MailErrorCodes.INVALID_ARGUMENT, "account JSON or alias is required")
        }
        if (alias != null && (json != null || account.containsKey(MailContract.KEY_SECRET_PASSWORD) || account.containsKey(MailContract.KEY_SECRET_ACCESS_TOKEN))) {
            return error(MailErrorCodes.INVALID_ARGUMENT, "account alias excludes inline account fields")
        }
        if (json != null) {
            runCatching { JSONObject(json) }.getOrElse { return error(MailErrorCodes.INVALID_ARGUMENT, "account is not a JSON object") }
        }
        if (account.containsKey(MailContract.KEY_SECRET_PASSWORD) && account.containsKey(MailContract.KEY_SECRET_ACCESS_TOKEN)) {
            return error(MailErrorCodes.INVALID_ARGUMENT, "password and access token are exclusive")
        }
        return null
    }

    /** Which secret accompanies the account; [validateAccount] has already excluded both at once. */
    fun secretKind(account: Bundle): SecretKind = when {
        account.containsKey(MailContract.KEY_SECRET_PASSWORD) -> SecretKind.PASSWORD
        account.containsKey(MailContract.KEY_SECRET_ACCESS_TOKEN) -> SecretKind.ACCESS_TOKEN
        else -> SecretKind.NONE
    }

    /** The secret characters, or null when the key is absent; the caller wraps them in `MailSecret` at once. */
    fun secret(account: Bundle, kind: SecretKind): String? = when (kind) {
        SecretKind.PASSWORD -> account.getString(MailContract.KEY_SECRET_PASSWORD)
        SecretKind.ACCESS_TOKEN -> account.getString(MailContract.KEY_SECRET_ACCESS_TOKEN)
        // A browser sign-in never travels over the Binder: its tokens live in the plugin's store only.
        SecretKind.OAUTH2, SecretKind.NONE -> null
    }

    fun notifyClosed(callback: IMailSessionCallback, lastError: JSONObject?, reason: String = "refused") {
        try {
            callback.onStatus(status(MailContract.STATE_CLOSED, reason, lastError))
        } catch (_: RemoteException) {
        }
    }

    fun deliver(callback: IMailCallCallback?, response: Bundle) {
        callback ?: return
        try {
            callback.onResult(response)
        } catch (_: RemoteException) {
        }
    }

    fun progress(callback: IMailCallCallback?, progress: Bundle) {
        callback ?: return
        try {
            callback.onProgress(progress)
        } catch (_: RemoteException) {
        }
    }

    /** Reads the request id without trusting the rest of the envelope. */
    fun requestId(request: Bundle?): String? {
        val json = request?.getString(MailContract.KEY_REQUEST_JSON) ?: return null
        return runCatching { JSONObject(json).optString(MailContract.FIELD_ID).takeIf { it.isNotEmpty() } }.getOrNull()
    }

    fun requestOp(request: Bundle?): String? {
        val json = request?.getString(MailContract.KEY_REQUEST_JSON) ?: return null
        return runCatching { JSONObject(json).optString(MailContract.FIELD_OP).takeIf { it.isNotEmpty() } }.getOrNull()
    }

    /** The `args` object as a JSON string, `{}` when absent; null when present but not an object. */
    fun requestArgs(request: Bundle?): String? {
        val json = request?.getString(MailContract.KEY_REQUEST_JSON) ?: return "{}"
        return runCatching {
            val root = JSONObject(json)
            if (!root.has(MailContract.FIELD_ARGS) || root.isNull(MailContract.FIELD_ARGS)) "{}" else root.getJSONObject(MailContract.FIELD_ARGS).toString()
        }.getOrNull()
    }
}
