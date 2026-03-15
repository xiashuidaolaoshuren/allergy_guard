package com.xiashuidaolaoshuren.allergyguard

import android.content.Intent
import android.os.Bundle
import android.transition.Fade
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.xiashuidaolaoshuren.allergyguard.databinding.ActivityMainBinding
import com.xiashuidaolaoshuren.allergyguard.ui.AllergenListActivity
import com.xiashuidaolaoshuren.allergyguard.ui.CameraScanActivity
import com.xiashuidaolaoshuren.allergyguard.ui.HistoryActivity
import com.xiashuidaolaoshuren.allergyguard.ui.SettingsActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.exitTransition = Fade()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupEntranceAnimations()

        binding.buttonManageAllergens.setOnClickListener {
            startActivity(Intent(this, AllergenListActivity::class.java))
        }

        binding.buttonStartScan.setOnClickListener {
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, binding.logoContainer, "transition_scan")
            startActivity(Intent(this, CameraScanActivity::class.java), options.toBundle())
        }

        binding.buttonViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.buttonSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupEntranceAnimations() {
        val viewsToAnimate = listOf(
            binding.logoContainer,
            binding.buttonStartScan,
            binding.buttonManageAllergens,
            binding.buttonViewHistory,
            binding.buttonSettings
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