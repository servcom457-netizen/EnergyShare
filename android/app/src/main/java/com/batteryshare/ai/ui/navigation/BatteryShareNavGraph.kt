package com.batteryshare.ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.batteryshare.ai.ui.screens.ConnectionScreen
import com.batteryshare.ai.ui.screens.HistoryScreen
import com.batteryshare.ai.ui.screens.HomeScreen
import com.batteryshare.ai.ui.screens.ProtectionScreen
import com.batteryshare.ai.ui.screens.SettingsScreen
import com.batteryshare.ai.ui.screens.UsbOtgScreen
import com.batteryshare.ai.ui.screens.WirelessScreen
import com.batteryshare.ai.ui.viewmodel.BatteryShareUiState
import com.batteryshare.ai.ui.viewmodel.BatteryShareViewModel

sealed class BatteryShareRoute(val route: String) {
    data object Home : BatteryShareRoute("home")
    data object UsbOtg : BatteryShareRoute("usb_otg")
    data object Wireless : BatteryShareRoute("wireless")
    data object Protection : BatteryShareRoute("protection")
    data object Connection : BatteryShareRoute("connection")
    data object History : BatteryShareRoute("history")
    data object Settings : BatteryShareRoute("settings")
}

@Composable
fun BatteryShareNavGraph(
    navController: NavHostController,
    uiState: BatteryShareUiState,
    viewModel: BatteryShareViewModel,
    onRequestPermissions: () -> Unit,
    onOpenSystemBatterySettings: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = BatteryShareRoute.Home.route
    ) {
        composable(BatteryShareRoute.Home.route) {
            HomeScreen(
                uiState = uiState,
                onShareEnergy = { navController.navigate(BatteryShareRoute.UsbOtg.route) },
                onReceiveEnergy = { navController.navigate(BatteryShareRoute.Wireless.route) },
                onOpenConnection = { navController.navigate(BatteryShareRoute.Connection.route) },
                onOpenProtection = { navController.navigate(BatteryShareRoute.Protection.route) },
                onOpenHistory = { navController.navigate(BatteryShareRoute.History.route) },
                onOpenSettings = { navController.navigate(BatteryShareRoute.Settings.route) },
                onStopActiveSession = { viewModel.stopActiveSession() }
            )
        }
        composable(BatteryShareRoute.UsbOtg.route) {
            UsbOtgScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onRefresh = { viewModel.refreshHardwareSnapshot() },
                onStartMode = { mode -> viewModel.startMonitoringSession(mode) },
                onStopSession = { viewModel.stopActiveSession() },
                onOpenProtection = { navController.navigate(BatteryShareRoute.Protection.route) }
            )
        }
        composable(BatteryShareRoute.Wireless.route) {
            WirelessScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onOpenSystemBatterySettings = onOpenSystemBatterySettings,
                onStartMode = { mode -> viewModel.startMonitoringSession(mode) },
                onStopSession = { viewModel.stopActiveSession() }
            )
        }
        composable(BatteryShareRoute.Protection.route) {
            ProtectionScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onUpdateConfig = { minPct, maxDelta, maxTemp, autoStop, thermalAlert, anomalyAlert ->
                    viewModel.updateProtectionConfig(
                        minDonorBatteryPct = minPct,
                        maxShareDeltaPct = maxDelta,
                        maxTempCelsius = maxTemp,
                        autoStopOnCritical = autoStop,
                        thermalAlertEnabled = thermalAlert,
                        anomalyAlertEnabled = anomalyAlert
                    )
                },
                onEmergencyStop = { viewModel.stopActiveSession("Arrêt d'urgence protection") }
            )
        }
        composable(BatteryShareRoute.Connection.route) {
            ConnectionScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onRequestPermissions = onRequestPermissions,
                onStartScan = { viewModel.startPeerDiscovery() },
                onSelectPeer = { peer -> viewModel.selectPeer(peer) },
                onApproveAuth = { viewModel.approvePeerAuthorization() },
                onRejectAuth = { viewModel.rejectPeerAuthorization() }
            )
        }
        composable(BatteryShareRoute.History.route) {
            HistoryScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onDeleteSession = { id -> viewModel.deleteHistorySession(id) },
                onClearAll = { viewModel.clearHistory() }
            )
        }
        composable(BatteryShareRoute.Settings.route) {
            SettingsScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onRequestPermissions = onRequestPermissions,
                onOpenProtection = { navController.navigate(BatteryShareRoute.Protection.route) }
            )
        }
    }
}
