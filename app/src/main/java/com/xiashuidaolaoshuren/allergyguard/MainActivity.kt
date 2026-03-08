package com.xiashuidaolaoshuren.allergyguard

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.xiashuidaolaoshuren.allergyguard.databinding.ActivityMainBinding
import com.xiashuidaolaoshuren.allergyguard.ui.AllergenListActivity
import com.xiashuidaolaoshuren.allergyguard.ui.CameraScanActivity
import com.xiashuidaolaoshuren.allergyguard.ui.HistoryActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.buttonManageAllergens.setOnClickListener {
            startActivity(Intent(this, AllergenListActivity::class.java))
        }

        binding.buttonStartScan.setOnClickListener {
            startActivity(Intent(this, CameraScanActivity::class.java))
        }

        binding.buttonViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }
}