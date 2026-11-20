package io.github.supermonster003.autojs6.plugin.three.stamp.mail

import android.content.Context
import android.content.Intent
import java.io.IOException
import java.util.Locale

/**
 * Maps the UI locale to the bundled changelog asset shipped for it (roadmap D29: the release
 * history is read from `assets/doc/CHANGELOG-<lang>.md`, English when no translation ships).
 */
internal object ReleaseHistoryAssetPolicy {
    val BUNDLED = listOf(
        "CHANGELOG-ar.md", "CHANGELOG-en.md", "CHANGELOG-es.md", "CHANGELOG-fr.md", "CHANGELOG-ja.md",
        "CHANGELOG-ko.md", "CHANGELOG-ru.md", "CHANGELOG-zh-Hans.md", "CHANGELOG-zh-Hant-HK.md", "CHANGELOG-zh-Hant-TW.md",
    )

    fun assetFor(locale: Locale): String = when (locale.language.lowercase(Locale.ROOT)) {
        "zh" -> when {
            locale.script.equals("Hans", ignoreCase = true) -> "CHANGELOG-zh-Hans.md"
            locale.country.equals("HK", ignoreCase = true) ||
                locale.country.equals("MO", ignoreCase = true) -> "CHANGELOG-zh-Hant-HK.md"
            locale.script.equals("Hant", ignoreCase = true) ||
                locale.country.equals("TW", ignoreCase = true) -> "CHANGELOG-zh-Hant-TW.md"
            else -> "CHANGELOG-zh-Hans.md"
        }
        "ar" -> "CHANGELOG-ar.md"
        "es" -> "CHANGELOG-es.md"
        "fr" -> "CHANGELOG-fr.md"
        "ja" -> "CHANGELOG-ja.md"
        "ko" -> "CHANGELOG-ko.md"
        "ru" -> "CHANGELOG-ru.md"
        else -> FALLBACK
    }

    /** The asset paths to try in order: the one for [locale], then English. */
    fun candidatesFor(locale: Locale): List<String> =
        listOf(assetFor(locale), FALLBACK).distinct().map { "doc/$it" }

    /**
     * Whether the asset is written right to left. Its paragraphs keep that direction even when they
     * open with a Latin word such as README, which would otherwise make the whole line left to right.
     */
    fun isRightToLeft(assetName: String): Boolean = assetName.substringAfterLast('/') == "CHANGELOG-ar.md"

    private const val FALLBACK = "CHANGELOG-en.md"
}

internal data class ReleaseHistoryItem(val tag: String?, val text: String)

internal data class ReleaseHistoryEntry(
    val version: String,
    val date: String?,
    val items: List<ReleaseHistoryItem>,
) {
    /** The items by category, each category once, in the order the categories first appear (untagged: tag null). */
    val groups: List<ReleaseHistoryGroup>
        get() = items.groupBy { it.tag }.map { (tag, grouped) -> ReleaseHistoryGroup(tag, grouped.map { it.text }) }

    /** How many items each category has, in the same order; untagged items are not counted. */
    val categoryCounts: List<Pair<String, Int>>
        get() = groups.mapNotNull { group -> group.tag?.let { it to group.items.size } }
}

internal data class ReleaseHistoryGroup(val tag: String?, val items: List<String>)

/**
 * Parses the narrow markdown dialect used by the bundled changelogs: `# vX.Y.Z` or `## vX.Y.Z` version
 * headings, `###### date` lines, and `` * `tag` text `` bullets. Inline code markers inside the text are
 * dropped. Anything else (the section title, the separator lines) is ignored.
 */
internal object ReleaseHistoryParser {
    private val BULLET_PATTERN = Regex("^\\*\\s+(?:`([^`]+)`\\s*)?(.*)$")
    private val INLINE_CODE_PATTERN = Regex("`([^`]+)`")
    private val VERSION_PATTERN = Regex("^#{1,2}\\s+(v\\S+)\\s*$")

    fun parse(markdown: String): List<ReleaseHistoryEntry> {
        val entries = mutableListOf<ReleaseHistoryEntry>()
        var version: String? = null
        var date: String? = null
        var items = mutableListOf<ReleaseHistoryItem>()

        fun flush() {
            val flushedVersion = version ?: return
            entries.add(ReleaseHistoryEntry(flushedVersion, date, items.toList()))
        }

        for (rawLine in markdown.lineSequence()) {
            val line = rawLine.trim()
            val versionMatch = VERSION_PATTERN.matchEntire(line)
            when {
                versionMatch != null -> {
                    flush()
                    version = versionMatch.groupValues[1]
                    date = null
                    items = mutableListOf()
                }
                line.startsWith("###### ") -> date = line.removePrefix("###### ").trim()
                line.startsWith("* ") && version != null -> {
                    val match = BULLET_PATTERN.matchEntire(line) ?: continue
                    val tag = match.groupValues[1].takeIf { it.isNotEmpty() }
                    val text = INLINE_CODE_PATTERN.replace(match.groupValues[2].trim(), "$1")
                    if (tag != null || text.isNotEmpty()) {
                        items.add(ReleaseHistoryItem(tag, text))
                    }
                }
            }
        }
        flush()
        return entries
    }
}

/** The releases shown and the asset they were read from (null when none could be read). */
internal data class LoadedReleaseHistory(val asset: String?, val entries: List<ReleaseHistoryEntry>) {
    val isRightToLeft: Boolean
        get() = asset != null && ReleaseHistoryAssetPolicy.isRightToLeft(asset)
}

/**
 * Returns the releases of the first candidate asset that holds at least one, so a missing, blank or unparsable
 * localized changelog falls back to the English one; empty when none of them does (the page shows its empty state).
 */
internal fun loadReleaseHistory(candidates: List<String>, loader: (String) -> String): LoadedReleaseHistory {
    for (path in candidates) {
        val markdown = try { loader(path) } catch (_: IOException) { continue }
        val entries = ReleaseHistoryParser.parse(markdown)
        if (entries.isNotEmpty()) return LoadedReleaseHistory(path, entries)
    }
    return LoadedReleaseHistory(null, emptyList())
}

/** Opens the release history page; the settings row and the about page both lead here. */
internal fun Context.openReleaseHistory() {
    startActivity(Intent(this, ReleaseHistoryActivity::class.java))
}
