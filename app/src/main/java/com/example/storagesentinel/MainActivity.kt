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
    var cleanedItems by remember { mutableStateOf<List<JunkItem>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val scannerService = remember {
        ScannerService(rootDirectory = Environment.getExternalStorageDirectory(), context = context)
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {}

    fun handleScanRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                startScan(coroutineScope, scannerService, { currentScanState = it }, { scanResults = it })
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:${context.packageName}".toUri()
                settingsLauncher.launch(intent)
            }
        } else {
            Toast.makeText(context, "This app requires a newer Android version to function properly.", Toast.LENGTH_LONG).show()
        }
    }

    fun handleCleanRequest() {
        showConfirmDialog = false
        val allJunkItems = scanResults.values.flatten()
        cleanedItems = allJunkItems
        startClean(coroutineScope, scannerService, allJunkItems) {
            currentScanState = ScanState.CLEAN_COMPLETE
        }
        currentScanState = ScanState.CLEANING
    }

    if (showConfirmDialog) {
        ConfirmationDialog(
            onConfirm = { handleCleanRequest() },
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
            verticalArrangement = if (currentScanState != ScanState.FINISHED) Arrangement.Center else Arrangement.Top
        ) {
            when (currentScanState) {
                ScanState.READY -> ReadyStateView { handleScanRequest() }
                ScanState.SCANNING -> ScanningStateView()
                ScanState.CLEANING -> CleaningStateView()
                ScanState.FINISHED -> ResultsDisplay(results = scanResults, onCleanClick = { showConfirmDialog = true })
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
        text = { Text("Are you sure you want to permanently delete these files? This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReadyStateView(onScanClick: () -> Unit) {
    Text("Ready to reclaim your storage.", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onScanClick) {
        Text("Start Scan")
    }
}

@Composable
fun ScanningStateView() {
    CircularProgressIndicator(modifier = Modifier.size(64.dp))
    Spacer(modifier = Modifier.height(16.dp))
    Text("Scanning files... This may take a moment.")
}

@Composable
fun CleaningStateView() {
    CircularProgressIndicator(modifier = Modifier.size(64.dp))
    Spacer(modifier = Modifier.height(16.dp))
    Text("Cleaning up junk files...")
}

@Composable
fun ResultsDisplay(results: Map<JunkType, List<JunkItem>>, onCleanClick: () -> Unit) {
    val totalJunkCount = results.values.sumOf { it.size }
    val totalJunkSize = results.values.flatten().sumOf { it.sizeBytes }

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
                JunkCategorySummary(
                    categoryName = key.label,
                    items = itemsList,
                    totalSize = itemsList.sumOf { it.sizeBytes }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (totalJunkCount > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onCleanClick, modifier = Modifier.fillMaxWidth()) {
                Text("Clean Now")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JunkCategorySummary(categoryName: String, items: List<JunkItem>, totalSize: Long) {
    var isExpanded by remember { mutableStateOf(false) }
    val itemCount = items.size

    Card(
        onClick = { isExpanded = !isExpanded },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "$itemCount items found", style = MaterialTheme.typography.bodySmall)
                }
                Text(text = formatBytes(totalSize), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                items.forEach { item ->
                    Text(
                        text = item.path,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
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
    onStateChange: (ScanState) -> Unit,
    onResultsReady: (Map<JunkType, List<JunkItem>>) -> Unit
) {
    onStateChange(ScanState.SCANNING)
    scope.launch {
        try {
            val results = scannerService.startFullScan()
            onResultsReady(results)
            onStateChange(ScanState.FINISHED)
        } catch (e: Exception) {
            println("Scan Error: ${e.message}")
            onStateChange(ScanState.READY)
        }
    }
}

private fun startClean(
    scope: CoroutineScope,
    scannerService: ScannerService,
    itemsToClean: List<JunkItem>,
    onCleanComplete: () -> Unit
) {
    scope.launch {
        try {
            scannerService.deleteJunkItems(itemsToClean)
            onCleanComplete()
        } catch (e: Exception) {
            println("Clean Error: ${e.message}")
        }
    }
}

@Composable
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / 1024.0.pow(digitGroups), units[digitGroups])
}
