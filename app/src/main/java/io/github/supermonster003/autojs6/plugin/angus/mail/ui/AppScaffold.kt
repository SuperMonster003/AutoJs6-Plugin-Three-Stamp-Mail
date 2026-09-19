package io.github.supermonster003.autojs6.plugin.angus.mail.ui

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import io.github.supermonster003.autojs6.plugin.angus.mail.ConfiguredActivity

internal class Scaffold(
    val root: LinearLayout,
    val toolbar: Toolbar,
    val scroll: ScrollView?,
    val content: LinearLayout,
)

internal class ContentPadding(
    val horizontalDp: Int,
    val topDp: Int,
    val bottomDp: Int,
) {
    companion object {
        /** For screens made of full-bleed rows and hairlines. */
        val NONE = ContentPadding(0, Ui.SPACE_SM, Ui.SECTION_GAP)

        /** For screens made of cards or free-form text. */
        val SCREEN = ContentPadding(Ui.SCREEN_MARGIN, Ui.SPACE_LG, Ui.SECTION_GAP)
    }
}

/**
 * Shared page frame for the settings-family screens: edge-to-edge window, app-colored status-bar
 * backdrop, toolbar with back navigation, hairline, then a (usually scrollable) content column.
 */
internal fun ConfiguredActivity.buildScaffold(
    @StringRes titleResource: Int,
    showBack: Boolean = true,
    subtitle: CharSequence? = null,
    scrollable: Boolean = true,
    contentPadding: ContentPadding = ContentPadding.NONE,
): Scaffold {
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(appPalette.windowBackground)
    }
    val statusBarBackground = createStatusBarBackground()
    root.addView(
        statusBarBackground,
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0),
    )
    val toolbar = createAppToolbar(titleResource, showBack, subtitle)
    root.addView(
        toolbar,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ),
    )
    root.addView(
        View(this).apply { setBackgroundColor(appPalette.divider) },
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, uiDp(1)),
    )
    val content = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPaddingRelative(
            uiDp(contentPadding.horizontalDp),
            uiDp(contentPadding.topDp),
            uiDp(contentPadding.horizontalDp),
            uiDp(contentPadding.bottomDp),
        )
    }
    var scroll: ScrollView? = null
    if (scrollable) {
        scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(
                content,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    } else {
        root.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }
    applySystemBarInsets(root, statusBarBackground)
    return Scaffold(root, toolbar, scroll, content)
}
