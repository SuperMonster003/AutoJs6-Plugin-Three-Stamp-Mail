package io.github.supermonster003.autojs6.plugin.angus.mail

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerConfig
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerFilter
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStores
import io.github.supermonster003.autojs6.plugin.angus.mail.trigger.BootReceiver
import io.github.supermonster003.autojs6.plugin.angus.mail.trigger.MailWatchService
import io.github.supermonster003.autojs6.plugin.angus.mail.trigger.TriggerStores
import io.github.supermonster003.autojs6.plugin.angus.mail.trigger.WatchKeeper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Configures (or removes) a background watch on an account saved by [RealAccountSettingsDeviceTest]
 * for the P8 device matrix (`.python/run_trigger_matrix.py`). Runs through `am instrument`
 * directly, so the watch config survives the test; the driver then starts the watch service
 * through the Watches page, because the instrumentation's end force-stops this process.
 * Arguments: `mailAlias` (saved account), `watchId`, optional `watchFolder` (INBOX),
 * `watchMode` (auto / idle / poll), `watchIntervalMs`, `watchFrom`, `watchSubject`
 * (comma-separated), `watchBoot` (true enables the boot receiver). Nothing secret is involved:
 * the account's secret stays in the store and the keeper decrypts it in this process only.
 */
@RunWith(AndroidJUnit4::class)
class RealAccountWatchDeviceTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val arguments
        get() = InstrumentationRegistry.getArguments()

    @Test
    fun configuresAWatchForTheHost() {
        val alias = arguments.getString("mailAlias")
        val watchId = arguments.getString("watchId")
        assumeTrue("mailAlias / watchId not given", !alias.isNullOrBlank() && !watchId.isNullOrBlank())
        stopWatchService()
        assertNotNull("the account '$alias' must be saved first (run_settings_real_account.py)", AccountStores.of(context).get(alias!!))
        val config = TriggerConfig(
            triggerId = watchId!!,
            alias = alias,
            folder = arguments.getString("watchFolder")?.takeIf { it.isNotBlank() } ?: "INBOX",
            mode = arguments.getString("watchMode")?.takeIf { it.isNotBlank() && it != "auto" },
            pollIntervalMs = arguments.getString("watchIntervalMs")?.toLongOrNull() ?: 60_000L,
            filter = TriggerFilter(
                from = TriggerFilter.split(arguments.getString("watchFrom").orEmpty()),
                subject = TriggerFilter.split(arguments.getString("watchSubject").orEmpty()),
            ),
        ).validated()
        val stored = TriggerStores.of(context).put(config)
        assertEquals(config.triggerId, stored.triggerId)
        val boot = arguments.getString("watchBoot")?.toBoolean() ?: false
        BootReceiver.setEnabled(context, boot)
        assertEquals(boot, BootReceiver.isEnabled(context))
        // A keeper of this (instrumentation) process must not hold the account's connection: the driver starts the service.
        WatchKeeper.of(context).stopAll("instrumentation")
        Log.i(TAG, "configured watch=${config.triggerId} alias=${config.alias} folder=${config.folder} mode=${config.mode ?: "auto"} interval=${config.pollIntervalMs} filter=${!config.filter.isEmpty} boot=$boot watches=${TriggerStores.of(context).list().size}")
    }

    @Test
    fun removesTheWatch() {
        val watchId = arguments.getString("watchId")
        assumeTrue("watchId not given", !watchId.isNullOrBlank())
        stopWatchService()
        WatchKeeper.of(context).stopAll("instrumentation")
        val removed = TriggerStores.of(context).remove(watchId!!)
        BootReceiver.setEnabled(context, false)
        Log.i(TAG, "removed watch=$watchId existed=$removed remaining=${TriggerStores.of(context).list().size}")
        assertTrue(TriggerStores.of(context).get(watchId) == null)
    }

    /**
     * The instrumentation's force-stop makes Android restart the sticky watch service inside this
     * process one second later; a service still running when the instrumentation finishes is
     * reported as "Process crashed" on Android 9. Stopping it first keeps the run deterministic;
     * the driver starts the service afterwards.
     */
    private fun stopWatchService() {
        context.stopService(Intent(context, MailWatchService::class.java))
    }

    private companion object {
        const val TAG = "RealAccountWatch"
    }
}
