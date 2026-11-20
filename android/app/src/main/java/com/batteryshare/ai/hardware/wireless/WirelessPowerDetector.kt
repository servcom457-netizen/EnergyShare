package com.batteryshare.ai.hardware.wireless

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.batteryshare.ai.domain.model.BatteryTelemetry
import com.batteryshare.ai.domain.model.WirelessReverseCapability

class WirelessPowerDetector(private val context: Context) {

    private val oemReverseShareManufacturers = setOf(
        "samsung",
        "google",
        "xiaomi",
        "huawei",
        "oneplus",
        "oppo",
        "honor",
        "sony"
    )

    fun inspect(telemetry: BatteryTelemetry): WirelessReverseCapability {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty()
        val normalizedMfg = manufacturer.lowercase()
        val isOemKnownForReverseQi = oemReverseShareManufacturers.any { normalizedMfg.contains(it) }

        val settingsHint = when {
            normalizedMfg.contains("samsung") ->
                "Paramètres > Batterie > Partage d'énergie sans fil"
            normalizedMfg.contains("google") ->
                "Paramètres > Batterie > Partage de batterie"
            normalizedMfg.contains("xiaomi") ->
                "Paramètres > Batterie > Recharge sans fil inversée"
            normalizedMfg.contains("oneplus") || normalizedMfg.contains("oppo") ->
                "Paramètres > Batterie > Recharge sans fil inversée"
            else ->
                "Paramètres > Batterie > Partage d'énergie / Recharge inversée (si disponible)"
        }

        // L'API publique Android ne fournit aucun commutateur universel pour activer
        // la bobine Qi en émission : seule la réception (BATTERY_PLUGGED_WIRELESS) est observable.
        val note = if (telemetry.isWirelessPlugged) {
            "Réception sans fil Qi active détectée par le contrôleur batterie Android."
        } else {
            "Aucune API Android publique ne permet d'activer ou de simuler l'émission Qi inversée par logiciel. " +
                "L'activation matérielle doit être effectuée dans les paramètres constructeur si le modèle embarque une bobine bidirectionnelle."
        }

        return WirelessReverseCapability(
            isReceivingWirelessChargeNow = telemetry.isWirelessPlugged,
            deviceManufacturer = manufacturer.ifBlank { "Android" },
            deviceModel = model.ifBlank { "Appareil" },
            oemReverseShareSupportedHint = isOemKnownForReverseQi,
            oemSettingsActionHint = settingsHint,
            hardwareControllableViaPublicApi = false,
            diagnosticNote = note
        )
    }

    fun buildOpenBatterySettingsIntent(): Intent {
        val pm = context.packageManager
        val batteryUsageIntent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (batteryUsageIntent.resolveActivity(pm) != null) {
            return batteryUsageIntent
        }
        return Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
