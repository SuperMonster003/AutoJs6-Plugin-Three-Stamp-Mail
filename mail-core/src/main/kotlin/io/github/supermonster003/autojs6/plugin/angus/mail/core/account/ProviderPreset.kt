package io.github.supermonster003.autojs6.plugin.angus.mail.core.account

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One endpoint of a provider preset as stored in `providers.json`. */
@Serializable
data class PresetEndpoint(
    val host: String,
    val port: Int,
    val tls: String = TlsMode.SSL.id,
) {
    fun toEndpoint(): MailEndpoint = MailEndpoint(host, port, TlsMode.entries.first { it.id == tls })
}

/**
 * Public connection facts of one mail provider (roadmap D10, appendix C). Presets carry hosts,
 * ports, TLS modes, accepted authentication methods, and folder conventions; never an account.
 */
@Serializable
data class ProviderPreset(
    val id: String,
    val name: String,
    /** Address domains served by the provider; informational, the plugin never probes domains. */
    val domains: List<String> = emptyList(),
    val imap: PresetEndpoint? = null,
    val pop3: PresetEndpoint? = null,
    val smtp: PresetEndpoint? = null,
    /** Authentication methods the provider accepts ([AuthMethod.id] values), in order of preference. */
    val auth: List<String> = listOf(AuthMethod.PASSWORD.id),
    /** What the user has to paste as the secret (app password, authorization code, OAuth token). */
    val authHint: String,
    /** True when the server files a copy of every SMTP submission into the sent folder by itself. */
    val autoSavesSent: Boolean = false,
    /** IMAP name of the sent folder, used by `saveToSent` when [autoSavesSent] is false. */
    val sentFolder: String? = null,
    /** True when the IMAP server refuses clients that do not send the `ID` command (163 / 126). */
    val requiresClientId: Boolean = false,
    /** Provider help page describing the client settings. */
    val docsUrl: String,
    @SerialName("notes")
    val notes: String? = null,
) {
    fun endpoint(protocol: MailProtocol): PresetEndpoint? = when (protocol) {
        MailProtocol.IMAP -> imap
        MailProtocol.POP3 -> pop3
        MailProtocol.SMTP -> smtp
    }

    fun accepts(method: AuthMethod): Boolean = method.id in auth
}

/** Root document of `providers.json`. */
@Serializable
data class ProviderCatalog(
    /** Bumped whenever an entry changes; reported to the host as `mailProvidersVersion`. */
    val version: Int,
    val providers: List<ProviderPreset>,
)
