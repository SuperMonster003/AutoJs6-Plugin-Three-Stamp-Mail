package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import android.content.Context
import android.os.Bundle
import io.github.supermonster003.autojs6.plugin.angus.mail.AngusMailPlugin
import io.github.supermonster003.autojs6.plugin.angus.mail.angusMailPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.angus.mail.capabilitiesBundle
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.ProviderPresets
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSession
import io.github.supermonster003.autojs6.plugin.angus.mail.toPluginInfo
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.mail.api.IMailPlugin
import org.autojs.plugin.mail.api.IMailSession
import org.autojs.plugin.mail.api.IMailSessionCallback
import org.autojs.plugin.mail.api.MailContract

/**
 * `IMailPlugin` implementation (roadmap P1.3 / P2.5). `openSession` validates the bundle, parses
 * the account JSON through the mail core, and returns a [MailSessionBinder]; no network happens
 * before the first `call` (contract B.3). Saved-account aliases arrive with P4.
 */
internal class MailPluginBinder(private val context: Context) : IMailPlugin.Stub() {

    override fun getInfo(): PluginInfo = context.angusMailPluginRuntimeInfo().toPluginInfo()

    override fun getCapabilities(): Bundle = context.angusMailPluginRuntimeInfo().capabilitiesBundle()

    override fun openSession(account: Bundle?, callback: IMailSessionCallback?): IMailSession? {
        MailBundles.validateAccount(account)?.let { validation ->
            callback?.let { MailBundles.notifyClosed(it, validation) }
            return null
        }
        account!!
        val json = account.getString(MailContract.KEY_ACCOUNT_JSON)
        if (json == null) {
            callback?.let { MailBundles.notifyClosed(it, MailBundles.error(MailErrorCode.ACCOUNT_NOT_FOUND, "saved accounts are not available yet (roadmap P4)", retryable = false)) }
            return null
        }
        val kind = MailBundles.secretKind(account)
        val secretText = MailBundles.secret(account, kind)
        val session = try {
            if (kind != SecretKind.NONE && secretText.isNullOrEmpty()) {
                throw MailException.invalidArgument("the secret must not be empty")
            }
            val parsed = MailAccountOptions.parse(json, kind, defaults())
            MailSession(parsed, MailSecret(secretText ?: ""))
        } catch (e: MailException) {
            callback?.let { MailBundles.notifyClosed(it, MailBundles.error(e)) }
            return null
        }
        return MailSessionBinder(session, callback)
    }

    override fun listProviders(): Bundle = MailBundles.json(MailContract.KEY_PROVIDERS_JSON, ProviderPresets.toJson())

    override fun listSavedAccounts(): Bundle = MailBundles.json(MailContract.KEY_ACCOUNTS_JSON, "[]")

    /** The IMAP `ID` payload for providers that require it (163 / 126): names this plugin, never the account. */
    private fun defaults(): MailAccountOptions.Defaults {
        val info = context.angusMailPluginRuntimeInfo()
        return MailAccountOptions.Defaults(
            clientId = mapOf(
                "name" to "AutoJs6-Plugin-Angus-Mail",
                "version" to info.versionName,
                "vendor" to AngusMailPlugin.AUTHOR,
                "support-email" to AngusMailPlugin.SUPPORT_EMAIL,
            ),
        )
    }
}
