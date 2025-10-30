package com.example.storagesentinel.api

import com.example.storagesentinel.model.JunkItem
import kotlinx.coroutines.flow.Flow

/**
 * Defines the contract for the scanning and cleaning service.
 * Higher-level components (ViewModels) will use this interface to interact with the service.
 */
interface ScannerApi {
    /**
     * Initiates a file system scan and returns a Flow that emits batches of found junk items.
     */
    fun scan(): Flow<List<JunkItem>>

    /**
     * Deletes the given list of junk items from the file system.
     * This is a suspending function and must be called from a coroutine.
     */
    suspend fun delete(items: List<JunkItem>)
}
