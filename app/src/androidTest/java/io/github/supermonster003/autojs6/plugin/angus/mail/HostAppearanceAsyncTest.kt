package io.github.supermonster003.autojs6.plugin.angus.mail

import android.os.Bundle
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Read-only probe in the real official app UID. No fake host or preference changes. */
class HostAppearanceAsyncTest {
    @Test fun cachedReadAvoidsMainThreadIpcAndTheOfficialHostSnapshotArrivesAsynchronously() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
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
        // These AVD fixtures have the installed/enabled official host. The provider's guard and
        // queryBlocking's worker-thread check must both pass for an AVAILABLE result.
        assertEquals(AutoJs6HostAvailability.AVAILABLE, actual.availability)
        assertNotNull(snapshot)
    }
}
