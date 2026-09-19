package io.github.supermonster003.autojs6.plugin.angus.mail.ui

import android.content.res.ColorStateList
import android.util.TypedValue
import android.widget.ImageButton
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.button.MaterialButton
import io.github.supermonster003.autojs6.plugin.angus.mail.AppColorPolicy
import io.github.supermonster003.autojs6.plugin.angus.mail.ConfiguredActivity

private fun ConfiguredActivity.baseButton(): MaterialButton = MaterialButton(this).apply {
    applyBaseButtonMetrics()
}

private fun MaterialButton.applyBaseButtonMetrics() {
    isAllCaps = false
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    typeface = Ui.mediumTypeface
    minimumHeight = context.uiDp(Ui.TOUCH_TARGET)
    minHeight = context.uiDp(Ui.TOUCH_TARGET)
    minimumWidth = 0
    minWidth = 0
    insetTop = 0
    insetBottom = 0
    setPaddingRelative(context.uiDp(Ui.SPACE_XL), 0, context.uiDp(Ui.SPACE_XL), 0)
}

private fun disabledAndDefaultStates(): Array<IntArray> = arrayOf(
    intArrayOf(-android.R.attr.state_enabled),
    intArrayOf(),
)

/** High-emphasis filled button in the theme's primary color. */
internal fun ConfiguredActivity.filledButton(
    @StringRes textResource: Int,
    onClick: () -> Unit,
): MaterialButton = baseButton().apply {
    text = getString(textResource)
    backgroundTintList = ColorStateList(
        disabledAndDefaultStates(),
        intArrayOf(appPalette.surfaceVariant, appPalette.primary),
    )
    setTextColor(
        ColorStateList(
            disabledAndDefaultStates(),
            intArrayOf(
                AppColorPolicy.withAlpha(appPalette.secondaryText, 0x99),
                appPalette.onPrimary,
            ),
        ),
    )
    rippleColor = ColorStateList.valueOf(AppColorPolicy.withAlpha(appPalette.onPrimary, 0x33))
    setOnClickListener { onClick() }
}

/** Medium-emphasis tonal button on a soft accent fill. */
internal fun ConfiguredActivity.tonalButton(
    @StringRes textResource: Int,
    onClick: () -> Unit,
): MaterialButton = baseButton().apply {
    text = getString(textResource)
    backgroundTintList = ColorStateList(
        disabledAndDefaultStates(),
        intArrayOf(
            AppColorPolicy.withAlpha(appPalette.secondaryText, 0x14),
            accentTone(appPalette.accent),
        ),
    )
    setTextColor(
        ColorStateList(
            disabledAndDefaultStates(),
            intArrayOf(
                AppColorPolicy.withAlpha(appPalette.secondaryText, 0x99),
                appPalette.accent,
            ),
        ),
    )
    rippleColor = ColorStateList.valueOf(accentRipple(appPalette.accent))
    setOnClickListener { onClick() }
}

/** Low-emphasis borderless text button in the accent color. */
internal fun ConfiguredActivity.textButton(
    @StringRes textResource: Int,
    onClick: () -> Unit,
): MaterialButton = MaterialButton(
    this,
    null,
    androidx.appcompat.R.attr.borderlessButtonStyle,
).apply {
    applyBaseButtonMetrics()
    setPaddingRelative(context.uiDp(Ui.SPACE_MD), 0, context.uiDp(Ui.SPACE_MD), 0)
    text = getString(textResource)
    setTextColor(
        ColorStateList(
            disabledAndDefaultStates(),
            intArrayOf(
                AppColorPolicy.withAlpha(appPalette.secondaryText, 0x99),
                appPalette.accent,
            ),
        ),
    )
    rippleColor = ColorStateList.valueOf(accentRipple(appPalette.accent))
    setOnClickListener { onClick() }
}

/** 48dp borderless icon button; the content description is mandatory for accessibility. */
internal fun ConfiguredActivity.iconButton(
    @DrawableRes iconResource: Int,
    @StringRes contentDescriptionResource: Int,
    tint: Int = appPalette.secondaryText,
    onClick: () -> Unit,
): ImageButton = ImageButton(this).apply {
    minimumWidth = uiDp(Ui.ICON_BUTTON_SIZE)
    minimumHeight = uiDp(Ui.ICON_BUTTON_SIZE)
    background = null
    applyThemedSelectableBackground(borderless = true)
    scaleType = ImageView.ScaleType.CENTER
    setImageDrawable(tintedDrawable(iconResource, tint))
    contentDescription = getString(contentDescriptionResource)
    setOnClickListener { onClick() }
}
