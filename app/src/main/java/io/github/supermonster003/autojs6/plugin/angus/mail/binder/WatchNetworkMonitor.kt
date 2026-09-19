package io.github.supermonster003.autojs6.plugin.angus.mail.binder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.os.PowerManager
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.Watcher
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Tells the running watches when the device's default network changes (roadmap P5 device matrix:
 * Wi-Fi off and on, Wi-Fi to cellular) and when the device leaves Doze. A watch blocked in IDLE
 * on a connection the old network took with it would otherwise notice only at its next renew,
 * and a watch whose reconnects timed out while Doze froze the network sleeps in a backoff of up
 * to `WATCH_BACKOFF_MAX_MS` after the device woke up (P5 evidence: 235 s on a Sony API 28);
 * [Watcher.reconnect] drops the connection or the pause so the loop reconnects at once with the
 * backoff reset. Registered with the system only while at least one watch runs
 * (`ACCESS_NETWORK_STATE`, declared already; the idle-mode broadcast needs no permission).
 * Failures to register leave the watches to their own timers.
 */
internal class WatchNetworkMonitor(context: Context) {

    private val application = context.applicationContext
    private val manager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val power = application.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val watchers = CopyOnWriteArraySet<Watcher>()
    private val lock = Any()
    private var registered = false
    private var idleReceiverRegistered = false

    @Volatile
    private var current: Network? = null

    @Volatile
    private var idle = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val previous = current
            current = network
            // the first callback after registration only reports the network in use
            if (previous != null && previous != network) kick(REASON_CHANGED)
        }

        override fun onLost(network: Network) {
            if (current == network) {
                current = null
                kick(REASON_LOST)
            }
        }
    }

    private val idleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED) return
            val now = power?.isDeviceIdleMode == true
            val before = idle
            idle = now
            if (before && !now) kick(REASON_IDLE_ENDED)
        }
    }

    /** Watches told about network changes right now. */
    val size: Int get() = watchers.size

    fun attach(watcher: Watcher) {
        synchronized(lock) {
            watchers += watcher
            val connectivity = manager
            if (!registered && connectivity != null) {
                registered = runCatching { connectivity.registerDefaultNetworkCallback(callback) }.isSuccess
            }
            if (!idleReceiverRegistered && power != null) {
                idle = power.isDeviceIdleMode
                idleReceiverRegistered = runCatching { application.registerReceiver(idleReceiver, IntentFilter(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)) }.isSuccess
            }
        }
    }

    fun detach(watcher: Watcher) {
        synchronized(lock) {
            watchers -= watcher
            if (watchers.isEmpty()) {
                if (registered) {
                    registered = false
                    current = null
                    runCatching { manager?.unregisterNetworkCallback(callback) }
                }
                if (idleReceiverRegistered) {
                    idleReceiverRegistered = false
                    runCatching { application.unregisterReceiver(idleReceiver) }
                }
            }
        }
    }

    private fun kick(reason: String) {
        watchers.forEach { runCatching { it.reconnect(reason) } }
    }

    companion object {
        const val REASON_CHANGED = "network-changed"
        const val REASON_LOST = "network-lost"
        const val REASON_IDLE_ENDED = "device-idle-ended"

        @Volatile
        private var instance: WatchNetworkMonitor? = null

        fun of(context: Context): WatchNetworkMonitor = instance ?: synchronized(this) {
            instance ?: WatchNetworkMonitor(context.applicationContext).also { instance = it }
        }
    }
}
