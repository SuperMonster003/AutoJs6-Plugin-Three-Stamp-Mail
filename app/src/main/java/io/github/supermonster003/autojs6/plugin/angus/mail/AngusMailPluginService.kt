package io.github.supermonster003.autojs6.plugin.angus.mail

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.MailPluginBinder

/**
 * Entry point the AutoJs6 host binds to (action `org.autojs.plugin.MAIL`, category `mail`).
 *
 * Hands out the `IMailPlugin.Stub` of [MailPluginBinder]: plugin info and capabilities answer the
 * host contract of `mail-api.aar`, and sessions already speak the JSON envelope. Until roadmap P2
 * lands the request router, every operation is answered with `UNSUPPORTED_OPERATION`.
 */
class AngusMailPluginService : Service() {

    private val binder: IBinder by lazy { MailPluginBinder(applicationContext) }

    override fun onBind(intent: Intent?): IBinder = binder
}
