package io.github.supermonster003.autojs6.plugin.angus.mail.trigger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.supermonster003.autojs6.plugin.angus.mail.R
import io.github.supermonster003.autojs6.plugin.angus.mail.core.message.MimeLeniency

/**
 * The foreground service that keeps the background watches alive while no script runs (roadmap
 * P8). Type `specialUse` (roadmap D40): the `dataSync` type of the original plan is capped at
 * six hours per day and cannot start from `BOOT_COMPLETED` on Android 15 and later, while a
 * mail watch is exactly the long-lived connection the special-use type exists for. Every start
 * command reconciles the [WatchKeeper] with the [TriggerStore]; when no watch is enabled the
 * service stops itself, so it runs only while there is something to keep. The persistent
 * notification (channel [CHANNEL_ID], low importance) names how many watches are connected and
 * opens the watches page; without `POST_NOTIFICATIONS` on Android 13 and later the service still
 * runs and the notification is simply not shown. Sticky, so the system restarts it after a
 * process death and the watches come back.
 */
class MailWatchService : Service() {

    private lateinit var keeper: WatchKeeper
    private val handler = Handler(Looper.getMainLooper())
    private val changed: () -> Unit = { handler.post { if (started) updateNotification() } }

    @Volatile
    private var started = false

    override fun onCreate() {
        super.onCreate()
        MimeLeniency.install()
        keeper = WatchKeeper.of(this)
        keeper.addListener(changed)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        started = true
        val running = keeper.sync()
        Log.i(TAG, "start running=$running startId=$startId redelivered=${flags and START_FLAG_REDELIVERY != 0} retry=${flags and START_FLAG_RETRY != 0}")
        if (running == 0) {
            started = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        updateNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "destroy watches=${keeper.running}")
        started = false
        keeper.removeListener(changed)
        keeper.stopAll(io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerStatusDocument.REASON_SERVICE_STOPPED)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching { manager.notify(NOTIFICATION_ID, buildNotification()) }
    }

    private fun buildNotification(): Notification {
        ensureChannel()
        val total = runCatching { TriggerStores.of(this).list().count { it.enabled } }.getOrDefault(0)
        val connected = keeper.connected
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, WatchesActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mail_24)
            .setContentTitle(getString(R.string.watches_notification_title))
            .setContentText(getString(R.string.watches_notification_text, connected, total))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.watches_notification_channel), NotificationManager.IMPORTANCE_LOW).apply {
            description = getString(R.string.watches_notification_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "MailWatchService"
        const val CHANNEL_ID = "background_watch"
        const val NOTIFICATION_ID = 0x4D41

        /** The property value the manifest's `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` carries (Android 14 and later). */
        const val SPECIAL_USE_SUBTYPE = "mail_background_watch"

        /**
         * Starts (or re-syncs) the service; false when Android refused the start, which happens
         * for a background caller on Android 12 and later (the settings page, the boot receiver
         * and a bound host are allowed). The caller may fall back to an in-process `sync`.
         */
        fun start(context: Context): Boolean = try {
            ContextCompat.startForegroundService(context, Intent(context, MailWatchService::class.java))
            true
        } catch (_: IllegalStateException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
