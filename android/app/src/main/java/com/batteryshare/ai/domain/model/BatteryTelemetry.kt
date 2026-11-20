package com.batteryshare.ai.domain.model

enum class BatteryChargeStatus {
    CHARGING,
    DISCHARGING,
    FULL,
    NOT_CHARGING,
    UNKNOWN
}

enum class PluggedSource {
    NONE,
    AC,
    USB,
    WIRELESS
}

enum class BatteryHealthStatus {
    GOOD,
    OVERHEAT,
    DEAD,
    OVER_VOLTAGE,
    COLD,
    UNKNOWN
}

data class BatteryTelemetry(
    val levelPct: Int = 0,
    val status: BatteryChargeStatus = BatteryChargeStatus.UNKNOWN,
    val pluggedSource: PluggedSource = PluggedSource.NONE,
    val temperatureCelsius: Float? = null,
    val voltageMv: Int = 0,
    val currentNowMicroA: Int? = null,
    val health: BatteryHealthStatus = BatteryHealthStatus.UNKNOWN,
    val technology: String = "Inconnue",
    val timestampMs: Long = System.currentTimeMillis()
) {
    val isCharging: Boolean
        get() = status == BatteryChargeStatus.CHARGING || status == BatteryChargeStatus.FULL

    val isWirelessPlugged: Boolean
        get() = pluggedSource == PluggedSource.WIRELESS

    val isUsbPlugged: Boolean
        get() = pluggedSource == PluggedSource.USB
}
