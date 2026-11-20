package com.batteryshare.ai.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.batteryshare.ai.AppContainer
import com.batteryshare.ai.BatteryShareApplication
import com.batteryshare.ai.data.local.entity.EnergySessionEntity
import com.batteryshare.ai.data.local.entity.ProtectionConfigEntity
import com.batteryshare.ai.domain.model.BatteryTelemetry
import com.batteryshare.ai.domain.model.CapabilityAudit
import com.batteryshare.ai.domain.model.ProtectionSeverity
import com.batteryshare.ai.domain.model.ProtectionStopReason
import com.batteryshare.ai.domain.model.ProtectionVerdict
import com.batteryshare.ai.domain.model.TransferMode
import com.batteryshare.ai.domain.model.UsbPowerCapability
import com.batteryshare.ai.domain.model.WirelessReverseCapability
import com.batteryshare.ai.hardware.connectivity.DiscoveredPeer
import com.batteryshare.ai.hardware.connectivity.PeerConnectionState
import com.batteryshare.ai.hardware.permissions.PermissionHelper
import com.batteryshare.ai.hardware.service.EnergyMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActiveEnergySession(
    val entity: EnergySessionEntity,
    val mode: TransferMode,
    val startDonorBatteryPct: Int,
    val peakTempCelsius: Float?,
    val connectionAnomalyDetected: Boolean = false
)

data class BatteryShareUiState(
    val telemetry: BatteryTelemetry = BatteryTelemetry(),
    val usbCapability: UsbPowerCapability? = null,
    val wirelessCapability: WirelessReverseCapability? = null,
    val capabilityAudit: CapabilityAudit? = null,
    val protectionConfig: ProtectionConfigEntity = ProtectionConfigEntity(),
    val protectionVerdict: ProtectionVerdict = ProtectionVerdict(
        severity = ProtectionSeverity.NOMINAL,
        shouldStopSession = false,
        reason = ProtectionStopReason.NONE,
        alertMessage = null
    ),
    val peerState: PeerConnectionState = PeerConnectionState(),
    val activeSession: ActiveEnergySession? = null,
    val historySessions: List<EnergySessionEntity> = emptyList(),
    val missingPermissions: List<String> = emptyList(),
    val bannerMessage: String? = null
)

