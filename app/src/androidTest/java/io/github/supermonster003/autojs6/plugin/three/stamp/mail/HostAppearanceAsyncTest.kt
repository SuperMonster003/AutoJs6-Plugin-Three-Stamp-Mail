package io.github.supermonster003.autojs6.plugin.three.stamp.mail

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Read-only probe: an enabled official host is optional. No fake host or preference changes. */
class HostAppearanceAsyncTest {
    @Test fun cachedReadAvoidsMainThreadIpcAndRefreshReportsTheHostState() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        // Inspect the fixture outside the timed main-thread read. CI installs only this plugin;
        // local integration fixtures also install the enabled, matching-signature AutoJs6 host.
        val hostInstalled = try {
            @Suppress("DEPRECATION")
            context.packageManager.getApplicationInfo(ThreeStampMailPlugin.HOST_PACKAGE_NAME, PackageManager.MATCH_DISABLED_COMPONENTS)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
        var elapsed = Long.MAX_VALUE
        instrumentation.runOnMainSync {
            val start = SystemClock.elapsedRealtime()
            AutoJs6HostSettingsClient.query(context)
            elapsed = SystemClock.elapsedRealtime() - start
        }
        assertTrue("A cached read must not wait for a provider process: $elapsed ms", elapsed < 250)
        val ready = CountDownLatch(1)
        val result = AtomicReference<AutoJs6HostSettingsResult>()
        AutoJs6HostSettingsClient.refresh(context, force = true) {
            result.set(AutoJs6HostSettingsClient.query(context))
            ready.countDown()
        }
        assertTrue("Host appearance refresh did not complete", ready.await(15, TimeUnit.SECONDS))
        val actual = result.get()
        val snapshot = actual.snapshot
        instrumentation.sendStatus(0, Bundle().apply {
            putString("themeHostProbe", JSONObject().apply {
                put("package", context.packageName)
                put("availability", actual.availability.name)
                put("mainReadMillis", elapsed)
                put("primary", snapshot?.themeColorPrimary)
                put("accent", snapshot?.themeColorAccent)
                put("darkPolicy", snapshot?.darkModePolicy?.name)
                put("darkActive", snapshot?.darkModeActive)
                put("language", snapshot?.resolvedLanguageTag)
            }.toString())
        })
        if (hostInstalled) {
            // Preserve the strict real-host integration check, including its provider guard.
            assertEquals(AutoJs6HostAvailability.AVAILABLE, actual.availability)
            assertNotNull(snapshot)
            assertTrue(actual.selectable)
        } else {
            // Standalone installation is supported: absence must be explicit, not a stale
            // snapshot or a generic IPC failure that would hide a broken worker-thread check.
            assertEquals(AutoJs6HostAvailability.NOT_INSTALLED, actual.availability)
            assertNull(snapshot)
            assertFalse(actual.selectable)
            assertTrue(actual.definitiveAbsence)
        }
    }
}
