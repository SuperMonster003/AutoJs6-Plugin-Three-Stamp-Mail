package io.github.supermonster003.autojs6.plugin.angus.mail.core.error

import jakarta.mail.AuthenticationFailedException
import jakarta.mail.FolderClosedException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.session.SinkFailedException
import jakarta.mail.FolderNotFoundException
import jakarta.mail.MessageRemovedException
import jakarta.mail.MessagingException
import jakarta.mail.MethodNotSupportedException
import jakarta.mail.ReadOnlyFolderException
import jakarta.mail.SendFailedException
import jakarta.mail.StoreClosedException
import jakarta.mail.internet.ParseException
import org.eclipse.angus.mail.iap.BadCommandException
import org.eclipse.angus.mail.iap.CommandFailedException
import org.eclipse.angus.mail.iap.ConnectionException
import org.eclipse.angus.mail.iap.ProtocolException
import org.eclipse.angus.mail.smtp.SMTPSendFailedException
import org.eclipse.angus.mail.util.MailConnectException
import java.io.EOFException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.ClosedByInterruptException
import java.security.cert.CertificateException
import javax.net.ssl.SSLException

/** Gmail's reply to STAT when POP is disabled for the account: `[SYS/PERM] Your account is not enabled for POP access`. */
private const val POP_ACCESS_DISABLED = "not enabled for POP"

/**
 * Turns whatever Jakarta Mail, the JDK, or the mail core itself throws into a [MailException]
 * with a contract error code (roadmap D18). Messages and details pass through the [Redactor], so a
 * server reply that echoes a credential never reaches the envelope.
 */
class ExceptionMapper(private val redactor: Redactor) {

    fun map(throwable: Throwable, context: String? = null): MailException {
        if (throwable is MailException) return throwable
        val chain = chainOf(throwable)
        val classified = classify(chain)
        val detail = redactor.scrubOrNull(chain.firstNotNullOfOrNull { it.message?.takeIf(String::isNotBlank) })
        val message = buildString {
            if (context != null) append(context).append(": ")
            append(classified.message)
        }
        val details = when {
            detail == null -> null
            classified.details != null -> redactor.scrub("${classified.details}; $detail")
            else -> detail
        }
        return MailException(classified.code, redactor.scrub(message), details, classified.retryable, throwable)
    }

    private class Classified(val code: String, val message: String, val retryable: Boolean = MailErrorCode.isRetryableByDefault(code), val details: String? = null)

