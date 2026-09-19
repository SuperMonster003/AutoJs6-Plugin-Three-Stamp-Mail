package io.github.supermonster003.autojs6.plugin.angus.mail.ui

import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.materialswitch.MaterialSwitch
import io.github.supermonster003.autojs6.plugin.angus.mail.ConfiguredActivity
import io.github.supermonster003.autojs6.plugin.angus.mail.R

/** Accent-colored section label above a group of setting rows. */
internal fun ConfiguredActivity.sectionHeader(@StringRes titleResource: Int): TextView =
    TextView(this).apply {
        text = getString(titleResource)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_SECTION)
        typeface = Ui.mediumTypeface
        setTextColor(appPalette.accent)
        setPaddingRelative(
            uiDp(Ui.SCREEN_MARGIN),
            uiDp(Ui.SECTION_GAP),
            uiDp(Ui.SCREEN_MARGIN),
            uiDp(Ui.SPACE_SM),
        )
    }

/** 1dp divider between row groups, inset from the leading edge by default. */
internal fun ConfiguredActivity.hairline(insetStartDp: Int = Ui.SCREEN_MARGIN): View =
    View(this).apply {
        setBackgroundColor(appPalette.divider)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            uiDp(1),
        ).apply {
            marginStart = uiDp(insetStartDp)
        }
    }

internal class SettingRow(
    val view: LinearLayout,
    val titleView: TextView,
    val summaryView: TextView,
    val switchView: MaterialSwitch? = null,
)

private fun ConfiguredActivity.rowShell(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    minimumHeight = uiDp(64)
    setPaddingRelative(
        uiDp(Ui.SCREEN_MARGIN),
        uiDp(Ui.SPACE_MD),
        uiDp(Ui.SCREEN_MARGIN),
        uiDp(Ui.SPACE_MD),
    )
    layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    )
}

private fun ConfiguredActivity.rowLeadingIcon(@DrawableRes iconResource: Int): ImageView =
    ImageView(this).apply {
        setImageDrawable(tintedDrawable(iconResource, appPalette.secondaryText))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        layoutParams = LinearLayout.LayoutParams(uiDp(Ui.ICON_SIZE), uiDp(Ui.ICON_SIZE)).apply {
            marginEnd = uiDp(Ui.SPACE_LG)
        }
    }

private fun ConfiguredActivity.rowTextColumn(
    title: CharSequence,
    summary: CharSequence?,
): Triple<LinearLayout, TextView, TextView> {
    val titleView = TextView(this).apply {
        text = title
        setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_ITEM)
        setTextColor(appPalette.primaryText)
    }
    val summaryView = TextView(this).apply {
        text = summary
        setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_SECONDARY)
        setTextColor(appPalette.secondaryText)
        setLineSpacing(0f, 1.1f)
        setPaddingRelative(0, uiDp(3), 0, 0)
        visibility = if (summary.isNullOrEmpty()) View.GONE else View.VISIBLE
    }
    val column = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(titleView)
        addView(summaryView)
    }
    return Triple(column, titleView, summaryView)
}

/** Standard setting row: optional leading icon, title/summary column, optional value + chevron. */
internal fun ConfiguredActivity.settingRow(
    title: CharSequence,
    summary: CharSequence? = null,
    @DrawableRes iconResource: Int? = null,
    value: CharSequence? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
): SettingRow {
    val row = rowShell()
    if (iconResource != null) row.addView(rowLeadingIcon(iconResource))
    val (column, titleView, summaryView) = rowTextColumn(title, summary)
    row.addView(
        column,
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
    )
    if (value != null) {
        row.addView(
            TextView(this).apply {
                text = value
                setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_SECONDARY)
                setTextColor(appPalette.secondaryText)
                setPaddingRelative(uiDp(Ui.SPACE_MD), 0, 0, 0)
            },
        )
    }
    if (showChevron && onClick != null) {
        row.addView(
            ImageView(this).apply {
                setImageDrawable(
                    tintedDrawable(R.drawable.ic_chevron_right_24, appPalette.secondaryText),
                )
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                alpha = 0.6f
                layoutParams = LinearLayout.LayoutParams(
                    uiDp(Ui.ICON_SIZE),
                    uiDp(Ui.ICON_SIZE),
                ).apply {
                    marginStart = uiDp(Ui.SPACE_SM)
                }
            },
        )
    }
    if (onClick != null) {
        row.isClickable = true
        row.isFocusable = true
        row.applyThemedSelectableBackground()
        row.setOnClickListener { onClick() }
    }
    return SettingRow(row, titleView, summaryView)
}

/** Setting row whose trailing switch mirrors row taps, keeping one large touch target. */
internal fun ConfiguredActivity.switchRow(
    title: CharSequence,
    summary: CharSequence? = null,
    @DrawableRes iconResource: Int? = null,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
): SettingRow {
    val row = rowShell()
    if (iconResource != null) row.addView(rowLeadingIcon(iconResource))
    val (column, titleView, summaryView) = rowTextColumn(title, summary)
    row.addView(
        column,
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
    )
    val switchView = MaterialSwitch(this).apply {
        isChecked = checked
        thumbTintList = switchThumbTintList()
        trackTintList = switchTrackTintList()
        // The row is the single interactive surface; the switch only displays state.
        isClickable = false
        isFocusable = false
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            marginStart = uiDp(Ui.SPACE_MD)
        }
    }
    row.addView(switchView)
    row.isClickable = true
    row.isFocusable = true
    row.applyThemedSelectableBackground()
    row.setOnClickListener {
        switchView.isChecked = !switchView.isChecked
        onToggle(switchView.isChecked)
    }
    return SettingRow(row, titleView, summaryView, switchView)
}
