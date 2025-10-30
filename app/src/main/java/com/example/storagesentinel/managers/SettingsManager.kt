package com.example.storagesentinel.managers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    private val isProUserKey = booleanPreferencesKey("is_pro_user")
    private val largeFileThresholdKey = longPreferencesKey("large_file_threshold_mb")
    private val isAutoCleanEnabledKey = booleanPreferencesKey("is_auto_clean_enabled")
    private val autoCleanFrequencyKey = stringPreferencesKey("auto_clean_frequency")
    private val autoCleanCategoriesKey = stringSetPreferencesKey("auto_clean_categories")

    val isProUser: Flow<Boolean> = context.dataStore.data.map { it[isProUserKey] ?: false }

    val largeFileThresholdMb: Flow<Long> = context.dataStore.data.map { it[largeFileThresholdKey] ?: 100L }

    val isAutoCleanEnabled: Flow<Boolean> = context.dataStore.data.map { it[isAutoCleanEnabledKey] ?: false }

    val autoCleanFrequency: Flow<String> = context.dataStore.data.map { it[autoCleanFrequencyKey] ?: "WEEKLY" }

    val autoCleanCategories: Flow<Set<String>> = context.dataStore.data.map { it[autoCleanCategoriesKey] ?: emptySet() }

    suspend fun setProUser(isPro: Boolean) {
        context.dataStore.edit { it[isProUserKey] = isPro }
    }

    suspend fun setLargeFileThresholdMb(threshold: Long) {
        context.dataStore.edit { it[largeFileThresholdKey] = threshold }
    }

    suspend fun setAutoCleanEnabled(isEnabled: Boolean) {
        context.dataStore.edit { it[isAutoCleanEnabledKey] = isEnabled }
    }

    suspend fun setAutoCleanFrequency(frequency: String) {
        context.dataStore.edit { it[autoCleanFrequencyKey] = frequency }
    }

    suspend fun setAutoCleanCategories(categories: Set<String>) {
        context.dataStore.edit { it[autoCleanCategoriesKey] = categories }
    }
}
