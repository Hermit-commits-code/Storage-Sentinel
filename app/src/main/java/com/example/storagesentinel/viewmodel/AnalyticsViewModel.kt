package com.example.storagesentinel.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.storagesentinel.managers.AnalyticsManager
import com.example.storagesentinel.model.AnalyticsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val analyticsManager = AnalyticsManager(application)
    
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()
    
    init {
        // Observe analytics data and update UI state
        analyticsManager.getAnalyticsUiState()
            .onEach { analyticsState ->
                _uiState.value = analyticsState
            }
            .launchIn(viewModelScope)
            
        // Take an initial storage snapshot
        viewModelScope.launch {
            analyticsManager.recordStorageSnapshot()
        }
    }
    
    fun refreshAnalytics() {
        viewModelScope.launch {
            analyticsManager.recordStorageSnapshot()
        }
    }
}