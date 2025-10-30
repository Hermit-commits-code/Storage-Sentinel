package com.example.storagesentinel.ui.composables

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.storagesentinel.billing.BillingManager
import com.example.storagesentinel.model.CategorySelection
import com.example.storagesentinel.model.JunkCategory
import com.example.storagesentinel.model.ScannerUiState
import com.example.storagesentinel.utils.formatFileSize
import com.example.storagesentinel.viewmodel.ScannerViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScannerScreen(
    modifier: Modifier = Modifier, 
    scannerViewModel: ScannerViewModel = viewModel(), 
    billingManager: BillingManager,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToDuplicates: () -> Unit,
    onNavigateToJunkList: (String) -> Unit
) {
    val uiState by scannerViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            scannerViewModel.startScan()
        }
    }
    val legacyPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) scannerViewModel.startScan() }

    if (uiState.showProUpgradeDialog) {
        ProUpgradeDialog(
            billingManager = billingManager,
            onDismiss = { scannerViewModel.dismissProUpgradeDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Sentinel") },
                actions = {
                    IconButton(onClick = onNavigateToAnalytics) { Icon(Icons.Default.Info, "Analytics") }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Settings") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (!Environment.isExternalStorageManager()) {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply { data = Uri.fromParts("package", context.packageName, null) }
                        settingsLauncher.launch(intent)
                    } else {
                        scannerViewModel.startScan()
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        legacyPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    } else {
                        scannerViewModel.startScan()
                    }
                }
            }) {
                if (uiState.isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                } else {
                    Icon(Icons.Default.Refresh, "Rescan")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            SummaryHeader(uiState, scannerViewModel)
            
            // Usage limits banner for free users
            if (!uiState.isProUser && uiState.remainingScansToday >= 0) {
                UsageLimitsBanner(
                    remainingScans = uiState.remainingScansToday,
                    onUpgradeClick = { scannerViewModel.showProUpgradeDialog() }
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (uiState.scanResults.isEmpty() && !uiState.isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ready to scan. Tap the refresh button to begin.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.scanResults, key = { "${it.category.name}-${uiState.isProUser}" }) { selection ->
                        CategoryCard(
                            selection = selection, 
                            isProUser = uiState.isProUser, 
                            onLongClick = { category, isSelected ->
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                scannerViewModel.handleCategorySelection(category, isSelected)
                            },
                            onClick = {
                                val isProFeature = selection.category in setOf(JunkCategory.RESIDUAL_APP_DATA, JunkCategory.LARGE_FILE, JunkCategory.DUPLICATE_FILE)
                                val isLocked = isProFeature && !uiState.isProUser
                                
                                if (isLocked) {
                                    // Show PRO upgrade dialog for locked features
                                    scannerViewModel.handleCategorySelection(selection.category, true)
                                } else {
                                    // Navigate to detail screens for unlocked features
                                    if (selection.category == JunkCategory.DUPLICATE_FILE) {
                                        onNavigateToDuplicates()
                                    } else {
                                        onNavigateToJunkList(selection.category.name)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryHeader(uiState: ScannerUiState, viewModel: ScannerViewModel) {
    val totalFound = uiState.scanResults.sumOf { it.totalSize }
    val isAnySelected = uiState.scanResults.any { it.isSelected }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text("Total Found", style = MaterialTheme.typography.titleMedium)
        Text(formatFileSize(totalFound), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.cleanSelectedItems() },
            enabled = isAnySelected && !uiState.isScanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            val buttonText = if (isAnySelected) "Clean Up ${formatFileSize(uiState.totalSelectedSize)}" else "Select items to clean"
            Text(buttonText)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryCard(
    selection: CategorySelection,
    isProUser: Boolean,
    onLongClick: (JunkCategory, Boolean) -> Unit,
    onClick: () -> Unit
) {
    val isProFeature = selection.category in setOf(JunkCategory.RESIDUAL_APP_DATA, JunkCategory.LARGE_FILE, JunkCategory.DUPLICATE_FILE)
    val isLocked = isProFeature && !isProUser

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = onClick,
            onLongClick = { 
                if (isLocked) {
                    // For locked features, trigger the PRO upgrade dialog
                    onLongClick(selection.category, true)
                } else {
                    // For unlocked features, toggle selection
                    onLongClick(selection.category, !selection.isSelected)
                }
            }
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = selection.isSelected, 
                onCheckedChange = { newCheckedState ->
                    if (isLocked) {
                        // For locked features, show PRO upgrade dialog
                        onLongClick(selection.category, true)
                    } else {
                        // For unlocked features, update selection
                        onLongClick(selection.category, newCheckedState)
                    }
                }, 
                enabled = !isLocked
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(selection.category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                Text("${selection.totalCount} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isLocked) {
                Icon(Icons.Default.Lock, contentDescription = "PRO feature", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp))
            } else {
                Text(formatFileSize(selection.totalSize), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
        }
    }
}


@Composable
private fun UsageLimitsBanner(
    remainingScans: Int,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (remainingScans == 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (remainingScans > 0) {
                        "Free Tier: $remainingScans scan${if (remainingScans != 1) "s" else ""} remaining today"
                    } else {
                        "Daily scan limit reached"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (remainingScans > 0) {
                        "Upgrade to PRO for unlimited scans"
                    } else {
                        "Upgrade to PRO for unlimited scanning"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Button(
                onClick = onUpgradeClick,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Upgrade")
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
