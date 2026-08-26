package br.com.aydasoft.nfescanner.scanner

import android.os.SystemClock
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import br.com.aydasoft.nfescanner.model.NFeAccessKey
import br.com.aydasoft.nfescanner.model.NFeOcrKeyExtractor
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Analyzer sequencial com duas rotas:
 *
 * 1. código de barras/QR, entregue na primeira leitura válida porque o próprio
 *    formato e a chave de acesso já possuem verificadores;
 * 2. OCR da linha impressa, confirmado duas vezes dentro de uma janela curta.
 */
class NFeBarcodeAnalyzer(
    private val scanner: BarcodeScanner,
    private val textRecognizer: TextRecognizer,
    private val onAccessKey: (NFeAccessKey) -> Unit,
    private val onError: (Throwable) -> Unit,
) : ImageAnalysis.Analyzer, Closeable {

    private val closed = AtomicBoolean(false)
    private val delivered = AtomicBoolean(false)
    private val errorReported = AtomicBoolean(false)

    private var lastCandidate: String? = null
    private var consecutiveDetections = 0
    private var lastCandidateSeenAtMs = 0L
    private var analyzedFrameCount = 0

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (closed.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees,
        )

        val runOcrOnThisFrame = analyzedFrameCount++ % OCR_FRAME_INTERVAL == 0

        try {
            scanner.process(image).addOnCompleteListener { barcodeTask ->
                if (closed.get() || delivered.get()) {
                    imageProxy.close()
                    return@addOnCompleteListener
                }

                val barcodeCandidate = if (barcodeTask.isSuccessful) {
                    barcodeTask.result.asSequence()
                        .mapNotNull { barcode -> NFeAccessKey.extract(barcode.rawValue) }
                        .firstOrNull()
                } else {
                    reportError(barcodeTask.exception)
                    null
                }

                if (barcodeCandidate != null) {
                    deliverBarcodeCandidate(barcodeCandidate)
                    imageProxy.close()
                } else if (runOcrOnThisFrame) {
                    processPrintedKey(
                        image = image,
                        imageProxy = imageProxy,
                    )
                } else {
                    imageProxy.close()
                }
            }
        } catch (throwable: Throwable) {
            imageProxy.close()
            reportError(throwable)
        }
    }

    private fun processPrintedKey(
        image: InputImage,
        imageProxy: ImageProxy,
    ) {
        try {
            textRecognizer.process(image)
                .addOnSuccessListener { recognizedText ->
                    if (closed.get() || delivered.get()) return@addOnSuccessListener

                    val candidate = NFeOcrKeyExtractor.extract(
                        recognizedText.candidateTexts(),
                    )
                    registerOcrCandidate(candidate)
                }
                .addOnFailureListener(::reportError)
                .addOnCompleteListener { imageProxy.close() }
        } catch (throwable: Throwable) {
            imageProxy.close()
            reportError(throwable)
        }
    }

    private fun deliverBarcodeCandidate(candidate: NFeAccessKey) {
        if (delivered.compareAndSet(false, true)) {
            onAccessKey(candidate)
        }
    }

    private fun registerOcrCandidate(candidate: NFeAccessKey?) {
        if (candidate == null) return

        val now = SystemClock.elapsedRealtime()
        val isSameRecentCandidate = candidate.value == lastCandidate &&
            now - lastCandidateSeenAtMs <= OCR_CONFIRMATION_WINDOW_MS

        if (isSameRecentCandidate) {
            consecutiveDetections += 1
        } else {
            lastCandidate = candidate.value
            consecutiveDetections = 1
        }
        lastCandidateSeenAtMs = now

        if (
            consecutiveDetections >= REQUIRED_OCR_DETECTIONS &&
            delivered.compareAndSet(false, true)
        ) {
            onAccessKey(candidate)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            scanner.close()
            textRecognizer.close()
        }
    }

    private fun reportError(throwable: Throwable?) {
        if (throwable != null && errorReported.compareAndSet(false, true)) {
            onError(throwable)
        }
    }

    private companion object {
        const val OCR_FRAME_INTERVAL = 3
        const val REQUIRED_OCR_DETECTIONS = 2
        const val OCR_CONFIRMATION_WINDOW_MS = 2_500L
    }
}

private fun Text.candidateTexts(): Sequence<String> = sequence {
    for (block in textBlocks) {
        for (line in block.lines) {
            yield(line.text)
        }
        yield(block.text)
    }
    yield(text)
}
