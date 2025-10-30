package com.example.storagesentinel.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.storagesentinel.managers.SettingsManager
import com.example.storagesentinel.model.JunkCategory
import com.example.storagesentinel.services.ScannerService
import kotlinx.coroutines.flow.first

class CleaningWorker(appContext: Context, workerParams: WorkerParameters): CoroutineWorker(appContext, workerParams) {

    companion object {
        const val AUTO_CLEAN_CATEGORIES_KEY = "AUTO_CLEAN_CATEGORIES"
    }

    override suspend fun doWork(): Result {
        Log.d("CleaningWorker", "Work starting...")

        try {
            val settingsManager = SettingsManager(applicationContext)
            val scannerService = ScannerService(applicationContext, settingsManager)

            // Get the categories to clean from the worker's input data
            val autoCleanCategoryNames = inputData.getStringArray(AUTO_CLEAN_CATEGORIES_KEY) ?: return Result.failure()
            val autoCleanCategories = autoCleanCategoryNames.map { JunkCategory.valueOf(it) }.toSet()

            if (autoCleanCategories.isEmpty()) {
                Log.d("CleaningWorker", "No categories configured for auto-clean. Work finished.")
                return Result.success()
            }

            Log.d("CleaningWorker", "Auto-cleaning categories: $autoCleanCategories")

            // Perform a full scan
            val allJunkItems = scannerService.scan().first()

            // Filter for the items that need to be auto-cleaned
            val itemsToDelete = allJunkItems.filter { it.category in autoCleanCategories }

            if (itemsToDelete.isEmpty()) {
                Log.d("CleaningWorker", "Scan found no items to auto-clean. Work finished.")
                return Result.success()
            }

            // Delete the items
            Log.d("CleaningWorker", "Found ${itemsToDelete.size} items to auto-clean. Deleting...")
            scannerService.delete(itemsToDelete)

            Log.d("CleaningWorker", "Work finished successfully.")
            return Result.success()

        } catch (e: Exception) {
            Log.e("CleaningWorker", "Work failed with exception: ${e.message}")
            return Result.failure()
        }
    }
}
