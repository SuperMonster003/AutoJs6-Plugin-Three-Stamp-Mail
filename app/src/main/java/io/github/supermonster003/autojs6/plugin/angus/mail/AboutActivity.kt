package io.github.supermonster003.autojs6.plugin.angus.mail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.Ui
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.buildScaffold
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.hairline
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.roundedFill
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.settingRow
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.showSnackbar

/** About screen: identity header, project links, and version / developer / license details. */
class AboutActivity : ConfiguredActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scaffold = buildScaffold(R.string.about_title)
        buildAboutContent(scaffold.content)
        setContentView(scaffold.root)
    }

    private fun buildAboutContent(content: LinearLayout) {
        content.addView(headerView())
        content.addView(hairline())
        buildLinkRows(content)
        content.addView(
            settingRow(
                title = getString(R.string.about_source_code),
                summary = PROJECT_URI,
                iconResource = R.drawable.ic_code_24,
                onClick = { openUri(PROJECT_URI) },
            ).view,
        )
        content.addView(
            settingRow(
                title = getString(R.string.about_developer_home),
                summary = DEVELOPER_URI,
                iconResource = R.drawable.ic_person_24,
                onClick = { openUri(DEVELOPER_URI) },
            ).view,
        )
        content.addView(
            settingRow(
                title = getString(R.string.about_open_source_license),
                summary = LICENSE_URI,
                iconResource = R.drawable.ic_description_24,
                onClick = { openUri(LICENSE_URI) },
            ).view,
        )
        content.addView(hairline())
        val info = angusMailPluginRuntimeInfo()
        content.addView(
            infoBlock(
                R.string.about_section_version,
                getString(R.string.about_version_details, info.versionName, info.versionCode, info.versionDate),
            ),
        )
        content.addView(infoBlock(R.string.about_section_developer, info.author))
        content.addView(infoBlock(R.string.about_section_license, getString(R.string.about_license_summary)))
    }

    /** Rows above the project links; later roadmap items add theirs here. */
    private fun buildLinkRows(content: LinearLayout) = Unit

    private fun headerView(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPaddingRelative(
            uiDp(Ui.SPACE_XXL),
            uiDp(Ui.SPACE_XXXL),
            uiDp(Ui.SPACE_XXL),
            uiDp(Ui.SPACE_LG),
        )
        addView(
            ImageView(context).apply {
                setImageResource(R.mipmap.ic_launcher)
                contentDescription = getString(R.string.app_name)
                background = roundedFill(appPalette.surface, Ui.RADIUS_SHEET, appPalette.outline)
                clipToOutline = true
            },
            LinearLayout.LayoutParams(uiDp(88), uiDp(88)),
        )
        addView(
            TextView(context).apply {
                text = getString(R.string.app_name)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_DISPLAY)
                typeface = Ui.mediumTypeface
                gravity = Gravity.CENTER
                setTextColor(appPalette.primaryText)
                setPaddingRelative(0, uiDp(Ui.SPACE_LG), 0, uiDp(Ui.SPACE_XS))
            },
        )
        addView(
            TextView(context).apply {
                text = getString(R.string.about_summary)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_BODY)
                setTextColor(appPalette.secondaryText)
                gravity = Gravity.CENTER
                setLineSpacing(0f, Ui.LINE_SPACING_BODY)
            },
        )
    }

    private fun infoBlock(@StringRes titleResource: Int, value: CharSequence): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPaddingRelative(
                uiDp(Ui.SCREEN_MARGIN),
                uiDp(Ui.SPACE_XL),
                uiDp(Ui.SCREEN_MARGIN),
                0,
            )
            addView(
                TextView(context).apply {
                    text = getString(titleResource)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_SECTION)
                    typeface = Ui.mediumTypeface
                    setTextColor(appPalette.accent)
                },
            )
            addView(
                TextView(context).apply {
                    text = value
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    setTextColor(appPalette.primaryText)
                    setTextIsSelectable(true)
                    setPaddingRelative(0, uiDp(Ui.SPACE_XS), 0, 0)
                },
            )
        }

    private fun openUri(uri: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        }.onFailure {
            showSnackbar(
                findViewById(android.R.id.content),
                getString(R.string.open_link_failed),
            )
        }
    }

    private companion object {
        const val PROJECT_URI = "https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail"
        const val DEVELOPER_URI = "https://github.com/SuperMonster003"
        const val LICENSE_URI = "$PROJECT_URI/blob/master/LICENSE"
    }
}
