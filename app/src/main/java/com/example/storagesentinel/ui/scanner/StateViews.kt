package com.example.storagesentinel.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.typography
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
    Text("Ready to reclaim your storage.", style = typography.titleLarge)
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
fun PostCleanSummary(cleanedItems: List<JunkItem>, onFinish: () -> Unit) {
    val totalCleanedSize = cleanedItems.sumOf { it.sizeBytes }
    val totalCleanedCount = cleanedItems.size

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Clean Complete!", style = typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Reclaimed: ${formatBytes(totalCleanedSize)}", style = typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Removed $totalCleanedCount items", style = typography.bodyMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onFinish) { Text("Finish") }
    }
}
