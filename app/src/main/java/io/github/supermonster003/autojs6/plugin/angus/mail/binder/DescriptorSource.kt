package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.AttachmentSource
import java.io.IOException
import java.io.InputStream

/**
 * An [AttachmentSource] over one descriptor the host handed to `call` (contract B.3: the host
 * keeps its own descriptor until `onResult`, this copy belongs to the plugin and the Binder closes
 * it before the result goes out). Every [open] duplicates the descriptor and rewinds it, so the
 * same message can be serialized more than once: once for SMTP and once more for the IMAP
 * `APPEND` of the sent copy. Only seekable files qualify; a pipe could be read a single time.
 */
internal class DescriptorSource private constructor(
    private val descriptor: ParcelFileDescriptor,
    override val size: Long,
) : AttachmentSource {

    override fun open(): InputStream {
        val copy = descriptor.dup()
        try {
            Os.lseek(copy.fileDescriptor, 0, OsConstants.SEEK_SET)
        } catch (e: ErrnoException) {
            copy.close()
            throw IOException("attachment descriptor cannot be rewound (${OsConstants.errnoName(e.errno)})", e)
        }
        return ParcelFileDescriptor.AutoCloseInputStream(copy)
    }

    companion object {
        /**
         * Wraps the descriptors of one call in order; the error names only the index. The caller
         * owns the descriptors and closes every one of them whether this succeeds or not.
         */
        fun wrap(descriptors: List<ParcelFileDescriptor?>): List<DescriptorSource> = descriptors.mapIndexed { index, descriptor ->
            val fd = descriptor?.fileDescriptor
            if (descriptor == null || fd == null || !fd.valid()) throw MailException.invalidArgument("descriptor $index is not open")
            try {
                Os.lseek(fd, 0, OsConstants.SEEK_CUR)
            } catch (e: ErrnoException) {
                throw MailException.invalidArgument("descriptor $index is not a seekable file", OsConstants.errnoName(e.errno))
            }
            DescriptorSource(descriptor, descriptor.statSize)
        }
    }
}
