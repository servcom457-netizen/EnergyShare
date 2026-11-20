package com.batteryshare.ai

import android.app.Application
import com.batteryshare.ai.data.local.BatteryShareDatabase
import com.batteryshare.ai.data.repository.BatteryShareRepository
import com.batteryshare.ai.hardware.battery.BatteryTelemetryMonitor
import com.batteryshare.ai.hardware.connectivity.PeerConnectionManager
import com.batteryshare.ai.hardware.protection.ProtectionMonitor
import com.batteryshare.ai.hardware.usb.UsbPowerController
import com.batteryshare.ai.hardware.wireless.WirelessPowerDetector

class AppContainer(application: Application) {
    private val database by lazy { BatteryShareDatabase.getInstance(application) }

    val repository by lazy {
        BatteryShareRepository(
            sessionDao = database.energySessionDao(),
            protectionDao = database.protectionConfigDao()
        )
    }

    val batteryMonitor by lazy { BatteryTelemetryMonitor(application) }
    val usbController by lazy { UsbPowerController(application) }
    val wirelessDetector by lazy { WirelessPowerDetector(application) }
    val protectionMonitor by lazy { ProtectionMonitor(application) }
    val peerConnectionManager by lazy { PeerConnectionManager(application) }
}

class BatteryShareApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
