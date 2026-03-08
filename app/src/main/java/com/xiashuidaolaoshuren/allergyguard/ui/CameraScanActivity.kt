package com.xiashuidaolaoshuren.allergyguard.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xiashuidaolaoshuren.allergyguard.R
import com.xiashuidaolaoshuren.allergyguard.data.AppDatabase
import com.xiashuidaolaoshuren.allergyguard.data.RoomAllergenRepository
import com.xiashuidaolaoshuren.allergyguard.data.RoomScanHistoryRepository
import com.xiashuidaolaoshuren.allergyguard.databinding.ActivityCameraScanBinding
import com.xiashuidaolaoshuren.allergyguard.logic.CameraFrameAnalyzer
import com.xiashuidaolaoshuren.allergyguard.ui.camera.CameraScanViewModel
import com.xiashuidaolaoshuren.allergyguard.ui.camera.OverlayView
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraScanBinding
    private val viewModel: CameraScanViewModel by viewModels {
        val database = AppDatabase.getInstance(applicationContext)
        val allergenRepository = RoomAllergenRepository(database.allergenDao())
        val scanHistoryRepository = RoomScanHistoryRepository(database.scanHistoryDao())
        CameraScanViewModel.Factory(allergenRepository, scanHistoryRepository)
    }
    private lateinit var cameraExecutor: ExecutorService
    private var frameAnalyzer: CameraFrameAnalyzer? = null
    private var allergenAlertDialog: AlertDialog? = null

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            viewModel.onPermissionResult(isGranted)
            if (isGranted) {
                bindCameraUseCases()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityCameraScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = getString(R.string.camera_scan_title)
        binding.previewViewCamera.scaleType = PreviewView.ScaleType.FILL_CENTER

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupInsets()
        observeUiState()
        ensureCameraPermissionAndStart()
    }

    override fun onDestroy() {
        allergenAlertDialog?.dismiss()
        allergenAlertDialog = null
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
                            binding.textCameraStatus.text = if (
                                state.statusMessageResId == R.string.camera_detected_allergens &&
                                state.detectedAllergens.isNotEmpty()
                            ) {
                                getString(
                                    state.statusMessageResId,
                                    state.detectedAllergens.joinToString(separator = ", ")
                                )
                            } else {
                                getString(state.statusMessageResId)
                            }
                            binding.textCameraStatus.visibility = android.view.View.VISIBLE
                        } else {
                            binding.textCameraStatus.visibility = android.view.View.GONE
                        }
                    }
                }

                launch {
                    viewModel.allergenAlertEvents.collect { event ->
                        showAllergenAlertDialog(event.detectedAllergens)
                    }
                }
            }
        }
    }

    private fun showAllergenAlertDialog(detectedAllergens: List<String>) {
        if (detectedAllergens.isEmpty()) {
            return
        }

        val allergenList = detectedAllergens.joinToString(separator = ", ")
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.camera_allergen_alert_title)
            .setMessage(getString(R.string.camera_allergen_alert_message, allergenList))
            .setCancelable(true)
            .setPositiveButton(R.string.action_ok, null)
            .create()

        allergenAlertDialog?.dismiss()
        allergenAlertDialog = dialog
        dialog.show()
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

    private fun bindCameraUseCases() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewViewCamera.getSurfaceProvider())
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        frameAnalyzer?.close()
                        frameAnalyzer = CameraFrameAnalyzer(
                            callbackExecutor = ContextCompat.getMainExecutor(this),
                            onTextRecognized = viewModel::onTextRecognized,
                            onOcrError = viewModel::onOcrError,
                            processEveryNFrames = OCR_PROCESS_EVERY_N_FRAMES,
                            isFrontCamera = false
                        )
                        it.setAnalyzer(cameraExecutor, frameAnalyzer!!)
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

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

    private companion object {
        const val OCR_PROCESS_EVERY_N_FRAMES = 3
    }
}