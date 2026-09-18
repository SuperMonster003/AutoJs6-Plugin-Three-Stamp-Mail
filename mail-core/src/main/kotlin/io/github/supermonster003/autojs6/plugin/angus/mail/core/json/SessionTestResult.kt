package io.github.supermonster003.autojs6.plugin.angus.mail.core.json

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/** The `error` object of a response envelope (contract `FIELD_ERROR_*`). */
@Serializable
data class ErrorDocument(
    val code: String,
    val message: String,
    val details: String? = null,
    val retryable: Boolean,
)

fun MailException.toDocument(): ErrorDocument = ErrorDocument(code, message, details, retryable)

@Serializable
data class EndpointDocument(
    val host: String,
    val port: Int,
    val tls: String,
) {
    companion object {
        fun of(endpoint: MailEndpoint): EndpointDocument = EndpointDocument(endpoint.host, endpoint.port, endpoint.tls.id)
    }
}

/** The account as scripts see it through `client.account` and `session.test`: never a secret. */
@Serializable
data class AccountDocument(
    val address: String,
    val user: String,
    val name: String? = null,
    val auth: String,
    val receive: String,
    val provider: String? = null,
    val imap: EndpointDocument? = null,
    val pop3: EndpointDocument? = null,
    val smtp: EndpointDocument? = null,
    /** True when a configured endpoint runs without TLS or with certificate checks disabled (roadmap D25). */
    val insecure: Boolean,
    val debug: Boolean,
) {
    companion object {
        fun of(account: MailAccount): AccountDocument = AccountDocument(
            address = account.address,
            user = account.username,
            name = account.displayName,
            auth = account.auth.id,
            receive = account.receive.id,
            provider = account.provider?.id,
            imap = account.imap?.let(EndpointDocument::of),
            pop3 = account.pop3?.let(EndpointDocument::of),
            smtp = account.smtp?.let(EndpointDocument::of),
            insecure = account.insecure,
            debug = account.debug,
        )
    }
}

/** Outcome of probing one endpoint in `session.test`. */
@Serializable
data class EndpointReport(
    val protocol: String,
    val host: String,
    val port: Int,
    val tls: String,
    val ok: Boolean,
    val elapsedMs: Long,
    /** IMAP capabilities, POP3 CAPA keys, or SMTP EHLO extensions the server advertised. */
    val capabilities: List<String> = emptyList(),
    val error: ErrorDocument? = null,
)

/** Result document of the `session.test` operation. */
@Serializable
data class SessionTestResult(
    val ok: Boolean,
    val account: AccountDocument,
    val imap: EndpointReport? = null,
    val pop3: EndpointReport? = null,
    val smtp: EndpointReport? = null,
    val elapsedMs: Long,
)

/** The result document of `session.test` as the Binder sends it; keeps `kotlinx.serialization` inside this module. */
fun SessionTestResult.toJson(): String = MailJson.format.encodeToString(this)
