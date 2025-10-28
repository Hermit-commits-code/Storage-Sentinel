package com.example.storagesentinel

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.storagesentinel.ui.theme.StorageSentinelTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StorageSentinelTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ScannerScreen()
                }
            }
        }
    }
}

enum class ScanState {
    READY,
    SCANNING,
    FINISHED,
    CLEANING,
    CLEAN_COMPLETE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen() {
    val context = LocalContext.current
    var currentScanState by remember { mutableStateOf(ScanState.READY) }
    var scanResults by remember { mutableStateOf<Map<JunkType, List<JunkItem>>>(emptyMap()) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectionToClean by remember { mutableStateOf<Set<JunkType>>(emptySet()) }
    var currentlyCleaning by remember { mutableStateOf<JunkType?>(null) }
    var cleanedItems by remember { mutableStateOf<List<JunkItem>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val scannerService = remember {
        ScannerService(rootDirectory = Environment.getExternalStorageDirectory(), context = context)
    }

    // --- START OF FIX ---

    // Hoist this function definition up so the launcher can reference it.
    fun handleScanRequest() {
        currentScanState = ScanState.SCANNING
        startScan(coroutineScope, scannerService) { results ->
            scanResults = results
            currentScanState = ScanState.FINISHED
        }
    }

    // Define handlePermissionRequest before it's used in the launcher.
    fun handlePermissionRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                handleScanRequest()
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:${context.packageName}".toUri()
                // Now settingsLauncher can be defined, as it's declared below.
            }
        } else {
            Toast.makeText(context, "This feature requires Android 11+.", Toast.LENGTH_LONG).show()
        }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Re-check permission after user returns from settings
        // Now this call is valid because the function is defined above.
        handlePermissionRequest()
    }

    // Update the permission request logic to launch the now-defined launcher.
    fun launchPermissionRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                handleScanRequest()
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:${context.packageName}".toUri()
                settingsLauncher.launch(intent)
            }
        } else {
            Toast.makeText(context, "This feature requires Android 11+.", Toast.LENGTH_LONG).show()
        }
    }

    // --- END OF FIX ---


    fun handleCleanRequest(selected: Set<JunkType>) {
        showConfirmDialog = false
        val itemsToClean = scanResults.filterKeys { it in selected }.values.flatten()
        cleanedItems = itemsToClean
        currentScanState = ScanState.CLEANING
        startClean(coroutineScope, scannerService, itemsToClean, onProgress = { currentlyCleaning = it }) { errors ->
            if (errors.isEmpty()) {
                currentScanState = ScanState.CLEAN_COMPLETE
            } else {
                errors.forEach { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                handleScanRequest() // Re-run scan to show what was left over
            }
        }
    }

    if (showConfirmDialog) {
        ConfirmationDialog(
            onConfirm = { handleCleanRequest(selectionToClean) },
            onDismiss = { showConfirmDialog = false }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Storage Sentinel") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = when (currentScanState) {
                ScanState.READY, ScanState.CLEAN_COMPLETE -> Arrangement.Center
                else -> Arrangement.Top
            }
        ) {
            when (currentScanState) {
                // Update this call to use the new launcher function
                ScanState.READY -> ReadyStateView { launchPermissionRequest() }
                ScanState.SCANNING -> ScanningStateView()
                ScanState.CLEANING -> CleaningStateView(currentlyCleaning?.label)
                ScanState.FINISHED -> ResultsDisplay(
                    results = scanResults,
                    onCleanClick = { selectedCategories ->
                        selectionToClean = selectedCategories
                        showConfirmDialog = true
                    }
                )
                ScanState.CLEAN_COMPLETE -> PostCleanSummary(
                    cleanedItems = cleanedItems,
                    onFinish = {
                        scanResults = emptyMap()
                        currentScanState = ScanState.READY
                    }
                )
            }
        }
    }
}

