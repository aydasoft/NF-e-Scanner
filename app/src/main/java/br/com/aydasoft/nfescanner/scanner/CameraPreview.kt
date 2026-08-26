package br.com.aydasoft.nfescanner.scanner

import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.aydasoft.nfescanner.model.NFeAccessKey
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CameraPreview(
    torchEnabled: Boolean,
    onFlashAvailabilityChanged: (Boolean) -> Unit,
    onAccessKeyDetected: (NFeAccessKey) -> Unit,
    onError: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    val currentOnAccessKey by rememberUpdatedState(onAccessKeyDetected)
    val currentOnError by rememberUpdatedState(onError)
    val currentOnFlashAvailabilityChanged by
        rememberUpdatedState(onFlashAvailabilityChanged)

    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    val analysisExecutor = remember(lifecycleOwner) {
        Executors.newSingleThreadExecutor()
    }

    val scanner = remember(lifecycleOwner) {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_ITF,
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    val textRecognizer = remember(lifecycleOwner) {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    val analyzer = remember(scanner, textRecognizer) {
        NFeBarcodeAnalyzer(
            scanner = scanner,
            textRecognizer = textRecognizer,
            onAccessKey = { currentOnAccessKey(it) },
            onError = { currentOnError(it) },
        )
    }

    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(torchEnabled, boundCamera) {
        val camera = boundCamera ?: return@LaunchedEffect
        if (camera.cameraInfo.hasFlashUnit()) {
            camera.cameraControl.enableTorch(torchEnabled)
        }
    }

    @Suppress("DEPRECATION")
    DisposableEffect(lifecycleOwner, previewView, analyzer) {
        val disposed = AtomicBoolean(false)
        var cameraProvider: ProcessCameraProvider? = null

        val preview = Preview.Builder()
            .build()
            .also { it.surfaceProvider = previewView.surfaceProvider }

        val analysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1920, 1080))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(analysisExecutor, analyzer) }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            if (disposed.get()) return@addListener

            try {
                val provider = cameraProviderFuture.get()
                if (disposed.get()) return@addListener

                cameraProvider = provider
                val camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )

                boundCamera = camera
                currentOnFlashAvailabilityChanged(camera.cameraInfo.hasFlashUnit())
            } catch (throwable: Throwable) {
                currentOnError(throwable)
            }
        }, mainExecutor)

        onDispose {
            disposed.set(true)
            analysis.clearAnalyzer()
            boundCamera?.cameraControl?.enableTorch(false)
            cameraProvider?.unbind(preview, analysis)
            analyzer.close()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}
