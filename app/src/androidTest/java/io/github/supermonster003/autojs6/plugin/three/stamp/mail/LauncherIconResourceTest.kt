package io.github.supermonster003.autojs6.plugin.three.stamp.mail

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Resource-only checks. Do not change app preferences, task state, or system night mode. */
@RunWith(AndroidJUnit4::class)
class LauncherIconResourceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Suppress("DEPRECATION")
    private fun resources(night: Int): Resources {
        val base = context.resources
        val configuration = Configuration(base.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
        }
        // An isolated Resources also lets us test the undefined-mode fallback, which
        // createConfigurationContext would inherit from the base configuration.
        return Resources(base.assets, base.displayMetrics, configuration)
    }

    @Test fun transparentChoiceRemainsABitmapInBothModes() {
        for (mode in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
            val icon = resources(mode).getDrawable(R.mipmap.ic_launcher, null)
            assertTrue("Transparent choice must never resolve to an adaptive icon", icon is BitmapDrawable)
            val bitmap = (icon as BitmapDrawable).bitmap
            assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
            var opaqueCount = 0
            var transparentCount = 0
            var brightness = 0L
            val colors = mutableSetOf<Int>()
            for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                if (Color.alpha(pixel) == 255) {
                    brightness += Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)
                    colors += pixel
                    opaqueCount++
                } else if (Color.alpha(pixel) == 0) transparentCount++
            }
            assertTrue("The glyph must remain visible", opaqueCount > bitmap.width)
            assertTrue("Envelope folds must retain their tonal detail", colors.size > 8)
            val average = brightness.toDouble() / (3 * opaqueCount)
            assertTrue("Wrong theme artwork", if (mode == Configuration.UI_MODE_NIGHT_NO) average < 96 else average > 160)
            assertTrue("The Transparent choice must not contain a filled background", transparentCount > bitmap.width * bitmap.height / 2)
        }
    }

    @Test fun systemIconsUseAdaptiveLayersWithMatchingLegacyFallbacks() {
        for (mode in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES, Configuration.UI_MODE_NIGHT_UNDEFINED)) {
          for (resource in listOf(R.mipmap.ic_launcher_system, R.mipmap.ic_launcher_system_light, R.mipmap.ic_launcher_system_auto)) {
            val light = resource == R.mipmap.ic_launcher_system_light || (resource == R.mipmap.ic_launcher_system_auto && mode == Configuration.UI_MODE_NIGHT_NO)
            val background = if (light) 0xfffafafa.toInt() else 0xff212121.toInt()
            val icon = resources(mode).getDrawable(resource, null)
            if (Build.VERSION.SDK_INT >= 26) {
                assertTrue("An API 26+ launcher must always receive an adaptive icon (mode $mode)", icon is AdaptiveIconDrawable)
                val adaptive = icon as AdaptiveIconDrawable
                assertEquals(background, (adaptive.background as ColorDrawable).color)
                assertTrue(adaptive.foreground is BitmapDrawable)
                val glyph = (adaptive.foreground as BitmapDrawable).bitmap
                val opaque = (0 until glyph.height).asSequence().flatMap { y -> (0 until glyph.width).asSequence().map { x -> glyph.getPixel(x, y) } }.filter { Color.alpha(it) == 255 }.toList()
                val average = opaque.map { (Color.red(it) + Color.green(it) + Color.blue(it)) / 3.0 }.average()
                assertTrue("Wrong adaptive foreground mode", if (light) average < 96 else average > 160)
                if (Build.VERSION.SDK_INT >= 33) assertTrue(adaptive.monochrome is BitmapDrawable)
            } else {
                assertTrue(icon is BitmapDrawable)
                val bitmap = (icon as BitmapDrawable).bitmap
                assertEquals(background, bitmap.getPixel(bitmap.width / 2, bitmap.height / 12))
                assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
            }
          }
        }
    }

    @Test fun applicationAndAutoKeepIndependentSystemIcons() {
        assertEquals(R.mipmap.ic_launcher_system, context.applicationInfo.icon)
        val auto = context.packageManager.getActivityInfo(LauncherIconMode.AUTO.component(context), android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS)
        assertEquals("Auto must retain its own ID so the launcher resolves the current configuration", R.mipmap.ic_launcher_system_auto, auto.icon)
    }
}
