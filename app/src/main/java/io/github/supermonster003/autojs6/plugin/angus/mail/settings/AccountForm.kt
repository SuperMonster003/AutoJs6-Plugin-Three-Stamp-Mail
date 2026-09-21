package io.github.supermonster003.autojs6.plugin.angus.mail.settings

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.AuthMethod
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.OAuthLink
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.ProviderPreset
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.ProviderPresets
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MailJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthProviderId
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthProviders
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountAlias
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The host, port and TLS mode of one endpoint as typed into the editor. */
data class EndpointFields(
    val enabled: Boolean = false,
    val host: String = "",
    val port: String = "",
    val tls: TlsMode = TlsMode.SSL,
) {
    companion object {
        val DISABLED = EndpointFields()
    }
}

/**
 * The account editor's model (roadmap P4.2): every value the user can type or choose, never the
 * secret. [AccountFormPolicy] turns it into the account JSON the store keeps, and back.
 */
data class AccountForm(
    val alias: String = "",
    /** A preset id from the catalog, or null for a custom server. */
    val providerId: String? = null,
    val address: String = "",
    /** Login name; empty means the address. */
    val user: String = "",
    val name: String = "",
    val auth: AuthMethod = AuthMethod.PASSWORD,
    /** The browser sign-in behind an XOAUTH2 account (roadmap P9); null for a pasted token or a password. */
    val oauthProvider: OAuthProviderId? = null,
    val receive: MailProtocol = MailProtocol.IMAP,
    val imap: EndpointFields = EndpointFields.DISABLED,
    val pop3: EndpointFields = EndpointFields.DISABLED,
    val smtp: EndpointFields = EndpointFields.DISABLED,
) {
    val provider: ProviderPreset? get() = ProviderPresets.resolve(providerId)

    /** The kind of secret the chosen authentication needs. */
    val secretKind: SecretKind
        get() = when {
            auth != AuthMethod.XOAUTH2 -> SecretKind.PASSWORD
            oauthProvider != null -> SecretKind.OAUTH2
            else -> SecretKind.ACCESS_TOKEN
        }

    /** The browser sign-in the chosen preset offers, if any (Gmail: Google; Outlook.com and Microsoft 365: Microsoft). */
    val offeredOAuthProvider: OAuthProviderId? get() = OAuthProviders.forPreset(providerId)

    fun endpoint(protocol: MailProtocol): EndpointFields = when (protocol) {
        MailProtocol.IMAP -> imap
        MailProtocol.POP3 -> pop3
        MailProtocol.SMTP -> smtp
    }

    fun withEndpoint(protocol: MailProtocol, fields: EndpointFields): AccountForm = when (protocol) {
        MailProtocol.IMAP -> copy(imap = fields)
        MailProtocol.POP3 -> copy(pop3 = fields)
        MailProtocol.SMTP -> copy(smtp = fields)
    }

    /** True when the preset supplies this endpoint, so the editor cannot switch it off. */
    fun endpointFixedByPreset(protocol: MailProtocol): Boolean = provider?.endpoint(protocol) != null
}

/** Which input a [FormError] belongs to; null anchors the error to the whole form. */
enum class FormField { ALIAS, ADDRESS, IMAP_HOST, IMAP_PORT, POP3_HOST, POP3_PORT, SMTP_HOST, SMTP_PORT }

enum class FormProblem {
    ALIAS_REQUIRED,
    ALIAS_INVALID,
    ADDRESS_REQUIRED,
    ADDRESS_INVALID,
    HOST_REQUIRED,
    PORT_INVALID,
    RECEIVE_ENDPOINT_MISSING,
    NO_ENDPOINT,
}

data class FormError(val field: FormField?, val problem: FormProblem)

object AccountFormPolicy {

    fun blank(): AccountForm = AccountForm()

    /**
     * Switches the form to [preset] (null for a custom server): the endpoints the preset knows are
     * filled in, the authentication falls back to the preset's first accepted method when the
     * current one is refused, and the receive protocol follows what the preset offers. Switching to
     * a custom server keeps the current endpoints as a starting point.
     */
    fun applyPreset(form: AccountForm, preset: ProviderPreset?): AccountForm {
        if (preset == null) return form.copy(providerId = null, oauthProvider = null)
        var updated = form.copy(providerId = preset.id)
        MailProtocol.entries.forEach { protocol ->
            val endpoint = preset.endpoint(protocol)
            updated = updated.withEndpoint(
                protocol,
                if (endpoint != null) {
                    EndpointFields(true, endpoint.host, endpoint.port.toString(), TlsMode.fromId(endpoint.tls) ?: TlsMode.SSL)
                } else {
                    EndpointFields.DISABLED
                },
            )
        }
        if (!preset.accepts(updated.auth)) {
            updated = updated.copy(auth = preset.auth.firstNotNullOfOrNull(AuthMethod::fromId) ?: AuthMethod.PASSWORD)
        }
        if (updated.oauthProvider != null && updated.oauthProvider != OAuthProviders.forPreset(preset.id)) {
            updated = updated.copy(oauthProvider = null)
        }
        val receive = when {
            preset.imap != null -> MailProtocol.IMAP
            preset.pop3 != null -> MailProtocol.POP3
            else -> updated.receive
        }
        return updated.copy(receive = receive)
    }

