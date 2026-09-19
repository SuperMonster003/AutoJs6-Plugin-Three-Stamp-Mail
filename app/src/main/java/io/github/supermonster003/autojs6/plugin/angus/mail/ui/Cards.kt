package io.github.supermonster003.autojs6.plugin.angus.mail.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import io.github.supermonster003.autojs6.plugin.angus.mail.ConfiguredActivity

/** Rounded outlined card; `selected` swaps the fill/stroke to the accent tone. */
internal fun ConfiguredActivity.cardContainer(
    interactive: Boolean = false,
    selected: Boolean = false,
): LinearLayout {
    val fill = if (selected) accentTone(appPalette.accent) else appPalette.surface
    val stroke = if (selected) appPalette.accent else appPalette.outline
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = if (interactive) {
            roundedRippleFill(fill, accentRipple(appPalette.accent), Ui.RADIUS_CARD, stroke)
        } else {
            roundedFill(fill, Ui.RADIUS_CARD, stroke)
        }
        if (interactive) {
            isClickable = true
            isFocusable = true
        }
        setPaddingRelative(
            uiDp(Ui.SPACE_LG),
            uiDp(Ui.SPACE_LG - 2),
            uiDp(Ui.SPACE_LG),
            uiDp(Ui.SPACE_LG - 2),
        )
    }
}

/** Layout params for stacking cards in a vertical list. */
internal fun Context.cardListParams(
    topMarginDp: Int = 0,
    bottomMarginDp: Int = Ui.SPACE_MD,
): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT,
    ViewGroup.LayoutParams.WRAP_CONTENT,
).apply {
    topMargin = uiDp(topMarginDp)
    bottomMargin = uiDp(bottomMarginDp)
}