    private fun classify(chain: List<Throwable>): Classified {
        val auth = chain.find<AuthenticationFailedException>()
        if (auth != null) return Classified(MailErrorCode.AUTH_FAILED, "authentication failed")
        chain.find<SinkFailedException>()?.let { return Classified(MailErrorCode.IO_FAILED, it.message ?: "the download destination could not be written", retryable = false) }
        if (chain.any { it is SSLException || it is CertificateException }) {
            return Classified(MailErrorCode.TLS_FAILED, "TLS handshake failed", retryable = false)
        }
        if (chain.any { it is MessagingException && it.mentionsStarttls() }) {
            return Classified(MailErrorCode.TLS_FAILED, "the server does not offer the required STARTTLS upgrade", retryable = false)
        }
        if (chain.any { it is SocketTimeoutException }) return Classified(MailErrorCode.TIMEOUT, "the server did not answer in time")
        if (chain.any { it is MailConnectException || it is UnknownHostException || it is ConnectException || it is NoRouteToHostException }) {
            return Classified(MailErrorCode.CONNECT_FAILED, "could not connect to the server")
        }
        if (chain.any { it is FolderNotFoundException }) return Classified(MailErrorCode.FOLDER_NOT_FOUND, "folder not found")
        if (chain.any { it is MessageRemovedException }) return Classified(MailErrorCode.MESSAGE_NOT_FOUND, "message no longer exists")
        if (chain.any { it is FolderClosedException || it is StoreClosedException || it is ConnectionException }) {
            return Classified(MailErrorCode.CONNECT_FAILED, "the connection to the server was lost")
        }
        chain.find<SendFailedException>()?.let { failure ->
            val invalid = failure.invalidAddresses.orEmpty().map { it.toString() }
            val unsent = failure.validUnsentAddresses.orEmpty().map { it.toString() }
            val summary = listOfNotNull(
                invalid.takeIf { it.isNotEmpty() }?.let { "rejected: ${it.joinToString(", ")}" },
                unsent.takeIf { it.isNotEmpty() }?.let { "not sent: ${it.joinToString(", ")}" },
            ).joinToString("; ").ifEmpty { null }
            val transient = failure is SMTPSendFailedException && failure.returnCode in 400..499
            return if (transient) {
                Classified(MailErrorCode.SERVER_ERROR, "the server temporarily refused the message", retryable = true, details = summary)
            } else {
                Classified(MailErrorCode.SEND_REJECTED, "the server rejected the message", retryable = false, details = summary)
            }
        }
        if (chain.any { it is ParseException }) return Classified(MailErrorCode.INVALID_ARGUMENT, "malformed address or header")
        if (chain.any { it is MethodNotSupportedException || it is ReadOnlyFolderException || it is UnsupportedOperationException }) {
            return Classified(MailErrorCode.UNSUPPORTED_OPERATION, "the server or protocol does not support this operation")
        }
        if (chain.any { it is InterruptedException || it is ClosedByInterruptException || (it is InterruptedIOException && it !is SocketTimeoutException) }) {
            return Classified(MailErrorCode.CANCELLED, "the operation was cancelled", retryable = false)
        }
        if (chain.any { it is CommandFailedException || it is BadCommandException || it is ProtocolException }) {
            return Classified(MailErrorCode.SERVER_ERROR, "the server rejected the command", retryable = false)
        }
        if (chain.any { it is SocketException || it is EOFException }) {
            return Classified(MailErrorCode.CONNECT_FAILED, "the connection to the server was lost")
        }
        // Gmail answers STAT with "[SYS/PERM] Your account is not enabled for POP access" after a successful login;
        // Angus surfaces it as a plain IOException, which would otherwise read as a retryable "I/O failed".
        if (chain.any { it is IOException && it.message?.contains(POP_ACCESS_DISABLED, ignoreCase = true) == true }) {
            return Classified(MailErrorCode.UNSUPPORTED_OPERATION, "POP access is not enabled for this account at the provider", retryable = false)
        }
        if (chain.any { it is IOException }) return Classified(MailErrorCode.IO_FAILED, "I/O failed")
        if (chain.any { it is MessagingException }) return Classified(MailErrorCode.SERVER_ERROR, "the mail server reported an error", retryable = false)
        if (chain.first() is IllegalArgumentException) return Classified(MailErrorCode.INVALID_ARGUMENT, "invalid argument")
        return Classified(MailErrorCode.INTERNAL, "internal error", retryable = false)
    }

    private fun MessagingException.mentionsStarttls(): Boolean {
        val text = message.orEmpty()
        return text.contains("STARTTLS", ignoreCase = true) || text.contains("STLS", ignoreCase = true)
    }

    private inline fun <reified T : Throwable> List<Throwable>.find(): T? = firstOrNull { it is T } as T?

    companion object {
        private const val MAX_CHAIN = 12

        /** The throwable and its causes, cycle-safe, plus the next exception of a [MessagingException]. */
        fun chainOf(throwable: Throwable): List<Throwable> {
            val chain = ArrayList<Throwable>()
            var current: Throwable? = throwable
            while (current != null && chain.size < MAX_CHAIN && current !in chain) {
                chain += current
                current = when (current) {
                    is MessagingException -> current.nextException ?: current.cause
                    else -> current.cause
                }
            }
            return chain
        }
    }
}
