package com.example.storagesentinel.model

// HIGH-VALUE categories are those that MUST be locked for non-PRO users.
enum class JunkCategory {
    EMPTY_FOLDER,        // LOW VALUE - FREE
    ZERO_BYTE_FILE,      // LOW VALUE - FREE
    TEMP_CACHE,          // LOW VALUE - FREE
    RESIDUAL_APP_DATA,   // HIGH VALUE - PRO-LOCKED
    LARGE_FILE,          // HIGH VALUE - PRO-LOCKED
    DUPLICATE_FILE,      // HIGH VALUE - PRO-LOCKED
}

// Data model for a single file/folder found by the ScannerService.
data class JunkItem(
    val id: String,
    val name: String,
    val path: String,
    val sizeInBytes: Long,
    val category: JunkCategory,
    val contentHash: String? = null // Added for grouping duplicates
)

// Data structure used in the ScannerUiState to display results in the UI.
data class CategorySelection(
    val category: JunkCategory,
    val totalSize: Long,
    val totalCount: Int,
    val isSelected: Boolean
)

// The primary UI State flow.
data class ScannerUiState(
    val isScanning: Boolean = false,
    val scanResults: List<CategorySelection> = emptyList(),
    val isProUser: Boolean = false, // The key flag for monetization
    val showProUpgradeDialog: Boolean = false,
    val totalSelectedSize: Long = 0L, // The new property for our summary
    val remainingScansToday: Int = 3, // Free tier usage tracking
    val canScan: Boolean = true // Whether user can initiate a scan
)
