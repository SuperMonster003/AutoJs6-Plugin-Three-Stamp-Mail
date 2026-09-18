package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import android.os.Bundle
import android.os.RemoteException
import org.autojs.plugin.mail.api.IMailSessionCallback
import org.autojs.plugin.mail.api.MailContract
import org.autojs.plugin.mail.api.MailErrorCodes
import org.json.JSONObject

/** Bundle and envelope helpers of the plugin side; every document is built with `org.json`. */
internal object MailBundles {

    fun json(key: String, document: String): Bundle = Bundle().apply {
        putInt(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
        putString(key, document)
    }

    fun status(state: String, reason: String? = null, lastError: JSONObject? = null): Bundle {
        val document = JSONObject().put(MailContract.FIELD_STATE, state)
        reason?.let { document.put(MailContract.FIELD_REASON, it) }
        lastError?.let { document.put(MailContract.FIELD_LAST_ERROR, it) }
        return json(MailContract.KEY_STATUS_JSON, document.toString())
    }

    fun error(code: String, message: String, retryable: Boolean = MailErrorCodes.isRetryableByDefault(code)): JSONObject {
        return JSONObject()
            .put(MailContract.FIELD_ERROR_CODE, code)
            .put(MailContract.FIELD_ERROR_MESSAGE, message)
            .put(MailContract.FIELD_ERROR_RETRYABLE, retryable)
    }

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
        if (alias != null && json != null) {
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

    fun notifyClosed(callback: IMailSessionCallback, lastError: JSONObject?, reason: String = "refused") {
        try {
            callback.onStatus(status(MailContract.STATE_CLOSED, reason, lastError))
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
}
