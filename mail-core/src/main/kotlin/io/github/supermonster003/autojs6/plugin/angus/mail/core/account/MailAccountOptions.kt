package io.github.supermonster003.autojs6.plugin.angus.mail.core.account

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MimeLeniency
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthProviderId
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MailJson
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.InternetAddress
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/** Which secret key accompanied the account JSON on the Binder (the secret itself never enters this module's JSON). */
enum class SecretKind {
    NONE,
    PASSWORD,
    ACCESS_TOKEN,

    /** The tokens of a browser sign-in (roadmap P9): a JSON document of `OAuthTokens`, kept by the plugin's store only. */
    OAUTH2,
}

/**
 * Parses and normalizes the account JSON of `mail.connect(options)` (roadmap appendix A.2) into a
 * [MailAccount]: the provider preset is merged first and explicit fields override it, `user`
 * defaults to `address`, ports default by protocol and TLS mode, and every illegal combination is
 * reported as [MailErrorCode.INVALID_ARGUMENT] with the offending field in the message. Secret
 * fields inside the JSON are rejected outright so that a misrouted credential can never be logged.
 */
object MailAccountOptions {

    init {
        MimeLeniency.install()
    }

    object Fields {
        const val PROVIDER = "provider"
        const val ADDRESS = "address"
        const val USER = "user"
        const val NAME = "name"
        const val AUTH = "auth"
        const val RECEIVE = "receive"
        const val IMAP = "imap"
        const val POP3 = "pop3"
        const val SMTP = "smtp"
        const val TIMEOUT = "timeout"
        const val TLS = "tls"
        const val DEBUG = "debug"
        const val CLIENT_ID = "clientId"
        const val OAUTH = "oauth"

        const val HOST = "host"
        const val PORT = "port"
        const val ENDPOINT_TLS = "tls"

        const val CONNECT = "connect"
        const val READ = "read"
        const val WRITE = "write"

        const val TRUST_ALL = "trustAll"

        val KNOWN: Set<String> = setOf(PROVIDER, ADDRESS, USER, NAME, AUTH, RECEIVE, IMAP, POP3, SMTP, TIMEOUT, TLS, DEBUG, CLIENT_ID, OAUTH)

        /** Keys that would carry a credential; they travel on dedicated Bundle keys, never in JSON. */
        val FORBIDDEN: Set<String> = setOf("password", "accessToken", "tokenProvider", "token", "secret")
    }

    /** Values the app supplies for what the JSON leaves out (the IMAP `ID` payload names the plugin). */
    data class Defaults(
        val clientId: Map<String, String> = mapOf("name" to "AutoJs6-Plugin-Angus-Mail"),
    )

    fun parse(json: String, secret: SecretKind, defaults: Defaults = Defaults()): MailAccount {
        val element = try {
            MailJson.format.parseToJsonElement(json)
        } catch (e: Exception) {
            throw MailException.invalidArgument("account is not valid JSON", e.message?.take(200))
        }
        val root = element as? JsonObject ?: throw MailException.invalidArgument("account must be a JSON object")
        return normalize(root, secret, defaults)
    }

