package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import android.content.Context
import android.os.Bundle
import android.os.RemoteException
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
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerStatusDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.toDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.MailSession
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.oauth.AccountSecrets
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStore
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStores
import io.github.supermonster003.autojs6.plugin.angus.mail.store.SavedAccountsDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.toPluginInfo
import io.github.supermonster003.autojs6.plugin.angus.mail.trigger.MailWatchService
import io.github.supermonster003.autojs6.plugin.angus.mail.trigger.WatchKeeper
import org.autojs.plugin.common.api.PluginInfo
import org.autojs.plugin.mail.api.IMailPlugin
import org.autojs.plugin.mail.api.IMailSession
import org.autojs.plugin.mail.api.IMailSessionCallback
import org.autojs.plugin.mail.api.IMailTrigger
import org.autojs.plugin.mail.api.IMailTriggerCallback
import org.autojs.plugin.mail.api.MailContract

/**
 * `IMailPlugin` implementation (roadmap P1.3 / P2.5 / P4.3). The metadata methods (`getInfo`,
 * `getCapabilities`, `listProviders`) answer any caller that holds the plugin permission;
 * `openSession` and `listSavedAccounts` go through the [CallerGuard] first and throw
 * `SecurityException` for anything but the installed same-signer AutoJs6 host. `openSession`
 * validates the bundle, parses the account JSON through the mail core (or resolves a saved-account
 * alias inside this process, so the secret never crosses the Binder), and returns a
 * [MailSessionBinder] bound to the caller's UID; no network happens before the first `call`
 * (contract B.3). `listSavedAccounts` renders the [AccountStore] without secrets. Contract
 * version 2 (roadmap P8): `openTrigger` subscribes the host to a background watch of the
 * [WatchKeeper] through a [MailTriggerBinder] and `listTriggers` renders the watches; a refused
 * subscription reports its reason through the callback's `onStatus` and returns null.
 */
internal class MailPluginBinder(
    private val context: Context,
    private val guard: CallerGuard,
    private val accounts: AccountStore = AccountStores.of(context),
    private val secrets: AccountSecrets = AccountSecrets.of(context),
    private val keeper: () -> WatchKeeper = { WatchKeeper.of(context) },
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

    /**
     * The alias form (roadmap P4.3): the record's document is normalized like an inline one, the
     * secret is decrypted here and copied into the session only; a browser sign-in (roadmap P9)
     * yields its access token, refreshed first when it is about to expire.
     */
    private fun openSavedAccount(alias: String): MailSession = secrets.withUsableSecret(alias) { saved, secret ->
        val parsed = MailAccountOptions.parse(saved.accountJson, saved.secretKind, defaults())
        MailSession(parsed, MailSecret(secret))
    }

    override fun listProviders(): Bundle = MailBundles.json(MailContract.KEY_PROVIDERS_JSON, ProviderPresets.toJson())

    override fun listSavedAccounts(): Bundle {
        guard.enforceHost()
        return MailBundles.json(MailContract.KEY_ACCOUNTS_JSON, SavedAccountsDocument.render(accounts.list()))
    }

    override fun openTrigger(options: Bundle?, callback: IMailTriggerCallback?): IMailTrigger? {
        val ownerUid = guard.enforceHost()
        callback ?: return null
        val parsed = try {
            TriggerOptions.parse(options?.getString(MailContract.KEY_TRIGGER_OPTIONS_JSON))
        } catch (e: MailException) {
            refuseTrigger(callback, null, e)
            return null
        }
        val keeper = keeper()
        // A host that subscribes while nothing runs yet (fresh plugin process) brings the service up when Android allows it.
        if (keeper.running == 0) MailWatchService.start(context)
        val generation = options?.getLong(MailContract.KEY_GENERATION, 0L) ?: 0L
        val binder = MailTriggerBinder(parsed.triggerId, generation, parsed.filter, callback, guard, ownerUid, keeper)
        keeper.subscribe(parsed, binder)?.let { refusal ->
            refuseTrigger(callback, parsed.triggerId, refusal)
            return null
        }
        if (!binder.link()) {
            keeper.unsubscribe(binder)
            return null
        }
        return binder
    }

    private fun refuseTrigger(callback: IMailTriggerCallback, triggerId: String?, error: MailException) {
        val status = TriggerStatusDocument(
            triggerId = triggerId.orEmpty(),
            state = TriggerStatusDocument.STATE_STOPPED,
            reason = REASON_REFUSED,
            lastError = error.toDocument(),
            since = System.currentTimeMillis(),
        )
        try {
            callback.onStatus(MailBundles.json(MailContract.KEY_STATUS_JSON, status.toJson()))
        } catch (_: RemoteException) {
        }
    }

    override fun listTriggers(): Bundle {
        guard.enforceHost()
        return MailBundles.json(MailContract.KEY_TRIGGERS_JSON, keeper().list().toJson())
    }

    private fun defaults(): MailAccountOptions.Defaults = context.angusMailAccountDefaults()

    companion object {
        /** `reason` of the `stopped` status a refused `openTrigger` reports. */
        const val REASON_REFUSED = "refused"
    }
}
