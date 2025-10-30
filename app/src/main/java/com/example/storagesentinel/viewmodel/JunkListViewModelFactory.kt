package com.example.storagesentinel.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.storagesentinel.managers.SettingsManager
import com.example.storagesentinel.services.ScannerService

class JunkListViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(JunkListViewModel::class.java)) {
            val settingsManager = SettingsManager(application)
            val scannerService = ScannerService(application, settingsManager)
            val savedStateHandle = extras.createSavedStateHandle()
            @Suppress("UNCHECKED_CAST")
            return JunkListViewModel(scannerService, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
