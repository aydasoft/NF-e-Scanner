package br.com.aydasoft.nfescanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.aydasoft.nfescanner.ui.NFeScannerApp
import br.com.aydasoft.nfescanner.ui.theme.NFeScannerTheme

class MainActivity : ComponentActivity() {

    private var cameraPermissionGranted by mutableStateOf(false)
    private var cameraPermissionRequested by mutableStateOf(false)

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            cameraPermissionRequested = true
            cameraPermissionGranted = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        cameraPermissionRequested =
            savedInstanceState?.getBoolean(KEY_PERMISSION_REQUESTED) ?: false
        refreshCameraPermission()

        setContent {
            NFeScannerTheme {
                NFeScannerApp(
                    cameraPermissionGranted = cameraPermissionGranted,
                    cameraPermissionPermanentlyDenied =
                        isCameraPermissionPermanentlyDenied(),
                    onRequestCameraPermission = ::requestCameraPermission,
                    onOpenAppSettings = ::openAppSettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCameraPermission()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_PERMISSION_REQUESTED, cameraPermissionRequested)
        super.onSaveInstanceState(outState)
    }

    private fun requestCameraPermission() {
        cameraPermissionRequested = true
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun refreshCameraPermission() {
        cameraPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isCameraPermissionPermanentlyDenied(): Boolean {
        return cameraPermissionRequested &&
            !cameraPermissionGranted &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA,
            )
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            ),
        )
    }

    private companion object {
        const val KEY_PERMISSION_REQUESTED = "camera_permission_requested"
    }
}
