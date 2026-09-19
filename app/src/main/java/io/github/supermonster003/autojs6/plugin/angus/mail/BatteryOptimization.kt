package io.github.supermonster003.autojs6.plugin.angus.mail

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * The battery-optimization guide of roadmap P4.6 (decision D27): reads whether the system may
 * pause this plugin in the background and names the two system screens the guide row opens.
 * Nothing here runs on its own; the settings row is the only caller, nothing is requested on
 * start, and the exclusion is never a precondition of any feature.
 */
internal object BatteryOptimization {
    const val PERMISSION = Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS

    fun isIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * The system dialog asking to exclude this plugin; backed by [PERMISSION]. The BatteryLife lint
     * note concerns store policy; the plugin is sideloaded through the AutoJs6 plugin center (D27).
     */
    @SuppressLint("BatteryLife")
    fun exclusionRequest(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + context.packageName))

    /** The system list where an exclusion can be reverted. */
    fun exclusionList(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    /** Starts [intent] if the device offers the screen; false otherwise, never throws. */
    fun open(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
