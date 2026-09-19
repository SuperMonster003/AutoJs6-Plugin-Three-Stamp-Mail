package io.github.supermonster003.autojs6.plugin.angus.mail

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.HostCallerGuard
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.MailPluginBinder
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MimeLeniency
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStores

/**
 * Entry point the AutoJs6 host binds to (action `org.autojs.plugin.MAIL`, category `mail`).
 *
 * Hands out the `IMailPlugin.Stub` of [MailPluginBinder]: plugin info and capabilities answer the
 * host contract of `mail-api.aar`, sessions speak the JSON envelope of roadmap appendix B, saved
 * accounts come from the process-wide [AccountStores] instance (roadmap P4), and only the installed
 * same-signer AutoJs6 host may open a session ([HostCallerGuard], roadmap P2.5).
 */
class AngusMailPluginService : Service() {

    private val binder: IBinder by lazy {
        MailPluginBinder(applicationContext, HostCallerGuard(applicationContext), AccountStores.of(applicationContext))
    }

    override fun onCreate() {
        super.onCreate()
        // Some Jakarta MIME leniency switches are read once when the Jakarta class loads (roadmap P6 hostile input).
        MimeLeniency.install()
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
