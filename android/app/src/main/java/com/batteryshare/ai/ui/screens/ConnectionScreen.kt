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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.batteryshare.ai.hardware.connectivity.AuthorizationState
import com.batteryshare.ai.hardware.connectivity.DiscoveredPeer
import com.batteryshare.ai.hardware.connectivity.PeerTransport
import com.batteryshare.ai.ui.theme.ActiveGreen
import com.batteryshare.ai.ui.theme.CriticalRed
import com.batteryshare.ai.ui.theme.ElectricBlue
import com.batteryshare.ai.ui.theme.ElectricBlueBright
import com.batteryshare.ai.ui.theme.MutedSlate
import com.batteryshare.ai.ui.theme.NightBlueBg
import com.batteryshare.ai.ui.theme.NightBlueBorder
import com.batteryshare.ai.ui.theme.NightBlueSurface
import com.batteryshare.ai.ui.theme.WarningAmber
import com.batteryshare.ai.ui.viewmodel.BatteryShareUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    uiState: BatteryShareUiState,
    onBack: () -> Unit,
    onRequestPermissions: () -> Unit,
    onStartScan: () -> Unit,
    onSelectPeer: (DiscoveredPeer) -> Unit,
    onApproveAuth: () -> Unit,
    onRejectAuth: () -> Unit
) {
    val peerState = uiState.peerState

    Scaffold(
        containerColor = NightBlueBg,
        topBar = {
            TopAppBar(
                title = { Text("Connexion Bluetooth & Wi-Fi Direct", color = Color.White) },
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1D3D)),
                border = BorderStroke(1.dp, ElectricBlueBright),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Rôle réseau strictement informatif",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricBlueBright
                    )
                    Text(
                        text = "Bluetooth et Wi-Fi Direct servent exclusivement à découvrir les téléphones, demander l'autorisation mutuelle et synchroniser les niveaux de batterie. Aucune énergie électrique n'est transmise par liaison réseau.",
                        color = Color.White
                    )
                }
            }

            if (uiState.missingPermissions.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1607)),
                    border = BorderStroke(1.dp, WarningAmber),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Autorisations requises (${uiState.missingPermissions.size})", color = WarningAmber)
                        Button(
                            onClick = onRequestPermissions,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Text("Accorder les permissions Bluetooth / Wi-Fi")
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = NightBlueSurface),
                border = BorderStroke(1.dp, NightBlueBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Adaptateurs détectés", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("Bluetooth : ${if (peerState.bluetoothSupported) (if (peerState.bluetoothEnabled) "Activé" else "Désactivé") else "Non supporté"}", color = MutedSlate)
                    Text("Wi-Fi Direct : ${if (peerState.wifiDirectSupported) "Pris en charge" else "Non supporté"}", color = MutedSlate)
                    Text(peerState.networkStatusMessage, color = Color.White)

                    Button(
                        onClick = onStartScan,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text(if (peerState.isScanning) "RECHERCHE EN COURS..." else "RECHERCHER DES TÉLÉPHONES")
                    }
                }
            }

            if (peerState.authorizationState == AuthorizationState.AWAITING_LOCAL_CONSENT ||
                peerState.authorizationState == AuthorizationState.AWAITING_REMOTE_CONSENT
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NightBlueSurface),
                    border = BorderStroke(1.dp, ActiveGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Autorisation mutuelle : ${peerState.selectedPeer?.name ?: "Pair"}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onApproveAuth,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = ActiveGreen)
                            ) {
                                Text("Autoriser", color = NightBlueBg)
                            }
                            OutlinedButton(
                                onClick = onRejectAuth,
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, CriticalRed)
                            ) {
                                Text("Refuser", color = CriticalRed)
                            }
                        }
                    }
                }
            }

            peerState.remoteTelemetry?.let { remote ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                    border = BorderStroke(1.dp, ActiveGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Téléphone synchronisé : ${remote.deviceName}", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Batterie distante : ${remote.batteryPct}% · Température : ${remote.temperatureCelsius ?: "--"} °C", color = Color(0xFFD1FAE5))
                        Text("Compatibilité OTG distante : ${if (remote.supportsUsbOtg) "Oui" else "Non"}", color = Color(0xFFD1FAE5))
                    }
                }
            }

            Text("Appareils découverts (${peerState.peers.size})", style = MaterialTheme.typography.titleMedium, color = Color.White)
            if (peerState.peers.isEmpty()) {
                Text("Aucun appareil détecté pour le moment.", color = MutedSlate)
            } else {
                peerState.peers.forEach { peer ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NightBlueSurface),
                        border = BorderStroke(1.dp, NightBlueBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (peer.transport == PeerTransport.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = ElectricBlueBright
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(peer.name, color = Color.White)
                                    Text("${peer.transport.name} · ${peer.id}", color = MutedSlate)
                                }
                            }
                            Button(
                                onClick = { onSelectPeer(peer) },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                            ) {
                                Text("Connecter")
                            }
                        }
                    }
                }
            }
        }
    }
}