    fun normalize(root: JsonObject, secret: SecretKind, defaults: Defaults = Defaults()): MailAccount {
        root.keys.firstOrNull { it in Fields.FORBIDDEN }?.let { key ->
            throw MailException.invalidArgument("account JSON must not carry secrets (field '$key'); pass them through the secret keys")
        }
        root.keys.firstOrNull { it !in Fields.KNOWN }?.let { key ->
            throw MailException.invalidArgument("unknown account field '$key'", "known fields: ${Fields.KNOWN.joinToString(", ")}")
        }

        val address = root.string(Fields.ADDRESS) ?: throw MailException.invalidArgument("'${Fields.ADDRESS}' is required")
        validateAddress(address)
        val username = root.string(Fields.USER) ?: address
        val displayName = root.string(Fields.NAME)
        val provider = root.string(Fields.PROVIDER)?.let(ProviderPresets::require)
        val auth = resolveAuth(root.string(Fields.AUTH), secret, provider)

        val imap = endpoint(root, Fields.IMAP, MailProtocol.IMAP, provider)
        val pop3 = endpoint(root, Fields.POP3, MailProtocol.POP3, provider)
        val smtp = endpoint(root, Fields.SMTP, MailProtocol.SMTP, provider)
        if (imap == null && pop3 == null && smtp == null) {
            throw MailException.invalidArgument("no imap, pop3 or smtp endpoint; pass '${Fields.PROVIDER}' or the host fields")
        }
        val receive = resolveReceive(root.string(Fields.RECEIVE), imap, pop3)

        val timeouts = root.obj(Fields.TIMEOUT)?.let { timeouts ->
            timeouts.rejectUnknown(Fields.TIMEOUT, setOf(Fields.CONNECT, Fields.READ, Fields.WRITE))
            val read = timeouts.long("${Fields.TIMEOUT}.${Fields.READ}") ?: MailLimits.DEFAULT_READ_TIMEOUT_MS
            wrapArgument {
                MailTimeouts(
                    connectMillis = timeouts.long("${Fields.TIMEOUT}.${Fields.CONNECT}") ?: MailLimits.DEFAULT_CONNECT_TIMEOUT_MS,
                    readMillis = read,
                    writeMillis = timeouts.long("${Fields.TIMEOUT}.${Fields.WRITE}") ?: read,
                )
            }
        } ?: MailTimeouts()

        val trustAll = root.obj(Fields.TLS)?.let { tls ->
            tls.rejectUnknown(Fields.TLS, setOf(Fields.TRUST_ALL))
            tls.bool("${Fields.TLS}.${Fields.TRUST_ALL}") ?: false
        } ?: false
        val debug = root.bool(Fields.DEBUG) ?: false
        val clientId = clientId(root, provider, defaults)
        val oauth = oauthLink(root, secret)

        return MailAccount(
            address = address,
            username = username,
            displayName = displayName,
            auth = auth,
            receive = receive,
            imap = imap,
            pop3 = pop3,
            smtp = smtp,
            trustAll = trustAll,
            timeouts = timeouts,
            clientId = clientId,
            debug = debug,
            provider = provider,
            oauth = oauth,
        )
    }

    /**
     * The `oauth` object of an account that signed in through the browser (roadmap P9): present
     * exactly when the secret is [SecretKind.OAUTH2]; a script cannot pass it with a password or a
     * token, and a sign-in record without it is refused.
     */
    private fun oauthLink(root: JsonObject, secret: SecretKind): OAuthLink? {
        val oauth = root.obj(Fields.OAUTH)
        if (oauth == null) {
            if (secret == SecretKind.OAUTH2) throw MailException.invalidArgument("'${Fields.OAUTH}' is required for an account that signed in through the browser")
            return null
        }
        if (secret != SecretKind.OAUTH2) throw MailException.invalidArgument("'${Fields.OAUTH}' belongs to accounts saved by the plugin's browser sign-in only")
        oauth.rejectUnknown(Fields.OAUTH, OAuthLink.FIELDS)
        val provider = oauth.string("${Fields.OAUTH}.${OAuthLink.FIELD_PROVIDER}")
            ?: throw MailException.invalidArgument("'${Fields.OAUTH}.${OAuthLink.FIELD_PROVIDER}' is required")
        if (OAuthProviderId.fromId(provider) == null) {
            throw MailException.invalidArgument("'${Fields.OAUTH}.${OAuthLink.FIELD_PROVIDER}' must be one of ${OAuthProviderId.entries.joinToString(", ") { it.id }}: '$provider'")
        }
        return wrapArgument {
            OAuthLink(
                provider = provider,
                authorizedAt = oauth.long("${Fields.OAUTH}.${OAuthLink.FIELD_AUTHORIZED_AT}") ?: 0,
                expiresAt = oauth.long("${Fields.OAUTH}.${OAuthLink.FIELD_EXPIRES_AT}") ?: 0,
                needsReauth = oauth.bool("${Fields.OAUTH}.${OAuthLink.FIELD_NEEDS_REAUTH}") ?: false,
            )
        }
    }

