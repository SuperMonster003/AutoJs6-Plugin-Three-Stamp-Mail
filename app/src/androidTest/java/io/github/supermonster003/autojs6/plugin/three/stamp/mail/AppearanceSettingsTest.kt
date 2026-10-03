package io.github.supermonster003.autojs6.plugin.three.stamp.mail

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.junit.Assert.*
import org.junit.Test
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.ui.materialDialog

/** AVD only. Preserve exact appearance preferences and component states in finally. */
class AppearanceSettingsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val store get() = ApplicationSettingsStore(context)

    @Test fun appearanceRowsUseTheAgreedOrderAndDimensionsAndCancelDoesNotSave() = preserve {
        store.save(ApplicationSettings(themeSelection = AppThemeSelection.CUSTOM, customThemeColor = 0xff3f51b5.toInt(), language = AppLanguage.ENGLISH, darkMode = AppDarkMode.LIGHT))
        val saved = store.load()
        ActivityScenario.launch(AppSettingsActivity::class.java).use { scenario ->
            var previousY = -1
            val titles = listOf(R.string.settings_language, R.string.settings_dark_mode,
                R.string.settings_theme_color, R.string.launcher_icon_title)
            scenario.onActivity { activity ->
                for (title in titles) {
                    val row = row(activity, title)
                    assertTrue(row.minimumHeight >= activity.uiDp(72))
                    assertEquals(activity.uiDp(24), row.paddingStart)
                    assertEquals(activity.uiDp(24), row.paddingEnd)
                    val position = IntArray(2).also(row::getLocationOnScreen)
                    assertTrue("Appearance row order", position[1] > previousY)
                    previousY = position[1]
                    val texts = views(row).filterIsInstance<TextView>()
                    assertEquals(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16f, activity.resources.displayMetrics), texts.first().textSize, 0.5f)
                    assertEquals(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, activity.resources.displayMetrics), texts[1].textSize, 0.5f)
                }
            }
            for (title in titles) {
                lateinit var cancelled: AlertDialog
                scenario.onActivity { activity -> row(activity, title).performClick() }
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    val dialog = activity.presentedDialog!!
                    if (title == R.string.settings_theme_color) {
                        dialog.findViewById<View>(android.R.id.content)!!.findViewWithTag<TextInputEditText>("theme-color-input").setText("rgb(12, 34, 56)")
                    } else {
                        val index = dialog.listView.adapter.count - 1
                        dialog.listView.performItemClick(null, index, dialog.listView.adapter.getItemId(index))
                    }
                    assertEquals("Selecting a draft must not save", saved, store.load())
                    cancelled = dialog
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
                }
                instrumentation.waitForIdleSync()
                assertFalse(cancelled.isShowing)
                assertEquals("Cancel must not save", saved, store.load())
            }
        }
    }

    @Test fun customPreviewIsLocalInvalidRgbCannotSaveAndNeutralSurfacesSurviveConfirmation() = preserve {
        store.save(ApplicationSettings(themeSelection = AppThemeSelection.CUSTOM, customThemeColor = 0xff3f51b5.toInt(),
            language = AppLanguage.ENGLISH, darkMode = AppDarkMode.LIGHT))
        ActivityScenario.launch(AppSettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(0xfff3f4f5.toInt(), activity.appPalette.windowBackground)
                assertEquals(0xffffffff.toInt(), activity.appPalette.surface)
                row(activity, R.string.settings_theme_color).performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val dialog = activity.presentedDialog!!
                val root = dialog.findViewById<View>(android.R.id.content)!!
                val input = root.findViewWithTag<TextInputEditText>("theme-color-input")
                input.setText("rgb(12, 34, 56)")
                assertEquals(0xff3f51b5.toInt(), store.load().customThemeColor)
                assertEquals(ThemeAccentRoles.fromSeed(0xff0c2238.toInt(), false).primary, root.findViewWithTag<MaterialButton>("theme-color-preview").backgroundTintList!!.defaultColor)
                assertEquals(0xfff3f4f5.toInt(), activity.appPalette.windowBackground)
                input.setText("rgb(256, 0, 0)")
                assertFalse(dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled)
                assertEquals(0xff3f51b5.toInt(), store.load().customThemeColor)
                input.setText("0C2238")
                assertTrue(dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            }
            instrumentation.waitForIdleSync()
            assertEquals(0xff0c2238.toInt(), store.load().customThemeColor)
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals(ThemeAccentRoles.fromSeed(0xff0c2238.toInt(), false).primary, activity.appPalette.primary)
                assertEquals(0xfff3f4f5.toInt(), activity.appPalette.windowBackground)
                assertEquals(0xffffffff.toInt(), activity.appPalette.surface)
            }
            store.save(store.load().copy(darkMode = AppDarkMode.DARK))
            scenario.recreate()
            lateinit var oldDialog: AlertDialog
            scenario.onActivity { activity ->
                assertEquals(0xff121212.toInt(), activity.appPalette.windowBackground)
                assertEquals(0xff1e1e1e.toInt(), activity.appPalette.surface)
                assertEquals(0xffe6e1e5.toInt(), activity.appPalette.primaryText)
                assertEquals(0xffb9bac0.toInt(), activity.appPalette.secondaryText)
                row(activity, R.string.settings_theme_color).performClick()
                oldDialog = activity.presentedDialog!!
                oldDialog.findViewById<View>(android.R.id.content)!!.findViewWithTag<TextInputEditText>("theme-color-input").setText("#F44336")
            }
            scenario.recreate()
            assertFalse("Rotation destroys the old dialog instead of leaking its window", oldDialog.isShowing)
            assertEquals("Rotation cancels the unsaved draft", 0xff0c2238.toInt(), store.load().customThemeColor)
        }
    }

    @Test fun recycledMultiChoiceRowsUseTheCurrentThemeInsteadOfTheStaticBrand() = preserve {
        store.save(ApplicationSettings(themeSelection = AppThemeSelection.CUSTOM, customThemeColor = 0xffb3261e.toInt(),
            language = AppLanguage.ENGLISH, darkMode = AppDarkMode.LIGHT))
        ActivityScenario.launch(AppSettingsActivity::class.java).use { scenario ->
            lateinit var dialog: AlertDialog
            scenario.onActivity { activity ->
                dialog = activity.materialDialog().setTitle("Theme fixture")
                    .setMultiChoiceItems(Array(30) { "Choice $it" }, BooleanArray(30)) { _, _, _ -> }
                    .setPositiveButton(android.R.string.ok, null).show()
                activity.tintDialogButtons(dialog)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val view = views(dialog.listView).filterIsInstance<CheckedTextView>().first()
                assertEquals(activity.appPalette.accent, view.checkMarkTintList!!.getColorForState(intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked), 0))
                dialog.listView.setSelection(27)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val view = views(dialog.listView).filterIsInstance<CheckedTextView>().first()
                assertEquals(activity.appPalette.accent, view.checkMarkTintList!!.getColorForState(intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked), 0))
                dialog.dismiss()
            }
        }
    }

    private fun row(activity: AppSettingsActivity, title: Int): LinearLayout {
        val text = views(activity.findViewById(android.R.id.content)).filterIsInstance<TextView>()
            .first { it.text.toString() == activity.getString(title) }
        return text.parent.parent as LinearLayout
    }

    private fun views(root: View): List<View> = buildList {
        add(root)
        if (root is ViewGroup) for (index in 0 until root.childCount) addAll(views(root.getChildAt(index)))
    }

    private fun preserve(block: () -> Unit) {
        val preferences = context.getSharedPreferences("application-settings", Context.MODE_PRIVATE)
        val before = preferences.all.toMap()
        val pm = context.packageManager
        val aliases = LauncherIconMode.entries.associateWith { pm.getComponentEnabledSetting(it.component(context)) }
        val previous = LauncherIcons.current(context)
        try { block() } finally {
            val editor = preferences.edit().clear()
            for ((key, value) in before) when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                else -> error("Unsupported preference type")
            }
            assertTrue(editor.commit())
            aliases.entries.sortedBy { if (it.key == previous) 0 else 1 }.forEach { (mode, state) ->
                pm.setComponentEnabledSetting(mode.component(context), state, PackageManager.DONT_KILL_APP)
            }
            assertEquals(before, preferences.all)
        }
    }
}
