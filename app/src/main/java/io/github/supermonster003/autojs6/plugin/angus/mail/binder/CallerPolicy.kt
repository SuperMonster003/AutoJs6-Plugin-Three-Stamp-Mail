package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import io.github.supermonster003.autojs6.plugin.angus.mail.AngusMailPlugin

/**
 * The decision behind [CallerGuard], free of Android types so the JVM tests can cover every
 * branch (roadmap P2.5 `CallerGuard`, aligned with the MCP Server plugin's `HostCallerVerifier`):
 * the calling UID must be the UID the installed AutoJs6 host runs under, the host must be at least
 * the build that ships the mail contract, and the host and this plugin must carry the same signer
 * set. The manifest permission `org.autojs.permission.PLUGIN` keeps strangers from binding at all;
 * this is the second line for a caller that obtained the permission anyway.
 */
internal object CallerPolicy {

    /** Package names accepted as the host; the AutoJs6 host has no debug application id suffix. */
    val HOST_PACKAGE_NAMES: List<String> = listOf(AngusMailPlugin.HOST_PACKAGE_NAME)

    /** What `Binder.getCallingUid` and `PackageManager.getPackagesForUid` say about the caller. */
    data class Caller(val uid: Int, val packages: Set<String>)

    /** What the package manager says about an installed host package; SHA-256 digests of the signers. */
    data class InstalledHost(val packageName: String, val uid: Int, val versionCode: Long, val signers: Set<String>)

    /**
     * Null when [caller] is the installed host, otherwise why it is not. Every reason names the
     * AutoJs6 host so a refused caller can tell the check apart from a contract error.
     */
    fun refusal(
        caller: Caller,
        hosts: List<InstalledHost>,
        pluginSigners: Set<String>,
        requiredHostVersion: Long = AngusMailPlugin.REQUIRED_HOST_VERSION,
    ): String? {
        val installed = hosts.filter { it.packageName in HOST_PACKAGE_NAMES }
        if (installed.isEmpty()) return "the AutoJs6 host (${HOST_PACKAGE_NAMES.joinToString()}) is not installed"
        val host = installed.firstOrNull { it.uid == caller.uid }
            ?: return "uid ${caller.uid} is not the installed AutoJs6 host"
        if (host.packageName !in caller.packages) return "the AutoJs6 host does not run under uid ${caller.uid}"
        if (host.versionCode < requiredHostVersion) return "AutoJs6 host build ${host.versionCode} is older than the required build $requiredHostVersion"
        if (pluginSigners.isEmpty()) return "the plugin's own signer set is empty, so no AutoJs6 host can match it"
        if (host.signers != pluginSigners) return "the AutoJs6 host is not signed by the plugin's signer"
        return null
    }

    /** The [SecurityException] every refusal becomes; the wording matches the MCP Server plugin. */
    fun exception(reason: String): SecurityException = SecurityException("Caller is not the installed same-signer AutoJs6 host: $reason")
}
