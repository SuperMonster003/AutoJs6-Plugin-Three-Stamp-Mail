package io.github.supermonster003.autojs6.plugin.three.stamp.mail.core.session

import jakarta.activation.CommandMap
import jakarta.activation.MailcapCommandMap

/**
 * Registers the Angus Mail data content handlers in code. Jakarta Activation normally finds them
 * through `META-INF/mailcap` on the class path; an APK processed by R8 or a class loader that does
 * not expose that resource would otherwise fail with "no object DCH for MIME type multipart/mixed"
 * (roadmap P0.2). Registration is idempotent and safe to call from any thread.
 */
object MailcapRegistry {

    /** MIME type to handler class, mirroring the `META-INF/mailcap` shipped in `org.eclipse.angus:jakarta.mail`. */
    val HANDLERS: Map<String, String> = linkedMapOf(
        "text/plain" to "org.eclipse.angus.mail.handlers.text_plain",
        "text/html" to "org.eclipse.angus.mail.handlers.text_html",
        "text/xml" to "org.eclipse.angus.mail.handlers.text_xml",
        "multipart/*" to "org.eclipse.angus.mail.handlers.multipart_mixed",
        "message/rfc822" to "org.eclipse.angus.mail.handlers.message_rfc822",
    )

    @Volatile
    private var registered = false

    @Synchronized
    fun ensureRegistered() {
        if (registered) return
        val commandMap = CommandMap.getDefaultCommandMap() as? MailcapCommandMap ?: MailcapCommandMap()
        HANDLERS.forEach { (mimeType, handler) ->
            val fallback = if (mimeType == "multipart/*") "; x-java-fallback-entry=true" else ""
            commandMap.addMailcap("$mimeType;; x-java-content-handler=$handler$fallback")
        }
        CommandMap.setDefaultCommandMap(commandMap)
        registered = true
    }
}