    fun validate(form: AccountForm): List<FormError> {
        val errors = ArrayList<FormError>()
        val alias = form.alias.trim()
        when {
            alias.isEmpty() -> errors += FormError(FormField.ALIAS, FormProblem.ALIAS_REQUIRED)
            !AccountAlias.isValid(alias) -> errors += FormError(FormField.ALIAS, FormProblem.ALIAS_INVALID)
        }
        val address = form.address.trim()
        when {
            address.isEmpty() -> errors += FormError(FormField.ADDRESS, FormProblem.ADDRESS_REQUIRED)
            !looksLikeAddress(address) -> errors += FormError(FormField.ADDRESS, FormProblem.ADDRESS_INVALID)
        }
        var enabledEndpoints = 0
        MailProtocol.entries.forEach { protocol ->
            val fields = form.endpoint(protocol)
            if (!fields.enabled) return@forEach
            enabledEndpoints++
            if (fields.host.isBlank()) errors += FormError(hostField(protocol), FormProblem.HOST_REQUIRED)
            val port = fields.port.trim()
            if (port.isNotEmpty() && port.toIntOrNull()?.takeIf { it in 1..65535 } == null) {
                errors += FormError(portField(protocol), FormProblem.PORT_INVALID)
            }
        }
        if (enabledEndpoints == 0) {
            errors += FormError(null, FormProblem.NO_ENDPOINT)
        } else if (!form.endpoint(form.receive).enabled && form.endpoint(otherReceive(form.receive)).enabled) {
            // The chosen receive protocol has no server while the other one does; a send-only
            // account (SMTP alone) is allowed and gets no receive field at all.
            errors += FormError(null, FormProblem.RECEIVE_ENDPOINT_MISSING)
        }
        return errors
    }

    /**
     * The compact account document the store keeps: address, optional login name and display name,
     * the preset id, the authentication and receive protocol, and only those endpoints that the
     * preset does not already describe (so catalog updates still reach saved accounts).
     */
    fun toAccountJson(form: AccountForm, oauth: OAuthLink? = null): String {
        val preset = form.provider
        val address = form.address.trim()
        require((oauth != null) == (form.secretKind == SecretKind.OAUTH2)) { "the oauth link goes with a browser sign-in only" }
        val document = buildJsonObject {
            preset?.let { put(MailAccountOptions.Fields.PROVIDER, it.id) }
            put(MailAccountOptions.Fields.ADDRESS, address)
            form.user.trim().takeIf { it.isNotEmpty() && it != address }?.let { put(MailAccountOptions.Fields.USER, it) }
            form.name.trim().takeIf { it.isNotEmpty() }?.let { put(MailAccountOptions.Fields.NAME, it) }
            put(MailAccountOptions.Fields.AUTH, form.auth.id)
            oauth?.let { link ->
                put(
                    MailAccountOptions.Fields.OAUTH,
                    buildJsonObject {
                        put(OAuthLink.FIELD_PROVIDER, link.provider)
                        put(OAuthLink.FIELD_AUTHORIZED_AT, link.authorizedAt)
                        put(OAuthLink.FIELD_EXPIRES_AT, link.expiresAt)
                        put(OAuthLink.FIELD_NEEDS_REAUTH, link.needsReauth)
                    },
                )
            }
            if (form.endpoint(form.receive).enabled) put(MailAccountOptions.Fields.RECEIVE, form.receive.id)
            MailProtocol.entries.forEach { protocol ->
                val fields = form.endpoint(protocol)
                if (!fields.enabled) return@forEach
                val host = fields.host.trim()
                val port = fields.port.trim().toIntOrNull()
                val presetEndpoint = preset?.endpoint(protocol)
                if (presetEndpoint != null && host == presetEndpoint.host && port == presetEndpoint.port && fields.tls.id == presetEndpoint.tls) {
                    return@forEach
                }
                put(
                    protocol.id,
                    buildJsonObject {
                        put(MailAccountOptions.Fields.HOST, host)
                        port?.let { put(MailAccountOptions.Fields.PORT, it) }
                        put(MailAccountOptions.Fields.ENDPOINT_TLS, fields.tls.id)
                    },
                )
            }
        }
        return document.toString()
    }

