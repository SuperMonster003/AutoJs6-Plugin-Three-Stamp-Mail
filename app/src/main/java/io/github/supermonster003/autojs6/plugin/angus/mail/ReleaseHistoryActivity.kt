package io.github.supermonster003.autojs6.plugin.angus.mail

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.ContentPadding
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.Ui
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.accentTone
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.buildScaffold
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.cardContainer
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.cardListParams
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.emptyStateView
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.roundedFill
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
        else -> "CHANGELOG-en.md"
    }
}

internal data class ReleaseHistoryItem(val tag: String?, val text: String)

internal data class ReleaseHistoryEntry(
    val version: String,
    val date: String?,
    val items: List<ReleaseHistoryItem>,
)

/**
 * Parses the narrow markdown dialect of the bundled changelogs: `# vX.Y.Z` version headings,
 * `###### date` lines, and `` * `tag` text `` bullets. Anything else is ignored.
 */
internal object ReleaseHistoryParser {
    private val BULLET_PATTERN = Regex("^\\*\\s+(?:`([^`]+)`\\s*)?(.*)$")

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
            when {
                line.startsWith("# ") -> {
                    flush()
                    version = line.removePrefix("# ").trim()
                    date = null
                    items = mutableListOf()
                }
                line.startsWith("###### ") -> date = line.removePrefix("###### ").trim()
                line.startsWith("* ") && version != null -> {
                    val match = BULLET_PATTERN.matchEntire(line) ?: continue
                    val tag = match.groupValues[1].takeIf { it.isNotEmpty() }
                    val text = match.groupValues[2].trim()
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

/** Renders the bundled changelog of the current locale as one card per released version. */
class ReleaseHistoryActivity : ConfiguredActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scaffold = buildScaffold(
            R.string.release_history_title,
            contentPadding = ContentPadding.SCREEN,
        )
        val entries = loadReleaseHistory()?.let(ReleaseHistoryParser::parse).orEmpty()
        if (entries.isEmpty()) {
            scaffold.content.addView(
                emptyStateView(
                    getString(R.string.release_history_load_failed),
                    iconResource = R.drawable.ic_warning_24,
                ),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = uiDp(Ui.SECTION_GAP) },
            )
        } else {
            entries.forEach { entry ->
                scaffold.content.addView(versionCard(entry), cardListParams())
            }
        }
        setContentView(scaffold.root)
    }

    private fun loadReleaseHistory(): String? = runCatching {
        val assetName = ReleaseHistoryAssetPolicy.assetFor(resources.configuration.locales[0])
        assets.open("doc/$assetName").use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        }
    }.getOrNull()

    private fun versionCard(entry: ReleaseHistoryEntry): LinearLayout {
        val card = cardContainer()
        card.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(
                    TextView(context).apply {
                        text = entry.version
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_TITLE)
                        typeface = Ui.mediumTypeface
                        setTextColor(appPalette.primaryText)
                    },
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
                )
                if (entry.date != null) {
                    addView(
                        TextView(context).apply {
                            text = entry.date
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_CAPTION)
                            setTextColor(appPalette.secondaryText)
                        },
                    )
                }
            },
        )
        entry.items.forEach { item ->
            card.addView(
                bulletRow(item),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = uiDp(Ui.SPACE_SM + 2) },
            )
        }
        return card
    }

    private fun bulletRow(item: ReleaseHistoryItem): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        if (item.tag != null) {
            addView(
                TextView(context).apply {
                    text = item.tag
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_CAPTION)
                    typeface = Ui.mediumTypeface
                    setTextColor(appPalette.accent)
                    background = roundedFill(accentTone(appPalette.accent), Ui.RADIUS_CONTROL)
                    setPaddingRelative(uiDp(Ui.SPACE_SM), uiDp(2), uiDp(Ui.SPACE_SM), uiDp(2))
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    marginEnd = uiDp(Ui.SPACE_SM)
                    topMargin = uiDp(1)
                },
            )
        }
        addView(
            TextView(context).apply {
                text = item.text
                setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_BODY)
                setTextColor(appPalette.primaryText)
                setLineSpacing(0f, Ui.LINE_SPACING_BODY)
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
    }
}
