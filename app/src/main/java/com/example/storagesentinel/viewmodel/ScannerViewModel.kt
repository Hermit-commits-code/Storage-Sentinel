package com.example.storagesentinel.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.storagesentinel.api.ScannerApi
import com.example.storagesentinel.billing.BillingManager
import com.example.storagesentinel.managers.SettingsManager
import com.example.storagesentinel.model.CategorySelection
import com.example.storagesentinel.model.JunkCategory
import com.example.storagesentinel.model.JunkItem
import com.example.storagesentinel.model.ScannerUiState // CORRECT IMPORT ADDED
import com.example.storagesentinel.services.ScannerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application)
    private val scannerService = ScannerService(application, settingsManager)
    
    // Get billing manager from application
    private val billingManager = (application as com.example.storagesentinel.StorageSentinelApplication).billingManager

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val accumulatedResults = mutableListOf<JunkItem>()

    init {
        // Listen to billing manager for pro status instead of settings
        viewModelScope.launch {
            billingManager.isProVersion.collect { isPro ->
                _uiState.update { it.copy(isProUser = isPro) }
            }
        }
    }

    fun startScan() {
        viewModelScope.launch {
            scannerService.scan()
                .onStart {
                    _uiState.update { it.copy(isScanning = true, scanResults = emptyList(), totalSelectedSize = 0L) }
                    accumulatedResults.clear()
                }
                .catch { e ->
                    println("Scanner API error: ${e.message}")
                }
                .onCompletion {
                    _uiState.update { it.copy(isScanning = false) }
                }
                .collect { newBatch ->
                    accumulatedResults.addAll(newBatch)
                    val categorySelections = processResults(accumulatedResults)
                    _uiState.update { it.copy(scanResults = categorySelections) }
                }
        }
    }

    fun cleanSelectedItems() {
        viewModelScope.launch {
            val selectedCategories = _uiState.value.scanResults
                .filter { it.isSelected }
                .map { it.category }
                .toSet()

            val itemsToDelete = accumulatedResults.filter { it.category in selectedCategories }

            Log.d("ViewModel", "Cleaning ${itemsToDelete.size} items in categories: $selectedCategories")

            scannerService.delete(itemsToDelete)

            startScan()
        }
    }

    private fun processResults(items: List<JunkItem>): List<CategorySelection> {
        return items.groupBy { it.category }
            .map { (category, junkItems) ->
                CategorySelection(
                    category = category,
                    totalSize = junkItems.sumOf { it.sizeInBytes },
                    totalCount = junkItems.size,
                    isSelected = false
                )
            }
            .sortedBy { it.category.ordinal }
    }

    fun handleCategorySelection(category: JunkCategory, isSelected: Boolean) {
        val isProUser = _uiState.value.isProUser
        val proFeatures = setOf(JunkCategory.RESIDUAL_APP_DATA, JunkCategory.LARGE_FILE, JunkCategory.DUPLICATE_FILE)
        val isProFeature = category in proFeatures

        if (isSelected && isProFeature && !isProUser) {
            _uiState.update { it.copy(showProUpgradeDialog = true) }
            return
        }

        val newSelections = _uiState.value.scanResults.map {
            if (it.category == category) {
                it.copy(isSelected = isSelected)
            } else {
                it
            }
        }

        val newTotalSize = newSelections.filter { it.isSelected }.sumOf { it.totalSize }

        _uiState.update { it.copy(scanResults = newSelections, totalSelectedSize = newTotalSize) }
    }

    fun dismissProUpgradeDialog() {
        _uiState.update { it.copy(showProUpgradeDialog = false) }
    }

    fun developerToggleProStatus() {
        viewModelScope.launch {
            val currentStatus = billingManager.isProVersion.first()
            if (currentStatus) {
                billingManager.resetProVersion()
            } else {
                billingManager.simulateProPurchase()
            }
        }
    }

    // New function for the UI to call
    fun developerCreateFakeJunk() {
        viewModelScope.launch {
            scannerService.developer_createFakeJunk()
        }
    }
}
