package com.example.storagesentinel.legacy_ui.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.storagesentinel.data.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
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
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    var threshold by remember { mutableStateOf(settingsManager.getLargeFileThreshold().toString()) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Large File Threshold (MB)", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = threshold,
                onValueChange = { threshold = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (!it.isFocused) {
                            val newThreshold = threshold.toLongOrNull()
                                ?: SettingsManager.DEFAULT_LARGE_FILE_THRESHOLD
                            settingsManager.saveLargeFileThreshold(newThreshold)
                            threshold = newThreshold.toString() // Ensure UI reflects saved value
                            Toast
                                .makeText(
                                    context,
                                    "Threshold updated to $newThreshold MB",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            if (isProUser) {
                Spacer(modifier = Modifier.height(32.dp))
                Text("Scheduled Cleaning (PRO)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Automatic Cleaning")
                    Switch(checked = isScheduledCleaningEnabled, onCheckedChange = onScheduledCleaningEnabledChanged)
                }
                if(isScheduledCleaningEnabled){
                    Spacer(modifier = Modifier.height(16.dp))
                    FrequencySelector(frequency = scheduledCleaningFrequency, onFrequencyChange = onScheduledCleaningFrequencyChanged)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            Text("Ignore List", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(ignoreList.toList()) { path ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(path, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onRemoveFromIgnoreList(path) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove from ignore list")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencySelector(frequency: String, onFrequencyChange: (String) -> Unit) {
    val options = listOf("Daily", "Weekly", "Monthly")
    var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = frequency,
            onValueChange = {},
            readOnly = true,
            label = { Text("Frequency") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                        .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onFrequencyChange(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}
