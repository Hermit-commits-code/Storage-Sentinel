package com.example.storagesentinel.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.storagesentinel.managers.SettingsManager
import com.example.storagesentinel.services.ScannerService

class DuplicateFilesViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DuplicateFilesViewModel::class.java)) {
            // Create the dependencies the ViewModel needs
            val settingsManager = SettingsManager(application)
            val scannerService = ScannerService(application, settingsManager)
            @Suppress("UNCHECKED_CAST")
            return DuplicateFilesViewModel(scannerService, settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
