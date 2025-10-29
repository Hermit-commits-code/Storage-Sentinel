package com.example.storagesentinel.legacy_ui.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.storagesentinel.JunkItem
import com.example.storagesentinel.util.formatBytes

@Composable
fun ReadyStateView(onScanClick: () -> Unit, onCreateDummyFilesClick: () -> Unit) {
    Text("Ready to reclaim your storage.", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onScanClick, modifier = Modifier.fillMaxWidth(0.8f)) {
        Text("Start Scan")
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onCreateDummyFilesClick, modifier = Modifier.fillMaxWidth(0.8f)) {
        Text("Create Dummy Files (Debug)")
    }
}

@Composable
fun ScanningStateView(currentlyScanningPath: String?) {
    CircularProgressIndicator(modifier = Modifier.size(64.dp))
    Spacer(modifier = Modifier.height(16.dp))
    Text("Scanning files... This may take a moment.")
    if (currentlyScanningPath != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text("Currently scanning: $currentlyScanningPath")
    }
}

@Composable
fun CleaningStateView(category: String?) {
    CircularProgressIndicator(modifier = Modifier.size(64.dp))
    Spacer(modifier = Modifier.height(16.dp))
    Text(if (category != null) "Cleaning: $category..." else "Preparing to clean...")
}

@Composable
fun PostCleanSummary(cleanedItems: List<JunkItem>, onFinish: () -> Unit, onExportReport: () -> Unit) {
    val totalCleanedSize = cleanedItems.sumOf { it.sizeBytes }
    val totalCleanedCount = cleanedItems.size

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(128.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Clean Complete!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Reclaimed: ${formatBytes(totalCleanedSize)}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Removed $totalCleanedCount items", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(48.dp))
        Row {
            OutlinedButton(onClick = onExportReport) { Text("Export Report") }
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Button(onClick = onFinish) { Text("Finish") }
        }
    }
}
