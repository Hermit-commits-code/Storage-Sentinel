package com.example.storagesentinel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.storagesentinel.data.SettingsManager
import com.example.storagesentinel.util.ReportGenerator
import com.example.storagesentinel.workers.CleanWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
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
    val isProUser: Boolean = false,
    val isScheduledCleaningEnabled: Boolean = false,
    val scheduledCleaningFrequency: String = SettingsManager.DEFAULT_SCHEDULE_FREQUENCY
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context, // For WorkManager
    private val scannerService: ScannerService,
    private val settingsManager: SettingsManager,
    private val reportGenerator: ReportGenerator,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val proCategories = listOf("Residual App Data", "Duplicate Files", "Large Files")

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        _uiState.update {
            it.copy(
                ignoreList = settingsManager.getIgnoreList(),
                isProUser = settingsManager.getIsProUser(),
                isScheduledCleaningEnabled = settingsManager.getScheduledCleaningEnabled(),
                scheduledCleaningFrequency = settingsManager.getScheduledCleaningFrequency()
            )
        }
    }

    private fun applyDefaultSelections() {
        val defaultSelectionLabels = settingsManager.getDefaultSelection()
        val isPro = _uiState.value.isProUser

        val selection = if (defaultSelectionLabels.isEmpty()) {
            // First run or no defaults saved: select all non-PRO categories by default
            _uiState.value.scanResults.keys.filter { !proCategories.contains(it.label) }.toSet()
        } else {
            // User has saved defaults: load them, but respect the paywall
            defaultSelectionLabels.map { JunkType(it) }.filter { !proCategories.contains(it.label) || isPro }.toSet()
        }
        
        _uiState.update { it.copy(selectionToClean = selection) }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            onScanRequest()
        }
    }

    fun onScanRequest() {
        _uiState.update { it.copy(scanState = ScanState.SCANNING, selectionToClean = emptySet()) }
        viewModelScope.launch {
            val results = scannerService.startFullScan { path ->
                _uiState.update { it.copy(currentlyScanningPath = path) }
            }
            _uiState.update { it.copy(scanResults = results, scanState = ScanState.FINISHED) }
            applyDefaultSelections()
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

    fun onSaveDefaults() {
        val selectionToSave = uiState.value.selectionToClean.map { it.label }.toSet()
        settingsManager.saveDefaultSelection(selectionToSave)
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

    fun onScheduledCleaningEnabledChanged(enabled: Boolean) {
        settingsManager.saveScheduledCleaningEnabled(enabled)
        _uiState.update { it.copy(isScheduledCleaningEnabled = enabled) }
        scheduleCleaningWorker()
    }

    fun onScheduledCleaningFrequencyChanged(frequency: String) {
        settingsManager.saveScheduledCleaningFrequency(frequency)
        _uiState.update { it.copy(scheduledCleaningFrequency = frequency) }
        scheduleCleaningWorker()
    }
    
    private fun scheduleCleaningWorker(){
        if (uiState.value.isScheduledCleaningEnabled && uiState.value.isProUser) {
            val repeatInterval = when (uiState.value.scheduledCleaningFrequency) {
                "Daily" -> 1L
                "Weekly" -> 7L
                "Monthly" -> 30L
                else -> 7L // Default to weekly
            }
            val timeUnit = TimeUnit.DAYS

            val workRequest = PeriodicWorkRequestBuilder<CleanWorker>(repeatInterval, timeUnit)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "ScheduledCleanUp",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        } else {
            workManager.cancelUniqueWork("ScheduledCleanUp")
        }
    }

    fun onCategoryClick(junkType: JunkType) {
        if (junkType.label in proCategories && !uiState.value.isProUser) {
            onShowProUpgradeDialog()
        } else {
             if (junkType.label == "Duplicate Files") {
                _uiState.update { it.copy(isShowingDuplicates = true) }
            } else {
                _uiState.update { it.copy(viewingDetailsFor = junkType) }
            }
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
            if (junkType.label in proCategories && !uiState.value.isProUser) {
                onShowProUpgradeDialog()
                return
            }
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
            // Keep category-level selection in sync: if any item in the category is selected, include the category
            // If none are selected, remove the category from selectionToClean
            val anySelected = currentItems.any { it.isSelected }
            val newSelection = _uiState.value.selectionToClean.toMutableSet()
            if (anySelected) {
                // Respect PRO gating: do not auto-add PRO categories for free users
                if (!(junkType.label in proCategories && !_uiState.value.isProUser)) {
                    newSelection.add(junkType)
                }
            } else {
                newSelection.remove(junkType)
            }
            _uiState.update { it.copy(selectionToClean = newSelection) }
        }
    }

    fun onFinishCleaning() {
        _uiState.value = ScannerUiState()
        loadInitialState()
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
        loadInitialState()
    }

    fun onRemoveFromIgnoreList(path: String) {
        settingsManager.removeFromIgnoreList(path)
        loadInitialState()
    }
}
