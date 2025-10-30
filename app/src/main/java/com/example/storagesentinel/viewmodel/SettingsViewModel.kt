package com.example.storagesentinel.viewmodel

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storagesentinel.managers.SettingsManager
import com.example.storagesentinel.model.JunkCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isAutoCleanEnabled: Boolean = false,
    val autoCleanFrequency: String = "WEEKLY",
    val autoCleanCategories: Set<JunkCategory> = emptySet(),
    val largeFileThresholdMb: Long = 100L,
    val showBatteryOptimizationDialog: Boolean = false
)

class SettingsViewModel(private val settingsManager: SettingsManager, private val application: Application) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // This combines the flows from the manager and updates our single UI state
        combine(
            settingsManager.isAutoCleanEnabled,
            settingsManager.autoCleanFrequency,
            settingsManager.autoCleanCategories,
            settingsManager.largeFileThresholdMb
        ) { isEnabled, frequency, categories, threshold ->
            // Create a temporary state with the new settings
            SettingsUiState(
                isAutoCleanEnabled = isEnabled,
                autoCleanFrequency = frequency,
                autoCleanCategories = categories.mapNotNull { runCatching { JunkCategory.valueOf(it) }.getOrNull() }.toSet(),
                largeFileThresholdMb = threshold
            )
        }.onEach { newSettingsState ->
            // Atomically update the UI state, preserving the dialog flag
            _uiState.update { currentState ->
                currentState.copy(
                    isAutoCleanEnabled = newSettingsState.isAutoCleanEnabled,
                    autoCleanFrequency = newSettingsState.autoCleanFrequency,
                    autoCleanCategories = newSettingsState.autoCleanCategories,
                    largeFileThresholdMb = newSettingsState.largeFileThresholdMb
                    // Crucially, we do NOT touch the showBatteryOptimizationDialog flag here
                )
            }
        }.launchIn(viewModelScope)
    }

    fun setLargeFileThreshold(threshold: Long) {
        viewModelScope.launch { settingsManager.setLargeFileThresholdMb(threshold) }
    }

    fun onAutoCleanToggled() {
        val shouldBeEnabled = !_uiState.value.isAutoCleanEnabled
        if (shouldBeEnabled) {
            val powerManager = application.getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(application.packageName)
            if (!isIgnoringOptimizations) {
                _uiState.update { it.copy(showBatteryOptimizationDialog = true) }
                return
            }
        }
        viewModelScope.launch { settingsManager.setAutoCleanEnabled(shouldBeEnabled) }
    }

    fun onBatteryOptimizationResult() {
        val powerManager = application.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(application.packageName)) {
            viewModelScope.launch { settingsManager.setAutoCleanEnabled(true) }
        }
        dismissBatteryOptimizationDialog()
    }

    fun dismissBatteryOptimizationDialog() {
        _uiState.update { it.copy(showBatteryOptimizationDialog = false) }
    }

    fun setAutoCleanFrequency(frequency: String) {
        viewModelScope.launch { settingsManager.setAutoCleanFrequency(frequency) }
    }

    fun setAutoCleanCategories(categories: Set<JunkCategory>) {
        viewModelScope.launch { settingsManager.setAutoCleanCategories(categories.map { it.name }.toSet()) }
    }
}
