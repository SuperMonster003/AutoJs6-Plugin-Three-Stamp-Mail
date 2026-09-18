package io.github.supermonster003.autojs6.plugin.angus.mail.core.message

import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/**
 * Bytes of an outgoing attachment. [open] must return a fresh stream positioned at the start
 * every time, because a message is serialized once for SMTP and once more for the IMAP `APPEND`
 * of `saveToSent`. The app implements it over the host's read-only descriptors; tests use files.
 */
interface AttachmentSource {
    /** Size in bytes, or a negative value when unknown. */
    val size: Long

    fun open(): InputStream
}

class FileAttachmentSource(val file: File) : AttachmentSource {
    override val size: Long get() = file.length()
    override fun open(): InputStream = file.inputStream()
}

class BytesAttachmentSource(private val bytes: ByteArray) : AttachmentSource {
    override val size: Long get() = bytes.size.toLong()
    override fun open(): InputStream = ByteArrayInputStream(bytes)
}
