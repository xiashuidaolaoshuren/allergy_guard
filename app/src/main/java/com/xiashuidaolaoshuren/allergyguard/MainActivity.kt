package com.xiashuidaolaoshuren.allergyguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.transition.Fade
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.xiashuidaolaoshuren.allergyguard.databinding.ActivityMainBinding
import com.xiashuidaolaoshuren.allergyguard.ui.AllergenListActivity
import com.xiashuidaolaoshuren.allergyguard.ui.CameraScanActivity
import com.xiashuidaolaoshuren.allergyguard.ui.HistoryActivity
import com.xiashuidaolaoshuren.allergyguard.ui.SettingsActivity
import com.xiashuidaolaoshuren.allergyguard.util.NotificationHelper

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var notificationPermissionRequested = false

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createChannels(this)

        window.exitTransition = Fade()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupEntranceAnimations()

        binding.cardManageAllergens.setOnClickListener {
            startActivity(Intent(this, AllergenListActivity::class.java))
        }

        binding.cardStartScan.setOnClickListener {
            startActivity(Intent(this, CameraScanActivity::class.java))
        }

        binding.cardViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || notificationPermissionRequested) return
        // Request only after first frame/window focus to avoid splash screen pre-draw deadlock.
        notificationPermissionRequested = true
        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupEntranceAnimations() {
        val viewsToAnimate = listOf(
            binding.logoContainer,
            binding.cardStartScan,
            binding.cardManageAllergens,
            binding.cardViewHistory,
            binding.cardSettings
        )

        viewsToAnimate.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(100L * index)
                .start()
        }
    }
}