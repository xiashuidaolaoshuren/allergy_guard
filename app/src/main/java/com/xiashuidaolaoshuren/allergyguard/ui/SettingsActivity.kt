package com.xiashuidaolaoshuren.allergyguard.ui

import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.xiashuidaolaoshuren.allergyguard.R
import com.xiashuidaolaoshuren.allergyguard.databinding.ActivitySettingsBinding
import com.xiashuidaolaoshuren.allergyguard.logic.TranslationManager
import com.xiashuidaolaoshuren.allergyguard.receiver.ConnectivityReceiver
import com.xiashuidaolaoshuren.allergyguard.ui.translation.TranslationLanguageAdapter
import com.xiashuidaolaoshuren.allergyguard.ui.translation.TranslationLanguageUiModel

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var adapter: TranslationLanguageAdapter
    private var languageItems: List<TranslationLanguageUiModel> = emptyList()
    private var connectivityReceiver: ConnectivityReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = "Settings"
        setupInsets()
        setupRecyclerView()
        loadLanguages()
    }

    override fun onStart() {
        super.onStart()
        connectivityReceiver = ConnectivityReceiver { onWifiConnected() }
        @Suppress("DEPRECATION")
        registerReceiver(
            connectivityReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
    }

    override fun onStop() {
        super.onStop()
        connectivityReceiver?.let { unregisterReceiver(it) }
        connectivityReceiver = null
    }

    private fun onWifiConnected() {
        val pendingItems = languageItems.filter { !it.isDownloaded && !it.isDownloading }
        if (pendingItems.isEmpty()) return

        Snackbar.make(
            binding.root,
            getString(R.string.snackbar_wifi_download_prompt, pendingItems.size),
            Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.snackbar_action_download_all)) {
            pendingItems.forEach { item ->
                markDownloading(item.languageTag)
                TranslationManager.downloadModel(item.languageTag)
                    .addOnSuccessListener {
                        setDownloaded(item.languageTag, isDownloaded = true)
                    }
                    .addOnFailureListener { error ->
                        setDownloaded(item.languageTag, isDownloaded = false)
                        Toast.makeText(
                            this,
                            getString(R.string.translation_download_failed, item.displayName, error.message ?: ""),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }.show()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.settingsRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        adapter = TranslationLanguageAdapter { item ->
            markDownloading(item.languageTag)
            TranslationManager.downloadModel(item.languageTag)
                .addOnSuccessListener {
                    setDownloaded(item.languageTag, isDownloaded = true)
                }
                .addOnFailureListener { error ->
                    setDownloaded(item.languageTag, isDownloaded = false)
                    Toast.makeText(
                        this,
                        getString(R.string.translation_download_failed, item.displayName, error.message ?: ""),
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        binding.recyclerTranslationLanguages.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = this@SettingsActivity.adapter
        }
    }

    private fun loadLanguages() {
        val supported = TranslationManager.supportedLanguages()
        languageItems = supported.map {
            TranslationLanguageUiModel(
                languageTag = it.languageTag,
                displayName = it.displayName,
                flagEmoji = it.flagEmoji,
                isDownloaded = false,
                isDownloading = false
            )
        }
        adapter.submitList(languageItems)

        supported.forEach { language ->
            TranslationManager.isModelDownloaded(language.languageTag)
                .addOnSuccessListener { isDownloaded ->
                    setDownloaded(language.languageTag, isDownloaded)
                }
        }
    }

    private fun markDownloading(languageTag: String) {
        languageItems = languageItems.map {
            if (it.languageTag == languageTag) {
                it.copy(isDownloading = true)
            } else {
                it
            }
        }
        adapter.submitList(languageItems)
    }

    private fun setDownloaded(languageTag: String, isDownloaded: Boolean) {
        languageItems = languageItems.map {
            if (it.languageTag == languageTag) {
                it.copy(isDownloaded = isDownloaded, isDownloading = false)
            } else {
                it
            }
        }
        adapter.submitList(languageItems)
    }
}