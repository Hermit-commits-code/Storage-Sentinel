package com.example.storagesentinel.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.storagesentinel.managers.SettingsManager

class SettingsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val settingsManager = SettingsManager(application)
            @Suppress("UNCHECKED_CAST")
            // The ViewModel now needs the Application context again for the PowerManager
            return SettingsViewModel(settingsManager, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
