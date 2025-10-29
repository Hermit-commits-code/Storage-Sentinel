package com.example.storagesentinel.ui.settings

import androidx.compose.runtime.Composable
import com.example.storagesentinel.data.SettingsManager

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    ignoreList: Set<String>,
    onRemoveFromIgnoreList: (String) -> Unit,
    isProUser: Boolean,
    isScheduledCleaningEnabled: Boolean,
    scheduledCleaningFrequency: String,
    onScheduledCleaningEnabledChanged: (Boolean) -> Unit,
    onScheduledCleaningFrequencyChanged: (String) -> Unit
) {
    com.example.storagesentinel.legacy_ui.ui.settings.SettingsScreen(
        onBack = onBack,
        ignoreList = ignoreList,
        onRemoveFromIgnoreList = onRemoveFromIgnoreList,
        isProUser = isProUser,
        isScheduledCleaningEnabled = isScheduledCleaningEnabled,
        scheduledCleaningFrequency = scheduledCleaningFrequency,
        onScheduledCleaningEnabledChanged = onScheduledCleaningEnabledChanged,
        onScheduledCleaningFrequencyChanged = onScheduledCleaningFrequencyChanged
    )
}

@Composable
fun FrequencySelector(frequency: String, onFrequencyChange: (String) -> Unit) {
    com.example.storagesentinel.legacy_ui.ui.settings.FrequencySelector(frequency, onFrequencyChange)
}