    private fun validateAddress(address: String) {
        try {
            InternetAddress(address, true).validate()
        } catch (e: AddressException) {
            throw MailException.invalidArgument("'${Fields.ADDRESS}' is not a valid email address", e.message?.take(200))
        }
        if (address.length > MailLimits.MAX_OPTION_STRING_LENGTH) {
            throw MailException.invalidArgument("'${Fields.ADDRESS}' exceeds ${MailLimits.MAX_OPTION_STRING_LENGTH} characters")
        }
    }

    private fun resolveAuth(explicit: String?, secret: SecretKind, provider: ProviderPreset?): AuthMethod {
        val requested = explicit?.let { id ->
            AuthMethod.fromId(id) ?: throw MailException.invalidArgument("'${Fields.AUTH}' must be one of ${AuthMethod.entries.joinToString(", ") { it.id }}: '$id'")
        }
        val auth = when (secret) {
            SecretKind.NONE -> throw MailException.invalidArgument("a password or an access token is required")
            SecretKind.PASSWORD -> {
                if (requested == AuthMethod.XOAUTH2) throw MailException.invalidArgument("'${Fields.AUTH}' is xoauth2 but an access token was not supplied (a password was)")
                AuthMethod.PASSWORD
            }
            SecretKind.ACCESS_TOKEN -> {
                if (requested == AuthMethod.PASSWORD) throw MailException.invalidArgument("'${Fields.AUTH}' is password but a password was not supplied (an access token was)")
                AuthMethod.XOAUTH2
            }
            SecretKind.OAUTH2 -> {
                if (requested == AuthMethod.PASSWORD) throw MailException.invalidArgument("'${Fields.AUTH}' is password but the account signed in through the browser (XOAUTH2)")
                AuthMethod.XOAUTH2
            }
        }
        if (provider != null && !provider.accepts(auth)) {
            throw MailException(
                MailErrorCode.AUTH_MECHANISM_UNSUPPORTED,
                "provider '${provider.id}' accepts only ${provider.auth.joinToString(" or ")} authentication",
                provider.authHint,
                retryable = false,
            )
        }
        return auth
    }

    private fun endpoint(root: JsonObject, field: String, protocol: MailProtocol, provider: ProviderPreset?): MailEndpoint? {
        val preset = provider?.endpoint(protocol)
        val explicit = root[field]
        if (explicit == null || explicit is JsonNull) return preset?.toEndpoint()
        val obj = explicit as? JsonObject ?: throw MailException.invalidArgument("'$field' must be an object with host, port and tls")
        obj.rejectUnknown(field, setOf(Fields.HOST, Fields.PORT, Fields.ENDPOINT_TLS))
        val host = obj.string("$field.${Fields.HOST}") ?: preset?.host
            ?: throw MailException.invalidArgument("'$field.${Fields.HOST}' is required without a provider preset")
        val tls = obj.string("$field.${Fields.ENDPOINT_TLS}")?.let { id ->
            TlsMode.fromId(id) ?: throw MailException.invalidArgument("'$field.${Fields.ENDPOINT_TLS}' must be one of ${TlsMode.entries.joinToString(", ") { it.id }}: '$id'")
        }
        val presetEndpoint = preset?.toEndpoint()
        val effectiveTls = tls ?: presetEndpoint?.tls ?: TlsMode.SSL
        val overridesPreset = presetEndpoint == null || host != presetEndpoint.host || effectiveTls != presetEndpoint.tls
        val port = obj.long("$field.${Fields.PORT}")?.let { value ->
            if (value !in 1..65535) throw MailException.invalidArgument("'$field.${Fields.PORT}' must be within 1..65535: $value")
            value.toInt()
        } ?: if (overridesPreset) protocol.defaultPort(effectiveTls) else presetEndpoint!!.port
        if (host.length > MailLimits.MAX_OPTION_STRING_LENGTH || host.any { it.isWhitespace() || it == '/' }) {
            throw MailException.invalidArgument("'$field.${Fields.HOST}' is not a host name")
        }
        return MailEndpoint(host, port, effectiveTls)
    }

