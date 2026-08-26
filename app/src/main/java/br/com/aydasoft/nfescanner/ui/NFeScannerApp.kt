package br.com.aydasoft.nfescanner.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import br.com.aydasoft.nfescanner.model.NFeAccessKey
import br.com.aydasoft.nfescanner.ui.screens.ManualKeyDialog
import br.com.aydasoft.nfescanner.ui.screens.PortalScreen
import br.com.aydasoft.nfescanner.ui.screens.ResultScreen
import br.com.aydasoft.nfescanner.ui.screens.ScannerScreen

@Composable
fun NFeScannerApp(
    cameraPermissionGranted: Boolean,
    cameraPermissionPermanentlyDenied: Boolean,
    onRequestCameraPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    var screen by rememberSaveable { mutableStateOf(AppScreen.SCANNER) }
    var accessKeyValue by rememberSaveable { mutableStateOf<String?>(null) }
    var showManualEntry by rememberSaveable { mutableStateOf(false) }

    fun acceptAccessKey(accessKey: NFeAccessKey) {
        accessKeyValue = accessKey.value
        screen = AppScreen.RESULT
        showManualEntry = false
    }

    val accessKey = accessKeyValue?.let(NFeAccessKey::parse)

    LaunchedEffect(screen, accessKeyValue) {
        if (screen != AppScreen.SCANNER && accessKey == null) {
            accessKeyValue = null
            screen = AppScreen.SCANNER
        }
    }

    BackHandler(enabled = screen == AppScreen.RESULT) {
        accessKeyValue = null
        screen = AppScreen.SCANNER
    }

    when (screen) {
        AppScreen.SCANNER -> ScannerScreen(
            cameraPermissionGranted = cameraPermissionGranted,
            cameraPermissionPermanentlyDenied = cameraPermissionPermanentlyDenied,
            onRequestCameraPermission = onRequestCameraPermission,
            onOpenAppSettings = onOpenAppSettings,
            onManualEntry = { showManualEntry = true },
            onAccessKeyDetected = ::acceptAccessKey,
        )

        AppScreen.RESULT -> {
            accessKey?.let {
                ResultScreen(
                    accessKey = it,
                    onConsult = { screen = AppScreen.PORTAL },
                    onScanAnother = {
                        accessKeyValue = null
                        screen = AppScreen.SCANNER
                    },
                )
            }
        }

        AppScreen.PORTAL -> {
            accessKey?.let {
                PortalScreen(
                    accessKey = it,
                    onBack = { screen = AppScreen.RESULT },
                )
            }
        }
    }

    if (showManualEntry) {
        ManualKeyDialog(
            onDismiss = { showManualEntry = false },
            onValidKey = ::acceptAccessKey,
        )
    }
}

private enum class AppScreen {
    SCANNER,
    RESULT,
    PORTAL,
}
