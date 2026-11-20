package com.batteryshare.ai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.batteryshare.ai.domain.model.ProtectionSeverity
import com.batteryshare.ai.ui.theme.ActiveGreen
import com.batteryshare.ai.ui.theme.CriticalRed
import com.batteryshare.ai.ui.theme.ElectricBlueBright
import com.batteryshare.ai.ui.theme.MutedSlate
import com.batteryshare.ai.ui.theme.NightBlueBg
import com.batteryshare.ai.ui.theme.NightBlueBorder
import com.batteryshare.ai.ui.theme.NightBlueSurface
import com.batteryshare.ai.ui.theme.WarningAmber
import com.batteryshare.ai.ui.viewmodel.BatteryShareUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectionScreen(
    uiState: BatteryShareUiState,
    onBack: () -> Unit,
    onUpdateConfig: (Int?, Int?, Float?, Boolean?, Boolean?, Boolean?) -> Unit,
    onEmergencyStop: () -> Unit
) {
    val config = uiState.protectionConfig
    val verdict = uiState.protectionVerdict

    val statusColor = when (verdict.severity) {
        ProtectionSeverity.NOMINAL -> ActiveGreen
        ProtectionSeverity.WARNING -> WarningAmber
        ProtectionSeverity.CRITICAL_STOP -> CriticalRed
    }

    Scaffold(
        containerColor = NightBlueBg,
        topBar = {
            TopAppBar(
                title = { Text("Protection énergétique", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NightBlueBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NightBlueSurface),
                border = BorderStroke(1.dp, statusColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Verdict sécurité : ${verdict.reason.label}",
                        style = MaterialTheme.typography.titleMedium,
                        color = statusColor
                    )
                    Text(
                        text = verdict.alertMessage ?: "Température, santé batterie et seuils donneur dans la plage nominale.",
                        color = Color.White
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = NightBlueSurface),
                border = BorderStroke(1.dp, NightBlueBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Seuil minimal de batterie donneur : ${config.minDonorBatteryPct}%", color = Color.White)
                    Slider(
                        value = config.minDonorBatteryPct.toFloat(),
                        onValueChange = { onUpdateConfig(it.roundToInt(), null, null, null, null, null) },
                        valueRange = 15f..70f,
                        colors = SliderDefaults.colors(thumbColor = ElectricBlueBright, activeTrackColor = ElectricBlueBright)
                    )

                    Text("Limite maximale de partage par session : ${config.maxShareDeltaPct}%", color = Color.White)
                    Slider(
                        value = config.maxShareDeltaPct.toFloat(),
                        onValueChange = { onUpdateConfig(null, it.roundToInt(), null, null, null, null) },
                        valueRange = 5f..50f,
                        colors = SliderDefaults.colors(thumbColor = ActiveGreen, activeTrackColor = ActiveGreen)
                    )

                    Text("Plafond thermique de sécurité : ${String.format("%.1f", config.maxTempCelsius)} °C", color = Color.White)
                    Slider(
                        value = config.maxTempCelsius,
                        onValueChange = { onUpdateConfig(null, null, (it * 2).roundToInt() / 2f, null, null, null) },
                        valueRange = 36f..46f,
                        colors = SliderDefaults.colors(thumbColor = WarningAmber, activeTrackColor = WarningAmber)
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = NightBlueSurface),
                border = BorderStroke(1.dp, NightBlueBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Arrêt automatique si seuil critique", color = Color.White)
                        Switch(
                            checked = config.autoStopOnCritical,
                            onCheckedChange = { onUpdateConfig(null, null, null, it, null, null) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Alertes thermiques", color = Color.White)
                        Switch(
                            checked = config.thermalAlertEnabled,
                            onCheckedChange = { onUpdateConfig(null, null, null, null, it, null) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Alertes anomalies de connexion", color = Color.White)
                        Switch(
                            checked = config.anomalyAlertEnabled,
                            onCheckedChange = { onUpdateConfig(null, null, null, null, null, it) }
                        )
                    }
                }
            }

            if (uiState.activeSession != null) {
                Button(
                    onClick = onEmergencyStop,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CriticalRed)
                ) {
                    Text("ARRÊTER LE PARTAGE IMMÉDIATEMENT")
                }
            } else {
                Text(
                    text = "Aucune session active en cours. Les règles sont sauvegardées dans Room Database.",
                    color = MutedSlate
                )
            }
        }
    }
}
