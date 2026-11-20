package com.batteryshare.ai.hardware.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.batteryshare.ai.MainActivity
import com.batteryshare.ai.R

class EnergyMonitorService : Service() {

    companion object {
        const val ACTION_START = "com.batteryshare.ai.action.START_MONITOR"
        const val ACTION_STOP = "com.batteryshare.ai.action.STOP_MONITOR"
        const val EXTRA_MODE_LABEL = "extra_mode_label"
        private const val CHANNEL_ID = "batteryshare_monitor_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context, modeLabel: String) {
            val intent = Intent(context, EnergyMonitorService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MODE_LABEL, modeLabel)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, EnergyMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val modeLabel = intent?.getStringExtra(EXTRA_MODE_LABEL) ?: "Surveillance active"
        ensureChannel()
        val notification = buildNotification(modeLabel)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(modeLabel: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, EnergyMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_batteryshare_logo)
            .setContentTitle("BatteryShare AI — $modeLabel")
            .setContentText("Surveillance batterie, température et sécurité active.")
            .setContentIntent(openPending)
            .addAction(0, "Arrêter", stopPending)
            .setOngoing(true)
            .build()
    }
}