    /**
     * Reads a stored document back into the editor. Lenient on purpose: a record whose preset has
     * left the catalog opens as a custom server with whatever endpoints were written explicitly.
     */
    fun fromAccountJson(json: String, secretKind: SecretKind = SecretKind.PASSWORD): AccountForm {
        val root = runCatching { MailJson.format.parseToJsonElement(json) as? JsonObject }.getOrNull() ?: return blank()
        val preset = ProviderPresets.resolve(root.text(MailAccountOptions.Fields.PROVIDER))
        val auth = root.text(MailAccountOptions.Fields.AUTH)?.let(AuthMethod::fromId)
            ?: if (secretKind == SecretKind.ACCESS_TOKEN || secretKind == SecretKind.OAUTH2) AuthMethod.XOAUTH2 else AuthMethod.PASSWORD
        val oauthProvider = (root[MailAccountOptions.Fields.OAUTH] as? JsonObject)?.text(OAuthLink.FIELD_PROVIDER)?.let(OAuthProviderId::fromId)
            ?.takeIf { secretKind == SecretKind.OAUTH2 }
        var form = AccountForm(
            providerId = preset?.id,
            address = root.text(MailAccountOptions.Fields.ADDRESS).orEmpty(),
            user = root.text(MailAccountOptions.Fields.USER).orEmpty(),
            name = root.text(MailAccountOptions.Fields.NAME).orEmpty(),
            auth = auth,
            oauthProvider = oauthProvider,
        )
        MailProtocol.entries.forEach { protocol ->
            val presetEndpoint = preset?.endpoint(protocol)
            val explicit = root[protocol.id] as? JsonObject
            val fields = when {
                explicit != null -> {
                    val tls = explicit.text(MailAccountOptions.Fields.ENDPOINT_TLS)?.let(TlsMode::fromId)
                        ?: presetEndpoint?.tls?.let(TlsMode::fromId)
                        ?: TlsMode.SSL
                    val host = explicit.text(MailAccountOptions.Fields.HOST) ?: presetEndpoint?.host.orEmpty()
                    val port = explicit.number(MailAccountOptions.Fields.PORT)
                        ?: presetEndpoint?.takeIf { it.host == host && it.tls == tls.id }?.port
                        ?: protocol.defaultPort(tls)
                    EndpointFields(true, host, port.toString(), tls)
                }
                presetEndpoint != null -> EndpointFields(true, presetEndpoint.host, presetEndpoint.port.toString(), TlsMode.fromId(presetEndpoint.tls) ?: TlsMode.SSL)
                else -> EndpointFields.DISABLED
            }
            form = form.withEndpoint(protocol, fields)
        }
        val receive = root.text(MailAccountOptions.Fields.RECEIVE)?.let(MailProtocol::fromId)?.takeIf { it != MailProtocol.SMTP }
            ?: if (!form.imap.enabled && form.pop3.enabled) MailProtocol.POP3 else MailProtocol.IMAP
        return form.copy(receive = receive)
    }

    /** The port an endpoint falls back to when its field is cleared. */
    fun defaultPort(protocol: MailProtocol, tls: TlsMode): Int = protocol.defaultPort(tls)

    private fun looksLikeAddress(address: String): Boolean {
        val at = address.indexOf('@')
        return at > 0 && at < address.length - 1 && address.none { it.isWhitespace() }
    }

    private fun otherReceive(receive: MailProtocol): MailProtocol =
        if (receive == MailProtocol.IMAP) MailProtocol.POP3 else MailProtocol.IMAP

    private fun hostField(protocol: MailProtocol): FormField = when (protocol) {
        MailProtocol.IMAP -> FormField.IMAP_HOST
        MailProtocol.POP3 -> FormField.POP3_HOST
        MailProtocol.SMTP -> FormField.SMTP_HOST
    }

    private fun portField(protocol: MailProtocol): FormField = when (protocol) {
        MailProtocol.IMAP -> FormField.IMAP_PORT
        MailProtocol.POP3 -> FormField.POP3_PORT
        MailProtocol.SMTP -> FormField.SMTP_PORT
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }

    private fun JsonObject.number(key: String): Int? =
        (this[key] as? JsonPrimitive)?.takeUnless { it.isString }?.content?.toIntOrNull()
}