    private fun resolveReceive(explicit: String?, imap: MailEndpoint?, pop3: MailEndpoint?): MailProtocol {
        val requested = explicit?.let { id ->
            when (MailProtocol.fromId(id)) {
                MailProtocol.IMAP -> MailProtocol.IMAP
                MailProtocol.POP3 -> MailProtocol.POP3
                else -> throw MailException.invalidArgument("'${Fields.RECEIVE}' must be imap or pop3: '$id'")
            }
        }
        if (requested != null) {
            val present = if (requested == MailProtocol.IMAP) imap != null else pop3 != null
            if (!present) throw MailException.invalidArgument("'${Fields.RECEIVE}' is ${requested.id} but there is no ${requested.id} endpoint")
            return requested
        }
        return if (imap == null && pop3 != null) MailProtocol.POP3 else MailProtocol.IMAP
    }

    private fun clientId(root: JsonObject, provider: ProviderPreset?, defaults: Defaults): Map<String, String> {
        val explicit = root.obj(Fields.CLIENT_ID)
        if (explicit == null) return if (provider?.requiresClientId == true) defaults.clientId else emptyMap()
        if (explicit.size > MailLimits.MAX_CLIENT_ID_ENTRIES) {
            throw MailException.invalidArgument("'${Fields.CLIENT_ID}' has more than ${MailLimits.MAX_CLIENT_ID_ENTRIES} entries")
        }
        return explicit.entries.associate { (key, value) ->
            val text = (value as? JsonPrimitive)?.takeIf { it.isString }?.content
                ?: throw MailException.invalidArgument("'${Fields.CLIENT_ID}.$key' must be a string")
            if (key.isBlank() || key.length > MailLimits.MAX_OPTION_STRING_LENGTH || text.length > MailLimits.MAX_OPTION_STRING_LENGTH) {
                throw MailException.invalidArgument("'${Fields.CLIENT_ID}.$key' exceeds ${MailLimits.MAX_OPTION_STRING_LENGTH} characters")
            }
            key to text
        }
    }

    private inline fun <T> wrapArgument(block: () -> T): T = try {
        block()
    } catch (e: IllegalArgumentException) {
        throw MailException.invalidArgument(e.message ?: "invalid argument")
    }

    private fun JsonObject.rejectUnknown(field: String, known: Set<String>) {
        keys.firstOrNull { it !in known }?.let { key ->
            throw MailException.invalidArgument("unknown field '$field.$key'", "known fields: ${known.joinToString(", ")}")
        }
    }

    private fun JsonObject.string(path: String): String? {
        val element = this[path.substringAfterLast('.')] ?: return null
        if (element is JsonNull) return null
        val primitive = element as? JsonPrimitive
        if (primitive == null || !primitive.isString) throw MailException.invalidArgument("'$path' must be a string")
        val text = primitive.contentOrNull?.trim().orEmpty()
        if (text.isEmpty()) throw MailException.invalidArgument("'$path' must not be blank")
        if (text.length > MailLimits.MAX_OPTION_STRING_LENGTH) throw MailException.invalidArgument("'$path' exceeds ${MailLimits.MAX_OPTION_STRING_LENGTH} characters")
        return text
    }

    private fun JsonObject.long(path: String): Long? {
        val element = this[path.substringAfterLast('.')] ?: return null
        if (element is JsonNull) return null
        val primitive = element as? JsonPrimitive
        val value = primitive?.takeUnless { it.isString }?.longOrNull
        return value ?: throw MailException.invalidArgument("'$path' must be an integer")
    }

    private fun JsonObject.bool(path: String): Boolean? {
        val element = this[path.substringAfterLast('.')] ?: return null
        if (element is JsonNull) return null
        val primitive = element as? JsonPrimitive
        val value = primitive?.takeUnless { it.isString }?.booleanOrNull
        return value ?: throw MailException.invalidArgument("'$path' must be a boolean")
    }

    private fun JsonObject.obj(field: String): JsonObject? {
        val element: JsonElement = this[field] ?: return null
        if (element is JsonNull) return null
        return element as? JsonObject ?: throw MailException.invalidArgument("'$field' must be an object")
    }
}
