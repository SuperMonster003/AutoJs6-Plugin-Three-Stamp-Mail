package io.github.supermonster003.autojs6.plugin.angus.mail

import android.content.Context
import android.os.Build
import android.os.Bundle
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.mail.api.MailCapabilityKeys
import org.autojs.plugin.mail.api.MailContract

/** Collects the installed package version and the localized metadata of this plugin. */
internal fun Context.angusMailPluginRuntimeInfo(): AngusMailPluginRuntimeInfo {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }
    return AngusMailPluginRuntimeInfo(
        name = getString(R.string.app_name),
        description = getString(R.string.plugin_description),
        instruction = resources.openRawResource(R.raw.plugin_instruction)
            .bufferedReader()
            .use { it.readText() },
        versionName = packageInfo.versionName.orEmpty(),
        versionCode = versionCode,
        versionDate = getString(R.string.plugin_version_date),
    )
}

/** Maps the pure-data view onto the host contract parcelable. */
internal fun AngusMailPluginRuntimeInfo.toPluginInfo(): PluginInfo {
    val runtimeInfo = this
    return PluginInfo().apply {
        name = runtimeInfo.name
        description = runtimeInfo.description
        instruction = runtimeInfo.instruction
        author = runtimeInfo.author
        collaborators = null
        versionName = runtimeInfo.versionName
        versionCode = runtimeInfo.versionCode
        versionDate = runtimeInfo.versionDate
        id = runtimeInfo.id
        engine = runtimeInfo.engine
        variant = runtimeInfo.variant
        supportedAbis = runtimeInfo.supportedAbis
        capabilities = runtimeInfo.capabilitiesBundle()
    }
}

/**
 * The capabilities the host reads before it binds. Only the required host version is reported
 * until the mail contract (`MailCapabilityKeys`, roadmap P1.1) is staged; P2.5 adds the contract
 * version, the protocol set, the authentication mechanisms, and the feature set.
 */
internal fun AngusMailPluginRuntimeInfo.capabilitiesBundle(): Bundle = Bundle().apply {
    putLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION, requiresHostVersion)
    putInt(MailCapabilityKeys.CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
    putStringArray(MailCapabilityKeys.PROTOCOLS, AngusMailPlugin.PROTOCOLS.toTypedArray())
    putStringArray(MailCapabilityKeys.AUTH_MECHANISMS, AngusMailPlugin.AUTH_MECHANISMS.toTypedArray())
    putStringArray(MailCapabilityKeys.FEATURES, AngusMailPlugin.FEATURES.toTypedArray())
    putInt(MailCapabilityKeys.PROVIDERS_VERSION, AngusMailPlugin.PROVIDERS_VERSION)
    putString(MailCapabilityKeys.LIBRARY_VERSION, AngusMailPlugin.MAIL_LIBRARY_VERSION)
}
