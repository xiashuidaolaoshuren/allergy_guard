package com.xiashuidaolaoshuren.allergyguard.ui.camera

import android.graphics.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiashuidaolaoshuren.allergyguard.R
import com.xiashuidaolaoshuren.allergyguard.data.AllergenRepository
import com.xiashuidaolaoshuren.allergyguard.logic.AllergenTextMatcher
import com.xiashuidaolaoshuren.allergyguard.logic.OcrFrameData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class CameraScanViewModel(
    private val repository: AllergenRepository,
    private val alertCooldownMs: Long = DEFAULT_ALERT_COOLDOWN_MS,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()
    private val _allergenAlertEvents = MutableSharedFlow<AllergenAlertEvent>(extraBufferCapacity = 1)
    val allergenAlertEvents: SharedFlow<AllergenAlertEvent> = _allergenAlertEvents.asSharedFlow()
    private var enabledAllergenNames: List<String> = emptyList()
    private var lastAlertTimestampMs: Long = Long.MIN_VALUE

    init {
        viewModelScope.launch {
            repository.allergens.collect { allergens ->
                enabledAllergenNames = allergens
                    .filter { it.isEnabled }
                    .map { it.name }
            }
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.value = if (isGranted) {
            CameraUiState(showStatus = true, statusMessageResId = R.string.camera_scanning)
        } else {
            CameraUiState(showStatus = true, statusMessageResId = R.string.camera_permission_denied)
        }
    }

    fun onCameraReady() {
        _uiState.value = CameraUiState(showStatus = true, statusMessageResId = R.string.camera_scanning)
    }

    fun onCameraInitError() {
        _uiState.value = CameraUiState(showStatus = true, statusMessageResId = R.string.camera_initialization_error)
    }

    fun onTextRecognized(frameData: OcrFrameData) {
        if (frameData.fullText.isBlank()) {
            _uiState.value = CameraUiState(showStatus = true, statusMessageResId = R.string.camera_scanning)
            return
        }

        val matchedAllergens = AllergenTextMatcher.findMatches(frameData.fullText, enabledAllergenNames)
        val overlayBlocks = frameData.textBlocks.map { block ->
            OverlayBlockUi(
                text = block.text,
                sourceBoundingBox = block.boundingBox,
                isAllergen = AllergenTextMatcher.findMatches(block.text, enabledAllergenNames).isNotEmpty()
            )
        }
        val overlayFrame = OverlayFrameUi(
            blocks = overlayBlocks,
            sourceWidth = frameData.sourceWidth,
            sourceHeight = frameData.sourceHeight,
            isFrontCamera = frameData.isFrontCamera
        )

        if (matchedAllergens.isEmpty()) {
            _uiState.value = CameraUiState(
                showStatus = true,
                statusMessageResId = R.string.camera_no_allergens_found,
                overlayFrame = overlayFrame
            )
            return
        }

        maybeEmitAllergenAlert(matchedAllergens)

        _uiState.value = CameraUiState(
            showStatus = true,
            statusMessageResId = R.string.camera_detected_allergens,
            detectedAllergens = matchedAllergens,
            overlayFrame = overlayFrame
        )
    }

    fun onOcrError() {
        _uiState.value = CameraUiState(showStatus = true, statusMessageResId = R.string.camera_ocr_error)
    }

    private fun maybeEmitAllergenAlert(matchedAllergens: List<String>) {
        val nowMs = nowProvider()
        if (!shouldEmitAlert(nowMs, lastAlertTimestampMs, alertCooldownMs)) {
            return
        }

        lastAlertTimestampMs = nowMs
        _allergenAlertEvents.tryEmit(
            AllergenAlertEvent(
                detectedAllergens = matchedAllergens.distinct()
            )
        )
    }

    data class CameraUiState(
        val showStatus: Boolean = false,
        val statusMessageResId: Int? = null,
        val detectedAllergens: List<String> = emptyList(),
        val overlayFrame: OverlayFrameUi? = null
    )

    data class OverlayBlockUi(
        val text: String,
        val sourceBoundingBox: Rect,
        val isAllergen: Boolean
    )

    data class OverlayFrameUi(
        val blocks: List<OverlayBlockUi>,
        val sourceWidth: Int,
        val sourceHeight: Int,
        val isFrontCamera: Boolean
    )

    data class AllergenAlertEvent(
        val detectedAllergens: List<String>
    )

    class Factory(
        private val repository: AllergenRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CameraScanViewModel::class.java)) {
                return CameraScanViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        private const val DEFAULT_ALERT_COOLDOWN_MS = 4_000L

        internal fun shouldEmitAlert(nowMs: Long, lastAlertMs: Long, cooldownMs: Long): Boolean {
            val validCooldownMs = cooldownMs.coerceAtLeast(0L)
            if (lastAlertMs == Long.MIN_VALUE) {
                return true
            }
            return nowMs - lastAlertMs >= validCooldownMs
        }
    }
}