package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.AuthMethod
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MimeLeniency
import java.util.Properties

/**
 * Translates a [MailAccount] into the `jakarta.mail` property set for one protocol. The result
 * never contains a secret: authentication goes through the session authenticator. With a
 * [SocketRegistry] the plain socket under every connection comes from the registry's factory, so
 * the session can abort a blocked operation from another thread (roadmap P2.5 `cancel`).
 */
object MailSessionProperties {

    /** Provider name of [protocol] under [tls]; Angus Mail registers `imaps`, `pop3s`, and `smtps` as separate providers. */
    fun providerName(protocol: MailProtocol, tls: TlsMode): String =
        if (tls == TlsMode.SSL) "${protocol.id}s" else protocol.id

    fun build(account: MailAccount, protocol: MailProtocol, sockets: SocketRegistry? = null): Properties {
        val endpoint = account.endpoint(protocol)
        val provider = providerName(protocol, endpoint.tls)
        val prefix = "mail.$provider"
        return Properties().apply {
            // Lenient MIME handling for real-world messages (roadmap D21: charset and header fallbacks).
            put("mail.mime.charset", "UTF-8")
            put("mail.mime.decodetext.strict", "false")
            put("mail.mime.decodefilename", "true")
            put("mail.mime.encodefilename", "true")
            put("mail.mime.parameters.strict", "false")
            put("mail.mime.address.strict", "false")
            // Hostile multiparts (roadmap P6): the session-level switches of MimeLeniency.
            MimeLeniency.apply(this)

            if (protocol == MailProtocol.SMTP) {
                put("mail.transport.protocol", provider)
            } else {
                put("mail.store.protocol", provider)
            }

            put("$prefix.host", endpoint.host)
            put("$prefix.port", endpoint.port.toString())
            put("$prefix.connectiontimeout", account.timeouts.connectMillis.toString())
            put("$prefix.timeout", account.timeouts.readMillis.toString())
            put("$prefix.writetimeout", account.timeouts.writeMillis.toString())
            put("$prefix.auth", "true")
            // Never fall back from the requested TLS mode or authentication mechanism to a weaker one.
            when (endpoint.tls) {
                TlsMode.SSL -> {
                    put("$prefix.ssl.enable", "true")
                    put("$prefix.ssl.checkserveridentity", (!account.trustAll).toString())
                }
                TlsMode.STARTTLS -> {
                    put("$prefix.ssl.enable", "false")
                    put("$prefix.starttls.enable", "true")
                    put("$prefix.starttls.required", "true")
                    put("$prefix.ssl.checkserveridentity", (!account.trustAll).toString())
                }
                TlsMode.NONE -> {
                    put("$prefix.ssl.enable", "false")
                    put("$prefix.starttls.enable", "false")
                }
            }
            if (account.trustAll && endpoint.tls != TlsMode.NONE) {
                put("$prefix.ssl.trust", "*")
            }
            if (sockets != null) {
                // Angus asks this factory for the unconnected socket and connects, times out and
                // layers TLS on it itself (SocketFetcher), for implicit SSL and STARTTLS alike;
                // without the fallback switch a failed connect would be retried on an untracked socket.
                put("$prefix.socketFactory", sockets.factory)
                put("$prefix.socketFactory.fallback", "false")
            }
            when (account.auth) {
                AuthMethod.PASSWORD -> put("$prefix.auth.mechanisms", "LOGIN PLAIN")
                AuthMethod.XOAUTH2 -> put("$prefix.auth.mechanisms", "XOAUTH2")
            }
            if (protocol == MailProtocol.IMAP) {
                // Fetch bodies in chunks and never flip the SEEN flag as a side effect of reading.
                put("$prefix.partialfetch", "true")
                put("$prefix.fetchsize", "65536")
                put("$prefix.peek", "true")
            }
            if (protocol == MailProtocol.SMTP) {
                put("$prefix.from", account.address)
                // EHLO name: a fixed literal avoids the blocking reverse lookup of the device's own address.
                put("$prefix.localhost", "localhost")
            }
        }
    }
}
