package com.xiashuidaolaoshuren.allergyguard.logic

import java.util.Locale

data class ScanCoordinate(
    val latitude: Double,
    val longitude: Double
)

object ScanLocationCodec {
    fun encode(coordinate: ScanCoordinate): String {
        return "${coordinate.latitude},${coordinate.longitude}"
    }

    fun decode(raw: String?): ScanCoordinate? {
        if (raw.isNullOrBlank()) {
            return null
        }

        val parts = raw.split(',')
        if (parts.size != 2) {
            return null
        }

        val latitude = parts[0].trim().toDoubleOrNull() ?: return null
        val longitude = parts[1].trim().toDoubleOrNull() ?: return null
        return ScanCoordinate(latitude = latitude, longitude = longitude)
    }

    fun formatForDisplay(coordinate: ScanCoordinate): String {
        return String.format(Locale.getDefault(), "Lat %.5f, Lng %.5f", coordinate.latitude, coordinate.longitude)
    }
}
