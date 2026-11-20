package com.batteryshare.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batteryshare.ai.domain.model.BatteryChargeStatus
import com.batteryshare.ai.ui.components.BatteryShareLogo
import com.batteryshare.ai.ui.theme.ActiveGreen
import com.batteryshare.ai.ui.theme.CriticalRed
import com.batteryshare.ai.ui.theme.ElectricBlue
import com.batteryshare.ai.ui.theme.ElectricBlueBright
import com.batteryshare.ai.ui.theme.MutedSlate
import com.batteryshare.ai.ui.theme.NightBlueBg
import com.batteryshare.ai.ui.theme.NightBlueBorder
import com.batteryshare.ai.ui.theme.NightBlueSurface
import com.batteryshare.ai.ui.viewmodel.BatteryShareUiState

@Composable
fun HomeScreen(
    uiState: BatteryShareUiState,
    onShareEnergy: () -> Unit,
    onReceiveEnergy: () -> Unit,
    onOpenConnection: () -> Unit,
    onOpenProtection: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onStopActiveSession: () -> Unit
) {
    val telemetry = uiState.telemetry
    val progress by animateFloatAsState(
        targetValue = (telemetry.levelPct / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450),
        label = "battery_progress"
    )

    val statusText = when (telemetry.status) {
        BatteryChargeStatus.CHARGING -> "En charge (${telemetry.pluggedSource.name})"
        BatteryChargeStatus.DISCHARGING -> "En décharge"
        BatteryChargeStatus.FULL -> "Batterie pleine"
        BatteryChargeStatus.NOT_CHARGING -> "Non en charge"
        BatteryChargeStatus.UNKNOWN -> "État indisponible"
    }

    val compatibilitySummary = buildString {
        val otg = if (uiState.usbCapability?.hasUsbHostFeature == true) "USB-OTG prêt" else "OTG non déclaré"
        val qi = if (uiState.wirelessCapability?.isReceivingWirelessChargeNow == true) {
            "Qi actif"
        } else if (uiState.wirelessCapability?.oemReverseShareSupportedHint == true) {
            "Qi inversé via paramètres OEM"
        } else {
            "Qi à vérifier"
        }
        append("$otg · $qi")
    }

    Scaffold(containerColor = NightBlueBg) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            val isWide = maxWidth >= 680.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BatteryShareLogo(
                            size = 44.dp,
                            isActiveTransfer = uiState.activeSession != null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "BatteryShare AI",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "Version 1.0 · Gestion énergétique",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSlate
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = uiState.activeSession != null) {
                    val active = uiState.activeSession
                    if (active != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                            border = BorderStroke(1.dp, ActiveGreen),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Opération active : ${active.mode.label}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Départ ${active.startDonorBatteryPct}% · Actuel ${telemetry.levelPct}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFD1FAE5)
                                    )
                                }
                                Button(
                                    onClick = onStopActiveSession,
                                    colors = ButtonDefaults.buttonColors(containerColor = CriticalRed)
                                ) {
                                    Text("ARRÊTER")
                                }
                            }
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = NightBlueSurface),
                    border = BorderStroke(1.dp, NightBlueBorder),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "NIVEAU ACTUEL",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MutedSlate
                                )
                                Text(
                                    text = "${telemetry.levelPct}%",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.activeSession != null) ActiveGreen else Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = telemetry.temperatureCelsius?.let { String.format("%.1f °C", it) }
                                        ?: "Temp. indisponible",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ElectricBlueBright
                                )
                                Text(
                                    text = "${telemetry.voltageMv} mV · ${telemetry.technology}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedSlate
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = if (uiState.activeSession != null) ActiveGreen else ElectricBlueBright,
                            trackColor = NightBlueBorder
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "État : $statusText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = compatibilitySummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSlate
                            )
                        }
                    }
                }

                val primaryActions: @Composable () -> Unit = {
                    Button(
                        onClick = onShareEnergy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("PARTAGER MON ÉNERGIE", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onReceiveEnergy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ActiveGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = NightBlueBg)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("RECEVOIR DE L'ÉNERGIE", color = NightBlueBg, fontWeight = FontWeight.Bold)
                    }
                }

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onShareEnergy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("PARTAGER MON ÉNERGIE", fontWeight = FontWeight.Bold)
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onReceiveEnergy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ActiveGreen),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = NightBlueBg)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("RECEVOIR DE L'ÉNERGIE", color = NightBlueBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    primaryActions()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenConnection,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        border = BorderStroke(1.dp, NightBlueBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.BluetoothConnected, contentDescription = null, tint = ElectricBlueBright)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CONNEXION", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = onOpenProtection,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        border = BorderStroke(1.dp, NightBlueBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = ActiveGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PROTECTION", color = Color.White)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenHistory,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        border = BorderStroke(1.dp, NightBlueBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("HISTORIQUE", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        border = BorderStroke(1.dp, NightBlueBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PARAMÈTRES", color = Color.White)
                    }
                }
            }
        }
    }
}
