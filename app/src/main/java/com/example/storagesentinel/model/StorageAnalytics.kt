package com.example.storagesentinel.model

import java.time.LocalDate

data class StorageAnalytics(
    val date: LocalDate = LocalDate.now(),
    val totalStorageBytes: Long = 0L,
    val usedStorageBytes: Long = 0L,
    val freedStorageBytes: Long = 0L,
    val filesCleanedCount: Int = 0,
    val categoriesCleaned: Set<JunkCategory> = emptySet()
)

data class StorageTrend(
    val period: String, // "Today", "This Week", "This Month"
    val storageFreed: Long,
    val filesRemoved: Int,
    val mostCommonCategory: JunkCategory?
)

data class AnalyticsUiState(
    val currentStorageUsed: Long = 0L,
    val currentStorageTotal: Long = 0L,
    val totalFreedAllTime: Long = 0L,
    val totalFilesCleanedAllTime: Int = 0,
    val trends: List<StorageTrend> = emptyList(),
    val lastCleanDate: LocalDate? = null,
    val canCleanToday: Long = 0L // Estimated cleanable storage
)