package com.bitchat.network.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bitchat.app.MainActivity

/**
 * Foreground service that keeps the BLE mesh network alive while the app is
 * in the background.
 *
 * ## Why a foreground service?
 *
 * Android imposes strict background-execution limits on BLE scanning and
 * advertising. A foreground service with an ongoing notification is the
 * recommended way to maintain long-running BLE operations without the system
 * killing the process. Starting in Android 12 (API 31) a foreground service
 * type of `connectedDevice` or `dataSync` must also be declared in the
 * manifest for certain categories.
 *
 * ## Lifecycle
 *
 * 1. **Created** — the system instantiates the service; [onCreate] creates the
 *    notification channel required by Android 8+ (API 26+).
 * 2. **Started** — [onStartCommand] promotes the service to the foreground
 *    with [startForeground] and returns [START_STICKY] so the system will
 *    restart the service if it is killed.
 * 3. **Bound** — activities or other components may bind via [LocalBinder] to
 *    communicate directly with the service instance (e.g. to start/stop
 *    scanning).
 * 4. **Destroyed** — [onDestroy] is called when the service is explicitly
 *    stopped or the system reclaims memory.
 *
 * ## Notification channel
 *
 * The channel is created once in [onCreate] with [IMPORTANCE_LOW] so the
 * notification stays silent and does not appear on the lock screen — this is
 * appropriate for a persistent "running in background" indicator.
 */
class BleForegroundService : Service() {

    /**
     * Binder that exposes the service instance to bound clients.
     *
     * Clients cast the returned [IBinder] to [LocalBinder] and call
     * [LocalBinder.getService] to get a direct reference, enabling synchronous
     * method calls without IPC overhead.
     */
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): BleForegroundService = this@BleForegroundService
    }

    /** Returns the local binder so bound components can access the service. */
    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        // Android 8+ requires a notification channel before any notification
        // can be posted. Creating it here guarantees it exists before
        // startForeground() is called in onStartCommand.
        createNotificationChannel()
    }

    /**
     * Promotes this service to the foreground on every start request.
     *
     * Using [START_STICKY] tells the system to recreate the service if it is
     * killed, preserving BLE connectivity across memory-pressure events.
     *
     * @param intent  optional intent with extras for command routing.
     * @param flags   bitmask indicating how the service was started.
     * @param startId a unique integer identifying this start request.
     * @return [START_STICKY] to request automatic restart.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        // Must be called within 5 seconds of startForegroundService() to
        // avoid an ANR. The notification is updated later if needed.
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    /**
     * Creates the low-importance notification channel used by this service.
     *
     * Channels are registered with the system once; subsequent calls with the
     * same [CHANNEL_ID] are no-ops unless the user has overridden the channel
     * settings. The [IMPORTANCE_LOW] level means the notification will appear
     * in the shade but will not make sound or heads-up, which is appropriate
     * for a persistent connection indicator.
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "BitChat BLE Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Maintains Bluetooth connections for BitChat"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Builds the persistent foreground notification.
     *
     * The notification's content intent opens [MainActivity], so tapping the
     * notification brings the user back into the app. [FLAG_IMMUTABLE] is
     * required on API 31+ to prevent the system from mutating the intent.
     * [FLAG_UPDATE_CURRENT] ensures that if the notification is reposted, the
     * pending intent is refreshed rather than duplicated.
     */
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BitChat")
            .setContentText("Maintaining connections...")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Cannot be swiped away while the service runs
            .build()
    }

    /**
     * Called when the service is being destroyed.
     *
     * Subclasses should release BLE resources (scanner, advertiser, GATT
     * server) here. The current implementation is a no-op placeholder; BLE
     * teardown is handled elsewhere in the mesh layer.
     */
    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        /** Unique notification ID used to post and update the foreground notification. */
        private const val NOTIFICATION_ID = 1001

        /** Notification channel identifier registered in [createNotificationChannel]. */
        private const val CHANNEL_ID = "bitchat_ble_service"
    }
}