class BatteryShareViewModel(
    application: Application,
    private val container: AppContainer
) : AndroidViewModel(application) {

    private val _activeSession = MutableStateFlow<ActiveEnergySession?>(null)
    private val _missingPermissions = MutableStateFlow(PermissionHelper.missingPermissions(application))
    private val _bannerMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BatteryShareUiState> = combine(
        container.batteryMonitor.telemetry,
        container.usbController.usbCapability,
        container.repository.observeProtectionConfig(),
        container.peerConnectionManager.state,
        container.repository.observeSessions(),
        _activeSession,
        _missingPermissions,
        _bannerMessage
    ) { values ->
        val telemetry = values[0] as BatteryTelemetry
        val usbCap = values[1] as UsbPowerCapability
        val config = values[2] as ProtectionConfigEntity
        val peerState = values[3] as PeerConnectionState
        @Suppress("UNCHECKED_CAST")
        val history = values[4] as List<EnergySessionEntity>
        val activeSession = values[5] as ActiveEnergySession?
        @Suppress("UNCHECKED_CAST")
        val missingPerms = values[6] as List<String>
        val banner = values[7] as String?

        val wirelessCap = container.wirelessDetector.inspect(telemetry)
        val verdict = container.protectionMonitor.evaluateAndNotify(
            telemetry = telemetry,
            config = config,
            startDonorBatteryPct = activeSession?.startDonorBatteryPct,
            connectionAnomalyDetected = activeSession?.connectionAnomalyDetected == true
        )

        val audit = CapabilityAudit(
            operationalCapabilities = listOf(
                "Télémétrie batterie temps réel (niveau, état, température, tension)",
                "Protections thermiques, seuil minimal et arrêt de sécurité",
                "Persistance locale Room Database de l'historique et des règles",
                "Découverte Bluetooth & Wi-Fi Direct et synchronisation des états",
                "Détection matérielle USB Host / OTG et état de charge Qi"
            ),
            hardwareDependentCapabilities = listOf(
                "Négociation effective de puissance USB-C / USB-PD SOURCE ou SINK",
                "Activation matérielle de l'émission sans fil Qi inversée (paramètres OEM)",
                "Courant électrique réel (mA) selon le contrôleur PMIC du téléphone"
            ),
            usbCapability = usbCap,
            wirelessCapability = wirelessCap
        )

        BatteryShareUiState(
            telemetry = telemetry,
            usbCapability = usbCap,
            wirelessCapability = wirelessCap,
            capabilityAudit = audit,
            protectionConfig = config,
            protectionVerdict = verdict,
            peerState = peerState,
            activeSession = activeSession,
            historySessions = history,
            missingPermissions = missingPerms,
            bannerMessage = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BatteryShareUiState()
    )

    init {
        container.batteryMonitor.start()
        container.usbController.start()
        container.peerConnectionManager.initialize()

        viewModelScope.launch {
            container.repository.ensureDefaultProtectionConfig()
        }

        viewModelScope.launch {
            container.batteryMonitor.telemetry.collect { telemetry ->
                container.usbController.updateWithTelemetry(telemetry)
                handleLiveProtectionCheck(telemetry)
            }
        }
    }

    private fun handleLiveProtectionCheck(telemetry: BatteryTelemetry) {
        val currentSession = _activeSession.value ?: return
        val updatedPeakTemp = listOfNotNull(
            currentSession.peakTempCelsius,
            telemetry.temperatureCelsius
        ).maxOrNull()
        _activeSession.update { it?.copy(peakTempCelsius = updatedPeakTemp) }

        val config = uiState.value.protectionConfig
        val verdict = container.protectionMonitor.evaluateAndNotify(
            telemetry = telemetry,
            config = config,
            startDonorBatteryPct = currentSession.startDonorBatteryPct,
            connectionAnomalyDetected = currentSession.connectionAnomalyDetected
        )
        if (verdict.shouldStopSession) {
            stopActiveSession(verdict.reason.label)
        }
    }

    fun refreshPermissions() {
        _missingPermissions.value = PermissionHelper.missingPermissions(getApplication())
    }

    fun refreshHardwareSnapshot() {
        container.batteryMonitor.refreshNow()
        container.usbController.updateWithTelemetry(container.batteryMonitor.telemetry.value)
        container.peerConnectionManager.refreshAdapterState()
    }

    fun startMonitoringSession(mode: TransferMode) {
        val telemetry = uiState.value.telemetry
        val config = uiState.value.protectionConfig
        val peerName = uiState.value.peerState.selectedPeer?.name ?: "Appareil local / non appairé"

        val initialVerdict = container.protectionMonitor.evaluateAndNotify(
            telemetry = telemetry,
            config = config,
            startDonorBatteryPct = telemetry.levelPct,
            connectionAnomalyDetected = false
        )
        if (initialVerdict.shouldStopSession &&
            (mode == TransferMode.USB_OTG_SHARE || mode == TransferMode.WIRELESS_SHARE)
        ) {
            _bannerMessage.value = initialVerdict.alertMessage ?: "Seuil de protection atteint."
            return
        }

        viewModelScope.launch {
            val entity = EnergySessionEntity(
                mode = mode.label,
                peerDeviceName = peerName,
                startBatteryPct = telemetry.levelPct,
                endBatteryPct = telemetry.levelPct,
                peakTempCelsius = telemetry.temperatureCelsius,
                startedAtMs = System.currentTimeMillis(),
                endedAtMs = null,
                terminationReason = "Session active",
                hardwareTransferObserved = when (mode) {
                    TransferMode.USB_OTG_SHARE, TransferMode.USB_OTG_RECEIVE ->
                        uiState.value.usbCapability?.hardwareTransferGuaranteed == true
                    TransferMode.WIRELESS_RECEIVE -> telemetry.isWirelessPlugged
                    TransferMode.WIRELESS_SHARE -> false
                }
            )
            val id = container.repository.startSession(entity)
            _activeSession.value = ActiveEnergySession(
                entity = entity.copy(id = id),
                mode = mode,
                startDonorBatteryPct = telemetry.levelPct,
                peakTempCelsius = telemetry.temperatureCelsius
            )
            EnergyMonitorService.start(getApplication(), mode.label)
            _bannerMessage.value = "Suivi actif démarré : ${mode.label}."
        }
    }

    fun stopActiveSession(reason: String = ProtectionStopReason.USER_STOPPED.label) {
        val active = _activeSession.value ?: return
        val telemetry = uiState.value.telemetry
        val hardwareObserved = active.entity.hardwareTransferObserved ||
            telemetry.isCharging ||
            uiState.value.usbCapability?.hardwareTransferGuaranteed == true

        viewModelScope.launch {
            container.repository.finishSession(
                session = active.entity,
                endBatteryPct = telemetry.levelPct,
                peakTempCelsius = active.peakTempCelsius ?: telemetry.temperatureCelsius,
                terminationReason = reason,
                hardwareTransferObserved = hardwareObserved
            )
            _activeSession.value = null
            EnergyMonitorService.stop(getApplication())
            _bannerMessage.value = "Session arrêtée : $reason."
        }
    }

    fun updateProtectionConfig(
        minDonorBatteryPct: Int? = null,
        maxShareDeltaPct: Int? = null,
        maxTempCelsius: Float? = null,
        autoStopOnCritical: Boolean? = null,
        thermalAlertEnabled: Boolean? = null,
        anomalyAlertEnabled: Boolean? = null
    ) {
        val current = uiState.value.protectionConfig
        val updated = current.copy(
            minDonorBatteryPct = (minDonorBatteryPct ?: current.minDonorBatteryPct).coerceIn(10, 80),
            maxShareDeltaPct = (maxShareDeltaPct ?: current.maxShareDeltaPct).coerceIn(5, 70),
            maxTempCelsius = (maxTempCelsius ?: current.maxTempCelsius).coerceIn(35f, 48f),
            autoStopOnCritical = autoStopOnCritical ?: current.autoStopOnCritical,
            thermalAlertEnabled = thermalAlertEnabled ?: current.thermalAlertEnabled,
            anomalyAlertEnabled = anomalyAlertEnabled ?: current.anomalyAlertEnabled
        )
        viewModelScope.launch {
            container.repository.saveProtectionConfig(updated)
        }
    }

    fun startPeerDiscovery() {
        container.peerConnectionManager.startDiscovery(uiState.value.telemetry)
    }

    fun selectPeer(peer: DiscoveredPeer) {
        container.peerConnectionManager.selectAndRequestAuthorization(peer, uiState.value.telemetry)
    }

    fun approvePeerAuthorization() {
        container.peerConnectionManager.approveLocalAuthorization(uiState.value.telemetry)
    }

    fun rejectPeerAuthorization() {
        container.peerConnectionManager.rejectAuthorization()
    }

    fun buildOpenBatterySettingsIntent(): Intent =
        container.wirelessDetector.buildOpenBatterySettingsIntent()

    fun deleteHistorySession(id: Long) {
        viewModelScope.launch { container.repository.deleteSession(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { container.repository.clearHistory() }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        container.batteryMonitor.stop()
        container.usbController.stop()
        container.peerConnectionManager.release()
    }

    companion object {
        fun factory(app: BatteryShareApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return BatteryShareViewModel(app, app.container) as T
                }
            }
    }
}
