package com.batteryshare.ai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.batteryshare.ai.ui.theme.ActiveGreen
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
fun SettingsScreen(
    uiState: BatteryShareUiState,
    onBack: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenProtection: () -> Unit
) {
    val audit = uiState.capabilityAudit

    Scaffold(
        containerColor = NightBlueBg,
        topBar = {
            TopAppBar(
                title = { Text("Paramètres & Audit matériel", color = Color.White) },
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("BatteryShare AI — Version 1.0", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("Architecture MVVM · Jetpack Compose · Material 3 · Room · Android 8.0+ (API 26+)", color = MutedSlate)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = NightBlueSurface),
                border = BorderStroke(1.dp, ActiveGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Fonctions opérationnelles (100 % actives)", style = MaterialTheme.typography.titleMedium, color = ActiveGreen)
                    audit?.operationalCapabilities?.forEach { item ->
                        Text("• $item", color = Color.White)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = NightBlueSurface),
                border = BorderStroke(1.dp, WarningAmber),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Fonctions dépendantes du matériel / OEM", style = MaterialTheme.typography.titleMedium, color = WarningAmber)
                    audit?.hardwareDependentCapabilities?.forEach { item ->
                        Text("• $item", color = Color.White)
                    }
                }
            }

            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text(
                    if (uiState.missingPermissions.isEmpty()) "Toutes les permissions accordées"
                    else "Accorder ${uiState.missingPermissions.size} permission(s) manquante(s)"
                )
            }

            OutlinedButton(
                onClick = onOpenProtection,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, ElectricBlueBright)
            ) {
                Text("Règles de protection donneur", color = Color.White)
            }
        }
    }
}
