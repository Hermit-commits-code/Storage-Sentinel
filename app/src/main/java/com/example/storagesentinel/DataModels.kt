package com.example.storagesentinel

/**
 * A data class representing a category of junk files.
 */
data class JunkType(val label: String)

/**
 * A data class representing a single junk file or folder found on the device.
 * @param path The full location of the file/folder on the device.
 * @param sizeBytes The size of the file in bytes.
 * @param type The category this junk item belongs to.
 * @param isSelected Whether the user has selected this item for deletion.
 */
data class JunkItem(
    val path: String,
    val sizeBytes: Long,
    val type: JunkType,
    val isSelected: Boolean = true // Default to true; all items are selected initially.
)
