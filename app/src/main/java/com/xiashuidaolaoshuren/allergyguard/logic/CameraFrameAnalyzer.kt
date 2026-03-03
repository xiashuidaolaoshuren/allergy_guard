package com.xiashuidaolaoshuren.allergyguard.logic

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.Closeable
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

class CameraFrameAnalyzer(
    private val callbackExecutor: Executor,
    private val onTextRecognized: (String) -> Unit,
    private val onOcrError: () -> Unit
) : ImageAnalysis.Analyzer, Closeable {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val isProcessingFrame = AtomicBoolean(false)

    override fun analyze(image: ImageProxy) {
        if (!isProcessingFrame.compareAndSet(false, true)) {
            image.close()
            return
        }

        val mediaImage = image.image
        if (mediaImage == null) {
            isProcessingFrame.set(false)
            image.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
        textRecognizer
            .process(inputImage)
            .addOnSuccessListener(callbackExecutor) { visionText ->
                onTextRecognized(visionText.text)
            }
            .addOnFailureListener(callbackExecutor) { error ->
                Log.e(TAG, "OCR analysis failed", error)
                onOcrError()
            }
            .addOnCompleteListener {
                isProcessingFrame.set(false)
                image.close()
            }
    }

    override fun close() {
        textRecognizer.close()
    }

    private companion object {
        const val TAG = "CameraFrameAnalyzer"
    }
}