package com.xiashuidaolaoshuren.allergyguard.logic

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class CameraFrameAnalyzer : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        try {
            val width = image.width
            val height = image.height
            val rotation = image.imageInfo.rotationDegrees
            Log.d(TAG, "Frame analyzed: ${width}x${height}, rotation=$rotation")
        } finally {
            image.close()
        }
    }

    private companion object {
        const val TAG = "CameraFrameAnalyzer"
    }
}