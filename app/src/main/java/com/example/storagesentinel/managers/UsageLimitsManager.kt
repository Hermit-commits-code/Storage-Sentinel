package com.example.storagesentinel.managers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

private val Context.usageLimitsDataStore: DataStore<Preferences> by preferencesDataStore(name = "usage_limits")

data class UsageLimits(
    val scansToday: Int = 0,
    val lastScanDate: Long = 0L,
    val maxFreeScansPerDay: Int = 3
) {
    fun hasReachedDailyLimit(): Boolean = scansToday >= maxFreeScansPerDay
    
    fun isNewDay(): Boolean {
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        val lastScanDay = LocalDate.ofEpochDay(lastScanDate / 86400).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        return today != lastScanDay
    }
    
    fun remainingScansToday(): Int = maxOf(0, maxFreeScansPerDay - scansToday)
}

class UsageLimitsManager(private val context: Context) {
    
    companion object {
        private val SCANS_TODAY_KEY = intPreferencesKey("scans_today")
        private val LAST_SCAN_DATE_KEY = longPreferencesKey("last_scan_date")
    }
    
    val usageLimits: Flow<UsageLimits> = context.usageLimitsDataStore.data
        .map { preferences ->
            UsageLimits(
                scansToday = preferences[SCANS_TODAY_KEY] ?: 0,
                lastScanDate = preferences[LAST_SCAN_DATE_KEY] ?: 0L
            )
        }
    
    suspend fun recordScanAttempt(): UsageLimits {
        val today = System.currentTimeMillis()
        var newUsageLimits: UsageLimits? = null
        
        context.usageLimitsDataStore.edit { preferences ->
            val currentScans = preferences[SCANS_TODAY_KEY] ?: 0
            val lastScanDate = preferences[LAST_SCAN_DATE_KEY] ?: 0L
            
            val currentLimits = UsageLimits(currentScans, lastScanDate)
            
            if (currentLimits.isNewDay()) {
                // Reset count for new day
                preferences[SCANS_TODAY_KEY] = 1
                preferences[LAST_SCAN_DATE_KEY] = today
                newUsageLimits = UsageLimits(scansToday = 1, lastScanDate = today)
            } else {
                // Increment count for same day
                val newCount = currentScans + 1
                preferences[SCANS_TODAY_KEY] = newCount
                preferences[LAST_SCAN_DATE_KEY] = today
                newUsageLimits = UsageLimits(scansToday = newCount, lastScanDate = today)
            }
        }
        
        return newUsageLimits!!
    }
    
    suspend fun resetDailyLimits() {
        context.usageLimitsDataStore.edit { preferences ->
            preferences[SCANS_TODAY_KEY] = 0
            preferences[LAST_SCAN_DATE_KEY] = System.currentTimeMillis()
        }
    }
    
    suspend fun canPerformScan(isProUser: Boolean): Boolean {
        if (isProUser) return true
        
        val limits = usageLimits.map { it }.first() // Get current value
        return if (limits.isNewDay()) {
            true // New day, reset allowed
        } else {
            !limits.hasReachedDailyLimit()
        }
    }
    
    private suspend fun <T> Flow<T>.first(): T {
        var result: T? = null
        collect { value ->
            result = value
            return@collect
        }
        return result!!
    }
}