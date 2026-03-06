package com.xiashuidaolaoshuren.allergyguard.ui.camera

import android.graphics.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiashuidaolaoshuren.allergyguard.R
import com.xiashuidaolaoshuren.allergyguard.data.AllergenRepository
import com.xiashuidaolaoshuren.allergyguard.logic.AllergenTextMatcher
import com.xiashuidaolaoshuren.allergyguard.logic.OcrFrameData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CameraScanViewModel(
    private val repository: AllergenRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()
    private var enabledAllergenNames: List<String> = emptyList()

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
}