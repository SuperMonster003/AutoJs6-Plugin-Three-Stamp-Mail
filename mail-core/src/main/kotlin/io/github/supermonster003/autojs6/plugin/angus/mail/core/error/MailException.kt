package io.github.supermonster003.autojs6.plugin.angus.mail.core.error

/**
 * The one exception type that leaves the mail core. [code] is a [MailErrorCode]; [message] and
 * [details] are already redacted (no credentials, no Base64 payloads, bounded length), so they can
 * be copied into the response envelope as they are.
 */
class MailException(
    val code: String,
    message: String,
    val details: String? = null,
    val retryable: Boolean = MailErrorCode.isRetryableByDefault(code),
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    init {
        require(MailErrorCode.isKnown(code)) { "unknown mail error code: $code" }
    }

    override val message: String get() = super.message ?: code

    override fun toString(): String = "MailException($code: $message)"

    companion object {
        fun invalidArgument(message: String, details: String? = null): MailException =
            MailException(MailErrorCode.INVALID_ARGUMENT, message, details)

        fun unsupported(message: String): MailException = MailException(MailErrorCode.UNSUPPORTED_OPERATION, message)

        fun sessionClosed(message: String = "session is closed"): MailException =
            MailException(MailErrorCode.SESSION_CLOSED, message, retryable = false)
    }
}
