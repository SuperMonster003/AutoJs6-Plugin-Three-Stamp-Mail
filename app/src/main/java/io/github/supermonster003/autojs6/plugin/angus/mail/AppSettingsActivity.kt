package io.github.supermonster003.autojs6.plugin.angus.mail

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.StringRes
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.buildScaffold
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.hairline
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.inputDialog
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.sectionHeader
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.settingRow
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.singleChoiceDialog
import java.util.Locale

/**
 * Application settings (roadmap P4.2): appearance (language, dark mode, theme color, each able to
 * follow the AutoJs6 host) and the information entries. Nothing here touches the account store.
 */
class AppSettingsActivity : ConfiguredActivity() {

    private lateinit var settingsStore: ApplicationSettingsStore
    private lateinit var settings: ApplicationSettings
    private lateinit var hostResult: AutoJs6HostSettingsResult

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsStore = ApplicationSettingsStore(applicationContext)
        settings = settingsStore.load()
        hostResult = AutoJs6HostSettingsClient.query(this)
        val scaffold = buildScaffold(R.string.settings_title)
        buildSettingsContent(scaffold.content)
        applyThemeToControls(scaffold.content)
        setContentView(scaffold.root)
    }

    private fun buildSettingsContent(content: LinearLayout) {
        content.addView(sectionHeader(R.string.settings_section_appearance))
        content.addView(
            settingRow(
                title = getString(R.string.settings_language),
                summary = languageSummary(),
                iconResource = R.drawable.ic_language_24,
                onClick = ::showLanguageDialog,
            ).view,
        )
        content.addView(
            settingRow(
                title = getString(R.string.settings_dark_mode),
                summary = darkModeSummary(),
                iconResource = R.drawable.ic_dark_mode_24,
                onClick = ::showDarkModeDialog,
            ).view,
        )
        content.addView(
            settingRow(
                title = getString(R.string.settings_theme_color),
                summary = themeSummary(),
                iconResource = R.drawable.ic_palette_24,
                onClick = ::showThemeColorDialog,
            ).view,
        )
        content.addView(hairline())
        buildExtraSections(content)
        content.addView(sectionHeader(R.string.settings_section_information))
        buildInformationRows(content)
        content.addView(
            settingRow(
                title = getString(R.string.about_title),
                summary = getString(R.string.about_app_summary),
                iconResource = R.drawable.ic_info_24,
                onClick = { startActivity(Intent(this, AboutActivity::class.java)) },
            ).view,
        )
    }

    /** Sections between appearance and information; later roadmap items add theirs here. */
    private fun buildExtraSections(content: LinearLayout) = Unit

    /** Information rows placed above the about entry: the release history (roadmap P4.5, D29). */
    private fun buildInformationRows(content: LinearLayout) {
        content.addView(
            settingRow(
                title = getString(R.string.release_history_title),
                summary = getString(R.string.release_history_summary),
                iconResource = R.drawable.ic_article_24,
                onClick = { startActivity(Intent(this, ReleaseHistoryActivity::class.java)) },
            ).view,
        )
    }

    // region Appearance dialogs

    private fun showLanguageDialog() {
        val values = AppLanguage.entries
        val labels: List<CharSequence> = values.mapIndexed { index, language ->
            val label = getString(language.labelResource())
            if (index == 0) followAutoJs6ChoiceLabel(label, quotedFollowSystemLabel()) else label
        }
        singleChoiceDialog(
            title = getString(R.string.settings_language),
            labels = labels,
            checkedIndex = values.indexOf(settings.language),
        ) { index -> saveSettings(settings.copy(language = values[index])) }
    }

    private fun showDarkModeDialog() {
        val values = AppDarkMode.entries
        val labels: List<CharSequence> = values.mapIndexed { index, mode ->
            val label = getString(mode.labelResource())
            if (index == 0) followAutoJs6ChoiceLabel(label, quotedFollowSystemLabel()) else label
        }
        singleChoiceDialog(
            title = getString(R.string.settings_dark_mode),
            labels = labels,
            checkedIndex = values.indexOf(settings.darkMode),
        ) { index -> saveSettings(settings.copy(darkMode = values[index])) }
    }

    private fun showThemeColorDialog() {
        val choices = themeChoices()
        val checkedIndex = selectedThemeChoiceIndex(choices)
        val labels: List<CharSequence> = choices.mapIndexed { index, choice ->
            val label = getString(choice.labelResource)
            when {
                choice.followAutoJs6 -> followAutoJs6ChoiceLabel(
                    label,
                    AppSettingsPolicy.colorHex(followedThemeColor()),
                )
                choice.color != null -> "$label (${AppSettingsPolicy.colorHex(choice.color)})"
                index == checkedIndex && settings.themeSelection == AppThemeSelection.CUSTOM ->
                    "$label (${AppSettingsPolicy.colorHex(settings.customThemeColor)})"
                else -> label
            }
        }
        singleChoiceDialog(
            title = getString(R.string.settings_theme_color),
            labels = labels,
            checkedIndex = checkedIndex,
        ) { index ->
            val choice = choices[index]
            when {
                choice.followAutoJs6 -> saveSettings(
                    settings.copy(themeSelection = AppThemeSelection.FOLLOW_AUTOJS6),
                )
                choice.color != null -> saveSettings(
                    settings.copy(
                        themeSelection = AppThemeSelection.CUSTOM,
                        customThemeColor = choice.color,
                    ),
                )
                else -> showCustomThemeColorDialog()
            }
        }
    }

    private fun showCustomThemeColorDialog() {
        inputDialog(
            title = getString(R.string.theme_custom_title),
            initialValue = AppSettingsPolicy.colorHex(settings.customThemeColor),
            hint = "#RRGGBB",
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS,
            maxLength = 7,
            positiveResource = R.string.action_save,
            validate = { value ->
                if (AppSettingsPolicy.parseOpaqueColor(value) == null) {
                    getString(R.string.theme_custom_error)
                } else {
                    null
                }
            },
        ) { value ->
            val color = AppSettingsPolicy.parseOpaqueColor(value) ?: return@inputDialog
            saveSettings(
                settings.copy(
                    themeSelection = AppThemeSelection.CUSTOM,
                    customThemeColor = color,
                ),
            )
        }
    }

    private fun themeChoices(): List<ThemeChoice> = listOf(
        ThemeChoice(R.string.follow_autojs6, followAutoJs6 = true),
        ThemeChoice(R.string.theme_color_brand, AppSettingsPolicy.BRAND_THEME_COLOR),
        ThemeChoice(R.string.theme_color_teal, AppSettingsPolicy.TEAL_THEME_COLOR),
        ThemeChoice(R.string.theme_color_blue, AppSettingsPolicy.BLUE_THEME_COLOR),
        ThemeChoice(R.string.theme_color_green, AppSettingsPolicy.GREEN_THEME_COLOR),
        ThemeChoice(R.string.theme_color_purple, AppSettingsPolicy.PURPLE_THEME_COLOR),
        ThemeChoice(R.string.theme_color_custom),
    )

    private fun selectedThemeChoiceIndex(choices: List<ThemeChoice>): Int =
        when (settings.themeSelection) {
            AppThemeSelection.FOLLOW_AUTOJS6 -> 0
            AppThemeSelection.CUSTOM -> {
                val color = AppSettingsPolicy.normalizeOpaqueColor(settings.customThemeColor)
                choices.indexOfFirst { it.color == color }.takeIf { it >= 1 }
                    ?: choices.lastIndex
            }
        }

    // endregion

    // region Summaries and labels

    private fun saveSettings(updated: ApplicationSettings) {
        settingsStore.save(updated)
        settings = updated
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
        recreate()
    }

    private fun languageSummary(): String = when (settings.language) {
        AppLanguage.FOLLOW_AUTOJS6 -> followAutoJs6Summary(resolvedHostLanguageLabel())
        else -> getString(settings.language.labelResource())
    }

    private fun darkModeSummary(): String = when (settings.darkMode) {
        AppDarkMode.FOLLOW_AUTOJS6 -> followAutoJs6Summary(
            getString(
                hostResult.snapshot?.darkModePolicy?.labelResource()
                    ?: R.string.follow_system,
            ),
        )
        else -> getString(settings.darkMode.labelResource())
    }

    private fun themeSummary(): String = when (settings.themeSelection) {
        AppThemeSelection.FOLLOW_AUTOJS6 -> followAutoJs6Summary(
            AppSettingsPolicy.colorHex(followedThemeColor()),
        )
        AppThemeSelection.CUSTOM -> AppSettingsPolicy.colorHex(settings.customThemeColor)
    }

    private fun followAutoJs6Summary(value: String): String =
        getString(R.string.follow_autojs6_summary, value)

    private fun followedThemeColor(): Int =
        AppSettingsPolicy.resolveAutoJs6ThemeColor(hostResult.snapshot?.themeColorPrimary)

    private fun resolvedHostLanguageLabel(): String {
        val tag = hostResult.snapshot?.resolvedLanguageTag?.trim().orEmpty()
        if (tag.isEmpty()) return getString(R.string.follow_system)
        val language = AppSettingsPolicy.languageForResolvedTag(tag)
        if (language != null) return getString(language.labelResource())
        return Locale.forLanguageTag(tag).displayName.ifBlank { tag }
    }

    private fun quotedFollowSystemLabel(): String =
        "\"${getString(R.string.follow_system)}\""

    /**
     * Label for the "follow AutoJs6" choice. When the host is not usable, a smaller secondary
     * line explains which fallback value would apply, instead of hiding or disabling the choice.
     */
    private fun followAutoJs6ChoiceLabel(title: String, fallbackValue: String): CharSequence {
        if (hostResult.selectable) return title
        val subtitle = getString(R.string.follow_autojs6_fallback_subtitle, fallbackValue)
        return SpannableString("$title\n$subtitle").apply {
            val start = title.length + 1
            setSpan(
                ForegroundColorSpan(appPalette.secondaryText),
                start,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            setSpan(RelativeSizeSpan(0.82f), start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(
                TypefaceSpan("sans-serif-light"),
                start,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }

    @StringRes
    private fun AppDarkMode.labelResource(): Int = when (this) {
        AppDarkMode.FOLLOW_AUTOJS6 -> R.string.follow_autojs6
        AppDarkMode.FOLLOW_SYSTEM -> R.string.follow_system
        AppDarkMode.LIGHT -> R.string.dark_mode_light
        AppDarkMode.DARK -> R.string.dark_mode_dark
    }

    @StringRes
    private fun AutoJs6DarkModePolicy.labelResource(): Int = when (this) {
        AutoJs6DarkModePolicy.FOLLOW_SYSTEM -> R.string.follow_system
        AutoJs6DarkModePolicy.LIGHT -> R.string.dark_mode_light
        AutoJs6DarkModePolicy.DARK -> R.string.dark_mode_dark
    }

    @StringRes
    private fun AppLanguage.labelResource(): Int = when (this) {
        AppLanguage.FOLLOW_AUTOJS6 -> R.string.follow_autojs6
        AppLanguage.FOLLOW_SYSTEM -> R.string.follow_system
        AppLanguage.CHINESE_SIMPLIFIED -> R.string.language_zh_hans
        AppLanguage.CHINESE_TRADITIONAL_HONG_KONG -> R.string.language_zh_hant_hk
        AppLanguage.CHINESE_TRADITIONAL_TAIWAN -> R.string.language_zh_hant_tw
        AppLanguage.ENGLISH -> R.string.language_en
        AppLanguage.FRENCH -> R.string.language_fr
        AppLanguage.SPANISH -> R.string.language_es
        AppLanguage.JAPANESE -> R.string.language_ja
        AppLanguage.KOREAN -> R.string.language_ko
        AppLanguage.RUSSIAN -> R.string.language_ru
        AppLanguage.ARABIC -> R.string.language_ar
    }

    // endregion
}

private data class ThemeChoice(
    @param:StringRes val labelResource: Int,
    val color: Int? = null,
    val followAutoJs6: Boolean = false,
)
