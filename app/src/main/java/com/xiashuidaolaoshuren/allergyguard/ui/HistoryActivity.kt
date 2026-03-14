package com.xiashuidaolaoshuren.allergyguard.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.xiashuidaolaoshuren.allergyguard.R
import com.xiashuidaolaoshuren.allergyguard.data.AppDatabase
import com.xiashuidaolaoshuren.allergyguard.data.RoomScanHistoryRepository
import com.xiashuidaolaoshuren.allergyguard.databinding.ActivityHistoryBinding
import com.xiashuidaolaoshuren.allergyguard.ui.history.HistoryViewModel
import com.xiashuidaolaoshuren.allergyguard.ui.history.ScanHistoryAdapter
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyAdapter: ScanHistoryAdapter

    private val viewModel: HistoryViewModel by viewModels {
        val scanHistoryDao = AppDatabase.getInstance(applicationContext).scanHistoryDao()
        val repository = RoomScanHistoryRepository(scanHistoryDao)
        HistoryViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = getString(R.string.history_title)

        setupInsets()
        setupRecyclerView()
        observeScanHistory()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.historyRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = ScanHistoryAdapter { scanResult ->
            openMap(selectedScanId = scanResult.id)
        }
        binding.recyclerHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
        }

        binding.buttonViewMap.setOnClickListener {
            openMap(selectedScanId = null)
        }
    }

    private fun openMap(selectedScanId: String?) {
        val intent = Intent(this, MapActivity::class.java)
        if (!selectedScanId.isNullOrBlank()) {
            intent.putExtra(MapActivity.EXTRA_SELECTED_SCAN_ID, selectedScanId)
        }
        startActivity(intent)
    }

    private fun observeScanHistory() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scanHistory.collect { history ->
                    historyAdapter.submitList(history)
                    binding.textHistoryEmpty.visibility = if (history.isEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
        }
    }
}
