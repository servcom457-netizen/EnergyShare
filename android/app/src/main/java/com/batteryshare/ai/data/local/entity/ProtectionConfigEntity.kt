package com.batteryshare.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "protection_config")
data class ProtectionConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val minDonorBatteryPct: Int = 25,
    val maxShareDeltaPct: Int = 20,
    val maxTempCelsius: Float = 42.0f,
    val autoStopOnCritical: Boolean = true,
    val thermalAlertEnabled: Boolean = true,
    val anomalyAlertEnabled: Boolean = true
)
