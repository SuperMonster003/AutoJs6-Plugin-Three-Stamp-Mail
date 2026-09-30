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
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.SettingRow
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.buildScaffold
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.confirmDialog
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.hairline
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.inputDialog
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.sectionHeader
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.settingRow
import io.github.supermonster003.autojs6.plugin.angus.mail.trigger.TriggerStores
import io.github.supermonster003.autojs6.plugin.angus.mail.trigger.WatchKeeper
import io.github.supermonster003.autojs6.plugin.angus.mail.trigger.WatchesActivity
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.singleChoiceDialog
import java.util.Locale

/**
 * Application settings (roadmap P4.2): appearance (language, dark mode, theme color, each able to
 * follow the AutoJs6 host) and the information entries. Nothing here touches the account store.
 */
class AppSettingsActivity : ConfiguredActivity() {

    internal lateinit var launcherIconRow: SettingRow
    internal var launcherIconDialog: androidx.appcompat.app.AlertDialog? = null
        private set

    private lateinit var settingsStore: ApplicationSettingsStore
    private lateinit var settings: ApplicationSettings
    private lateinit var hostResult: AutoJs6HostSettingsResult
    private var batteryRow: SettingRow? = null
    private var watchesRow: SettingRow? = null

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
                iconResource = R.drawable.ic_settings_language,
                onClick = ::showLanguageDialog,
            ).view,
        )
        content.addView(
            settingRow(
                title = getString(R.string.settings_dark_mode),
                summary = darkModeSummary(),
                iconResource = R.drawable.ic_settings_night,
                onClick = ::showDarkModeDialog,
            ).view,
        )
        content.addView(
            settingRow(
                title = getString(R.string.settings_theme_color),
                summary = themeSummary(),
                iconResource = R.drawable.ic_settings_theme,
                onClick = ::showThemeColorDialog,
            ).view,
        )
        launcherIconRow = settingRow(
            title = getString(R.string.launcher_icon_title),
            summary = getString(launcherIconLabels[LauncherIcons.current(this).ordinal]),
            iconResource = R.drawable.ic_settings_launcher,
            onClick = ::showLauncherIconDialog,
        )
        content.addView(launcherIconRow.view)
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

    /** Sections between appearance and information: the background watches (roadmap P8) and the battery-optimization guide (roadmap P4.6, D27). */
    private fun buildExtraSections(content: LinearLayout) {
        content.addView(sectionHeader(R.string.settings_section_background))
        val watches = settingRow(
            title = getString(R.string.watches_title),
            summary = watchesSummary(),
            iconResource = R.drawable.ic_visibility_24,
            onClick = { startActivity(Intent(this, WatchesActivity::class.java)) },
        )
        watchesRow = watches
        content.addView(watches.view)
        val row = settingRow(
            title = getString(R.string.battery_title),
            summary = batterySummary(),
            iconResource = R.drawable.ic_battery_24,
            onClick = ::onBatteryRowClicked,
        )
        batteryRow = row
        content.addView(row.view)
        content.addView(hairline())
    }

    override fun onResume() {
        super.onResume()
        // The system dialog or list may have changed the state while this screen was paused.
        batteryRow?.summaryView?.text = batterySummary()
        watchesRow?.summaryView?.text = watchesSummary()
    }

    private fun watchesSummary(): String {
        val configured = runCatching { TriggerStores.of(this).list().size }.getOrDefault(0)
        return getString(R.string.watches_summary_counts, configured, WatchKeeper.of(this).connected)
    }

    private fun batterySummary(): String = getString(
        if (BatteryOptimization.isIgnored(this)) R.string.battery_summary_ignored else R.string.battery_summary_optimized,
    )

    /** Already excluded: the system list lets the user revert; otherwise explain first, then ask. */
    private fun onBatteryRowClicked() {
        if (BatteryOptimization.isIgnored(this)) {
            openBatteryScreen(BatteryOptimization.exclusionList())
            return
        }
        confirmDialog(
            getString(R.string.battery_dialog_title),
            getString(R.string.battery_dialog_message),
            R.string.battery_dialog_action,
        ) { openBatteryScreen(BatteryOptimization.exclusionRequest(this)) }
    }

    private fun openBatteryScreen(intent: Intent) {
        if (!BatteryOptimization.open(this, intent)) {
            Toast.makeText(this, R.string.battery_request_failed, Toast.LENGTH_SHORT).show()
        }
    }

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

    private fun showLauncherIconDialog() {
        val modes = LauncherIconMode.entries
        val labels: List<CharSequence> = modes.map { mode ->
            val label = getString(launcherIconLabels[mode.ordinal])
            val note = when (mode) {
                LauncherIconMode.AUTO -> R.string.launcher_icon_auto_note
                LauncherIconMode.TRANSPARENT -> R.string.launcher_icon_transparent_note
                else -> null
            }
            if (note == null) label else SpannableString("$label\n${getString(note)}").apply {
                setSpan(RelativeSizeSpan(14f / 16f), label.length + 1, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(ForegroundColorSpan(appPalette.secondaryText), label.length + 1, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        launcherIconDialog = singleChoiceDialog(
            title = getString(R.string.launcher_icon_title),
            labels = labels,
            checkedIndex = modes.indexOf(LauncherIcons.current(this)),
            confirmSelection = true,
        ) { index ->
            val succeeded = runCatching { LauncherIcons.select(this, modes[index]) }.isSuccess
            launcherIconRow.summaryView.text = getString(launcherIconLabels[LauncherIcons.current(this).ordinal])
            Toast.makeText(this, if (succeeded) R.string.launcher_icon_applied_note else R.string.launcher_icon_failed, Toast.LENGTH_LONG).show()
        }
        // The checked dark row must not scroll the first choice out of view.
        launcherIconDialog?.listView?.post { launcherIconDialog?.listView?.setSelection(0) }
    }

    internal companion object {
        val launcherIconLabels = listOf(R.string.launcher_icon_light, R.string.launcher_icon_dark, R.string.launcher_icon_auto, R.string.launcher_icon_transparent)
    }

    private fun showLanguageDialog() {
        hostResult = AutoJs6HostSettingsClient.query(this)
        val values = AppLanguage.entries
        val labels: List<CharSequence> = values.mapIndexed { index, language ->
            val label = getString(language.labelResource())
            if (index == 0) followAutoJs6ChoiceLabel(label, quotedFollowSystemLabel()) else label
        }
        singleChoiceDialog(
            title = getString(R.string.settings_language),
            labels = labels,
            checkedIndex = values.indexOf(settings.language),
            confirmSelection = true,
        ) { index -> saveSettings(settings.copy(language = values[index])) }
    }

    private fun showDarkModeDialog() {
        hostResult = AutoJs6HostSettingsClient.query(this)
        val values = AppDarkMode.entries
        val labels: List<CharSequence> = values.mapIndexed { index, mode ->
            val label = getString(mode.labelResource())
            if (index == 0) followAutoJs6ChoiceLabel(label, quotedFollowSystemLabel()) else label
        }
        singleChoiceDialog(
            title = getString(R.string.settings_dark_mode),
            labels = labels,
            checkedIndex = values.indexOf(settings.darkMode),
            confirmSelection = true,
        ) { index -> saveSettings(settings.copy(darkMode = values[index])) }
    }

    private fun showThemeColorDialog() {
        hostResult = AutoJs6HostSettingsClient.query(this)
        presentedDialog = ThemeColorChooser.show(
            this,
            if (settings.themeSelection == AppThemeSelection.FOLLOW_AUTOJS6) null else settings.customThemeColor,
            followedThemeColor(),
            ThemeColorChooser.Palette(appPalette.accent, appPalette.surface, appPalette.primaryText, appPalette.secondaryText, appPalette.outline),
            ThemeColorChooser.Labels(getString(R.string.settings_theme_color), getString(R.string.follow_autojs6),
                getString(R.string.theme_picker_presets), getString(R.string.theme_picker_custom), getString(R.string.theme_picker_input),
                getString(R.string.theme_picker_invalid), getString(R.string.theme_picker_preview)),
        ) { color ->
            saveSettings(if (color == null) settings.copy(themeSelection = AppThemeSelection.FOLLOW_AUTOJS6)
                else settings.copy(themeSelection = AppThemeSelection.CUSTOM, customThemeColor = color))
        }
    }

    // endregion

    // region Summaries and labels

    private fun saveSettings(updated: ApplicationSettings) {
        if (updated == settings) return
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
            setSpan(RelativeSizeSpan(14f / 16f), start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
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
