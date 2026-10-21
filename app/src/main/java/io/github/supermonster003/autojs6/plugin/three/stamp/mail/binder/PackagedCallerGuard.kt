package io.github.supermonster003.autojs6.plugin.three.stamp.mail.binder

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import org.autojs.plugin.mail.api.MailActions

/** The separate packaged entry permits inline credentials and pins handles to their creator. */
internal class PackagedCallerGuard(private val context: Context) : CallerGuard {
    override fun enforceHost(): Int {
        val uid = Binder.getCallingUid()
        if (context.checkPermission(MailActions.PACKAGED_PERMISSION, Binder.getCallingPid(), uid) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Mail runtime permission is required")
        }
        return uid
    }

    override fun enforceOwner(ownerUid: Int) {
        if (enforceHost() != ownerUid) throw SecurityException("Mail session belongs to another caller")
    }
}
