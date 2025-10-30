package com.example.storagesentinel.managers

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.example.storagesentinel.database.AnalyticsDatabase
import com.example.storagesentinel.database.CleaningSession
import com.example.storagesentinel.database.StorageSnapshot
import com.example.storagesentinel.model.AnalyticsUiState
import com.example.storagesentinel.model.JunkCategory
import com.example.storagesentinel.model.JunkItem
import com.example.storagesentinel.model.StorageTrend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime

class AnalyticsManager(context: Context) {
    private val database = AnalyticsDatabase.getDatabase(context)
    private val cleaningDao = database.cleaningSessionDao()
    private val snapshotDao = database.storageSnapshotDao()
    
    suspend fun recordCleaningSession(
        storageFreed: Long,
        filesRemoved: Int,
        categoriesCleaned: Set<JunkCategory>,
        sessionType: String = "manual"
    ) {
        val session = CleaningSession(
            storageFreed = storageFreed,
            filesRemoved = filesRemoved,
            categoriesCleaned = categoriesCleaned,
            sessionType = sessionType
        )
        cleaningDao.insertSession(session)
        
        // Also record current storage snapshot
        recordStorageSnapshot()
    }
    
    suspend fun recordStorageSnapshot() {
        val storageInfo = getCurrentStorageInfo()
        val snapshot = StorageSnapshot(
            totalStorage = storageInfo.total,
            usedStorage = storageInfo.used,
            availableStorage = storageInfo.available
        )
        snapshotDao.insertSnapshot(snapshot)
    }
    
    fun getAnalyticsUiState(): Flow<AnalyticsUiState> {
        return combine(
            cleaningDao.getTotalStorageFreed(),
            cleaningDao.getTotalFilesRemoved(),
            cleaningDao.getRecentSessions()
        ) { totalFreed, totalFiles, recentSessions ->
            val currentStorage = getCurrentStorageInfo()
            val trends = generateTrends(recentSessions)
            
            AnalyticsUiState(
                currentStorageUsed = currentStorage.used,
                currentStorageTotal = currentStorage.total,
                totalFreedAllTime = totalFreed ?: 0L,
                totalFilesCleanedAllTime = totalFiles ?: 0,
                trends = trends,
                lastCleanDate = recentSessions.firstOrNull()?.timestamp?.toLocalDate(),
                canCleanToday = 0L // This would be calculated from potential cleanable files
            )
        }
    }
    
    private fun getCurrentStorageInfo(): StorageInfo {
        return try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
            val bytesTotal = stat.blockSizeLong * stat.blockCountLong
            val bytesUsed = bytesTotal - bytesAvailable
            
            StorageInfo(bytesTotal, bytesUsed, bytesAvailable)
        } catch (e: Exception) {
            StorageInfo(0L, 0L, 0L)
        }
    }
    
    private fun generateTrends(sessions: List<CleaningSession>): List<StorageTrend> {
        val now = LocalDateTime.now()
        val trends = mutableListOf<StorageTrend>()
        
        // Today's activity
        val today = sessions.filter { it.timestamp.toLocalDate() == now.toLocalDate() }
        if (today.isNotEmpty()) {
            trends.add(StorageTrend(
                period = "Today",
                storageFreed = today.sumOf { it.storageFreed },
                filesRemoved = today.sumOf { it.filesRemoved },
                mostCommonCategory = today.flatMap { it.categoriesCleaned }
                    .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            ))
        }
        
        // This week's activity
        val weekStart = now.minusDays(7)
        val thisWeek = sessions.filter { it.timestamp.isAfter(weekStart) }
        if (thisWeek.isNotEmpty()) {
            trends.add(StorageTrend(
                period = "This Week",
                storageFreed = thisWeek.sumOf { it.storageFreed },
                filesRemoved = thisWeek.sumOf { it.filesRemoved },
                mostCommonCategory = thisWeek.flatMap { it.categoriesCleaned }
                    .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            ))
        }
        
        // This month's activity
        val monthStart = now.minusDays(30)
        val thisMonth = sessions.filter { it.timestamp.isAfter(monthStart) }
        if (thisMonth.isNotEmpty()) {
            trends.add(StorageTrend(
                period = "This Month",
                storageFreed = thisMonth.sumOf { it.storageFreed },
                filesRemoved = thisMonth.sumOf { it.filesRemoved },
                mostCommonCategory = thisMonth.flatMap { it.categoriesCleaned }
                    .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            ))
        }
        
        return trends
    }
    
    data class StorageInfo(
        val total: Long,
        val used: Long,
        val available: Long
    )
}