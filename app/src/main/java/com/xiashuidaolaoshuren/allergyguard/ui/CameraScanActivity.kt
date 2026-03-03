package com.xiashuidaolaoshuren.allergyguard.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.xiashuidaolaoshuren.allergyguard.R
import com.xiashuidaolaoshuren.allergyguard.data.AppDatabase
import com.xiashuidaolaoshuren.allergyguard.data.RoomAllergenRepository
import com.xiashuidaolaoshuren.allergyguard.databinding.ActivityCameraScanBinding
import com.xiashuidaolaoshuren.allergyguard.logic.CameraFrameAnalyzer
import com.xiashuidaolaoshuren.allergyguard.ui.camera.CameraScanViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraScanBinding
    private val viewModel: CameraScanViewModel by viewModels {
        val allergenDao = AppDatabase.getInstance(applicationContext).allergenDao()
        val repository = RoomAllergenRepository(allergenDao)
        CameraScanViewModel.Factory(repository)
    }
    private lateinit var cameraExecutor: ExecutorService
    private var frameAnalyzer: CameraFrameAnalyzer? = null

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

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupInsets()
        observeUiState()
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
                viewModel.uiState.collect { state ->
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
                            onOcrError = viewModel::onOcrError
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
}