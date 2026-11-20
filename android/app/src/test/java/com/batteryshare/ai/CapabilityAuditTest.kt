package com.batteryshare.ai

import com.batteryshare.ai.domain.model.BatteryChargeStatus
import com.batteryshare.ai.domain.model.BatteryTelemetry
import com.batteryshare.ai.domain.model.PluggedSource
import com.batteryshare.ai.domain.model.UsbPowerCapability
import com.batteryshare.ai.domain.model.UsbPowerRole
import com.batteryshare.ai.domain.model.WirelessReverseCapability
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CapabilityAuditTest {

    @Test
    fun usbConnectionAlone_doesNotGuaranteeHardwareTransfer() {
        val capability = UsbPowerCapability(
            hasUsbHostFeature = true,
            hasUsbAccessoryFeature = false,
            isUsbDataOrDeviceConnected = true,
            connectedDevices = emptyList(),
            currentPowerRole = UsbPowerRole.NONE,
            canQueryPortRole = false,
            canOperateAsSinkViaBattery = true,
            canOperateAsSourceHardware = null,
            hardwareTransferGuaranteed = false,
            diagnosticNote = "Une liaison USB seule ne garantit pas un transfert électrique."
        )
        assertThat(capability.isUsbDataOrDeviceConnected).isTrue()
        assertThat(capability.hardwareTransferGuaranteed).isFalse()
    }

    @Test
    fun wirelessCapability_neverClaimsPublicApiReverseControl() {
        val telemetry = BatteryTelemetry(
            levelPct = 65,
            status = BatteryChargeStatus.DISCHARGING,
            pluggedSource = PluggedSource.NONE
        )
        val wireless = WirelessReverseCapability(
            isReceivingWirelessChargeNow = telemetry.isWirelessPlugged,
            deviceManufacturer = "Google",
            deviceModel = "Pixel",
            oemReverseShareSupportedHint = true,
            oemSettingsActionHint = "Paramètres > Batterie > Partage de batterie",
            hardwareControllableViaPublicApi = false,
            diagnosticNote = "Aucune API publique pour forcer l'émission Qi."
        )
        assertThat(wireless.isReceivingWirelessChargeNow).isFalse()
        assertThat(wireless.hardwareControllableViaPublicApi).isFalse()
    }
}
