package com.xiashuidaolaoshuren.allergyguard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    fun getScanHistory(): Flow<List<ScanResult>>

    @Query("SELECT * FROM scan_results WHERE hasAllergens = 1 ORDER BY timestamp DESC")
    fun getAllergenDetectedScanHistory(): Flow<List<ScanResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanResult(scanResult: ScanResult)

    @Query("DELETE FROM scan_results WHERE id = :id")
    suspend fun deleteScanResultById(id: String)

    @Query("DELETE FROM scan_results")
    suspend fun clearScanHistory()
}