@Composable
fun ConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Deletion") },
        text = { Text("Are you sure you want to permanently delete the selected files? This action cannot be undone.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ReadyStateView(onScanClick: () -> Unit) {
    Text("Ready to reclaim your storage.", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onScanClick) { Text("Start Scan") }
}

@Composable
fun ScanningStateView() {
    CircularProgressIndicator(modifier = Modifier.size(64.dp))
    Spacer(modifier = Modifier.height(16.dp))
    Text("Scanning files... This may take a moment.")
}

@Composable
fun CleaningStateView(category: String?) {
    CircularProgressIndicator(modifier = Modifier.size(64.dp))
    Spacer(modifier = Modifier.height(16.dp))
    Text(if (category != null) "Cleaning: $category..." else "Preparing to clean...")
}

@Composable
fun ResultsDisplay(results: Map<JunkType, List<JunkItem>>, onCleanClick: (Set<JunkType>) -> Unit) {
    val totalJunkCount = results.values.sumOf { it.size }
    val totalJunkSize = results.values.flatten().sumOf { it.sizeBytes }
    var selectedCategories by remember { mutableStateOf<Set<JunkType>>(emptySet()) }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Scan Complete!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Total Clutter Found: ${formatBytes(totalJunkSize)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Across $totalJunkCount items", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            val filtered = results.filter { it.value.isNotEmpty() }
            items(filtered.keys.toList()) { key ->
                val itemsList = filtered[key] ?: emptyList()
                val isSelected = selectedCategories.contains(key)
                JunkCategorySummary(
                    categoryName = key.label,
                    items = itemsList,
                    totalSize = itemsList.sumOf { it.sizeBytes },
                    isSelected = isSelected,
                    onSelectionChange = {
                        selectedCategories = if (isSelected) {
                            selectedCategories - key
                        } else {
                            selectedCategories + key
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (totalJunkCount > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onCleanClick(selectedCategories) },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedCategories.isNotEmpty()
            ) {
                Text("Clean Selected")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JunkCategorySummary(
    categoryName: String,
    items: List<JunkItem>,
    totalSize: Long,
    isSelected: Boolean,
    onSelectionChange: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val itemCount = items.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isSelected, onCheckedChange = { onSelectionChange() })
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(text = categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = "$itemCount items found", style = MaterialTheme.typography.bodySmall)
                }
                Text(text = formatBytes(totalSize), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { isExpanded = !isExpanded }) {
                    Text(if (isExpanded) "Hide" else "Show")
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                items.take(5).forEach { item ->
                    Text(text = item.path, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                }
            }
        }
    }
}

@Composable
fun PostCleanSummary(cleanedItems: List<JunkItem>, onFinish: () -> Unit) {
    val totalCleanedSize = cleanedItems.sumOf { it.sizeBytes }
    val totalCleanedCount = cleanedItems.size

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Clean Complete!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Reclaimed: ${formatBytes(totalCleanedSize)}", style = MaterialTheme.typography.titleLarge)
        Text("Removed $totalCleanedCount items", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onFinish) {
            Text("Finish")
        }
    }
}

private fun startScan(
    scope: CoroutineScope,
    scannerService: ScannerService,
    onResultsReady: (Map<JunkType, List<JunkItem>>) -> Unit
) {
    scope.launch {
        try {
            val results = scannerService.startFullScan()
            onResultsReady(results)
        } catch (e: Exception) {
            println("Scan Error: ${e.message}")
        }
    }
}

private fun startClean(
    scope: CoroutineScope,
    scannerService: ScannerService,
    itemsToClean: List<JunkItem>,
    onProgress: (JunkType) -> Unit,
    onComplete: (List<String>) -> Unit
) {
    scope.launch {
        val errors = mutableListOf<String>()
        val groupedItems = itemsToClean.groupBy { it.type }

        for ((type, items) in groupedItems) {
            onProgress(type)
            errors.addAll(scannerService.deleteJunkItems(items))
        }
        onComplete(errors)
    }
}

@Composable
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / 1024.0.pow(digitGroups), units[digitGroups])
}
