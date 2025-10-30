package com.example.storagesentinel.ui.composables

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.storagesentinel.model.JunkCategory
import com.example.storagesentinel.viewmodel.SettingsUiState
import com.example.storagesentinel.viewmodel.SettingsViewModel
import com.example.storagesentinel.viewmodel.SettingsViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val factory = SettingsViewModelFactory(application)
    val viewModel: SettingsViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    var showBatteryDialog by remember { mutableStateOf(false) }

    val batteryOptimizationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onBatteryOptimizationResult()
    }

    if (uiState.showBatteryOptimizationDialog) {
        BatteryOptimizationDialog(
            onDismiss = { viewModel.dismissBatteryOptimizationDialog() },
            onConfirm = {
                viewModel.dismissBatteryOptimizationDialog()
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                batteryOptimizationLauncher.launch(intent)
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            LargeFileThresholdSetting(uiState.largeFileThresholdMb) { viewModel.setLargeFileThreshold(it) }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            AutoCleanSettings(uiState, viewModel)
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Privacy Policy Section
            Text(
                text = "Privacy & Legal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPrivacyPolicy() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View Privacy Policy"
                )
            }
        }
    }
}

@Composable
private fun BatteryOptimizationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Action Required") },
        text = { Text("To ensure automatic cleaning runs reliably, please select Storage Sentinel from the list, then set its battery usage to \"Unrestricted\".") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Open Settings") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AutoCleanSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Enable Automatic Cleaning", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = uiState.isAutoCleanEnabled, onCheckedChange = { viewModel.onAutoCleanToggled() })
        }

        if (uiState.isAutoCleanEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Frequency", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = uiState.autoCleanFrequency == "WEEKLY", onClick = { viewModel.setAutoCleanFrequency("WEEKLY") })
                Text("Weekly")
                RadioButton(selected = uiState.autoCleanFrequency == "MONTHLY", onClick = { viewModel.setAutoCleanFrequency("MONTHLY") })
                Text("Monthly")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Categories to Auto-Clean", style = MaterialTheme.typography.titleSmall)
            val safeCategories = JunkCategory.entries.filter { it !in setOf(JunkCategory.LARGE_FILE, JunkCategory.DUPLICATE_FILE) }
            safeCategories.forEach { category ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { 
                        val newSet = uiState.autoCleanCategories.toMutableSet()
                        if (category in newSet) newSet.remove(category) else newSet.add(category)
                        viewModel.setAutoCleanCategories(newSet)
                    }
                ) {
                    Checkbox(checked = category in uiState.autoCleanCategories, onCheckedChange = null)
                    Text(category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
                }
            }
        }
    }
}

@Composable
private fun LargeFileThresholdSetting(currentThreshold: Long, onThresholdChange: (Long) -> Unit) {
    var sliderPosition by remember(currentThreshold) { mutableFloatStateOf(currentThreshold.toFloat()) }

    Column {
        Text("Large File Threshold", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Files larger than")
            Text("${sliderPosition.toLong()} MB", fontWeight = FontWeight.Bold)
        }
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = 50f..1000f,
            steps = 18,
            onValueChangeFinished = { onThresholdChange(sliderPosition.toLong()) }
        )
    }
}
