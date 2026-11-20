package io.github.supermonster003.autojs6.plugin.three.stamp.mail

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.ui.accentTone

/**
 * Renders the bundled changelog for the current locale (English when that one is missing) as one group per released
 * version, under the title "Release history" with the plugin name as the subtitle
 * (AUTOJS6_PLUGIN_RELEASE_HISTORY_AGENTS.md). The colors are those of the other pages ([AppThemePalette]).
 */
class ReleaseHistoryActivity : ConfiguredActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge to edge like the other pages; the scaffold pads the system bars and the display cutout.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val candidates = ReleaseHistoryAssetPolicy.candidatesFor(resources.configuration.locales[0])
        val history = loadReleaseHistory(candidates) { path ->
            assets.open(path).use { stream -> stream.readBytes().toString(Charsets.UTF_8) }
        }
        val palette = appPalette
        val colors = ReleaseHistoryColors(
            dark = palette.isDark,
            primary = palette.accent,
            // The category chips use the same tinted fill as the chips of the old page, made opaque on the page color.
            primaryContainer = ColorUtils.compositeColors(accentTone(palette.accent), palette.windowBackground),
            background = palette.windowBackground,
            surface = palette.windowBackground,
            onSurface = palette.primaryText,
            onSurfaceVariant = palette.secondaryText,
            outlineVariant = palette.divider,
        )
        setContent {
            ReleaseHistoryTheme(colors) {
                ReleaseHistoryScreen(entries = history.entries, onBack = ::finish, rightToLeft = history.isRightToLeft)
            }
        }
    }
}
