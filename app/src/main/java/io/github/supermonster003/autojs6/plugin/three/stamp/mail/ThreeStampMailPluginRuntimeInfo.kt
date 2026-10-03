package io.github.supermonster003.autojs6.plugin.three.stamp.mail

/**
 * Pure-data view of the metadata reported through `IPluginInfoProvider.getInfo()` (and, once the
 * host contract is staged, `IMailPlugin.getInfo()` / `getCapabilities()`, roadmap P2.5).
 *
 * Android-specific lookups (package version, localized strings, raw resources) happen in
 * [threeStampMailPluginRuntimeInfo]; this class keeps the mapping itself testable on the JVM.
 */
data class ThreeStampMailPluginRuntimeInfo(
    val name: String,
    val description: String,
    val instruction: String?,
    val versionName: String,
    val versionCode: Long,
    val versionDate: String,
) {
    val author: String get() = ThreeStampMailPlugin.AUTHOR
    val id: String get() = ThreeStampMailPlugin.ID
    val engine: String get() = ThreeStampMailPlugin.ENGINE
    val variant: String get() = ThreeStampMailPlugin.VARIANT

    /** Empty on purpose: the plugin ships no native code and runs on any ABI. */
    val supportedAbis: Array<String> get() = emptyArray()

    val requiresHostVersion: Long get() = ThreeStampMailPlugin.REQUIRED_HOST_VERSION
}
