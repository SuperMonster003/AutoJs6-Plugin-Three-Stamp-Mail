package io.github.supermonster003.autojs6.plugin.angus.mail.ui

import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import io.github.supermonster003.autojs6.plugin.angus.mail.AppColorPolicy
import io.github.supermonster003.autojs6.plugin.angus.mail.ConfiguredActivity
import io.github.supermonster003.autojs6.plugin.angus.mail.R

/** Centered placeholder for empty or failed content areas. */
internal fun ConfiguredActivity.emptyStateView(
    title: CharSequence,
    description: CharSequence? = null,
    @DrawableRes iconResource: Int? = null,
    action: View? = null,
): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    gravity = Gravity.CENTER_HORIZONTAL
    setPaddingRelative(
        uiDp(Ui.SPACE_XXL),
        uiDp(Ui.SPACE_XXL),
        uiDp(Ui.SPACE_XXL),
        uiDp(Ui.SPACE_XXL),
    )
    if (iconResource != null) {
        addView(
            FrameLayout(context).apply {
                background = roundedFill(appPalette.surfaceVariant, Ui.RADIUS_SHEET * 2)
                addView(
                    ImageView(context).apply {
                        setImageDrawable(tintedDrawable(iconResource, appPalette.secondaryText))
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    },
                    FrameLayout.LayoutParams(uiDp(28), uiDp(28), Gravity.CENTER),
                )
            },
            LinearLayout.LayoutParams(uiDp(64), uiDp(64)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
    }
    addView(
        TextView(context).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_TITLE)
            typeface = Ui.mediumTypeface
            gravity = Gravity.CENTER
            setTextColor(appPalette.primaryText)
            setPaddingRelative(0, uiDp(Ui.SPACE_LG), 0, uiDp(Ui.SPACE_SM))
        },
    )
    if (!description.isNullOrEmpty()) {
        addView(
            TextView(context).apply {
                text = description
                setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_BODY)
                setTextColor(appPalette.secondaryText)
                gravity = Gravity.CENTER
                setLineSpacing(0f, Ui.LINE_SPACING_BODY)
            },
        )
    }
    if (action != null) {
        addView(
            action,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = uiDp(Ui.SPACE_XL)
            },
        )
    }
}

/**
 * Card-shaped progress presenter for a long-running step: label row with an inline cancel action,
 * a Material linear indicator, and a metadata line.
 */
internal class ProgressPanel(private val activity: ConfiguredActivity) {
    private val palette get() = activity.appPalette

    private val titleView = TextView(activity).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_BODY)
        typeface = Ui.mediumTypeface
        setTextColor(palette.primaryText)
        maxLines = 2
    }
    private val metaView = TextView(activity).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_CAPTION)
        setTextColor(palette.secondaryText)
        setPaddingRelative(0, activity.uiDp(Ui.SPACE_XS), 0, 0)
        visibility = View.GONE
    }
    private val indicator = LinearProgressIndicator(activity).apply {
        max = PROGRESS_MAX
        setIndicatorColor(palette.accent)
        trackColor = AppColorPolicy.withAlpha(palette.accent, 0x33)
        trackCornerRadius = activity.uiDp(2)
        trackThickness = activity.uiDp(4)
    }
    private var cancelButton: View? = null

    val view: LinearLayout = activity.cardContainer().apply {
        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(titleView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        addView(header)
        addView(
            indicator,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = activity.uiDp(Ui.SPACE_MD) },
        )
        addView(metaView)
        visibility = View.GONE
    }

    fun withCancelAction(@StringRes contentDescriptionResource: Int, onCancel: () -> Unit): ProgressPanel {
        if (cancelButton == null) {
            val button = activity.iconButton(
                R.drawable.ic_close_24,
                contentDescriptionResource,
                onClick = onCancel,
            )
            (titleView.parent as LinearLayout).addView(button)
            cancelButton = button
        }
        return this
    }

    fun setCancelEnabled(enabled: Boolean) {
        cancelButton?.isEnabled = enabled
        cancelButton?.alpha = if (enabled) 1f else Ui.DISABLED_ALPHA
    }

    fun showIndeterminate(label: CharSequence, meta: CharSequence? = null) {
        titleView.text = label
        applyMeta(meta)
        view.visibility = View.VISIBLE
        if (!indicator.isIndeterminate) {
            indicator.visibility = View.INVISIBLE
            indicator.isIndeterminate = true
            indicator.visibility = View.VISIBLE
        }
    }

    fun hide() {
        view.visibility = View.GONE
    }

    val isShowing: Boolean get() = view.visibility == View.VISIBLE

    private fun applyMeta(meta: CharSequence?) {
        metaView.text = meta
        metaView.visibility = if (meta.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    companion object {
        const val PROGRESS_MAX = 10_000
    }
}

/** High-contrast snackbar consistent with the app palette in both modes. */
internal fun ConfiguredActivity.showSnackbar(
    anchor: View,
    message: CharSequence,
    duration: Int = Snackbar.LENGTH_SHORT,
) {
    Snackbar.make(anchor, message, duration).apply {
        setBackgroundTint(appPalette.primaryText)
        setTextColor(appPalette.windowBackground)
    }.show()
}
