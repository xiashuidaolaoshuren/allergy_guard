package com.xiashuidaolaoshuren.allergyguard.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.xiashuidaolaoshuren.allergyguard.R
import com.xiashuidaolaoshuren.allergyguard.data.AppDatabase
import com.xiashuidaolaoshuren.allergyguard.data.RoomScanHistoryRepository
import com.xiashuidaolaoshuren.allergyguard.data.ScanResult
import com.xiashuidaolaoshuren.allergyguard.databinding.ActivityMapBinding
import com.xiashuidaolaoshuren.allergyguard.logic.ScanLocationCodec
import com.xiashuidaolaoshuren.allergyguard.ui.map.MapViewModel
import kotlinx.coroutines.launch

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMapBinding
    private var googleMap: GoogleMap? = null
    private var latestScans: List<ScanResult> = emptyList()
    private var selectedScanId: String? = null

    private val viewModel: MapViewModel by viewModels {
        val repository = RoomScanHistoryRepository(AppDatabase.getInstance(applicationContext).scanHistoryDao())
        MapViewModel.Factory(repository)
    }

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                enableMyLocationLayerIfPermitted()
            } else {
                Snackbar.make(binding.mapRoot, R.string.location_permission_denied_optional, Snackbar.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = getString(R.string.map_title)

        selectedScanId = intent.getStringExtra(EXTRA_SELECTED_SCAN_ID)

        setupInsets()
        setupFilterUi()
        setupMap()
        observeMapState()
        ensureLocationPermissionOptional()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        enableMyLocationLayerIfPermitted()
        renderMarkers(latestScans)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mapRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragmentContainer)
            as com.google.android.gms.maps.SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupFilterUi() {
        binding.chipFilterAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.setFilter(MapViewModel.MapFilter.ALL)
            }
        }

        binding.chipFilterAllergens.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.setFilter(MapViewModel.MapFilter.ALLERGEN_ONLY)
            }
        }
    }

    private fun observeMapState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scansForMap.collect { scans ->
                    latestScans = scans
                    binding.textMapEmpty.visibility = if (scans.isEmpty()) View.VISIBLE else View.GONE
                    renderMarkers(scans)
                }
            }
        }
    }

    private fun renderMarkers(scans: List<ScanResult>) {
        val map = googleMap ?: return
        map.clear()

        if (scans.isEmpty()) {
            return
        }

        var selectedLatLng: LatLng? = null
        val boundsBuilder = LatLngBounds.Builder()

        scans.forEach { scan ->
            val coordinate = ScanLocationCodec.decode(scan.location) ?: return@forEach
            val position = LatLng(coordinate.latitude, coordinate.longitude)
            val title = if (scan.hasAllergens) {
                getString(R.string.history_status_allergen_detected)
            } else {
                getString(R.string.history_status_safe)
            }

            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(title)
                    .snippet(scan.textContent)
            )

            boundsBuilder.include(position)
            if (scan.id == selectedScanId) {
                selectedLatLng = position
            }
        }

        if (selectedLatLng != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 16f))
            selectedScanId = null
            return
        }

        runCatching {
            val bounds = boundsBuilder.build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        }.onFailure {
            val firstCoordinate = ScanLocationCodec.decode(scans.firstOrNull()?.location)
            if (firstCoordinate != null) {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(firstCoordinate.latitude, firstCoordinate.longitude),
                        14f
                    )
                )
            }
        }
    }

    private fun ensureLocationPermissionOptional() {
        val isGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!isGranted) {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun enableMyLocationLayerIfPermitted() {
        val map = googleMap ?: return
        val isGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            map.isMyLocationEnabled = true
        }
    }

    companion object {
        const val EXTRA_SELECTED_SCAN_ID = "extra_selected_scan_id"
    }
}
