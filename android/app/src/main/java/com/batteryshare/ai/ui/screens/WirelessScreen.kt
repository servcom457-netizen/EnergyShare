package com.batteryshare.ai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.batteryshare.ai.domain.model.TransferMode
import com.batteryshare.ai.ui.theme.ActiveGreen
import com.batteryshare.ai.ui.theme.CriticalRed
import com.batteryshare.ai.ui.theme.ElectricBlue
import com.batteryshare.ai.ui.theme.ElectricBlueBright
import com.batteryshare.ai.ui.theme.MutedSlate
import com.batteryshare.ai.ui.theme.NightBlueBg
import com.batteryshare.ai.ui.theme.NightBlueBorder
import com.batteryshare.ai.ui.theme.NightBlueSurface
import com.batteryshare.ai.ui.viewmodel.BatteryShareUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WirelessScreen(
    uiState: BatteryShareUiState,
    onBack: () -> Unit,
    onOpenSystemBatterySettings: () -> Unit,
    onStartMode: (TransferMode) -> Unit,
    onStopSession: () -> Unit
) {
    val wireless = uiState.wirelessCapability
    val telemetry = uiState.telemetry

    Scaffold(
        containerColor = NightBlueBg,
        topBar = {
            TopAppBar(
                title = { Text("Recharge sans fil inversée (Qi)", color = Color.White) },
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
                border = BorderStroke(1.dp, NightBlueBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row {
                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = ActiveGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("État matériel sans fil réel", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    Text(
                        text = "Appareil : ${wireless?.deviceManufacturer ?: ""} ${wireless?.deviceModel ?: ""}",
                        color = Color.White
                    )
                    Text(
                        text = "Réception Qi active : ${if (telemetry.isWirelessPlugged) "OUI (charge sans fil détectée)" else "NON"}",
                        color = if (telemetry.isWirelessPlugged) ActiveGreen else MutedSlate
                    )
                    Text(
                        text = "Batterie : ${telemetry.levelPct}% · Température : ${telemetry.temperatureCelsius ?: "--"} °C",
                        color = ElectricBlueBright
                    )
                    Text(
                        text = wireless?.diagnosticNote ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSlate
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
                    Text("Guide d'alignement et activation système", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("1. Retirez les coques épaisses ou métalliques des deux téléphones.", color = MutedSlate)
                    Text("2. Activez le partage d'énergie sans fil dans : ${wireless?.oemSettingsActionHint ?: "Paramètres Batterie"}", color = MutedSlate)
                    Text("3. Placez les deux appareils dos à dos, centrés sur le tiers central (bobine Qi).", color = MutedSlate)
                    Text("4. BatteryShare AI ne simule jamais une recharge sans fil : seul l'état réel rapporté par Android est affiché.", color = Color.White)
                }
            }

            OutlinedButton(
                onClick = onOpenSystemBatterySettings,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                border = BorderStroke(1.dp, ElectricBlueBright)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = ElectricBlueBright)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ouvrir les paramètres batterie système", color = Color.White)
            }

            if (uiState.activeSession == null) {
                Button(
                    onClick = { onStartMode(TransferMode.WIRELESS_RECEIVE) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ActiveGreen)
                ) {
                    Text("SURVEILLER RÉCEPTION SANS FIL", color = NightBlueBg)
                }
                Button(
                    onClick = { onStartMode(TransferMode.WIRELESS_SHARE) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("SURVEILLER PARTAGE SANS FIL (DONNEUR)")
                }
            } else {
                Button(
                    onClick = onStopSession,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CriticalRed)
                ) {
                    Text("ARRÊTER LA SURVEILLANCE")
                }
            }
        }
    }
}
