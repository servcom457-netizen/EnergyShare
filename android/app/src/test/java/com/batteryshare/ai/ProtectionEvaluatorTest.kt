package com.batteryshare.ai

import com.batteryshare.ai.domain.model.BatteryChargeStatus
import com.batteryshare.ai.domain.model.BatteryHealthStatus
import com.batteryshare.ai.domain.model.BatteryTelemetry
import com.batteryshare.ai.domain.model.PluggedSource
import com.batteryshare.ai.domain.model.ProtectionEvaluator
import com.batteryshare.ai.domain.model.ProtectionSeverity
import com.batteryshare.ai.domain.model.ProtectionStopReason
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProtectionEvaluatorTest {

    private fun baseTelemetry(
        levelPct: Int = 75,
        tempC: Float? = 32.0f,
        health: BatteryHealthStatus = BatteryHealthStatus.GOOD
    ) = BatteryTelemetry(
        levelPct = levelPct,
        status = BatteryChargeStatus.DISCHARGING,
        pluggedSource = PluggedSource.NONE,
        temperatureCelsius = tempC,
        voltageMv = 3950,
        currentNowMicroA = -320_000,
        health = health,
        technology = "Li-ion"
    )

    @Test
    fun nominalConditions_returnNominalVerdict() {
        val verdict = ProtectionEvaluator.evaluate(
            telemetry = baseTelemetry(levelPct = 80, tempC = 31.5f),
            startDonorBatteryPct = 85,
            minDonorBatteryPct = 25,
            maxShareDeltaPct = 20,
            maxTempCelsius = 42.0f,
            autoStopOnCritical = true,
            connectionAnomalyDetected = false
        )
        assertThat(verdict.severity).isEqualTo(ProtectionSeverity.NOMINAL)
        assertThat(verdict.shouldStopSession).isFalse()
    }

    @Test
    fun minBatteryReached_triggersCriticalStop() {
        val verdict = ProtectionEvaluator.evaluate(
            telemetry = baseTelemetry(levelPct = 25),
            startDonorBatteryPct = 60,
            minDonorBatteryPct = 25,
            maxShareDeltaPct = 40,
            maxTempCelsius = 42.0f,
            autoStopOnCritical = true,
            connectionAnomalyDetected = false
        )
        assertThat(verdict.severity).isEqualTo(ProtectionSeverity.CRITICAL_STOP)
        assertThat(verdict.shouldStopSession).isTrue()
        assertThat(verdict.reason).isEqualTo(ProtectionStopReason.MIN_BATTERY_REACHED)
    }

    @Test
    fun shareLimitReached_triggersCriticalStop() {
        val verdict = ProtectionEvaluator.evaluate(
            telemetry = baseTelemetry(levelPct = 60),
            startDonorBatteryPct = 80,
            minDonorBatteryPct = 20,
            maxShareDeltaPct = 20,
            maxTempCelsius = 42.0f,
            autoStopOnCritical = true,
            connectionAnomalyDetected = false
        )
        assertThat(verdict.severity).isEqualTo(ProtectionSeverity.CRITICAL_STOP)
        assertThat(verdict.shouldStopSession).isTrue()
        assertThat(verdict.reason).isEqualTo(ProtectionStopReason.SHARE_LIMIT_REACHED)
    }

    @Test
    fun overheat_triggersCriticalStop() {
        val verdict = ProtectionEvaluator.evaluate(
            telemetry = baseTelemetry(levelPct = 70, tempC = 43.2f),
            startDonorBatteryPct = 75,
            minDonorBatteryPct = 20,
            maxShareDeltaPct = 20,
            maxTempCelsius = 42.0f,
            autoStopOnCritical = true,
            connectionAnomalyDetected = false
        )
        assertThat(verdict.severity).isEqualTo(ProtectionSeverity.CRITICAL_STOP)
        assertThat(verdict.shouldStopSession).isTrue()
        assertThat(verdict.reason).isEqualTo(ProtectionStopReason.OVERHEAT_DETECTED)
    }

    @Test
    fun connectionAnomaly_triggersStop() {
        val verdict = ProtectionEvaluator.evaluate(
            telemetry = baseTelemetry(levelPct = 70),
            startDonorBatteryPct = 72,
            minDonorBatteryPct = 20,
            maxShareDeltaPct = 20,
            maxTempCelsius = 42.0f,
            autoStopOnCritical = true,
            connectionAnomalyDetected = true
        )
        assertThat(verdict.shouldStopSession).isTrue()
        assertThat(verdict.reason).isEqualTo(ProtectionStopReason.ABNORMAL_CONNECTION)
    }
}
