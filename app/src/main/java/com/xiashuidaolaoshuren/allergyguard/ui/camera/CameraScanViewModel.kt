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
import com.xiashuidaolaoshuren.allergyguard.data.AllergenAliasRepository
import com.xiashuidaolaoshuren.allergyguard.logic.AllergenSynonymMap
import com.xiashuidaolaoshuren.allergyguard.logic.AllergenTextMatcher
import com.xiashuidaolaoshuren.allergyguard.logic.CjkFoodTermSubstitutions
import com.xiashuidaolaoshuren.allergyguard.logic.OcrDebugProfiler
import com.xiashuidaolaoshuren.allergyguard.logic.OcrFrameData
import com.xiashuidaolaoshuren.allergyguard.logic.ScanCoordinate
import com.xiashuidaolaoshuren.allergyguard.logic.ScanLocationCodec
import com.xiashuidaolaoshuren.allergyguard.logic.TranslationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private data class FrameProcessingResult(
    val matchedAllergens: List<String>,
    val overlayFrame: CameraScanViewModel.OverlayFrameUi,
    val frameTextForBuffer: String
)

class CameraScanViewModel(
    private val repository: AllergenRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val aliasRepository: AllergenAliasRepository,
    private val locationProvider: suspend () -> ScanCoordinate? = { null },
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()
    private val _scanSavedEvents = MutableSharedFlow<ScanSavedEvent>(extraBufferCapacity = 1)
    val scanSavedEvents: SharedFlow<ScanSavedEvent> = _scanSavedEvents.asSharedFlow()
    // Map of allergen display name -> all synonyms (built-in + user aliases) for enabled allergens
    private var allergenSynonymMap: Map<String, List<String>> = emptyMap()
    private var lastSavedTimestampMs: Long = Long.MIN_VALUE
    private val sessionTextChunks = LinkedHashSet<String>()
    private val sessionDetectedAllergens = LinkedHashSet<String>()
    private var sessionHasAllergens: Boolean = false

    private var isProcessing = false

    init {
        viewModelScope.launch {
            combine(
                repository.allergens,
                aliasRepository.getAllAliases()
            ) { allergens, allAliases ->
                val aliasesByAllergenId = allAliases.groupBy({ it.allergenId }, { it.alias })
                allergens
                    .filter { it.isEnabled }
                    .associate { allergen ->
                        val builtInSynonyms = AllergenSynonymMap.getSynonyms(allergen.name)
                        val userAliases = aliasesByAllergenId[allergen.id] ?: emptyList()
                        allergen.name to (builtInSynonyms + userAliases)
                    }
            }.collect { map ->
                allergenSynonymMap = map
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
                val sourceLang = withContext(Dispatchers.Default) {
                    TranslationManager.identifyLanguage(frameData.fullText)
                }
                val needsTranslation = sourceLang != TranslateLanguage.ENGLISH &&
                    sourceLang != "und"

                if (needsTranslation) {
                    _uiState.value = _uiState.value.copy(
                        statusMessageResId = R.string.translation_status_downloading
                    )
                }

                val processingResult = withContext(Dispatchers.Default) {
                    val normalizedBlockTexts = frameData.textBlocks.map { block ->
                        CjkFoodTermSubstitutions.apply(block.text)
                    }
                    val originalFrameText = normalizedBlockTexts.joinToString(separator = " ")

                    val translatedFrameTextOrNull = if (needsTranslation) {
                        val translationStartMs = OcrDebugProfiler.frameStartToken()
                        TranslationManager.translateText(originalFrameText, sourceLang).also {
                            val translationLatencyMs = OcrDebugProfiler.frameStartToken() - translationStartMs
                            OcrDebugProfiler.markTranslationLatency(translationLatencyMs)
                        }
                    } else {
                        null
                    }

                    val primaryMatchText = translatedFrameTextOrNull ?: originalFrameText
                    val primaryMatches = AllergenTextMatcher.findMatches(primaryMatchText, allergenSynonymMap)
                    val fallbackOriginalMatches = if (
                        translatedFrameTextOrNull != null &&
                        translatedFrameTextOrNull != originalFrameText &&
                        primaryMatches.isEmpty()
                    ) {
                        AllergenTextMatcher.findMatches(originalFrameText, allergenSynonymMap)
                    } else {
                        emptyList()
                    }
                    val matchedAllergens = (primaryMatches + fallbackOriginalMatches).distinct()

                    val overlayBlocks = frameData.textBlocks.mapIndexed { index, block ->
                        val blockMatches = AllergenTextMatcher.findMatches(
                            normalizedBlockTexts[index],
                            allergenSynonymMap
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

                    FrameProcessingResult(
                        matchedAllergens = matchedAllergens,
                        overlayFrame = overlayFrame,
                        frameTextForBuffer = originalFrameText
                    )
                }

                if (needsTranslation) {
                    // Trigger async download to speed up next frames when model is missing.
                    TranslationManager.downloadModel(sourceLang)
                }

                addFrameToSessionBuffer(processingResult.frameTextForBuffer, processingResult.matchedAllergens)

                if (processingResult.matchedAllergens.isEmpty()) {
                    _uiState.value = CameraUiState(
                        showStatus = true,
                        statusMessageResId = R.string.camera_no_allergens_found,
                        overlayFrame = processingResult.overlayFrame
                    )
                } else {
                    _uiState.value = CameraUiState(
                        showStatus = false,
                        detectedAllergens = processingResult.matchedAllergens,
                        overlayFrame = processingResult.overlayFrame
                    )
                }
            } finally {
                isProcessing = false
            }
        }
    }

    private fun addFrameToSessionBuffer(rawText: String, matchedAllergens: List<String>) {
        val normalized = normalizeOcrText(rawText)
        if (normalized.isBlank()) {
            return
        }

        sessionTextChunks.add(normalized)
        sessionDetectedAllergens.addAll(matchedAllergens)
        sessionHasAllergens = sessionHasAllergens || matchedAllergens.isNotEmpty()
        OcrDebugProfiler.markSessionBufferSize(
            textChunkCount = sessionTextChunks.size,
            allergenCount = sessionDetectedAllergens.size
        )
    }

    fun onScanButtonPressed() {
        val mergedSessionText = normalizeOcrText(sessionTextChunks.joinToString(separator = " "))
        if (mergedSessionText.isBlank()) {
            _scanSavedEvents.tryEmit(ScanSavedEvent(R.string.camera_scan_nothing))
            return
        }

        val nowMs = nowProvider()
        val saveAllowedByCooldown = lastSavedTimestampMs == Long.MIN_VALUE ||
            nowMs - lastSavedTimestampMs >= MANUAL_SCAN_COOLDOWN_MS
        if (!saveAllowedByCooldown) {
            _scanSavedEvents.tryEmit(ScanSavedEvent(R.string.camera_scan_duplicate))
            return
        }

        lastSavedTimestampMs = nowMs
        val savedAllergens = sessionDetectedAllergens.toList()

        viewModelScope.launch {
            val encodedLocation = withContext(Dispatchers.IO) {
                locationProvider()?.let { ScanLocationCodec.encode(it) }
            }

            scanHistoryRepository.insertScanResult(
                ScanResult(
                    id = UUID.randomUUID().toString(),
                    timestamp = nowMs,
                    textContent = savedAllergens.joinToString(", "),
                    hasAllergens = sessionHasAllergens,
                    location = encodedLocation
                )
            )

            sessionTextChunks.clear()
            sessionHasAllergens = false
            sessionDetectedAllergens.clear()
            _scanSavedEvents.tryEmit(ScanSavedEvent(R.string.camera_scan_saved, savedAllergens))
        }
    }

    private fun normalizeOcrText(rawText: String): String {
        return rawText.trim().replace(Regex("\\s+"), " ")
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

    data class ScanSavedEvent(
        val messageResId: Int,
        val detectedAllergens: List<String> = emptyList()
    )

    class Factory(
        private val repository: AllergenRepository,
        private val scanHistoryRepository: ScanHistoryRepository,
        private val aliasRepository: AllergenAliasRepository,
        private val locationProvider: suspend () -> ScanCoordinate? = { null }
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CameraScanViewModel::class.java)) {
                return CameraScanViewModel(
                    repository = repository,
                    scanHistoryRepository = scanHistoryRepository,
                    aliasRepository = aliasRepository,
                    locationProvider = locationProvider
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        private const val MANUAL_SCAN_COOLDOWN_MS = 2_000L

        internal fun shouldEmitAlert(nowMs: Long, lastAlertMs: Long, cooldownMs: Long): Boolean {
            val validCooldownMs = cooldownMs.coerceAtLeast(0L)
            if (lastAlertMs == Long.MIN_VALUE) {
                return true
            }
            return nowMs - lastAlertMs >= validCooldownMs
        }
    }
}