package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import android.content.Context
import android.os.Bundle
import io.github.supermonster003.autojs6.plugin.angus.mail.AngusMailPlugin
import io.github.supermonster003.autojs6.plugin.angus.mail.angusMailAccountDefaults
import io.github.supermonster003.autojs6.plugin.angus.mail.angusMailPluginRuntimeInfo
import io.github.supermonster003.autojs6.plugin.angus.mail.capabilitiesBundle
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.ProviderPresets
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSession
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStore
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStores
import io.github.supermonster003.autojs6.plugin.angus.mail.store.SavedAccountsDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.toPluginInfo
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.mail.api.IMailPlugin
import org.autojs.plugin.mail.api.IMailSession
import org.autojs.plugin.mail.api.IMailSessionCallback
import org.autojs.plugin.mail.api.MailContract

/**
 * `IMailPlugin` implementation (roadmap P1.3 / P2.5 / P4.3). The metadata methods (`getInfo`,
 * `getCapabilities`, `listProviders`) answer any caller that holds the plugin permission;
 * `openSession` and `listSavedAccounts` go through the [CallerGuard] first and throw
 * `SecurityException` for anything but the installed same-signer AutoJs6 host. `openSession`
 * validates the bundle, parses the account JSON through the mail core (or resolves a saved-account
 * alias inside this process, so the secret never crosses the Binder), and returns a
 * [MailSessionBinder] bound to the caller's UID; no network happens before the first `call`
 * (contract B.3). `listSavedAccounts` renders the [AccountStore] without secrets.
 */
internal class MailPluginBinder(
    private val context: Context,
    private val guard: CallerGuard,
    private val accounts: AccountStore = AccountStores.of(context),
) : IMailPlugin.Stub() {

    override fun getInfo(): PluginInfo = context.angusMailPluginRuntimeInfo().toPluginInfo()

    override fun getCapabilities(): Bundle = context.angusMailPluginRuntimeInfo().capabilitiesBundle()

    override fun openSession(account: Bundle?, callback: IMailSessionCallback?): IMailSession? {
        val ownerUid = guard.enforceHost()
        MailBundles.validateAccount(account)?.let { validation ->
            callback?.let { MailBundles.notifyClosed(it, validation) }
            return null
        }
        account!!
        val session = try {
            val alias = account.getString(MailContract.KEY_ACCOUNT_ALIAS)
            if (alias != null) openSavedAccount(alias) else openInlineAccount(account)
        } catch (e: MailException) {
            callback?.let { MailBundles.notifyClosed(it, MailBundles.error(e)) }
            return null
        }
        return MailSessionBinder(session, callback, guard, ownerUid, WatchNetworkMonitor.of(context))
    }

    private fun openInlineAccount(account: Bundle): MailSession {
        val json = requireNotNull(account.getString(MailContract.KEY_ACCOUNT_JSON))
        val kind = MailBundles.secretKind(account)
        val secretText = MailBundles.secret(account, kind)
        if (kind != SecretKind.NONE && secretText.isNullOrEmpty()) {
            throw MailException.invalidArgument("the secret must not be empty")
        }
        val parsed = MailAccountOptions.parse(json, kind, defaults())
        return MailSession(parsed, MailSecret(secretText ?: ""))
    }

    /** The alias form (roadmap P4.3): the record's document is normalized like an inline one, the secret is decrypted here and copied into the session only. */
    private fun openSavedAccount(alias: String): MailSession = accounts.withSecret(alias) { saved, secret ->
        val parsed = MailAccountOptions.parse(saved.accountJson, saved.secretKind, defaults())
        MailSession(parsed, MailSecret(secret))
    }

    override fun listProviders(): Bundle = MailBundles.json(MailContract.KEY_PROVIDERS_JSON, ProviderPresets.toJson())

    override fun listSavedAccounts(): Bundle {
        guard.enforceHost()
        return MailBundles.json(MailContract.KEY_ACCOUNTS_JSON, SavedAccountsDocument.render(accounts.list()))
    }

    private fun defaults(): MailAccountOptions.Defaults = context.angusMailAccountDefaults()
}
