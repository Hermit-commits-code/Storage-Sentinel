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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    FINISHED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen() {
    val context = LocalContext.current
    var currentScanState by remember { mutableStateOf(ScanState.READY) }
    var scanResults by remember { mutableStateOf<Map<JunkType, List<JunkItem>>>(emptyMap()) }
    val coroutineScope = rememberCoroutineScope()
    val scannerService = remember {
        ScannerService(rootDirectory = Environment.getExternalStorageDirectory(), context = context)
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { 
        // This block runs when the user returns from the settings screen.
        // We don't need to do anything here; the `handleScanRequest` function will re-check the permission.
    }

    fun handleScanRequest() {
        // For modern Android, we check for MANAGE_EXTERNAL_STORAGE.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                // Permission is already granted.
                startScan(coroutineScope, scannerService, { currentScanState = it }, { scanResults = it })
            } else {
                // Permission is not granted, guide the user to the settings screen.
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:${context.packageName}".toUri()
                settingsLauncher.launch(intent)
            }
        } else {
            // For older Android versions, this app will not have the required permissions.
            Toast.makeText(context, "This app requires a newer Android version to function properly.", Toast.LENGTH_LONG).show()
        }
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
                ScanState.FINISHED -> ResultsDisplay(scanResults)
            }
        }
    }
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
fun ResultsDisplay(results: Map<JunkType, List<JunkItem>>) {
    val totalJunkCount = results.values.sumOf { it.size }
    val totalJunkSize = results.values.flatten().sumOf { it.sizeBytes }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Scan Complete!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Total Clutter Found: ${formatBytes(totalJunkSize)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Across $totalJunkCount items", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Clutter Breakdown:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            val filtered = results.filter { it.value.isNotEmpty() }
            items(filtered.keys.toList()) { key ->
                val itemsList = filtered[key] ?: emptyList()
                val categorySize = itemsList.sumOf { it.sizeBytes }
                JunkCategorySummary(
                    categoryName = key.label,
                    itemCount = itemsList.size,
                    totalSize = categorySize
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun JunkCategorySummary(categoryName: String, itemCount: Int, totalSize: Long) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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

@Composable
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / 1024.0.pow(digitGroups), units[digitGroups])
}
