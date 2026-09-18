package io.github.supermonster003.autojs6.plugin.angus.mail.core.session

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailEndpoint
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailSecret
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.ExceptionMapper
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.Redactor
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.AccountDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.EndpointReport
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SessionTestResult
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.toDocument
import java.io.Closeable

/**
 * One account's session (roadmap D15): the IMAP or POP3 store and the SMTP transport behind
 * [ConnectionGuard]s that connect lazily, expire when idle, and reconnect after a loss; the
 * redacted [trace]; and the exception mapping every operation goes through. Not thread-safe: the
 * app runs each session on one executor thread and calls [expireIdle] from a timer on that thread.
 */
class MailSession(
    val account: MailAccount,
    private val secret: MailSecret,
    private val clock: () -> Long = System::currentTimeMillis,
    idleTimeoutMillis: Long = MailLimits.SESSION_IDLE_TIMEOUT_MS,
) : Closeable {

    val redactor = Redactor(account.username, secret)
    val mapper = ExceptionMapper(redactor)
    val trace = ProtocolTrace(account.debug, redactor, clock)

    private val imapGuard = ConnectionGuard<ImapMailbox>(
        label = MailProtocol.IMAP.id,
        idleTimeoutMillis = idleTimeoutMillis,
        clock = clock,
        isAlive = { it.isConnected },
        onClose = { it.close() },
        connect = { ImapMailbox.connect(account, secret, trace) },
    )
    private val pop3Guard = ConnectionGuard<Pop3Mailbox>(
        label = MailProtocol.POP3.id,
        idleTimeoutMillis = idleTimeoutMillis,
        clock = clock,
        isAlive = { it.isConnected },
        onClose = { it.close() },
        connect = { Pop3Mailbox.connect(account, secret, trace) },
    )
    private val smtpGuard = ConnectionGuard<SmtpSender>(
        label = MailProtocol.SMTP.id,
        idleTimeoutMillis = idleTimeoutMillis,
        clock = clock,
        isAlive = { it.isConnected },
        onClose = { it.close() },
        connect = { SmtpSender(account, secret, trace).connect() },
    )

    var isClosed: Boolean = false
        private set

    /** The last failure any operation reported, for `getStatus`. */
    var lastError: MailException? = null
        private set

    /** Protocols with a live connection right now. */
    val connectedProtocols: List<MailProtocol>
        get() = listOfNotNull(
            MailProtocol.IMAP.takeIf { imapGuard.isConnected },
            MailProtocol.POP3.takeIf { pop3Guard.isConnected },
            MailProtocol.SMTP.takeIf { smtpGuard.isConnected },
        )

    /** Times each protocol connected; tests use it to prove reconnects. */
    fun connectCount(protocol: MailProtocol): Int = guard(protocol).connectCount

    fun <R> imap(retryOnLoss: Boolean = true, block: (ImapMailbox) -> R): R = guarded(MailProtocol.IMAP, imapGuard, retryOnLoss, block)

    fun <R> pop3(retryOnLoss: Boolean = true, block: (Pop3Mailbox) -> R): R = guarded(MailProtocol.POP3, pop3Guard, retryOnLoss, block)

    /** SMTP operations never retry by default: a send interrupted mid-way must not go out twice. */
    fun <R> smtp(retryOnLoss: Boolean = false, block: (SmtpSender) -> R): R = guarded(MailProtocol.SMTP, smtpGuard, retryOnLoss, block)

    /**
     * Probes the receive endpoint and the SMTP endpoint (roadmap P2.1 `session.test`): connects
     * each, lists its capabilities, and reports the round-trip time. Endpoint failures are part of
     * the result, not exceptions; only a closed session throws.
     */
    fun test(): SessionTestResult {
        ensureOpen()
        val started = clock()
        val imap = account.imap?.takeIf { account.receive == MailProtocol.IMAP }?.let { endpoint ->
            probe(MailProtocol.IMAP, endpoint) { imap(retryOnLoss = false) { it.capabilities() } }
        }
        val pop3 = account.pop3?.takeIf { account.receive == MailProtocol.POP3 }?.let { endpoint ->
            probe(MailProtocol.POP3, endpoint) { pop3(retryOnLoss = false) { it.capabilities() } }
        }
        val smtp = account.smtp?.let { endpoint ->
            probe(MailProtocol.SMTP, endpoint) { smtp { it.extensions() } }
        }
        val reports = listOfNotNull(imap, pop3, smtp)
        return SessionTestResult(
            ok = reports.all { it.ok },
            account = AccountDocument.of(account),
            imap = imap,
            pop3 = pop3,
            smtp = smtp,
            elapsedMs = clock() - started,
        )
    }

    private fun probe(protocol: MailProtocol, endpoint: MailEndpoint, capabilities: () -> List<String>): EndpointReport {
        val started = clock()
        return try {
            val advertised = capabilities()
            EndpointReport(protocol.id, endpoint.host, endpoint.port, endpoint.tls.id, ok = true, elapsedMs = clock() - started, capabilities = advertised)
        } catch (e: MailException) {
            EndpointReport(protocol.id, endpoint.host, endpoint.port, endpoint.tls.id, ok = false, elapsedMs = clock() - started, error = e.toDocument())
        }
    }

    /** Drops connections idle for longer than the session idle timeout; returns how many were dropped. */
    fun expireIdle(): Int = listOf(imapGuard, pop3Guard, smtpGuard).count { it.expireIfIdle() }

    /** Drops every connection but keeps the session usable; the next operation reconnects. */
    fun disconnect() {
        listOf(imapGuard, pop3Guard, smtpGuard).forEach { it.drop() }
    }

    override fun close() {
        if (isClosed) return
        isClosed = true
        listOf(imapGuard, pop3Guard, smtpGuard).forEach { runCatching { it.close() } }
        secret.clear()
    }

    private fun guard(protocol: MailProtocol): ConnectionGuard<*> = when (protocol) {
        MailProtocol.IMAP -> imapGuard
        MailProtocol.POP3 -> pop3Guard
        MailProtocol.SMTP -> smtpGuard
    }

    private fun <T : Any, R> guarded(protocol: MailProtocol, guard: ConnectionGuard<T>, retryOnLoss: Boolean, block: (T) -> R): R {
        ensureOpen()
        if (account.endpointOrNull(protocol) == null) {
            throw MailException.invalidArgument("account ${account.address} has no ${protocol.id} endpoint").also { lastError = it }
        }
        try {
            return guard.use(retryOnLoss, block)
        } catch (e: MailException) {
            lastError = e
            throw e
        } catch (e: Exception) {
            val mapped = mapper.map(e, "${protocol.id} ${endpointLabel(protocol)}")
            lastError = mapped
            throw mapped
        }
    }

    private fun endpointLabel(protocol: MailProtocol): String = account.endpointOrNull(protocol)?.toString() ?: protocol.id

    private fun ensureOpen() {
        if (isClosed) throw MailException(MailErrorCode.SESSION_CLOSED, "session is closed", retryable = false)
    }
}
