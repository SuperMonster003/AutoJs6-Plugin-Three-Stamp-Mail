package io.github.supermonster003.autojs6.plugin.angus.mail.trigger

import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.supermonster003.autojs6.plugin.angus.mail.AngusMailPlugin
import org.autojs.plugin.mail.api.MailActions
import org.autojs.plugin.mail.api.MailContract

/**
 * Wakes the AutoJs6 host when a background watch reports mail and no `openTrigger` subscription
 * is alive (roadmap P8, contract version 2): an explicit broadcast to the host package with
 * `MailActions.HOST_TRIGGER_ACTION`, sent and received behind the plugin permission. The host's
 * receiver is looked up first, so a host without it (mail contract version 1) is never woken;
 * a force-stopped host does not receive it either (no `FLAG_INCLUDE_STOPPED_PACKAGES`), which
 * respects the user's choice. The event document is the same as `IMailTriggerCallback.onMail`.
 */
internal open class HostTriggerSender(context: Context) {

    private companion object {
        const val TAG = "MailTriggerSender"
    }

    private val application = context.applicationContext

    /** True when the installed host declares a receiver for the trigger action. */
    open fun isHostReceiverPresent(): Boolean {
        val receivers = runCatching { application.packageManager.queryBroadcastReceivers(intent(), 0) }.getOrDefault(emptyList())
        return receivers.any { it.activityInfo?.packageName == AngusMailPlugin.HOST_PACKAGE_NAME }
    }

    /** Sends the event; false when the host has no receiver or the broadcast could not be sent. */
    open fun send(triggerId: String, eventJson: String): Boolean {
        if (!isHostReceiverPresent()) {
            Log.w(TAG, "watch=$triggerId host receiver missing, no broadcast")
            return false
        }
        val intent = intent()
            .putExtra(MailContract.KEY_TRIGGER_ID, triggerId)
            .putExtra(MailContract.KEY_EVENT_JSON, eventJson)
        val sent = runCatching { application.sendBroadcast(intent, MailActions.PLUGIN_PERMISSION) }.isSuccess
        Log.i(TAG, "watch=$triggerId broadcast sent=$sent bytes=${eventJson.length}")
        return sent
    }

    private fun intent(): Intent = Intent(MailActions.HOST_TRIGGER_ACTION)
        .setPackage(AngusMailPlugin.HOST_PACKAGE_NAME)
        .putExtra(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
}
