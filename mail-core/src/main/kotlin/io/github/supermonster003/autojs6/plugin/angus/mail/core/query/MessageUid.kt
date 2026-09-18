package io.github.supermonster003.autojs6.plugin.angus.mail.core.query

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import kotlinx.serialization.json.JsonPrimitive

/**
 * A message UID as scripts pass it (host protocol: `number | string`): the numeric UID of an IMAP
 * folder, or the opaque UIDL string of a POP3 mailbox (roadmap P2.4). Parsers create it for the
 * account's receive protocol, so IMAP code can rely on [imap] and POP3 code on [text].
 */
class MessageUid private constructor(val text: String, private val number: Long?) {

    /** The IMAP UID; only reachable through args parsed for an IMAP account. */
    val imap: Long get() = number ?: throw MailException.invalidArgument("'$text' is not an IMAP UID")

    /** How the UID appears in result documents: a number for IMAP, a string for POP3. */
    val json: JsonPrimitive get() = number?.let { JsonPrimitive(it) } ?: JsonPrimitive(text)

    override fun equals(other: Any?): Boolean = other is MessageUid && other.text == text && other.number == number

    override fun hashCode(): Int = text.hashCode()

    override fun toString(): String = text

    companion object {
        fun imap(uid: Long): MessageUid = MessageUid(uid.toString(), uid)

        fun pop3(uidl: String): MessageUid = MessageUid(uidl, null)

        /**
         * Parses [text] for [protocol]: IMAP UIDs must be positive integers, POP3 UIDLs are any
         * non-blank string (RFC 1939 allows 1 to 70 printable ASCII characters; the plugin keeps
         * whatever the server sent).
         */
        fun parse(text: String, protocol: MailProtocol, path: String): MessageUid = when (protocol) {
            MailProtocol.IMAP -> text.toLongOrNull()?.takeIf { it > 0 }?.let { MessageUid(text, it) }
                ?: throw MailException.invalidArgument("'$path' is not a valid IMAP UID: '${text.take(40)}'")
            MailProtocol.POP3 -> {
                if (text.length > MAX_UIDL_LENGTH) throw MailException.invalidArgument("'$path' is not a valid POP3 UIDL: '${text.take(40)}...'")
                MessageUid(text, null)
            }
            MailProtocol.SMTP -> throw MailException.invalidArgument("'$path': SMTP has no message UIDs")
        }

        /** Longest UIDL accepted from a script (RFC 1939 says 70; some servers go longer). */
        const val MAX_UIDL_LENGTH = 256
    }
}
