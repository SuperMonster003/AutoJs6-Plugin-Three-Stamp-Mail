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
    val FEATURES = listOf(MailContract.FEATURE_IDLE, MailContract.FEATURE_APPEND)
    /** Version of the built-in provider catalog (`mail-core/src/main/resources/providers.json`). */
    val PROVIDERS_VERSION: Int get() = ProviderPresets.version
    const val MAIL_LIBRARY_VERSION = "2.0.5"
}
