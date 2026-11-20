package io.github.supermonster003.autojs6.plugin.three.stamp.mail

import android.app.Activity
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The release history page (AUTOJS6_PLUGIN_RELEASE_HISTORY_AGENTS.md): its title is "Release history" with the plugin
 * name as the subtitle, the newest release starts expanded, and the settings page and the about page both open it.
 */
@RunWith(AndroidJUnit4::class)
class ReleaseHistoryPageInstrumentationTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @After fun finishActivities() {
        instrumentation.setInTouchMode(true)
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).forEach(Activity::finish)
        }
        instrumentation.waitForIdleSync()
    }

    /** Unmerged: the count chips sit inside the clickable header, which merges its children. */
    private fun exists(tag: String): Boolean =
        runCatching { compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)

    private fun resumed(): Activity? {
        var activity: Activity? = null
        instrumentation.runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
        }
        return activity
    }

    private fun assertThePageOpened() {
        compose.waitUntil(TIMEOUT) { resumed() is ReleaseHistoryActivity && exists("$TAG.list") }
        assertTitleAndSubtitle()
    }

    private fun assertTitleAndSubtitle() {
        val inTopBar = hasAnyAncestor(hasTestTag("stamp.topBar"))
        compose.onNode(hasText(context.getString(R.string.release_history_title)) and inTopBar).assertIsDisplayed()
        compose.onNode(hasText(context.getString(R.string.app_name)) and inTopBar).assertIsDisplayed()
    }

    @Test fun theTitleIsReleaseHistoryAndTheSubtitleThePluginName() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use {
            compose.waitUntil(TIMEOUT) { exists("$TAG.list") }
            assertTitleAndSubtitle()
            assertFalse(exists("$TAG.empty"))
        }
    }

    private fun versions(scenario: ActivityScenario<ReleaseHistoryActivity>): List<String> {
        var versions = emptyList<String>()
        scenario.onActivity { activity ->
            val candidates = ReleaseHistoryAssetPolicy.candidatesFor(activity.resources.configuration.locales[0])
            versions = loadReleaseHistory(candidates) { path ->
                activity.assets.open(path).use { it.readBytes().toString(Charsets.UTF_8) }
            }.entries.map { it.version }
        }
        return versions
    }

    /**
     * Scrolls the list to a version header. A release below the fold is not composed at all, so its tags stay missing
     * until it is scrolled into view (on the Nexus 5 profile of CI the expanded newest release fills the screen).
     */
    private fun scrollTo(version: String) {
        compose.onNodeWithTag("$TAG.list").performScrollToNode(hasTestTag("$TAG.entry.$version"))
    }

    private fun clickHeader(version: String) {
        scrollTo(version)
        compose.onNodeWithTag("$TAG.entry.$version").performClick()
    }

    /**
     * The newest release starts expanded, the others show their category counts; a header expands or collapses its
     * release, the top bar expands or collapses all of them, and the choice survives recreation.
     */
    @Test fun releasesExpandAndCollapse() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use { scenario ->
            compose.waitUntil(TIMEOUT) { exists("$TAG.list") }
            val versions = versions(scenario)
            assertTrue("releases: ${versions.size}", versions.size > 2)
            val (newest, second) = versions
            assertTrue(exists("$TAG.body.$newest"))
            assertFalse(exists("$TAG.counts.$newest"))
            scrollTo(second)
            assertFalse(exists("$TAG.body.$second"))
            assertTrue(exists("$TAG.counts.$second"))

            clickHeader(second)
            compose.waitUntil(TIMEOUT) { exists("$TAG.body.$second") }
            clickHeader(newest)
            compose.waitUntil(TIMEOUT) { !exists("$TAG.body.$newest") && exists("$TAG.counts.$newest") }

            scenario.recreate()
            compose.waitUntil(TIMEOUT) { exists("$TAG.list") }
            scrollTo(second)
            assertTrue(exists("$TAG.body.$second"))
            scrollTo(newest)
            assertFalse(exists("$TAG.body.$newest"))

            compose.onNodeWithTag("$TAG.toggleAll")
                .assertContentDescriptionEquals(context.getString(R.string.release_history_expand_all))
                .performClick()
            compose.waitUntil(TIMEOUT) { exists("$TAG.body.$newest") }
            compose.onNodeWithTag("$TAG.toggleAll")
                .assertContentDescriptionEquals(context.getString(R.string.release_history_collapse_all))
                .performClick()
            compose.waitUntil(TIMEOUT) { !exists("$TAG.body.$newest") && !exists("$TAG.body.$second") }
        }
    }

    /** Clicks the View row whose title is [title]: the nearest clickable ancestor of that text. */
    private fun clickRow(activity: Activity, title: String) {
        fun find(view: View): TextView? = when {
            view is TextView && view.text?.toString() == title -> view
            view is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { find(view.getChildAt(it)) }
            else -> null
        }
        instrumentation.runOnMainSync {
            val text = checkNotNull(find(activity.window.decorView)) { "no row titled $title" }
            val row = generateSequence<View>(text) { it.parent as? View }.first { it.isClickable }
            check(row.performClick()) { "the row titled $title did not take the click" }
        }
    }

    private fun opensFrom(activityClass: Class<out Activity>) {
        val intent = Intent(context, activityClass).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = instrumentation.startActivitySync(intent)
        instrumentation.waitForIdleSync()
        clickRow(activity, context.getString(R.string.release_history_title))
        assertThePageOpened()
    }

    @Test fun theSettingsRowOpensThePage() = opensFrom(AppSettingsActivity::class.java)

    @Test fun theAboutRowOpensThePage() = opensFrom(AboutActivity::class.java)

    private fun key(code: Int) {
        instrumentation.sendKeyDownUpSync(code)
        compose.waitForIdle()
    }

    /**
     * Leaves touch mode as a keyboard user does. Newer versions take [android.app.Instrumentation.setInTouchMode] at
     * once; older ones (API 24) learn it only from a key, so an up arrow goes first.
     */
    private fun leaveTouchMode() {
        instrumentation.setInTouchMode(false)
        key(KeyEvent.KEYCODE_DPAD_UP)
    }

    /** Runs [command] as the shell user (it connects UiAutomation) and returns its output. */
    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }

    /**
     * The focused window and app as `dumpsys window` reports them, for diagnostics only. API 35 leaves mCurrentFocus
     * out of `dumpsys window windows`, so the whole dump is read.
     */
    private fun focusedWindow(): String = runCatching {
        shell("dumpsys window").lineSequence().map(String::trim)
            .filter { it.startsWith("mCurrentFocus=") || it.startsWith("mFocusedApp=") }.distinct().joinToString()
            .ifEmpty { "mCurrentFocus not in dumpsys" }
    }.getOrElse { "dumpsys failed: $it" }

    private fun windowFocused(scenario: ActivityScenario<*>): Boolean = try {
        var focused = false
        compose.waitUntil(TIMEOUT) { scenario.onActivity { focused = it.hasWindowFocus() }; focused }
        true
    } catch (timeout: ComposeTimeoutException) {
        false
    }

    /**
     * Keys go to the focused window: while a system dialog has focus (an ANR or crash dialog of another app on a slow
     * emulator, say), every Tab would be lost. When focus does not come, the test closes system dialogs (error dialogs
     * take that as "wait" or "close") and brings the page back to the front, once, before it gives up.
     */
    private fun awaitWindowFocus(scenario: ActivityScenario<*>, visited: List<String> = emptyList()) {
        if (windowFocused(scenario)) return
        val before = focusedWindow()
        Log.w(LOG_TAG, "no window focus after $TIMEOUT ms ($before), closing system dialogs")
        shell("am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS")
        scenario.onActivity { it.startActivity(Intent(it, it.javaClass).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
        if (!windowFocused(scenario)) {
            fail("the page has no window focus after $TIMEOUT ms, nor after closing system dialogs; visited: $visited; " +
                "before: $before; after: ${focusedWindow()}")
        }
        Log.i(LOG_TAG, "window focus is back: ${focusedWindow()}")
    }

    /**
     * The test tag of the focused control (or of its nearest tagged ancestor), null when nothing has focus. The back
     * button has no tag of its own, so inside the top bar it is told apart by its content description.
     */
    private fun focusedControl(): String? {
        val node = compose.onAllNodes(isFocused(), useUnmergedTree = true).fetchSemanticsNodes().firstOrNull() ?: return null
        val tag = generateSequence(node) { it.parent }.mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }.firstOrNull()
        if (tag == "stamp.topBar") {
            val description = compose.onNode(isFocused()).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)
            if (description == listOf(context.getString(R.string.navigation_back))) return BACK
        }
        return tag ?: UNTAGGED
    }

    /** Presses Tab until focus returns to a control seen before; every focused control must be on screen. */
    private fun tabCycle(scenario: ActivityScenario<*>): List<String> {
        val visited = mutableListOf<String>()
        repeat(MAX_TABS) {
            awaitWindowFocus(scenario, visited)
            key(KeyEvent.KEYCODE_TAB)
            val control = focusedControl() ?: return@repeat
            if (control in visited) return visited
            compose.onNode(isFocused(), useUnmergedTree = true).assertIsDisplayed()
            visited += control
        }
        return visited
    }

    /**
     * Keyboard users read the whole page (AUTOJS6_PLUGIN_RELEASE_HISTORY_AGENTS.md section 10): Tab passes the back
     * button and then every version header in order, down to the oldest release.
     */
    @Test fun theReleaseHistoryCanBeReadToTheEndFromTheKeyboard() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use { scenario ->
            compose.waitUntil(TIMEOUT) { exists("$TAG.list") }
            val versions = versions(scenario)
            assertTrue("releases: ${versions.size}", versions.size > 2)
            awaitWindowFocus(scenario)
            leaveTouchMode()
            val visited = tabCycle(scenario)
            fun expect(ok: Boolean, what: String) { if (!ok) fail("$what; visited: $visited; ${focusedWindow()}") }
            expect(BACK in visited, "Tab never reached the back button")
            expect(UNTAGGED !in visited, "something focusable has no name")
            val expected = versions.map { "$TAG.entry.$it" }
            val entries = visited.filter { it.startsWith("$TAG.entry.") }
            expect(entries == expected, "the releases in Tab order: expected $expected, got $entries")
        }
    }

    private companion object {
        const val TAG = "stamp.releaseHistory"
        const val LOG_TAG = "ReleaseHistoryTest"
        const val TIMEOUT = 15_000L
        const val MAX_TABS = 120
        const val UNTAGGED = "(untagged)"
        const val BACK = "(back)"
    }
}
