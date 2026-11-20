package com.batteryshare.ai.domain.model

enum class ProtectionSeverity {
    NOMINAL,
    WARNING,
    CRITICAL_STOP
}

enum class ProtectionStopReason(val label: String) {
    NONE("Fonctionnement nominal"),
    MIN_BATTERY_REACHED("Seuil minimal donneur atteint"),
    SHARE_LIMIT_REACHED("Limite de partage atteinte"),
    OVERHEAT_DETECTED("Température excessive détectée"),
    CRITICAL_BATTERY_HEALTH("État batterie critique signalé"),
    ABNORMAL_CONNECTION("Anomalie de connexion détectée"),
    USER_STOPPED("Arrêt manuel par l'utilisateur")
}

data class ProtectionVerdict(
    val severity: ProtectionSeverity,
    val shouldStopSession: Boolean,
    val reason: ProtectionStopReason,
    val alertMessage: String?
)

object ProtectionEvaluator {
    const val THERMAL_WARNING_DELTA_C = 3.0f
    const val ABSOLUTE_CRITICAL_BATTERY_PCT = 10

    fun evaluate(
        telemetry: BatteryTelemetry,
        startDonorBatteryPct: Int?,
        minDonorBatteryPct: Int,
        maxShareDeltaPct: Int,
        maxTempCelsius: Float,
        autoStopOnCritical: Boolean,
        connectionAnomalyDetected: Boolean
    ): ProtectionVerdict {
        if (connectionAnomalyDetected && autoStopOnCritical) {
            return ProtectionVerdict(
                severity = ProtectionSeverity.CRITICAL_STOP,
                shouldStopSession = true,
                reason = ProtectionStopReason.ABNORMAL_CONNECTION,
                alertMessage = "Connexion électrique ou USB instable détectée : arrêt de sécurité."
            )
        }

        if (telemetry.health == BatteryHealthStatus.OVERHEAT ||
            (telemetry.temperatureCelsius != null && telemetry.temperatureCelsius >= maxTempCelsius)
        ) {
            return ProtectionVerdict(
                severity = ProtectionSeverity.CRITICAL_STOP,
                shouldStopSession = autoStopOnCritical,
                reason = ProtectionStopReason.OVERHEAT_DETECTED,
                alertMessage = "Température excessive (${telemetry.temperatureCelsius ?: "--"} °C >= $maxTempCelsius °C)."
            )
        }

        if (telemetry.health == BatteryHealthStatus.OVER_VOLTAGE ||
            telemetry.health == BatteryHealthStatus.DEAD
        ) {
            return ProtectionVerdict(
                severity = ProtectionSeverity.CRITICAL_STOP,
                shouldStopSession = autoStopOnCritical,
                reason = ProtectionStopReason.CRITICAL_BATTERY_HEALTH,
                alertMessage = "Santé batterie critique (${telemetry.health.name})."
            )
        }

        if (telemetry.levelPct <= ABSOLUTE_CRITICAL_BATTERY_PCT ||
            telemetry.levelPct <= minDonorBatteryPct
        ) {
            return ProtectionVerdict(
                severity = ProtectionSeverity.CRITICAL_STOP,
                shouldStopSession = autoStopOnCritical,
                reason = ProtectionStopReason.MIN_BATTERY_REACHED,
                alertMessage = "Niveau batterie (${telemetry.levelPct}%) au seuil minimal ($minDonorBatteryPct%)."
            )
        }

        if (startDonorBatteryPct != null) {
            val sharedDelta = (startDonorBatteryPct - telemetry.levelPct).coerceAtLeast(0)
            if (sharedDelta >= maxShareDeltaPct) {
                return ProtectionVerdict(
                    severity = ProtectionSeverity.CRITICAL_STOP,
                    shouldStopSession = autoStopOnCritical,
                    reason = ProtectionStopReason.SHARE_LIMIT_REACHED,
                    alertMessage = "Limite de partage atteinte ($sharedDelta% partagés / max $maxShareDeltaPct%)."
                )
            }
        }

        if (telemetry.temperatureCelsius != null &&
            telemetry.temperatureCelsius >= (maxTempCelsius - THERMAL_WARNING_DELTA_C)
        ) {
            return ProtectionVerdict(
                severity = ProtectionSeverity.WARNING,
                shouldStopSession = false,
                reason = ProtectionStopReason.NONE,
                alertMessage = "Température élevée (${telemetry.temperatureCelsius} °C) : surveillance rapprochée."
            )
        }

        return ProtectionVerdict(
            severity = ProtectionSeverity.NOMINAL,
            shouldStopSession = false,
            reason = ProtectionStopReason.NONE,
            alertMessage = null
        )
    }
}
