package com.xiashuidaolaoshuren.allergyguard.ui.camera

import android.graphics.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.mlkit.nl.translate.TranslateLanguage
import com.xiashuidaolaoshuren.allergyguard.R
import com.xiashuidaolaoshuren.allergyguard.data.AllergenRepository
import com.xiashuidaolaoshuren.allergyguard.data.ScanHistoryRepository
import com.xiashuidaolaoshuren.allergyguard.data.ScanResult
import com.xiashuidaolaoshuren.allergyguard.logic.AllergenTextMatcher
import com.xiashuidaolaoshuren.allergyguard.logic.OcrFrameData
import com.xiashuidaolaoshuren.allergyguard.logic.ScanCoordinate
import com.xiashuidaolaoshuren.allergyguard.logic.ScanLocationCodec
import com.xiashuidaolaoshuren.allergyguard.logic.TranslationManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class CameraScanViewModel(
    private val repository: AllergenRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val locationProvider: suspend () -> ScanCoordinate? = { null },
    private val alertCooldownMs: Long = DEFAULT_ALERT_COOLDOWN_MS,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()
    private val _allergenAlertEvents = MutableSharedFlow<AllergenAlertEvent>(extraBufferCapacity = 1)
    val allergenAlertEvents: SharedFlow<AllergenAlertEvent> = _allergenAlertEvents.asSharedFlow()
    private var enabledAllergenNames: List<String> = emptyList()
    private var lastAlertTimestampMs: Long = Long.MIN_VALUE
    private var lastSavedTimestampMs: Long = Long.MIN_VALUE
    private var lastSavedNormalizedText: String = ""
    private var lastSavedHasAllergens: Boolean = false

    private var isProcessing = false

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
        if (isProcessing) return
        if (frameData.fullText.isBlank()) {
            _uiState.value = CameraUiState(showStatus = true, statusMessageResId = R.string.camera_scanning)
            return
        }

        viewModelScope.launch {
            isProcessing = true
            try {
                // Determine if we need translation
                val sourceLang = TranslationManager.identifyLanguage(frameData.fullText)
                val needsTranslation = sourceLang != TranslateLanguage.ENGLISH &&
                    sourceLang != "und"

                val blockTextsForMatching = if (needsTranslation) {
                    _uiState.value = _uiState.value.copy(
                        statusMessageResId = R.string.translation_status_downloading
                    )
                    coroutineScope {
                        frameData.textBlocks.map { block ->
                            async {
                                TranslationManager.translateText(block.text, sourceLang) ?: block.text
                            }
                        }.awaitAll()
                    }
                } else {
                    frameData.textBlocks.map { it.text }
                }

                if (needsTranslation) {
                    // Trigger async download to speed up next frames when model is missing.
                    TranslationManager.downloadModel(sourceLang)
                }

                val currentText = blockTextsForMatching.joinToString(separator = " ")
                val matchedAllergens = AllergenTextMatcher.findMatches(currentText, enabledAllergenNames)

                val overlayBlocks = frameData.textBlocks.mapIndexed { index, block ->
                    val blockMatches = AllergenTextMatcher.findMatches(
                        blockTextsForMatching[index],
                        enabledAllergenNames
                    )
                    OverlayBlockUi(
                        text = block.text,
                        sourceBoundingBox = block.boundingBox,
                        isAllergen = blockMatches.isNotEmpty()
                    )
                }

                val overlayFrame = OverlayFrameUi(
                    blocks = overlayBlocks,
                    sourceWidth = frameData.sourceWidth,
                    sourceHeight = frameData.sourceHeight,
                    isFrontCamera = frameData.isFrontCamera
                )

                if (matchedAllergens.isEmpty()) {
                    maybePersistScanResult(currentText, hasAllergens = false)
                    _uiState.value = CameraUiState(
                        showStatus = true,
                        statusMessageResId = R.string.camera_no_allergens_found,
                        overlayFrame = overlayFrame
                    )
                } else {
                    maybeEmitAllergenAlert(matchedAllergens)
                    maybePersistScanResult(currentText, hasAllergens = true)

                    _uiState.value = CameraUiState(
                        showStatus = true,
                        statusMessageResId = R.string.camera_detected_allergens,
                        detectedAllergens = matchedAllergens,
                        overlayFrame = overlayFrame
                    )
                }
            } finally {
                isProcessing = false
            }
        }
    }

    private fun maybePersistScanResult(rawText: String, hasAllergens: Boolean) {
        val normalized = rawText.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) {
            return
        }

        val nowMs = nowProvider()
        val saveAllowedByCooldown = lastSavedTimestampMs == Long.MIN_VALUE ||
            nowMs - lastSavedTimestampMs >= SAVE_SCAN_COOLDOWN_MS
        val changedSinceLastSave =
            normalized != lastSavedNormalizedText || hasAllergens != lastSavedHasAllergens

        if (!saveAllowedByCooldown && !changedSinceLastSave) {
            return
        }

        lastSavedTimestampMs = nowMs
        lastSavedNormalizedText = normalized
        lastSavedHasAllergens = hasAllergens

        viewModelScope.launch {
            val encodedLocation = withContext(Dispatchers.IO) {
                locationProvider()?.let { ScanLocationCodec.encode(it) }
            }

            scanHistoryRepository.insertScanResult(
                ScanResult(
                    id = UUID.randomUUID().toString(),
                    timestamp = nowMs,
                    textContent = normalized,
                    hasAllergens = hasAllergens,
                    location = encodedLocation
                )
            )
        }
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
        private val repository: AllergenRepository,
        private val scanHistoryRepository: ScanHistoryRepository,
        private val locationProvider: suspend () -> ScanCoordinate? = { null }
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CameraScanViewModel::class.java)) {
                return CameraScanViewModel(
                    repository = repository,
                    scanHistoryRepository = scanHistoryRepository,
                    locationProvider = locationProvider
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        private const val DEFAULT_ALERT_COOLDOWN_MS = 4_000L
        private const val SAVE_SCAN_COOLDOWN_MS = 5_000L

        internal fun shouldEmitAlert(nowMs: Long, lastAlertMs: Long, cooldownMs: Long): Boolean {
            val validCooldownMs = cooldownMs.coerceAtLeast(0L)
            if (lastAlertMs == Long.MIN_VALUE) {
                return true
            }
            return nowMs - lastAlertMs >= validCooldownMs
        }
    }
}