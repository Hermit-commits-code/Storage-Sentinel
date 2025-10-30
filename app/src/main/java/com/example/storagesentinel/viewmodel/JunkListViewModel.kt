package com.example.storagesentinel.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storagesentinel.model.JunkCategory
import com.example.storagesentinel.model.JunkItem
import com.example.storagesentinel.services.ScannerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JunkListUiState(
    val title: String = "",
    val items: List<JunkItem> = emptyList(),
    val selectedItems: Set<String> = emptySet(),
    val totalSelectedSize: Long = 0L,
    val isScanning: Boolean = true
)

class JunkListViewModel(
    private val scannerService: ScannerService,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(JunkListUiState())
    val uiState: StateFlow<JunkListUiState> = _uiState.asStateFlow()

    private val category: JunkCategory = JunkCategory.valueOf(savedStateHandle.get<String>("category")!!)

    init {
        _uiState.update { it.copy(title = category.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() }) }
        loadJunkItems()
    }

    private fun loadJunkItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, selectedItems = emptySet(), totalSelectedSize = 0L) }
            val allItems = scannerService.scan().first().filter { it.category == category }
            _uiState.update { it.copy(items = allItems, isScanning = false) }
        }
    }

    fun toggleSelection(item: JunkItem) {
        val newSelectedIds = _uiState.value.selectedItems.toMutableSet()
        if (item.id in newSelectedIds) {
            newSelectedIds.remove(item.id)
        } else {
            newSelectedIds.add(item.id)
        }
        
        val newSelectedSize = _uiState.value.items
            .filter { it.id in newSelectedIds }
            .sumOf { it.sizeInBytes }

        _uiState.update { it.copy(selectedItems = newSelectedIds, totalSelectedSize = newSelectedSize) }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val itemsToDelete = _uiState.value.items.filter { it.id in _uiState.value.selectedItems }
            scannerService.delete(itemsToDelete)
            loadJunkItems()
        }
    }
}
