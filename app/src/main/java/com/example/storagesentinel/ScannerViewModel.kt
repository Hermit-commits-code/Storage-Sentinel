package com.example.storagesentinel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storagesentinel.data.SettingsManager
import com.example.storagesentinel.util.ReportGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScannerUiState(
    val scanState: ScanState = ScanState.READY,
    val scanResults: Map<JunkType, List<JunkItem>> = emptyMap(),
    val showConfirmDialog: Boolean = false,
    val showProUpgradeDialog: Boolean = false,
    val selectionToClean: Set<JunkType> = emptySet(),
    val currentlyCleaning: JunkType? = null,
    val cleanedItems: List<JunkItem> = emptyList(),
    val viewingDetailsFor: JunkType? = null,
    val cleaningErrors: List<String> = emptyList(),
    val isShowingSettings: Boolean = false,
    val currentlyScanningPath: String? = null,
    val isShowingDuplicates: Boolean = false,
    val ignoreList: Set<String> = emptySet(),
    val isProUser: Boolean = false
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val scannerService: ScannerService,
    private val settingsManager: SettingsManager,
    private val reportGenerator: ReportGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val duplicateFilesType = JunkType("Duplicate Files")

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        _uiState.update {
            it.copy(
                ignoreList = settingsManager.getIgnoreList(),
                isProUser = settingsManager.getIsProUser()
            )
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            onScanRequest()
        }
    }

    fun onScanRequest() {
        _uiState.update { it.copy(scanState = ScanState.SCANNING) }
        viewModelScope.launch {
            val results = scannerService.startFullScan { path ->
                _uiState.update { it.copy(currentlyScanningPath = path) }
            }
            _uiState.update { it.copy(scanResults = results, scanState = ScanState.FINISHED, currentlyScanningPath = null) }
        }
    }

    fun onCleanRequest() {
        _uiState.update { it.copy(showConfirmDialog = false, scanState = ScanState.CLEANING, cleaningErrors = emptyList()) }
        viewModelScope.launch {
            val itemsToClean = _uiState.value.scanResults.filterKeys { it in _uiState.value.selectionToClean }.values.flatten().filter { it.isSelected }
            val errors = scannerService.deleteJunkItems(itemsToClean)

            if (errors.isEmpty()) {
                _uiState.update { it.copy(cleanedItems = itemsToClean, scanState = ScanState.CLEAN_COMPLETE) }
            } else {
                _uiState.update { it.copy(cleaningErrors = errors) }
                onScanRequest() // Re-scan
            }
        }
    }

    fun createDummyFiles() {
        viewModelScope.launch {
            scannerService.createDummyJunkFiles()
        }
    }

    fun getReportContent(): String {
        return reportGenerator.generateReport(uiState.value.cleanedItems)
    }

    fun onUpgradeToPro() {
        settingsManager.saveIsProUser(true)
        _uiState.update { it.copy(isProUser = true, showProUpgradeDialog = false) }
    }

    fun onShowProUpgradeDialog() {
        _uiState.update { it.copy(showProUpgradeDialog = true) }
    }

    fun onDismissProUpgradeDialog() {
        _uiState.update { it.copy(showProUpgradeDialog = false) }
    }

    fun onCategoryClick(junkType: JunkType) {
        if (junkType == duplicateFilesType) {
            _uiState.update { it.copy(isShowingDuplicates = true) }
        } else {
            _uiState.update { it.copy(viewingDetailsFor = junkType) }
        }
    }

    fun onBackFromDetails() {
        _uiState.update { it.copy(viewingDetailsFor = null, isShowingDuplicates = false) }
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

    fun onShowSettings() {
        _uiState.update { it.copy(isShowingSettings = true) }
    }

    fun onHideSettings() {
        _uiState.update { it.copy(isShowingSettings = false) }
    }

    fun onAddToIgnoreList(item: JunkItem) {
        settingsManager.addToIgnoreList(item.path)
        loadInitialState() // Refresh the list
    }

    fun onRemoveFromIgnoreList(path: String) {
        settingsManager.removeFromIgnoreList(path)
        loadInitialState() // Refresh the list
    }
}
