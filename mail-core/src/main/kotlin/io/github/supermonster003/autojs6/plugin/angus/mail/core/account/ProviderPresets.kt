package io.github.supermonster003.autojs6.plugin.angus.mail.core.account

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MailJson
import kotlinx.serialization.encodeToString

/**
 * The built-in provider catalog (roadmap D10, appendix C), loaded once from the `providers.json`
 * resource of this module. Ids are matched case-insensitively after trimming.
 */
object ProviderPresets {

    const val RESOURCE = "providers.json"

    val catalog: ProviderCatalog by lazy {
        val stream = ProviderPresets::class.java.classLoader.getResourceAsStream(RESOURCE)
            ?: throw IllegalStateException("$RESOURCE is missing from the mail core resources")
        parse(stream.use { it.readBytes().toString(Charsets.UTF_8) })
    }

    /** Reported to the host as `mailProvidersVersion`; bumped whenever an entry changes. */
    val version: Int get() = catalog.version

    val all: List<ProviderPreset> get() = catalog.providers

    val ids: List<String> get() = all.map { it.id }

    fun resolve(id: String?): ProviderPreset? {
        val key = id?.trim()?.lowercase() ?: return null
        return all.firstOrNull { it.id == key }
    }

    /** Resolves [id] or fails with [MailErrorCode.PROVIDER_UNKNOWN] listing the known ids. */
    fun require(id: String): ProviderPreset = resolve(id)
        ?: throw MailException(MailErrorCode.PROVIDER_UNKNOWN, "unknown provider '${id.trim()}'", "known providers: ${ids.joinToString(", ")}")

    /** The catalog as the JSON document `listProviders` returns; presets carry no account data. */
    fun toJson(): String = MailJson.format.encodeToString(catalog)

    fun parse(json: String): ProviderCatalog = MailJson.format.decodeFromString(ProviderCatalog.serializer(), json)
}
