package com.xiashuidaolaoshuren.allergyguard.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.xiashuidaolaoshuren.allergyguard.R
import com.xiashuidaolaoshuren.allergyguard.data.AppDatabase
import com.xiashuidaolaoshuren.allergyguard.data.RoomAllergenRepository
import com.xiashuidaolaoshuren.allergyguard.data.RoomAllergenAliasRepository
import com.xiashuidaolaoshuren.allergyguard.data.RoomScanHistoryRepository
import com.xiashuidaolaoshuren.allergyguard.databinding.ActivityCameraScanBinding
import com.xiashuidaolaoshuren.allergyguard.logic.CameraFrameAnalyzer
import com.xiashuidaolaoshuren.allergyguard.logic.DeviceLocationProvider
import com.xiashuidaolaoshuren.allergyguard.logic.OcrScript
import com.xiashuidaolaoshuren.allergyguard.ui.camera.CameraScanViewModel
import com.xiashuidaolaoshuren.allergyguard.ui.camera.OverlayView
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraScanBinding
    private val deviceLocationProvider: DeviceLocationProvider by lazy {
        DeviceLocationProvider(applicationContext)
    }
    private val viewModel: CameraScanViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val allergenRepository = RoomAllergenRepository(database.allergenDao())
        val scanHistoryRepository = RoomScanHistoryRepository(database.scanHistoryDao())
        val aliasRepository = RoomAllergenAliasRepository(database.allergenAliasDao())
        CameraScanViewModel.Factory(
            repository = allergenRepository,
            scanHistoryRepository = scanHistoryRepository,
            aliasRepository = aliasRepository,
            locationProvider = { deviceLocationProvider.getCurrentLocationOrNull() }
        )
    }
    private lateinit var cameraExecutor: ExecutorService
    private var frameAnalyzer: CameraFrameAnalyzer? = null
    private var selectedScript: OcrScript = OcrScript.LATIN
    private var isFrontCamera: Boolean = false
    private var lastRenderedAllergens: List<String> = emptyList()

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            viewModel.onPermissionResult(isGranted)
            if (isGranted) {
                bindCameraUseCases()
            }
        }

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                binding.textCameraStatus.text = getString(R.string.location_permission_denied_optional)
                binding.textCameraStatus.visibility = android.view.View.VISIBLE
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCameraScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = getString(R.string.camera_scan_title)
        binding.previewViewCamera.scaleType = PreviewView.ScaleType.FILL_CENTER
        selectedScript = loadSelectedScript()
        updateScriptSelectorLabel()

        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.buttonScriptSelector.setOnClickListener {
            showScriptSelectorDialog()
        }
        binding.buttonFlipCamera.setOnClickListener {
            isFrontCamera = !isFrontCamera
            bindCameraUseCases()
        }
        binding.buttonScan.setOnClickListener {
            viewModel.onScanButtonPressed()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupInsets()
        observeUiState()
        ensureLocationPermissionOptional()
        ensureCameraPermissionAndStart()
    }

    override fun onDestroy() {
        frameAnalyzer?.close()
        frameAnalyzer = null
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.cameraScanRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.overlayViewCamera.render(
                            state.overlayFrame?.let { overlayFrame ->
                                OverlayView.OverlayRenderModel(
                                    blocks = overlayFrame.blocks.map { block ->
                                        OverlayView.OverlayBlock(
                                            text = block.text,
                                            sourceBoundingBox = block.sourceBoundingBox,
                                            isAllergen = block.isAllergen
                                        )
                                    },
                                    sourceWidth = overlayFrame.sourceWidth,
                                    sourceHeight = overlayFrame.sourceHeight,
                                    isFrontCamera = overlayFrame.isFrontCamera
                                )
                            }
                        )

                        if (state.showStatus && state.statusMessageResId != null) {
                            binding.textCameraStatus.text = getString(state.statusMessageResId)
                            binding.textCameraStatus.visibility = android.view.View.VISIBLE
                        } else {
                            binding.textCameraStatus.visibility = android.view.View.GONE
                        }

                        renderAllergenChips(state.detectedAllergens)
                    }
                }

                launch {
                    viewModel.scanSavedEvents.collect { event ->
                        if (event.messageResId == R.string.camera_scan_saved) {
                            showScanSavedDialog(event.detectedAllergens)
                        } else {
                            Snackbar.make(binding.cameraScanRoot, event.messageResId, Snackbar.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }
    }

    private fun showScanSavedDialog(detectedAllergens: List<String>) {
        if (isFinishing || isDestroyed) {
            return
        }

        val message = if (detectedAllergens.isEmpty()) {
            getString(R.string.camera_scan_saved_safe)
        } else {
            getString(R.string.camera_scan_saved_allergens, detectedAllergens.joinToString(", "))
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.camera_scan_saved_title)
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton(R.string.action_ok, null)
            .show()
    }

    private fun renderAllergenChips(detectedAllergens: List<String>) {
        if (detectedAllergens == lastRenderedAllergens) {
            return
        }

        lastRenderedAllergens = detectedAllergens

        if (detectedAllergens.isEmpty()) {
            binding.scrollAllergenChips.visibility = android.view.View.GONE
            binding.chipGroupAllergens.removeAllViews()
            return
        }

        runCatching {
            binding.scrollAllergenChips.visibility = android.view.View.VISIBLE
            binding.chipGroupAllergens.removeAllViews()
            detectedAllergens.forEach { allergen ->
                val chip = Chip(this, null, com.google.android.material.R.attr.chipStyle)
                chip.text = allergen
                chip.isCheckable = false
                chip.isCloseIconVisible = false
                chip.chipBackgroundColor = ColorStateList.valueOf(0xFFE53935.toInt())
                chip.setTextColor(Color.WHITE)
                binding.chipGroupAllergens.addView(chip)
            }
        }.onFailure {
            binding.scrollAllergenChips.visibility = android.view.View.GONE
            binding.chipGroupAllergens.removeAllViews()
            lastRenderedAllergens = emptyList()
        }
    }

    private fun ensureCameraPermissionAndStart() {
        val isGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            viewModel.onPermissionResult(true)
            bindCameraUseCases()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun ensureLocationPermissionOptional() {
        val isGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!isGranted) {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun bindCameraUseCases() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()

                val hasFrontCamera = hasCamera(cameraProvider, CameraSelector.DEFAULT_FRONT_CAMERA)
                val hasBackCamera = hasCamera(cameraProvider, CameraSelector.DEFAULT_BACK_CAMERA)

                if (!hasFrontCamera && !hasBackCamera) {
                    viewModel.onCameraInitError()
                    return@addListener
                }

                val requestedFrontCamera = isFrontCamera
                val effectiveFrontCamera = when {
                    requestedFrontCamera && hasFrontCamera -> true
                    !requestedFrontCamera && hasBackCamera -> false
                    hasBackCamera -> false
                    else -> true
                }

                if (effectiveFrontCamera != isFrontCamera) {
                    isFrontCamera = effectiveFrontCamera
                }

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewViewCamera.getSurfaceProvider())
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        frameAnalyzer?.close()
                        frameAnalyzer = CameraFrameAnalyzer(
                            callbackExecutor = ContextCompat.getMainExecutor(this@CameraScanActivity),
                            onTextRecognized = viewModel::onTextRecognized,
                            onOcrError = viewModel::onOcrError,
                            processEveryNFrames = OCR_PROCESS_EVERY_N_FRAMES,
                            isFrontCamera = effectiveFrontCamera,
                            script = selectedScript
                        )
                        it.setAnalyzer(cameraExecutor, frameAnalyzer!!)
                    }

                val cameraSelector = if (effectiveFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    viewModel.onCameraReady()
                } catch (_: Exception) {
                    viewModel.onCameraInitError()
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun hasCamera(
        cameraProvider: ProcessCameraProvider,
        selector: CameraSelector
    ): Boolean {
        return try {
            cameraProvider.hasCamera(selector)
        } catch (_: CameraInfoUnavailableException) {
            false
        }
    }

    private fun showScriptSelectorDialog() {
        val scripts = OcrScript.entries.toTypedArray()
        val scriptLabels = scripts.map { getString(getScriptLabelRes(it)) }.toTypedArray()
        val selectedIndex = scripts.indexOf(selectedScript).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.camera_script_selector_title)
            .setSingleChoiceItems(scriptLabels, selectedIndex) { dialog, which ->
                val newScript = scripts[which]
                if (newScript != selectedScript) {
                    selectedScript = newScript
                    persistSelectedScript(newScript)
                    updateScriptSelectorLabel()
                    bindCameraUseCases()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun updateScriptSelectorLabel() {
        binding.buttonScriptSelector.text = getString(getScriptLabelRes(selectedScript))
    }

    private fun getScriptLabelRes(script: OcrScript): Int {
        return when (script) {
            OcrScript.LATIN -> R.string.camera_script_latin
            OcrScript.CHINESE -> R.string.camera_script_chinese
            OcrScript.JAPANESE -> R.string.camera_script_japanese
            OcrScript.KOREAN -> R.string.camera_script_korean
        }
    }

    private fun loadSelectedScript(): OcrScript {
        val savedValue = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(KEY_SELECTED_SCRIPT, OcrScript.LATIN.name)
        return OcrScript.entries.firstOrNull { it.name == savedValue } ?: OcrScript.LATIN
    }

    private fun persistSelectedScript(script: OcrScript) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_SCRIPT, script.name)
            .apply()
    }

    private companion object {
        const val OCR_PROCESS_EVERY_N_FRAMES = 3
        const val PREFS_NAME = "camera_scan_prefs"
        const val KEY_SELECTED_SCRIPT = "selected_ocr_script"
    }
}