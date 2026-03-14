package com.xiashuidaolaoshuren.allergyguard.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiashuidaolaoshuren.allergyguard.data.ScanHistoryRepository
import com.xiashuidaolaoshuren.allergyguard.data.ScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MapViewModel(
    private val scanHistoryRepository: ScanHistoryRepository
) : ViewModel() {
    private val _filter = MutableStateFlow(MapFilter.ALL)
    val filter: StateFlow<MapFilter> = _filter

    val scansForMap: StateFlow<List<ScanResult>> = combine(
        scanHistoryRepository.scanHistoryWithLocation,
        scanHistoryRepository.allergenDetectedScanHistoryWithLocation,
        _filter
    ) { allScans, allergenOnlyScans, filter ->
        when (filter) {
            MapFilter.ALL -> allScans
            MapFilter.ALLERGEN_ONLY -> allergenOnlyScans
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun setFilter(newFilter: MapFilter) {
        _filter.value = newFilter
    }

    suspend fun getScanById(id: String): ScanResult? {
        return scanHistoryRepository.getScanResultById(id)
    }

    enum class MapFilter {
        ALL,
        ALLERGEN_ONLY
    }

    class Factory(
        private val scanHistoryRepository: ScanHistoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
                return MapViewModel(scanHistoryRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
