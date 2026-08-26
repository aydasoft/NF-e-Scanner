package br.com.aydasoft.nfescanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.aydasoft.nfescanner.R
import br.com.aydasoft.nfescanner.model.NFeAccessKey
import br.com.aydasoft.nfescanner.scanner.CameraPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    cameraPermissionGranted: Boolean,
    cameraPermissionPermanentlyDenied: Boolean,
    onRequestCameraPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onManualEntry: () -> Unit,
    onAccessKeyDetected: (NFeAccessKey) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var cameraErrorCount by remember { mutableIntStateOf(0) }
    val cameraErrorMessage = stringResource(R.string.camera_error)

    LaunchedEffect(cameraErrorCount) {
        if (cameraErrorCount > 0) {
            snackbarHostState.showSnackbar(cameraErrorMessage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.scanner_title)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        if (cameraPermissionGranted) {
            ScannerCameraContent(
                onManualEntry = onManualEntry,
                onAccessKeyDetected = onAccessKeyDetected,
                onCameraError = { cameraErrorCount += 1 },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        } else {
            CameraPermissionContent(
                permanentlyDenied = cameraPermissionPermanentlyDenied,
                onRequestPermission = onRequestCameraPermission,
                onOpenSettings = onOpenAppSettings,
                onManualEntry = onManualEntry,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
    }
}

@Composable
private fun ScannerCameraContent(
    onManualEntry: () -> Unit,
    onAccessKeyDetected: (NFeAccessKey) -> Unit,
    onCameraError: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
) {
    var flashAvailable by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.background(Color.Black),
    ) {
        CameraPreview(
            torchEnabled = torchEnabled,
            onFlashAvailabilityChanged = {
                flashAvailable = it
                if (!it) torchEnabled = false
            },
            onAccessKeyDetected = onAccessKeyDetected,
            onError = onCameraError,
            modifier = Modifier.fillMaxSize(),
        )

        Surface(
            color = Color.Black.copy(alpha = 0.66f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.scan_instruction),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .aspectRatio(2.45f)
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(18.dp),
                ),
        )

        Surface(
            color = Color.Black.copy(alpha = 0.72f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(20.dp),
            ) {
                if (flashAvailable) {
                    FilledTonalButton(
                        onClick = { torchEnabled = !torchEnabled },
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Default.FlashOff else Icons.Default.FlashOn,
                            contentDescription = null,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            stringResource(
                                if (torchEnabled) R.string.turn_flash_off else R.string.turn_flash_on,
                            ),
                        )
                    }
                }

                FilledTonalButton(onClick = onManualEntry) {
                    Icon(Icons.Default.Keyboard, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.enter_manually))
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionContent(
    permanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(28.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(28.dp),
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(22.dp).size(48.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.permission_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(
                if (permanentlyDenied) {
                    R.string.permission_permanently_denied
                } else {
                    R.string.permission_body
                },
            ),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = if (permanentlyDenied) onOpenSettings else onRequestPermission,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (permanentlyDenied) R.string.open_settings else R.string.grant_permission,
                ),
            )
        }
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(
            onClick = onManualEntry,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Keyboard, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.enter_manually))
        }
    }
}
