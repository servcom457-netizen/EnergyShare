package com.batteryshare.ai.hardware.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbPortStatus
import android.os.Build
import com.batteryshare.ai.domain.model.BatteryTelemetry
import com.batteryshare.ai.domain.model.PluggedSource
import com.batteryshare.ai.domain.model.UsbDeviceSummary
import com.batteryshare.ai.domain.model.UsbPowerCapability
import com.batteryshare.ai.domain.model.UsbPowerRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UsbPowerController(private val context: Context) {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
    private val packageManager = context.packageManager

    private val _usbCapability = MutableStateFlow(inspectCapability(BatteryTelemetry()))
    val usbCapability: StateFlow<UsbPowerCapability> = _usbCapability.asStateFlow()

    private var registered = false
    private var lastTelemetry = BatteryTelemetry()

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED,
                UsbManager.ACTION_USB_DEVICE_DETACHED,
                UsbManager.ACTION_USB_ACCESSORY_ATTACHED,
                UsbManager.ACTION_USB_ACCESSORY_DETACHED,
                "android.hardware.usb.action.USB_STATE" -> {
                    _usbCapability.value = inspectCapability(lastTelemetry)
                }
            }
        }
    }

    fun start() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
            addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
            addAction("android.hardware.usb.action.USB_STATE")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(usbReceiver, filter)
        }
        _usbCapability.value = inspectCapability(lastTelemetry)
        registered = true
    }

    fun stop() {
        if (!registered) return
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (_: IllegalArgumentException) {
            // Déjà désinscrit
        } finally {
            registered = false
        }
    }

    fun updateWithTelemetry(telemetry: BatteryTelemetry) {
        lastTelemetry = telemetry
        _usbCapability.value = inspectCapability(telemetry)
    }

    fun inspectCapability(telemetry: BatteryTelemetry): UsbPowerCapability {
        val hasUsbHost = packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        val hasUsbAccessory = packageManager.hasSystemFeature(PackageManager.FEATURE_USB_ACCESSORY)

        val deviceMap: Map<String, UsbDevice> = usbManager?.deviceList ?: emptyMap()
        val deviceSummaries = deviceMap.values.map { device ->
            UsbDeviceSummary(
                deviceName = device.deviceName,
                productName = device.productName,
                vendorId = device.vendorId,
                productId = device.productId,
                deviceClass = device.deviceClass
            )
        }

        val (portRole, canQueryPortRole, canSourceByPort) = queryUsbPortRoleIfPermitted(telemetry)

        val isConnected = deviceSummaries.isNotEmpty() || telemetry.pluggedSource == PluggedSource.USB
        val canSink = telemetry.pluggedSource == PluggedSource.USB || hasUsbHost || hasUsbAccessory

        // IMPORTANT : Une simple connexion USB ou présence d'un périphérique OTG
        // ne garantit JAMAIS un transfert électrique réel entre deux téléphones.
        val hardwareGuaranteed = (portRole == UsbPowerRole.SOURCE || portRole == UsbPowerRole.SINK) &&
            (telemetry.isCharging || telemetry.currentNowMicroA != null)

        val diagnostic = buildString {
            when {
                !hasUsbHost -> append("Le matériel ne déclare pas la fonctionnalité USB Host (OTG). ")
                portRole == UsbPowerRole.SOURCE -> append("Port USB-C détecté en rôle SOURCE (alimentation sortante active). ")
                portRole == UsbPowerRole.SINK -> append("Port USB-C détecté en rôle SINK (réception d'énergie). ")
                deviceSummaries.isNotEmpty() -> append("Périphérique USB détecté en mode hôte, mais le transfert électrique dépend de la négociation USB-PD/OTG du contrôleur PMIC. ")
                else -> append("Aucun câble USB-C/OTG actif détecté pour le moment. ")
            }
            append("Une liaison de données USB seule ne garantit pas un transfert de puissance électrique.")
        }

        return UsbPowerCapability(
            hasUsbHostFeature = hasUsbHost,
            hasUsbAccessoryFeature = hasUsbAccessory,
            isUsbDataOrDeviceConnected = isConnected,
            connectedDevices = deviceSummaries,
            currentPowerRole = portRole,
            canQueryPortRole = canQueryPortRole,
            canOperateAsSinkViaBattery = canSink,
            canOperateAsSourceHardware = canSourceByPort,
            hardwareTransferGuaranteed = hardwareGuaranteed,
            diagnosticNote = diagnostic
        )
    }

    private fun queryUsbPortRoleIfPermitted(telemetry: BatteryTelemetry): Triple<UsbPowerRole, Boolean, Boolean?> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || usbManager == null) {
            val fallbackRole = if (telemetry.pluggedSource == PluggedSource.USB) {
                UsbPowerRole.SINK
            } else {
                UsbPowerRole.UNAVAILABLE
            }
            return Triple(fallbackRole, false, null)
        }

        return try {
            val ports = usbManager.ports
            var detectedRole = UsbPowerRole.NONE
            var supportsSource: Boolean? = null

            for (port in ports) {
                val status = port.status ?: continue
                if (status.isConnected) {
                    detectedRole = when (status.currentPowerRole) {
                        UsbPortStatus.POWER_ROLE_SOURCE -> UsbPowerRole.SOURCE
                        UsbPortStatus.POWER_ROLE_SINK -> UsbPowerRole.SINK
                        else -> UsbPowerRole.NONE
                    }
                    supportsSource = status.isRoleCombinationSupported(
                        UsbPortStatus.POWER_ROLE_SOURCE,
                        UsbPortStatus.DATA_ROLE_HOST
                    )
                    break
                }
            }

            if (detectedRole == UsbPowerRole.NONE && telemetry.pluggedSource == PluggedSource.USB) {
                detectedRole = UsbPowerRole.SINK
            }
            Triple(detectedRole, true, supportsSource)
        } catch (_: SecurityException) {
            // Android 12+ réserve parfois UsbPort.getStatus() aux apps système MANAGE_USB
            val fallbackRole = if (telemetry.pluggedSource == PluggedSource.USB) {
                UsbPowerRole.SINK
            } else {
                UsbPowerRole.UNAVAILABLE
            }
            Triple(fallbackRole, false, null)
        }
    }
}
