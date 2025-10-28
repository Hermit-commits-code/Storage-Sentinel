package com.example.storagesentinel.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("StorageSentinelSettings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_LARGE_FILE_THRESHOLD = "large_file_threshold_mb"
        const val DEFAULT_LARGE_FILE_THRESHOLD = 100L // Default to 100MB
    }

    fun saveLargeFileThreshold(threshold: Long) {
        with(sharedPreferences.edit()) {
            putLong(KEY_LARGE_FILE_THRESHOLD, threshold)
            apply()
        }
    }

    fun getLargeFileThreshold(): Long {
        return sharedPreferences.getLong(KEY_LARGE_FILE_THRESHOLD, DEFAULT_LARGE_FILE_THRESHOLD)
    }
}
