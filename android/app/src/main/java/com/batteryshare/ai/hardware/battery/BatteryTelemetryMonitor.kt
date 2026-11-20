package com.batteryshare.ai.hardware.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.batteryshare.ai.domain.model.BatteryChargeStatus
import com.batteryshare.ai.domain.model.BatteryHealthStatus
import com.batteryshare.ai.domain.model.BatteryTelemetry
import com.batteryshare.ai.domain.model.PluggedSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatteryTelemetryMonitor(private val context: Context) {

    private val batteryManager =
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    private val _telemetry = MutableStateFlow(readSnapshot(null))
    val telemetry: StateFlow<BatteryTelemetry> = _telemetry.asStateFlow()

    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                _telemetry.value = readSnapshot(intent)
            }
        }
    }

    fun start() {
        if (registered) return
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = context.registerReceiver(receiver, filter)
        _telemetry.value = readSnapshot(stickyIntent)
        registered = true
    }

    fun stop() {
        if (!registered) return
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // Déjà désinscrit
        } finally {
            registered = false
        }
    }

    fun refreshNow() {
        val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        _telemetry.value = readSnapshot(sticky)
    }

    private fun readSnapshot(intent: Intent?): BatteryTelemetry {
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val computedPct = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale).toInt().coerceIn(0, 100)
        } else {
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
        }

        val statusInt = intent?.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN
        ) ?: BatteryManager.BATTERY_STATUS_UNKNOWN

        val status = when (statusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> BatteryChargeStatus.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryChargeStatus.DISCHARGING
            BatteryManager.BATTERY_STATUS_FULL -> BatteryChargeStatus.FULL
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryChargeStatus.NOT_CHARGING
            else -> BatteryChargeStatus.UNKNOWN
        }

        val pluggedInt = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val pluggedSource = when {
            (pluggedInt and BatteryManager.BATTERY_PLUGGED_WIRELESS) != 0 -> PluggedSource.WIRELESS
            (pluggedInt and BatteryManager.BATTERY_PLUGGED_AC) != 0 -> PluggedSource.AC
            (pluggedInt and BatteryManager.BATTERY_PLUGGED_USB) != 0 -> PluggedSource.USB
            else -> PluggedSource.NONE
        }

        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?: Int.MIN_VALUE
        val tempCelsius = if (tempTenths != Int.MIN_VALUE && tempTenths > -500 && tempTenths < 1500) {
            tempTenths / 10f
        } else {
            null
        }

        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        val currentRaw = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentMicroA = if (currentRaw != Int.MIN_VALUE && currentRaw != 0) currentRaw else null

        val healthInt = intent?.getIntExtra(
            BatteryManager.EXTRA_HEALTH,
            BatteryManager.BATTERY_HEALTH_UNKNOWN
        ) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN

        val health = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealthStatus.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealthStatus.OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealthStatus.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealthStatus.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealthStatus.COLD
            else -> BatteryHealthStatus.UNKNOWN
        }

        val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        return BatteryTelemetry(
            levelPct = computedPct,
            status = status,
            pluggedSource = pluggedSource,
            temperatureCelsius = tempCelsius,
            voltageMv = voltageMv,
            currentNowMicroA = currentMicroA,
            health = health,
            technology = tech,
            timestampMs = System.currentTimeMillis()
        )
    }
}
