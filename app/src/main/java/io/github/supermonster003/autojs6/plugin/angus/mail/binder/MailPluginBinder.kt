package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import android.content.Context
import android.os.Bundle
import io.github.supermonster003.autojs6.plugin.angus.mail.angusMailPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.angus.mail.capabilitiesBundle
import io.github.supermonster003.autojs6.plugin.angus.mail.toPluginInfo
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.mail.api.IMailPlugin
import org.autojs.plugin.mail.api.IMailSession
import org.autojs.plugin.mail.api.IMailSessionCallback
import org.autojs.plugin.mail.api.MailContract

/**
 * `IMailPlugin` implementation (roadmap P1.3 / P2.5). Discovery methods are complete; sessions
 * are [PlaceholderMailSession] instances until P2 routes operations to the mail core.
 */
internal class MailPluginBinder(private val context: Context) : IMailPlugin.Stub() {

    override fun getInfo(): PluginInfo = context.angusMailPluginRuntimeInfo().toPluginInfo()

    override fun getCapabilities(): Bundle = context.angusMailPluginRuntimeInfo().capabilitiesBundle()

    override fun openSession(account: Bundle?, callback: IMailSessionCallback?): IMailSession? {
        val validation = MailBundles.validateAccount(account)
        if (validation != null) {
            callback?.let { MailBundles.notifyClosed(it, validation) }
            return null
        }
        return PlaceholderMailSession(callback)
    }

    override fun listProviders(): Bundle = MailBundles.json(MailContract.KEY_PROVIDERS_JSON, "[]")

    override fun listSavedAccounts(): Bundle = MailBundles.json(MailContract.KEY_ACCOUNTS_JSON, "[]")
}
