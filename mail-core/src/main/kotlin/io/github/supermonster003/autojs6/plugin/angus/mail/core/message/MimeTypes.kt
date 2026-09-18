package io.github.supermonster003.autojs6.plugin.angus.mail.core.message

/** MIME type inference from file extensions for attachments without an explicit `mimeType`. */
object MimeTypes {

    const val OCTET_STREAM = "application/octet-stream"

    private val BY_EXTENSION: Map<String, String> = mapOf(
        "txt" to "text/plain", "log" to "text/plain", "md" to "text/markdown", "csv" to "text/csv",
        "html" to "text/html", "htm" to "text/html", "css" to "text/css", "xml" to "text/xml",
        "json" to "application/json", "js" to "text/javascript", "ics" to "text/calendar", "vcf" to "text/vcard",
        "rtf" to "application/rtf", "eml" to "message/rfc822",
        "pdf" to "application/pdf", "zip" to "application/zip", "7z" to "application/x-7z-compressed",
        "rar" to "application/vnd.rar", "gz" to "application/gzip", "tar" to "application/x-tar", "xz" to "application/x-xz",
        "jar" to "application/java-archive", "apk" to "application/vnd.android.package-archive",
        "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "png" to "image/png", "gif" to "image/gif",
        "webp" to "image/webp", "bmp" to "image/bmp", "svg" to "image/svg+xml", "ico" to "image/x-icon", "heic" to "image/heic",
        "mp3" to "audio/mpeg", "wav" to "audio/wav", "ogg" to "audio/ogg", "m4a" to "audio/mp4", "flac" to "audio/flac",
        "mp4" to "video/mp4", "mov" to "video/quicktime", "avi" to "video/x-msvideo", "mkv" to "video/x-matroska", "webm" to "video/webm",
        "doc" to "application/msword", "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xls" to "application/vnd.ms-excel", "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "ppt" to "application/vnd.ms-powerpoint", "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    )

    fun forName(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension.isEmpty() || extension == fileName.lowercase()) return OCTET_STREAM
        return BY_EXTENSION[extension] ?: OCTET_STREAM
    }

    /** The usual extension of [mimeType] (without the dot), or null when none is known. */
    fun extensionFor(mimeType: String): String? {
        val wanted = mimeType.lowercase()
        return BY_EXTENSION.entries.firstOrNull { it.value == wanted }?.key
    }

    /** A loose `type/subtype` check for script-supplied MIME types. */
    fun isValid(mimeType: String): Boolean = MIME_TYPE.matches(mimeType)

    private val MIME_TYPE = Regex("[A-Za-z0-9!#$&^_.+-]{1,127}/[A-Za-z0-9!#$&^_.+-]{1,127}")
}
