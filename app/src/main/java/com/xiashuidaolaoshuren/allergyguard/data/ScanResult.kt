package com.xiashuidaolaoshuren.allergyguard.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scan_results",
    indices = [Index(value = ["timestamp"])]
)
data class ScanResult(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val textContent: String,
    val hasAllergens: Boolean = false,
    val location: String? = null
)
