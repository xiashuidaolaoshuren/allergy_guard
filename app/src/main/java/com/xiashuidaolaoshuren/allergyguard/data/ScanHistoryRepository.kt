package com.xiashuidaolaoshuren.allergyguard.data

import kotlinx.coroutines.flow.Flow

interface ScanHistoryRepository {
    val scanHistory: Flow<List<ScanResult>>
    val scanHistoryWithLocation: Flow<List<ScanResult>>
    val allergenDetectedScanHistoryWithLocation: Flow<List<ScanResult>>

    suspend fun insertScanResult(scanResult: ScanResult)
    suspend fun getScanResultById(id: String): ScanResult?
    suspend fun deleteScanResultById(id: String)
    suspend fun clearScanHistory()
}

class RoomScanHistoryRepository(
    private val scanHistoryDao: ScanHistoryDao
) : ScanHistoryRepository {
    override val scanHistory: Flow<List<ScanResult>> = scanHistoryDao.getScanHistory()
    override val scanHistoryWithLocation: Flow<List<ScanResult>> = scanHistoryDao.getScanHistoryWithLocation()
    override val allergenDetectedScanHistoryWithLocation: Flow<List<ScanResult>> =
        scanHistoryDao.getAllergenDetectedScanHistoryWithLocation()

    override suspend fun insertScanResult(scanResult: ScanResult) {
        scanHistoryDao.insertScanResult(scanResult)
    }

    override suspend fun getScanResultById(id: String): ScanResult? {
        return scanHistoryDao.getScanResultById(id)
    }

    override suspend fun deleteScanResultById(id: String) {
        scanHistoryDao.deleteScanResultById(id)
    }

    override suspend fun clearScanHistory() {
        scanHistoryDao.clearScanHistory()
    }
}
