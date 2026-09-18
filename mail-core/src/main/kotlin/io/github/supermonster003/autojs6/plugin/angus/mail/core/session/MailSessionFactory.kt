package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import jakarta.mail.Authenticator
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Store
import jakarta.mail.Transport

/**
 * Creates `jakarta.mail` sessions and connected stores or transports for a [MailAccount]. Every
 * session is private to one account and protocol; nothing is cached in the JVM-wide default session.
 */
object MailSessionFactory {

    fun session(account: MailAccount, protocol: MailProtocol, secret: MailSecret): Session {
        MailcapRegistry.ensureRegistered()
        val authenticator = object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication =
                PasswordAuthentication(account.username, secret.reveal())
        }
        return Session.getInstance(MailSessionProperties.build(account, protocol), authenticator).apply {
            // Protocol traces would contain credentials; roadmap D28 exposes only a redacted summary.
            debug = false
        }
    }

    /** Connects an IMAP or POP3 store; the caller owns it and must close it. */
    fun connectStore(account: MailAccount, protocol: MailProtocol, secret: MailSecret, trace: ProtocolTrace = ProtocolTrace.disabled()): Store {
        require(protocol != MailProtocol.SMTP) { "SMTP is a transport, not a store" }
        val endpoint = account.endpoint(protocol)
        val session = session(account, protocol, secret)
        val store = session.getStore(MailSessionProperties.providerName(protocol, endpoint.tls))
        trace.timed(protocol.id, "connect $endpoint ${account.auth.id}") {
            store.connect(endpoint.host, endpoint.port, account.username, secret.reveal())
        }
        return store
    }

    /** Connects an SMTP transport; the caller owns it and must close it. */
    fun connectTransport(account: MailAccount, secret: MailSecret, trace: ProtocolTrace = ProtocolTrace.disabled()): Transport {
        val endpoint = account.endpoint(MailProtocol.SMTP)
        val session = session(account, MailProtocol.SMTP, secret)
        val transport = session.getTransport(MailSessionProperties.providerName(MailProtocol.SMTP, endpoint.tls))
        trace.timed(MailProtocol.SMTP.id, "connect $endpoint ${account.auth.id}") {
            transport.connect(endpoint.host, endpoint.port, account.username, secret.reveal())
        }
        return transport
    }
}
