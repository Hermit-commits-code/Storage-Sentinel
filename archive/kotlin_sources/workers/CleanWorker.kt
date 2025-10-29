package com.example.storagesentinel.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.storagesentinel.JunkType
import com.example.storagesentinel.ScannerService
import com.example.storagesentinel.data.SettingsManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CleanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val scannerService: ScannerService,
    private val settingsManager: SettingsManager
) : CoroutineWorker(context, workerParams) {

    private val proCategories = listOf("Residual App Data", "Duplicate Files", "Large Files")

    override suspend fun doWork(): Result {
        return try {
            val defaultSelectionLabels = settingsManager.getDefaultSelection()
            val isPro = settingsManager.getIsProUser()

            if (defaultSelectionLabels.isEmpty()) {
                return Result.success() // No defaults saved
            }

            val selectionToClean = defaultSelectionLabels.map { JunkType(it) }.filter {
                !proCategories.contains(it.label) || isPro
            }.toSet()

            if (selectionToClean.isEmpty()) {
                return Result.success() // Nothing to clean after filtering for PRO status
            }

            val scanResults = scannerService.startFullScan { /* no-op */ }

            val itemsToClean = scanResults
                .filterKeys { it in selectionToClean }
                .values
                .flatten()
                .filter { it.isSelected }

            if (itemsToClean.isNotEmpty()) {
                scannerService.deleteJunkItems(itemsToClean)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
