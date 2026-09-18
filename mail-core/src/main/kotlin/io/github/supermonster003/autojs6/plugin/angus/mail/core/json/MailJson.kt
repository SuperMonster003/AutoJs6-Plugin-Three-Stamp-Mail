package io.github.supermonster003.autojs6.plugin.angus.mail.core.json

import kotlinx.serialization.json.Json

/** The one `kotlinx.serialization` configuration of the mail core: compact, null-free, tolerant of unknown keys. */
object MailJson {

    val format: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = false
    }
}
