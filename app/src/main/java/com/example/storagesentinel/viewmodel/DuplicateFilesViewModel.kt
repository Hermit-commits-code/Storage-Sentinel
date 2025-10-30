package com.example.storagesentinel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storagesentinel.managers.SettingsManager
import com.example.storagesentinel.model.JunkCategory
import com.example.storagesentinel.model.JunkItem
import com.example.storagesentinel.services.ScannerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DuplicateFilesUiState(
    val duplicateGroups: Map<String, List<JunkItem>> = emptyMap(),
    val selectedItems: Set<String> = emptySet(),
    val totalSelectedSize: Long = 0L,
    val isScanning: Boolean = true,
    val isProUser: Boolean = false // Added for PRO status awareness
)

class DuplicateFilesViewModel(
    private val scannerService: ScannerService,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DuplicateFilesUiState())
    val uiState: StateFlow<DuplicateFilesUiState> = _uiState.asStateFlow()

    private var allDuplicates: List<JunkItem> = emptyList()

    init {
        // Observe PRO status
        settingsManager.isProUser.onEach { isPro ->
            _uiState.update { it.copy(isProUser = isPro) }
        }.launchIn(viewModelScope)

        loadDuplicates()
    }

    private fun loadDuplicates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }

            allDuplicates = scannerService.scan().first().filter { it.category == JunkCategory.DUPLICATE_FILE }

            val grouped = allDuplicates.groupBy { it.contentHash!! }

            _uiState.update { it.copy(duplicateGroups = grouped, isScanning = false) }
        }
    }

    fun toggleSelection(item: JunkItem) {
        // Here, you would ideally check for PRO status before allowing selection,
        // but for now, we'll let the UI handle the visual locking.
        val newSelectedIds = _uiState.value.selectedItems.toMutableSet()
        if (item.id in newSelectedIds) {
            newSelectedIds.remove(item.id)
        } else {
            newSelectedIds.add(item.id)
        }

        val newSelectedSize = allDuplicates
            .filter { it.id in newSelectedIds }
            .sumOf { it.sizeInBytes }

        _uiState.update { it.copy(selectedItems = newSelectedIds, totalSelectedSize = newSelectedSize) }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val itemsToDelete = allDuplicates.filter { it.id in _uiState.value.selectedItems }
            scannerService.delete(itemsToDelete)
            loadDuplicates()
        }
    }
}
