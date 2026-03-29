package com.xiashuidaolaoshuren.allergyguard.ui.history

import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiashuidaolaoshuren.allergyguard.data.ScanHistoryRepository
import com.xiashuidaolaoshuren.allergyguard.data.ScanResult
import com.xiashuidaolaoshuren.allergyguard.logic.ScanLocationCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class HistoryViewModel(
    private val appContext: Context,
    private val scanHistoryRepository: ScanHistoryRepository
) : ViewModel() {
    private val _scanHistory = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanHistory: StateFlow<List<ScanResult>> = _scanHistory.asStateFlow()
    private val _addressMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val addressMap: StateFlow<Map<String, String>> = _addressMap.asStateFlow()
    private val addressCache = mutableMapOf<String, String>()

    init {
        viewModelScope.launch {
            scanHistoryRepository.scanHistory.collect { history ->
                _scanHistory.value = history
                resolveAddresses(history)
            }
        }
    }

    fun deleteScan(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            scanHistoryRepository.deleteScanResultById(id)
        }
    }

    private fun resolveAddresses(history: List<ScanResult>) {
        val unresolved = history
            .mapNotNull { it.location }
            .distinct()
            .filter { !addressCache.containsKey(it) }

        if (unresolved.isEmpty()) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            unresolved.forEach { rawLocation ->
                val coordinate = ScanLocationCodec.decode(rawLocation)
                val address = if (coordinate != null) {
                    resolveApproximateAddress(coordinate.latitude, coordinate.longitude)
                        ?: ScanLocationCodec.formatForDisplay(coordinate)
                } else {
                    appContext.getString(com.xiashuidaolaoshuren.allergyguard.R.string.history_location_unknown)
                }

                addressCache[rawLocation] = address
            }
            _addressMap.value = addressCache.toMap()
        }
    }

    private suspend fun resolveApproximateAddress(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) {
            return null
        }

        val geocoder = Geocoder(appContext, Locale.getDefault())
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocodeApi33(geocoder, latitude, longitude)
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
        } ?: return null

        val area = address.subLocality ?: address.locality
        val city = address.locality ?: address.adminArea ?: address.countryName
        return when {
            !area.isNullOrBlank() && !city.isNullOrBlank() && area != city -> "$area, $city"
            !city.isNullOrBlank() -> city
            !area.isNullOrBlank() -> area
            else -> null
        }
    }

    private suspend fun geocodeApi33(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): android.location.Address? = suspendCancellableCoroutine { continuation ->
        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<android.location.Address>) {
                continuation.resume(addresses.firstOrNull())
            }

            override fun onError(errorMessage: String?) {
                continuation.resume(null)
            }
        })
    }

    class Factory(
        private val appContext: Context,
        private val scanHistoryRepository: ScanHistoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                return HistoryViewModel(appContext, scanHistoryRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
