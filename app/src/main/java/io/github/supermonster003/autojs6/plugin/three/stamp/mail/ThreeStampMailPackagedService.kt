package io.github.supermonster003.autojs6.plugin.three.stamp.mail

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.binder.MailPluginBinder
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.binder.PackagedCallerGuard
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.message.MimeLeniency

/** Independently signed apps may open only accounts whose credentials they provide themselves. */
class ThreeStampMailPackagedService : Service() {
    private val binder: IBinder by lazy {
        MailPluginBinder(applicationContext, PackagedCallerGuard(applicationContext), allowStoredAccounts = false)
    }

    override fun onCreate() {
        super.onCreate()
        MimeLeniency.install()
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
