package com.example.storagesentinel

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScannerUiState(
    val scanState: ScanState = ScanState.READY,
    val scanResults: Map<JunkType, List<JunkItem>> = emptyMap(),
    val showConfirmDialog: Boolean = false,
    val selectionToClean: Set<JunkType> = emptySet(),
    val currentlyCleaning: JunkType? = null,
    val cleanedItems: List<JunkItem> = emptyList(),
    val viewingDetailsFor: JunkType? = null,
    val cleaningErrors: List<String> = emptyList()
)

class ScannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onPermissionResult(granted: Boolean, context: Context) {
        if (granted) {
            onScanRequest(context)
        }
    }

    fun onScanRequest(context: Context) {
        _uiState.update { it.copy(scanState = ScanState.SCANNING) }
        viewModelScope.launch {
            val scannerService = ScannerService(Environment.getExternalStorageDirectory(), context)
            val results = scannerService.startFullScan()
            _uiState.update { it.copy(scanResults = results, scanState = ScanState.FINISHED) }
        }
    }

    fun onCleanRequest(context: Context) {
        _uiState.update { it.copy(showConfirmDialog = false, scanState = ScanState.CLEANING, cleaningErrors = emptyList()) }
        viewModelScope.launch {
            val scannerService = ScannerService(Environment.getExternalStorageDirectory(), context)
            val itemsToClean = _uiState.value.scanResults.filterKeys { it in _uiState.value.selectionToClean }.values.flatten().filter { it.isSelected }
            val errors = scannerService.deleteJunkItems(itemsToClean)

            if (errors.isEmpty()) {
                _uiState.update { it.copy(cleanedItems = itemsToClean, scanState = ScanState.CLEAN_COMPLETE) }
            } else {
                _uiState.update { it.copy(cleaningErrors = errors) }
                onScanRequest(context) // Re-scan
            }
        }
    }

    fun onCategoryClick(junkType: JunkType) {
        _uiState.update { it.copy(viewingDetailsFor = junkType) }
    }

    fun onBackFromDetails() {
        _uiState.update { it.copy(viewingDetailsFor = null) }
    }

    fun onShowConfirmDialog() {
        _uiState.update { it.copy(showConfirmDialog = true) }
    }

    fun onDismissConfirmDialog() {
        _uiState.update { it.copy(showConfirmDialog = false) }
    }

    fun onCategorySelectionChanged(junkType: JunkType, isSelected: Boolean) {
        val newSelection = _uiState.value.selectionToClean.toMutableSet()
        if (isSelected) {
            newSelection.add(junkType)
        } else {
            newSelection.remove(junkType)
        }
        _uiState.update { it.copy(selectionToClean = newSelection) }
    }

    fun onItemSelectionChanged(junkType: JunkType, item: JunkItem, isSelected: Boolean) {
        val currentItems = _uiState.value.scanResults[junkType]?.toMutableList() ?: return
        val itemIndex = currentItems.indexOfFirst { it.path == item.path }
        if (itemIndex != -1) {
            val updatedItem = currentItems[itemIndex].copy(isSelected = isSelected)
            currentItems[itemIndex] = updatedItem
            val newResults = _uiState.value.scanResults + (junkType to currentItems)
            _uiState.update { it.copy(scanResults = newResults) }
        }
    }

    fun onFinishCleaning() {
        _uiState.value = ScannerUiState() // Reset to initial state
    }

    fun onErrorsShown() {
        _uiState.update { it.copy(cleaningErrors = emptyList()) }
    }
}
