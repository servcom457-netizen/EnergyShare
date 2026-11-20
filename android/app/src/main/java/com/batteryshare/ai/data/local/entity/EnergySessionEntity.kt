package com.batteryshare.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "energy_sessions")
data class EnergySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mode: String,
    val peerDeviceName: String,
    val startBatteryPct: Int,
    val endBatteryPct: Int,
    val peakTempCelsius: Float?,
    val startedAtMs: Long,
    val endedAtMs: Long?,
    val terminationReason: String,
    val hardwareTransferObserved: Boolean
)
