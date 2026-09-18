package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import java.security.MessageDigest

/**
 * Who may open and use sessions (roadmap P2.5 `CallerGuard`). [HostCallerGuard] applies
 * [CallerPolicy] to the real caller through `Binder.getCallingUid` and the package manager;
 * [trusting] accepts everybody, for the in-process instrumentation tests whose caller is the
 * plugin's own UID.
 */
internal interface CallerGuard {

    /** The calling UID once it passed every check; throws [SecurityException] otherwise. */
    fun enforceHost(): Int

    /** A session method may only be called by the UID that opened the session, and it must still be the host. */
    fun enforceOwner(ownerUid: Int)

    companion object {
        fun trusting(): CallerGuard = object : CallerGuard {
            override fun enforceHost(): Int = Binder.getCallingUid()
            override fun enforceOwner(ownerUid: Int) = Unit
        }
    }
}

/** The production guard: only the installed same-signer AutoJs6 host of at least the required build passes. */
internal class HostCallerGuard(context: Context) : CallerGuard {

    private val packageManager = context.applicationContext.packageManager
    private val pluginPackageName = context.applicationContext.packageName

    override fun enforceHost(): Int = Binder.getCallingUid().also(::enforce)

    override fun enforceOwner(ownerUid: Int) {
        val callingUid = Binder.getCallingUid()
        if (callingUid != ownerUid) throw CallerPolicy.exception("uid $callingUid is not the uid $ownerUid that opened the session")
        enforce(callingUid)
    }

    private fun enforce(callingUid: Int) {
        val caller = CallerPolicy.Caller(callingUid, packageManager.getPackagesForUid(callingUid)?.toSet().orEmpty())
        val hosts = CallerPolicy.HOST_PACKAGE_NAMES.mapNotNull(::installedHost)
        CallerPolicy.refusal(caller, hosts, signerDigests(pluginPackageName))?.let { throw CallerPolicy.exception(it) }
    }

    private fun installedHost(packageName: String): CallerPolicy.InstalledHost? {
        val uid = try {
            packageManager.getApplicationInfo(packageName, 0).uid
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
        val versionCode = installedVersionCode(packageName) ?: return null
        return CallerPolicy.InstalledHost(packageName, uid, versionCode, signerDigests(packageName))
    }

    private fun installedVersionCode(packageName: String): Long? = try {
        val packageInfo = packageInfo(packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private fun signerDigests(packageName: String): Set<String> {
        val packageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                packageInfo(packageName, PackageManager.GET_SIGNATURES)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            return emptySet()
        }
        val signers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures.orEmpty()
        }
        return signers.mapTo(linkedSetOf()) { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
        }
    }

    private fun packageInfo(packageName: String, flags: Int): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, flags)
        }
}
