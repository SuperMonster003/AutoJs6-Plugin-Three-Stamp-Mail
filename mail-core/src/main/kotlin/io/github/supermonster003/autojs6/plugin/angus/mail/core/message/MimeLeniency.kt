package io.github.supermonster003.autojs6.plugin.angus.mail.core.message

import java.util.Properties

/**
 * Lenient MIME decoding for the damaged and the hostile mail the plugin meets (roadmap P6
 * hostile input, D39): the Jakarta Mail switches that make a broken part readable instead of
 * an exception (which the connection guard would otherwise take for a lost connection).
 *
 * Four of them are read once when the Jakarta class initializes ([STATIC_KEYS]), so [install]
 * runs from the first mail-core entry points (`MailAccountOptions`, `OutgoingMessageParser`,
 * `MimeTree`, `MessageMapper`, `MailSessionFactory`) and from the plugin service's `onCreate`;
 * the mail-core test task sets the same values on its JVM because a test class may load Jakarta
 * before any of these objects. Three are read per multipart from the session, so
 * [MailSessionProperties] copies them into every session through [apply].
 */
object MimeLeniency {

    /** Property name to value, every value a boolean literal. */
    val PROPERTIES: Map<String, String> = linkedMapOf(
        // static in MimeUtility: an unknown Content-Transfer-Encoding is read as identity instead of failing
        "mail.mime.ignoreunknownencoding" to "true",
        // static in MimeUtility: encoded words that are not delimited by whitespace are still decoded
        "mail.mime.decodetext.strict" to "false",
        // static in ParameterList: a malformed Content-Type / Content-Disposition parameter is skipped instead of failing the header
        "mail.mime.parameters.strict" to "false",
        // static in MimeBodyPart: RFC 2047 words inside file names are decoded
        "mail.mime.decodefilename" to "true",
        // per decoder stream: an invalid base64 / uuencode character is skipped instead of ending the read with an IOException
        "mail.mime.base64.ignoreerrors" to "true",
        "mail.mime.uudecode.ignoreerrors" to "true",
        // per multipart (session first, then system): no parts, no boundary parameter or no end boundary is not an error
        "mail.mime.multipart.allowempty" to "true",
        "mail.mime.multipart.ignoremissingboundaryparameter" to "true",
        "mail.mime.multipart.ignoremissingendboundary" to "true",
    )

    /** The switches Jakarta reads into a static field when the class loads. */
    val STATIC_KEYS: Set<String> = setOf(
        "mail.mime.ignoreunknownencoding",
        "mail.mime.decodetext.strict",
        "mail.mime.parameters.strict",
        "mail.mime.decodefilename",
    )

    /** The switches a `MimeMultipart` reads from the session properties of its message. */
    val SESSION_KEYS: Set<String> = setOf(
        "mail.mime.multipart.allowempty",
        "mail.mime.multipart.ignoremissingboundaryparameter",
        "mail.mime.multipart.ignoremissingendboundary",
    )

    @Volatile
    private var installed = false

    /** Sets every switch as a JVM system property; idempotent. */
    fun install() {
        if (installed) return
        PROPERTIES.forEach { (key, value) -> System.setProperty(key, value) }
        installed = true
    }

    /** Puts the session-level switches into [properties] and returns them. */
    fun apply(properties: Properties): Properties {
        SESSION_KEYS.forEach { key -> properties[key] = PROPERTIES.getValue(key) }
        return properties
    }
}
