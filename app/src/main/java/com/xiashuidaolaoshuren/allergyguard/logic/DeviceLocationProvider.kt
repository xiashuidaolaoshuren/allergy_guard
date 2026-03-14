package com.xiashuidaolaoshuren.allergyguard.logic

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

class DeviceLocationProvider(
    private val appContext: Context,
    private val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(appContext)
) {
    fun hasLocationPermission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return hasFine || hasCoarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationOrNull(): ScanCoordinate? {
        if (!hasLocationPermission()) {
            return null
        }

        val location = runCatching {
            fusedLocationProviderClient.lastLocation.await()
        }.getOrNull() ?: return null

        return ScanCoordinate(
            latitude = location.latitude,
            longitude = location.longitude
        )
    }
}
