package io.github.supermonster003.autojs6.plugin.angus.mail.trigger

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Restarts the background watches after a boot (roadmap P8: an opt-in switch of the watches
 * page, off by default). The component is disabled in the manifest; the switch enables it
 * through the package manager, so a device never wakes this plugin at boot unless the user
 * asked. `RECEIVE_BOOT_COMPLETED` is the only permission involved; the receiver starts the
 * foreground service only when at least one watch is enabled.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val enabled = runCatching { TriggerStores.of(context).hasEnabled() }.getOrDefault(false)
        val started = enabled && MailWatchService.start(context)
        android.util.Log.i("MailBootReceiver", "boot completed, watches enabled=$enabled service started=$started")
    }

    companion object {
        private fun component(context: Context) = ComponentName(context, BootReceiver::class.java)

        fun isEnabled(context: Context): Boolean =
            context.packageManager.getComponentEnabledSetting(component(context)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        fun setEnabled(context: Context, enabled: Boolean) {
            context.packageManager.setComponentEnabledSetting(
                component(context),
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
