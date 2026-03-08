package com.xiashuidaolaoshuren.allergyguard.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiashuidaolaoshuren.allergyguard.data.ScanHistoryRepository
import com.xiashuidaolaoshuren.allergyguard.data.ScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val scanHistoryRepository: ScanHistoryRepository
) : ViewModel() {
    private val _scanHistory = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanHistory: StateFlow<List<ScanResult>> = _scanHistory.asStateFlow()

    init {
        viewModelScope.launch {
            scanHistoryRepository.scanHistory.collect { history ->
                _scanHistory.value = history
            }
        }
    }

    class Factory(
        private val scanHistoryRepository: ScanHistoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                return HistoryViewModel(scanHistoryRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
