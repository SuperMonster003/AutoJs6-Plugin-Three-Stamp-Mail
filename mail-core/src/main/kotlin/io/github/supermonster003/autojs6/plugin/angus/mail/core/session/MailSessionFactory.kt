package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import jakarta.mail.Authenticator
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
        return store
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
