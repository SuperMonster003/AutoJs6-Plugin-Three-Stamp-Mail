package io.github.supermonster003.autojs6.plugin.angus.mail

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.ProviderPresets
import org.autojs.plugin.common.api.PluginActions
import org.autojs.plugin.mail.api.MailContract

/**
 * Identity constants shared by the manifest, the Binder services, the documentation, and the
 * tests. They must stay identical to the host-side registration (see `ROADMAP.md`, decision D1
 * and phase P1.1); the JVM manifest contract test fails when the manifest drifts from them.
 */
object AngusMailPlugin {

    const val PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.angus.mail"
    const val HOST_PACKAGE_NAME = "org.autojs.autojs6"

    /** Plugin ID names this implementation; the engine names the capability family (decision D1). */
    const val ID = "angus-mail"
    const val ENGINE = "mail"
    const val VARIANT = "default"
    const val AUTHOR = "SuperMonster003"

    /** `support-email` of the IMAP `ID` payload NetEase asks clients to send (name, version, vendor, support-email); the maintainer's public commit address. */
    const val SUPPORT_EMAIL = "30370009+SuperMonster003@users.noreply.github.com"

    /** Discovery contract of [AngusMailPluginService]. */
    const val SERVICE_ACTION = "org.autojs.plugin.MAIL"
    const val SERVICE_CATEGORY = "mail"

    /**
     * Parameterless settings entry the host starts behind the plugin permission
     * ([MailSettingsActivity], roadmap P4.3); mirrors `MailActions.OPEN_SETTINGS`.
     */
    const val SETTINGS_ACTION = "org.autojs.plugin.MAIL_SETTINGS"

    /** Settings extension advertised in the capabilities: 1 = [SETTINGS_ACTION] plus saved accounts. */
    const val SETTINGS_VERSION = 1

    /** Discovery contract of [AngusMailPluginInfoService]. */
    const val INFO_ACTION = PluginActions.INFO

    /**
     * Binder descriptor of the `IMailPlugin` AIDL that the host defines in its `mail-api` module
     * (roadmap P1.1). Until that contract is staged in `libs/`, the service exposes a placeholder
     * Binder carrying only this descriptor.
     */
    const val SERVICE_DESCRIPTOR = "org.autojs.plugin.mail.api.IMailPlugin"

    /**
     * Minimum AutoJs6 `versionCode` able to list this plugin. Provisional: the 6.8.0 host build
     * this skeleton was developed against; roadmap P1.4 replaces it with the host build that
     * ships the mail contract module.
     */
    const val REQUIRED_HOST_VERSION = 5282L

    /** Capability values advertised through `MailCapabilityKeys`; the mail core implements exactly these. */
    val PROTOCOLS = listOf(MailContract.PROTOCOL_IMAP, MailContract.PROTOCOL_POP3, MailContract.PROTOCOL_SMTP)
    val AUTH_MECHANISMS = listOf(MailContract.AUTH_PASSWORD, MailContract.AUTH_XOAUTH2)

    /**
     * Features the plugin implements today: `messages.append` (P2.2), the client-side filter
     * fallback of `messages.search` (P2.3 / P2.4), saved accounts behind `openSession(alias)` /
     * `listSavedAccounts` (P4), new-mail watches with IMAP IDLE (`idle`, P5) and the background
     * watches with `openTrigger` / `listTriggers` of contract version 2 (`backgroundWatch`, P8).
     */
    val FEATURES = listOf(MailContract.FEATURE_APPEND, MailContract.FEATURE_CLIENT_SEARCH_FALLBACK, MailContract.FEATURE_SAVED_ACCOUNTS, MailContract.FEATURE_IDLE, MailContract.FEATURE_BACKGROUND_WATCH)
    /** Version of the built-in provider catalog (`mail-core/src/main/resources/providers.json`). */
    val PROVIDERS_VERSION: Int get() = ProviderPresets.version
    const val MAIL_LIBRARY_VERSION = "2.0.5"
}
