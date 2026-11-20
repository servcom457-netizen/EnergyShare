package com.batteryshare.ai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.batteryshare.ai.ui.theme.ActiveGreen
import com.batteryshare.ai.ui.theme.CriticalRed
import com.batteryshare.ai.ui.theme.ElectricBlueBright
import com.batteryshare.ai.ui.theme.MutedSlate
import com.batteryshare.ai.ui.theme.NightBlueBg
import com.batteryshare.ai.ui.theme.NightBlueBorder
import com.batteryshare.ai.ui.theme.NightBlueSurface
import com.batteryshare.ai.ui.viewmodel.BatteryShareUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: BatteryShareUiState,
    onBack: () -> Unit,
    onDeleteSession: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

    Scaffold(
        containerColor = NightBlueBg,
        topBar = {
            TopAppBar(
                title = { Text("Historique Room Database", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                actions = {
                    if (uiState.historySessions.isNotEmpty()) {
                        TextButton(onClick = onClearAll) {
                            Text("Tout effacer", color = CriticalRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NightBlueBg)
            )
        }
    ) { padding ->
        if (uiState.historySessions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Aucune session enregistrée", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(
                    text = "Les sessions USB-C/OTG et sans fil surveillées apparaîtront ici.",
                    color = MutedSlate
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.historySessions, key = { it.id }) { session ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NightBlueSurface),
                        border = BorderStroke(1.dp, NightBlueBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(session.mode, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text(
                                    text = "${formatter.format(Date(session.startedAtMs))} · ${session.peerDeviceName}",
                                    color = MutedSlate
                                )
                                Text(
                                    text = "Batterie : ${session.startBatteryPct}% -> ${session.endBatteryPct}% · Pic : ${session.peakTempCelsius ?: "--"} °C",
                                    color = ElectricBlueBright
                                )
                                Text(
                                    text = "${session.terminationReason} · ${
                                        if (session.hardwareTransferObserved) "Flux matériel confirmé" else "Suivi logiciel seul"
                                    }",
                                    color = if (session.hardwareTransferObserved) ActiveGreen else MutedSlate
                                )
                            }
                            IconButton(onClick = { onDeleteSession(session.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = CriticalRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
