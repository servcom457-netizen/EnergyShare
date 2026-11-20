package com.batteryshare.ai.domain.model

enum class UsbPowerRole {
    SOURCE,
    SINK,
    NONE,
    UNAVAILABLE
}

enum class TransferMode(val label: String) {
    USB_OTG_SHARE("Partage USB-C / OTG"),
    USB_OTG_RECEIVE("Réception USB-C / OTG"),
    WIRELESS_SHARE("Partage sans fil Qi"),
    WIRELESS_RECEIVE("Réception sans fil Qi")
}

data class UsbDeviceSummary(
    val deviceName: String,
    val productName: String?,
    val vendorId: Int,
    val productId: Int,
    val deviceClass: Int
)

data class UsbPowerCapability(
    val hasUsbHostFeature: Boolean,
    val hasUsbAccessoryFeature: Boolean,
    val isUsbDataOrDeviceConnected: Boolean,
    val connectedDevices: List<UsbDeviceSummary>,
    val currentPowerRole: UsbPowerRole,
    val canQueryPortRole: Boolean,
    val canOperateAsSinkViaBattery: Boolean,
    val canOperateAsSourceHardware: Boolean?,
    val hardwareTransferGuaranteed: Boolean = false,
    val diagnosticNote: String
)

data class WirelessReverseCapability(
    val isReceivingWirelessChargeNow: Boolean,
    val deviceManufacturer: String,
    val deviceModel: String,
    val oemReverseShareSupportedHint: Boolean,
    val oemSettingsActionHint: String?,
    val hardwareControllableViaPublicApi: Boolean = false,
    val diagnosticNote: String
)

data class CapabilityAudit(
    val operationalCapabilities: List<String>,
    val hardwareDependentCapabilities: List<String>,
    val usbCapability: UsbPowerCapability,
    val wirelessCapability: WirelessReverseCapability
)
