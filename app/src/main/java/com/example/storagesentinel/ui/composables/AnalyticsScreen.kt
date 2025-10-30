package com.example.storagesentinel.ui.composables

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.storagesentinel.model.AnalyticsUiState
import com.example.storagesentinel.model.StorageTrend
import com.example.storagesentinel.utils.formatFileSize
import com.example.storagesentinel.viewmodel.AnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(onNavigateBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: AnalyticsViewModel = viewModel { AnalyticsViewModel(application) }
    val analyticsState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Analytics") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Overview Card
            item {
                StorageOverviewCard(analyticsState)
            }
            
            // Quick Stats Card
            item {
                QuickStatsCard(analyticsState)
            }
            
            // Trends Section
            item {
                Text(
                    "Storage Trends",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(analyticsState.trends) { trend ->
                TrendCard(trend)
            }
            
            // Smart Recommendation
            if (analyticsState.canCleanToday > 0) {
                item {
                    SmartRecommendationCard(analyticsState.canCleanToday)
                }
            }
        }
    }
}

@Composable
private fun StorageOverviewCard(state: AnalyticsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "Storage Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val usagePercentage = if (state.currentStorageTotal > 0) {
                (state.currentStorageUsed.toFloat() / state.currentStorageTotal.toFloat())
            } else 0f
            
            LinearProgressIndicator(
                progress = { usagePercentage },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${formatFileSize(state.currentStorageUsed)} used")
                Text("${formatFileSize(state.currentStorageTotal)} total")
            }
            
            Text(
                "${formatFileSize(state.currentStorageTotal - state.currentStorageUsed)} available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun QuickStatsCard(state: AnalyticsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "All-Time Stats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Storage Freed", formatFileSize(state.totalFreedAllTime))
                StatItem("Files Cleaned", state.totalFilesCleanedAllTime.toString())
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TrendCard(trend: StorageTrend) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(trend.period, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("${formatFileSize(trend.storageFreed)} freed", style = MaterialTheme.typography.bodyMedium)
                Text("${trend.filesRemoved} files removed", style = MaterialTheme.typography.bodySmall)
            }
            if (trend.mostCommonCategory != null) {
                Text(
                    trend.mostCommonCategory.name.replace("_", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun SmartRecommendationCard(cleanableStorage: Long) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    "💡 Smart Recommendation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "You can free up ${formatFileSize(cleanableStorage)} today!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Run a scan to see what can be cleaned.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}