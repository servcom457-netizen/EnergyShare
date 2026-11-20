package com.batteryshare.ai.hardware.protection

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.batteryshare.ai.R
import com.batteryshare.ai.data.local.entity.ProtectionConfigEntity
import com.batteryshare.ai.domain.model.BatteryTelemetry
import com.batteryshare.ai.domain.model.ProtectionEvaluator
import com.batteryshare.ai.domain.model.ProtectionSeverity
import com.batteryshare.ai.domain.model.ProtectionVerdict

class ProtectionMonitor(private val context: Context) {

    companion object {
        const val ALERT_CHANNEL_ID = "batteryshare_protection_alerts"
        private const val ALERT_NOTIFICATION_ID = 4021
    }

    private var lastAlertSentAtMs: Long = 0L

    init {
        ensureAlertChannel()
    }

    fun evaluateAndNotify(
        telemetry: BatteryTelemetry,
        config: ProtectionConfigEntity,
        startDonorBatteryPct: Int?,
        connectionAnomalyDetected: Boolean
    ): ProtectionVerdict {
        val verdict = ProtectionEvaluator.evaluate(
            telemetry = telemetry,
            startDonorBatteryPct = startDonorBatteryPct,
            minDonorBatteryPct = config.minDonorBatteryPct,
            maxShareDeltaPct = config.maxShareDeltaPct,
            maxTempCelsius = config.maxTempCelsius,
            autoStopOnCritical = config.autoStopOnCritical,
            connectionAnomalyDetected = connectionAnomalyDetected
        )

        val shouldAlert = when (verdict.severity) {
            ProtectionSeverity.CRITICAL_STOP -> config.thermalAlertEnabled || config.anomalyAlertEnabled
            ProtectionSeverity.WARNING -> config.thermalAlertEnabled
            ProtectionSeverity.NOMINAL -> false
        }

        if (shouldAlert && verdict.alertMessage != null) {
            val now = System.currentTimeMillis()
            if (now - lastAlertSentAtMs > 15_000L) {
                postAlertNotification(verdict.reason.label, verdict.alertMessage)
                lastAlertSentAtMs = now
            }
        }

        return verdict
    }

    private fun ensureAlertChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Alertes de protection BatteryShare AI",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes de seuil minimal, surchauffe ou anomalie USB/Qi."
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun postAlertNotification(title: String, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_batteryshare_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(ALERT_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS non encore accordée sur Android 13+
        }
    }
}
