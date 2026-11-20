package com.batteryshare.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.batteryshare.ai.hardware.permissions.PermissionHelper
import com.batteryshare.ai.ui.navigation.BatteryShareNavGraph
import com.batteryshare.ai.ui.theme.BatteryShareTheme
import com.batteryshare.ai.ui.theme.NightBlueBg
import com.batteryshare.ai.ui.viewmodel.BatteryShareViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BatteryShareViewModel by viewModels {
        BatteryShareViewModel.factory(application as BatteryShareApplication)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BatteryShareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NightBlueBg
                ) {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val navController = rememberNavController()

                    BatteryShareNavGraph(
                        navController = navController,
                        uiState = uiState,
                        viewModel = viewModel,
                        onRequestPermissions = { requestMissingPermissions() },
                        onOpenSystemBatterySettings = {
                            runCatching {
                                startActivity(viewModel.buildOpenBatterySettingsIntent())
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.refreshHardwareSnapshot()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
        viewModel.refreshHardwareSnapshot()
    }

    private fun requestMissingPermissions() {
        val missing = PermissionHelper.missingPermissions(this)
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}
