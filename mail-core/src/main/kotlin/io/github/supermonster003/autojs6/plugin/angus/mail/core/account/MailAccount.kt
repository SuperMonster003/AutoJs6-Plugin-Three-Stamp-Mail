package io.github.supermonster003.autojs6.plugin.angus.mail.core.account

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits

/** Transport security of one endpoint (roadmap appendix B.1 `tls`). */
enum class TlsMode(val id: String) {
    /** Implicit TLS from the first byte (IMAPS 993, POP3S 995, SMTPS 465). */
    SSL("ssl"),

    /** Plain connection upgraded with STARTTLS; the upgrade is mandatory, never opportunistic. */
    STARTTLS("starttls"),

    /** No encryption. Only for local test servers; the session is marked insecure (roadmap D25). */
    NONE("none"),
    ;

    companion object {
        fun fromId(id: String): TlsMode? = entries.firstOrNull { it.id == id }
    }
}

/** How the secret in [MailSecret] is presented to the server. */
enum class AuthMethod(val id: String) {
    /** Password, provider authorization code, or app password. */
    PASSWORD("password"),

    /** OAuth 2.0 access token sent through the XOAUTH2 SASL mechanism built into Angus Mail. */
    XOAUTH2("xoauth2"),
    ;

    companion object {
        fun fromId(id: String): AuthMethod? = entries.firstOrNull { it.id == id }
    }
}

enum class MailProtocol(val id: String) {
    IMAP("imap"),
    POP3("pop3"),
    SMTP("smtp"),
    ;

    /** Conventional port of the protocol under [tls] (RFC 8314 for the implicit TLS ports). */
    fun defaultPort(tls: TlsMode): Int = when (this) {
        IMAP -> if (tls == TlsMode.SSL) 993 else 143
        POP3 -> if (tls == TlsMode.SSL) 995 else 110
        SMTP -> when (tls) {
            TlsMode.SSL -> 465
            TlsMode.STARTTLS -> 587
            TlsMode.NONE -> 25
        }
    }

    companion object {
        fun fromId(id: String): MailProtocol? = entries.firstOrNull { it.id == id }
    }
}

data class MailEndpoint(
    val host: String,
    val port: Int,
    val tls: TlsMode = TlsMode.SSL,
) {
    init {
        require(host.isNotBlank()) { "host must not be blank" }
        require(port in 1..65535) { "port must be within 1..65535: $port" }
    }

    override fun toString(): String = "$host:$port/${tls.id}"
}

/** Socket timeouts of every connection of an account, in milliseconds (roadmap appendix B.5). */
data class MailTimeouts(
    val connectMillis: Long = MailLimits.DEFAULT_CONNECT_TIMEOUT_MS,
    val readMillis: Long = MailLimits.DEFAULT_READ_TIMEOUT_MS,
    val writeMillis: Long = readMillis,
) {
    init {
        listOf("connect" to connectMillis, "read" to readMillis, "write" to writeMillis).forEach { (name, value) ->
            require(value in 1..MailLimits.MAX_TIMEOUT_MS) { "$name timeout must be within 1..${MailLimits.MAX_TIMEOUT_MS} ms: $value" }
        }
    }

    companion object {
        fun uniform(millis: Long): MailTimeouts = MailTimeouts(millis, millis, millis)
    }
}

/**
 * Connection facts of one account after normalization ([MailAccountOptions]). Secrets live in
 * [MailSecret], never here, so instances may be logged and compared freely.
 */
data class MailAccount(
    val address: String,
    val username: String = address,
    /** Display name used in the `From` header of outgoing messages. */
    val displayName: String? = null,
    val auth: AuthMethod = AuthMethod.PASSWORD,
    /** Protocol used for every receive operation; [MailProtocol.SMTP] is not allowed here. */
    val receive: MailProtocol = MailProtocol.IMAP,
    val imap: MailEndpoint? = null,
    val pop3: MailEndpoint? = null,
    val smtp: MailEndpoint? = null,
    /** Accept any server certificate and skip host name verification; marks the session insecure (roadmap D25). */
    val trustAll: Boolean = false,
    val timeouts: MailTimeouts = MailTimeouts(),
    /** Key-value pairs sent with the IMAP `ID` command after login; empty means the command is not sent. */
    val clientId: Map<String, String> = emptyMap(),
    /** Collect the redacted protocol trace of the session (roadmap D28). */
    val debug: Boolean = false,
    /** The preset the endpoints were merged from, if any; carries the sent-folder convention. */
    val provider: ProviderPreset? = null,
) {
    init {
        require(address.isNotBlank()) { "address must not be blank" }
        require(username.isNotBlank()) { "username must not be blank" }
        require(receive != MailProtocol.SMTP) { "receive protocol must be imap or pop3" }
    }

    fun endpointOrNull(protocol: MailProtocol): MailEndpoint? = when (protocol) {
        MailProtocol.IMAP -> imap
        MailProtocol.POP3 -> pop3
        MailProtocol.SMTP -> smtp
    }

    fun endpoint(protocol: MailProtocol): MailEndpoint =
        endpointOrNull(protocol) ?: throw IllegalArgumentException("account $address has no ${protocol.id} endpoint")

    /** The endpoint receive operations use, or null when the account is send-only. */
    val receiveEndpoint: MailEndpoint? get() = endpointOrNull(receive)

    /** Protocols that have an endpoint, in contract order. */
    val configuredProtocols: List<MailProtocol> get() = MailProtocol.entries.filter { endpointOrNull(it) != null }

    /** True when any configured endpoint runs without TLS or with certificate checks disabled. */
    val insecure: Boolean
        get() = trustAll || listOfNotNull(imap, pop3, smtp).any { it.tls == TlsMode.NONE }
}

/**
 * A password, authorization code, or access token. The characters are wiped by [clear] and never
 * appear in [toString], so the object can travel through log statements without leaking.
 */
class MailSecret(secret: CharArray) {
    private val value: CharArray = secret.copyOf()

    constructor(secret: String) : this(secret.toCharArray())

    val isEmpty: Boolean get() = value.isEmpty()

    /** Materializes the secret for the Jakarta Mail authenticator, which only accepts strings. */
    fun reveal(): String = String(value)

    fun clear() {
        value.fill(' ')
    }

    override fun toString(): String = "MailSecret(***)"
}
