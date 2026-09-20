package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import jakarta.mail.Session
import jakarta.mail.URLName
import org.eclipse.angus.mail.iap.ConnectionException
import org.eclipse.angus.mail.iap.ProtocolException
import org.eclipse.angus.mail.imap.IMAPStore
import org.eclipse.angus.mail.imap.protocol.IMAPProtocol
import org.eclipse.angus.mail.util.MailLogger
import java.util.Properties
import java.util.concurrent.atomic.AtomicInteger

/**
 * An [IMAPStore] that sends the `ID` command (RFC 2971) on every connection it opens, right
 * after that connection has authenticated.
 *
 * Angus Mail keeps a small pool of connections (`mail.imap.connectionpoolsize`, default 1): the
 * connection made by `connect` serves store commands, an opened folder takes it, and a store
 * command issued while the folder holds it (`hasCapability`, `LIST`, `STATUS`, ...) opens a
 * further connection; when the folder closes, the pool keeps only as many as fit and drops the
 * rest. NetEase (163 / 126 / yeah.net) answers `NO SELECT Unsafe Login` (and the same for
 * `EXAMINE`) on any connection that has not identified itself, so [IMAPStore.id] on the first
 * connection alone is not enough: on 2026-09-18 the second connection (tag prefix `B`) failed
 * exactly like that after the first one had identified itself and had then been dropped.
 *
 * An empty [clientId] means "never send ID"; servers that do not advertise the `ID` capability
 * are left alone. The answer of the first identified connection is kept as [serverId].
 */
internal class IdentifyingImapStore(
    session: Session,
    url: URLName?,
    protocolName: String,
    isSSL: Boolean,
    private val clientId: Map<String, String>,
    private val trace: ProtocolTrace,
) : IMAPStore(session, url, protocolName, isSSL) {

    private val connections = AtomicInteger()
    private val identified = AtomicInteger()

    /** What the server answered to the first `ID` command, or null when none was sent or accepted. */
    @Volatile
    var serverId: Map<String, String>? = null
        private set

    /** How many connections have identified themselves so far (diagnostics and tests). */
    val identifiedConnections: Int get() = identified.get()

    /** How many connections were opened so far, identified or not. */
    val openedConnections: Int get() = connections.get()

    override fun newIMAPProtocol(host: String?, port: Int): IMAPProtocol =
        IdentifyingImapProtocol(name, host, port, session.properties, isSSL, logger, connections.incrementAndGet(), ::identify)

    private fun identify(protocol: IMAPProtocol, connection: Int) {
        if (clientId.isEmpty()) return
        // Angus refreshes the capabilities after the login only when the OK response did not carry
        // them; do it here already so a server that advertises ID only once authenticated is seen.
        if (protocol.hasCapability(PRELOGIN_MARKER)) {
            try {
                protocol.capability()
            } catch (e: ConnectionException) {
                throw e
            } catch (_: ProtocolException) {
                // Angus ignores the same failure; the pre-login capabilities stay in effect.
            }
        }
        if (!protocol.hasCapability("ID")) return
        val answer = try {
            trace.timed(MailProtocol.IMAP.id, "ID ${clientId.keys.joinToString(",")} (connection $connection)") { protocol.id(clientId) }
        } catch (e: ConnectionException) {
            throw e
        } catch (_: ProtocolException) {
            // The server refused the ID command itself; the login succeeded, so the connection
            // stays usable and whatever the server thinks of an unidentified client surfaces on
            // the next command with its own error.
            return
        }
        identified.incrementAndGet()
        if (serverId == null) serverId = answer ?: emptyMap()
    }

    private companion object {
        /** The marker Angus puts into the capability map before the login to detect a refresh. */
        const val PRELOGIN_MARKER = "__PRELOGIN__"
    }
}

/**
 * The protocol connection of an [IdentifyingImapStore]: reports every successful authentication
 * (whichever mechanism Angus Mail picked) so the store can identify the connection at once.
 */
internal class IdentifyingImapProtocol(
    name: String,
    host: String?,
    port: Int,
    props: Properties,
    isSSL: Boolean,
    logger: MailLogger,
    private val connection: Int,
    private val onAuthenticated: (IMAPProtocol, Int) -> Unit,
) : IMAPProtocol(name, host, port, props, isSSL, logger) {

    override fun login(u: String?, p: String?) {
        super.login(u, p)
        authenticated()
    }

    @Synchronized
    override fun authlogin(u: String?, p: String?) {
        super.authlogin(u, p)
        authenticated()
    }

    @Synchronized
    override fun authplain(authzid: String?, u: String?, p: String?) {
        super.authplain(authzid, u, p)
        authenticated()
    }

    @Synchronized
    override fun authntlm(authzid: String?, u: String?, p: String?) {
        super.authntlm(authzid, u, p)
        authenticated()
    }

    @Synchronized
    override fun authoauth2(u: String?, p: String?) {
        super.authoauth2(u, p)
        authenticated()
    }

    override fun sasllogin(allowed: Array<out String>?, realm: String?, authzid: String?, u: String?, p: String?) {
        super.sasllogin(allowed, realm, authzid, u, p)
        authenticated()
    }

    private fun authenticated() {
        if (isAuthenticated) onAuthenticated(this, connection)
    }

    /**
     * Angus enables `UTF8=ACCEPT` on every server that advertises it and then searches without
     * `CHARSET`, sending non-ASCII text as a UTF-8 quoted string or literal (RFC 6855). Gmail
     * advertises the extension and still answers such a `SEARCH` with `BAD Could not parse
     * command`, while `SEARCH CHARSET UTF-8` with a literal finds the messages (verified with a
     * real account on 2026-09-20, roadmap P6 provider matrix); every other preset provider lacks
     * the extension. The extension is therefore never enabled: searches always carry `CHARSET`
     * and mailbox names always use modified UTF-7, the path every server takes.
     */
    override fun enable(cap: String?) {
        if (cap.equals(UTF8_ACCEPT, ignoreCase = true)) return
        super.enable(cap)
    }

    private companion object {
        const val UTF8_ACCEPT = "UTF8=ACCEPT"
    }
}
