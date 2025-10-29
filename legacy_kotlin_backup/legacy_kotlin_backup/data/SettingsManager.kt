package com.example.storagesentinel.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("StorageSentinelSettings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_LARGE_FILE_THRESHOLD = "large_file_threshold_mb"
        const val DEFAULT_LARGE_FILE_THRESHOLD = 100L // Default to 100MB
        const val KEY_IGNORE_LIST = "ignore_list"
        const val KEY_IS_PRO_USER = "is_pro_user"
        const val KEY_DEFAULT_SELECTION = "default_selection"
        const val KEY_SCHEDULED_CLEANING_ENABLED = "scheduled_cleaning_enabled"
        const val KEY_SCHEDULED_CLEANING_FREQUENCY = "scheduled_cleaning_frequency"
        const val DEFAULT_SCHEDULE_FREQUENCY = "Weekly"
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

    fun removeFromIgnoreList(path: String) {
        val currentList = getIgnoreList().toMutableSet()
        currentList.remove(path)
        with(sharedPreferences.edit()) {
            putStringSet(KEY_IGNORE_LIST, currentList)
            apply()
        }
    }

    fun getIgnoreList(): Set<String> {
        return sharedPreferences.getStringSet(KEY_IGNORE_LIST, emptySet()) ?: emptySet()
    }

    fun saveIsProUser(isPro: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean(KEY_IS_PRO_USER, isPro)
            apply()
        }
    }

    fun getIsProUser(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_PRO_USER, false)
    }

    fun saveDefaultSelection(selection: Set<String>) {
        with(sharedPreferences.edit()) {
            putStringSet(KEY_DEFAULT_SELECTION, selection)
            apply()
        }
    }

    fun getDefaultSelection(): Set<String> {
        return sharedPreferences.getStringSet(KEY_DEFAULT_SELECTION, emptySet()) ?: emptySet()
    }

    fun saveScheduledCleaningEnabled(enabled: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean(KEY_SCHEDULED_CLEANING_ENABLED, enabled)
            apply()
        }
    }

    fun getScheduledCleaningEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SCHEDULED_CLEANING_ENABLED, false)
    }

    fun saveScheduledCleaningFrequency(frequency: String) {
        with(sharedPreferences.edit()) {
            putString(KEY_SCHEDULED_CLEANING_FREQUENCY, frequency)
            apply()
        }
    }

    fun getScheduledCleaningFrequency(): String {
        return sharedPreferences.getString(KEY_SCHEDULED_CLEANING_FREQUENCY, DEFAULT_SCHEDULE_FREQUENCY) ?: DEFAULT_SCHEDULE_FREQUENCY
    }
}
