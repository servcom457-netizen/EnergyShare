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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
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
import com.batteryshare.ai.ui.theme.WarningAmber
import com.batteryshare.ai.ui.viewmodel.BatteryShareUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsbOtgScreen(
    uiState: BatteryShareUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onStartMode: (TransferMode) -> Unit,
    onStopSession: () -> Unit,
    onOpenProtection: () -> Unit
) {
    val usb = uiState.usbCapability
    val telemetry = uiState.telemetry

    Scaffold(
        containerColor = NightBlueBg,
        topBar = {
            TopAppBar(
                title = { Text("USB-C / OTG & Alimentation", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser", tint = ElectricBlueBright)
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row {
                        Icon(Icons.Default.Usb, contentDescription = null, tint = ElectricBlueBright)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Diagnostic USB réel", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    Text(
                        text = "Connexion USB : ${if (usb?.isUsbDataOrDeviceConnected == true) "Détectée" else "Non détectée"}",
                        color = if (usb?.isUsbDataOrDeviceConnected == true) ActiveGreen else MutedSlate
                    )
                    Text(
                        text = "Rôle d'alimentation : ${usb?.currentPowerRole?.name ?: "UNAVAILABLE"}",
                        color = Color.White
                    )
                    Text(
                        text = "Source d'énergie (OTG/Host) : ${
                            when (usb?.canOperateAsSourceHardware) {
                                true -> "Supporté selon port USB-PD"
                                false -> "Non supporté par ce port"
                                null -> if (usb?.hasUsbHostFeature == true) "USB Host présent (à négocier via PMIC)" else "Indisponible"
                            }
                        }",
                        color = Color.White
                    )
                    Text(
                        text = "Réception d'énergie (Sink) : ${if (usb?.canOperateAsSinkViaBattery == true) "Prise en charge" else "Limitée"}",
                        color = Color.White
                    )
                    Text(
                        text = "Niveau actuel : ${telemetry.levelPct}% · ${telemetry.temperatureCelsius ?: "--"} °C",
                        color = ElectricBlueBright
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1607)),
                border = BorderStroke(1.dp, WarningAmber),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Avertissement matériel important",
                        style = MaterialTheme.typography.titleMedium,
                        color = WarningAmber
                    )
                    Text(
                        text = usb?.diagnosticNote
                            ?: "Une simple connexion USB entre deux téléphones ne garantit jamais un transfert électrique. Le sens du courant dépend du contrôleur USB-PD / OTG et du câble.",
                        style = MaterialTheme.typography.bodyMedium,
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Guide de branchement USB-C / OTG", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("1. Branchez un câble USB-C vers USB-C certifié ou un adaptateur OTG côté donneur.", color = MutedSlate)
                    Text("2. Sur Android, ouvrez la notification système « Chargement de l'appareil connecté ».", color = MutedSlate)
                    Text("3. Choisissez « Charger l'appareil connecté » sur le téléphone donneur.", color = MutedSlate)
                    Text("4. Démarrez le suivi ci-dessous pour activer la protection thermique et le seuil minimal.", color = MutedSlate)
                }
            }

            if (uiState.activeSession == null) {
                Button(
                    onClick = { onStartMode(TransferMode.USB_OTG_SHARE) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("SUIVRE EN MODE DONNEUR USB-C")
                }
                Button(
                    onClick = { onStartMode(TransferMode.USB_OTG_RECEIVE) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ActiveGreen)
                ) {
                    Text("SUIVRE EN MODE RÉCEPTEUR USB-C", color = NightBlueBg)
                }
            } else {
                Button(
                    onClick = onStopSession,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CriticalRed)
                ) {
                    Text("ARRÊTER LE SUIVI ACTIF")
                }
            }

            OutlinedButton(
                onClick = onOpenProtection,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                border = BorderStroke(1.dp, NightBlueBorder)
            ) {
                Text("Configurer les seuils de protection", color = Color.White)
            }
        }
    }
}
