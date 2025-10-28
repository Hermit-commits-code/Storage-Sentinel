package com.example.storagesentinel.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("StorageSentinelSettings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_LARGE_FILE_THRESHOLD = "large_file_threshold_mb"
        const val DEFAULT_LARGE_FILE_THRESHOLD = 100L // Default to 100MB
        const val KEY_IGNORE_LIST = "ignore_list"
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

    fun addToIgnoreList(path: String) {
        val currentList = getIgnoreList().toMutableSet()
        currentList.add(path)
        with(sharedPreferences.edit()) {
            putStringSet(KEY_IGNORE_LIST, currentList)
            apply()
        }
    }

    fun getIgnoreList(): Set<String> {
        return sharedPreferences.getStringSet(KEY_IGNORE_LIST, emptySet()) ?: emptySet()
    }
}
