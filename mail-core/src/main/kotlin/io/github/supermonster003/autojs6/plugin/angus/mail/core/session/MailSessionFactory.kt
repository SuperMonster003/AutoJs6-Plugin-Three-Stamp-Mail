package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.AuthMethod
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MimeLeniency
import jakarta.mail.AuthenticationFailedException
import jakarta.mail.Authenticator
import jakarta.mail.Folder
import jakarta.mail.MessagingException
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.Transport
import jakarta.mail.URLName

/**
 * Creates `jakarta.mail` sessions and connected stores or transports for a [MailAccount]. Every
 * session is private to one account and protocol; nothing is cached in the JVM-wide default session.
 */
object MailSessionFactory {

    init {
        MimeLeniency.install()
    }

    fun session(account: MailAccount, protocol: MailProtocol, secret: MailSecret, sockets: SocketRegistry? = null): Session {
        MailcapRegistry.ensureRegistered()
        val authenticator = object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication =
                PasswordAuthentication(account.username, secret.reveal())
        }
        return Session.getInstance(MailSessionProperties.build(account, protocol, sockets), authenticator).apply {
            // Protocol traces would contain credentials; roadmap D28 exposes only a redacted summary.
            debug = false
        }
    }

    /**
     * Connects an IMAP or POP3 store; the caller owns it and must close it. IMAP stores are
     * [IdentifyingImapStore]s so that every pooled connection sends the account's `ID` payload.
     */
    fun connectStore(account: MailAccount, protocol: MailProtocol, secret: MailSecret, trace: ProtocolTrace = ProtocolTrace.disabled(), sockets: SocketRegistry? = null): Store {
        require(protocol != MailProtocol.SMTP) { "SMTP is a transport, not a store" }
        val endpoint = account.endpoint(protocol)
        val session = session(account, protocol, secret, sockets)
        val provider = MailSessionProperties.providerName(protocol, endpoint.tls)
        val store = if (protocol == MailProtocol.IMAP) {
            val url = URLName(provider, endpoint.host, endpoint.port, null, account.username, null)
            IdentifyingImapStore(session, url, provider, endpoint.tls == TlsMode.SSL, account.clientId, trace)
        } else {
            session.getStore(provider)
        }
        trace.timed(protocol.id, "connect $endpoint ${account.auth.id}") {
            store.connect(endpoint.host, endpoint.port, account.username, secret.reveal())
        }
        if (protocol == MailProtocol.POP3 && account.auth == AuthMethod.XOAUTH2) verifyPop3OAuthLogin(store, trace)
        return store
    }

    /**
     * Angus Mail 2.0.5 loses a refused POP3 `AUTH XOAUTH2`: the server answers the refusal as a
     * SASL continuation (`+ <base64 JSON>`, Gmail does so per the XOAUTH2 specification), the
     * authenticator turns it into an `EOFException` that `Protocol.Authenticator.authenticate`
     * catches and ignores, and the store reports itself connected while the server still waits
     * for the SASL exchange to finish, so the first real command fails with `-ERR bad command`
     * (observed as `IO_FAILED Open failed` in the P6 provider matrix). A `STAT` right after the
     * login (the INBOX open) tells the two apart; the "not enabled for POP" refusal of a valid
     * token keeps its own mapping.
     */
    private fun verifyPop3OAuthLogin(store: Store, trace: ProtocolTrace) {
        try {
            trace.timed(MailProtocol.POP3.id, "verify xoauth2 login") {
                val inbox = store.getFolder("INBOX")
                inbox.open(Folder.READ_ONLY)
                inbox.close(false)
            }
        } catch (e: MessagingException) {
            if (e.chain().any { it.message?.contains("not enabled for POP", ignoreCase = true) == true }) throw e
            runCatching { store.close() }
            throw AuthenticationFailedException("the server did not accept the access token", e)
        }
    }

    private fun Throwable.chain(): List<Throwable> {
        val out = ArrayList<Throwable>()
        var current: Throwable? = this
        while (current != null && out.size < 8 && current !in out) {
            out += current
            current = (current as? MessagingException)?.nextException ?: current.cause
        }
        return out
    }

    /** Connects an SMTP transport; the caller owns it and must close it. */
    fun connectTransport(account: MailAccount, secret: MailSecret, trace: ProtocolTrace = ProtocolTrace.disabled(), sockets: SocketRegistry? = null): Transport {
        val endpoint = account.endpoint(MailProtocol.SMTP)
        val session = session(account, MailProtocol.SMTP, secret, sockets)
        val transport = session.getTransport(MailSessionProperties.providerName(MailProtocol.SMTP, endpoint.tls))
        trace.timed(MailProtocol.SMTP.id, "connect $endpoint ${account.auth.id}") {
            transport.connect(endpoint.host, endpoint.port, account.username, secret.reveal())
        }
        return transport
    }
}
