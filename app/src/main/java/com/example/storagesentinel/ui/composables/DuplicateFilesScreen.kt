package com.example.storagesentinel.ui.composables

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.storagesentinel.model.JunkItem
import com.example.storagesentinel.utils.formatFileSize
import com.example.storagesentinel.viewmodel.DuplicateFilesViewModel
import com.example.storagesentinel.viewmodel.DuplicateFilesViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateFilesScreen(onNavigateBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    val factory = DuplicateFilesViewModelFactory(application)
    val viewModel: DuplicateFilesViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicate Files") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isScanning) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.duplicateGroups.isEmpty()) {
                Text("No duplicate files found.", modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(uiState.duplicateGroups.entries.toList(), key = { it.key }) { (_, group) ->
                            DuplicateGroupCard(group, uiState.selectedItems, uiState.isProUser, viewModel::toggleSelection)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.deleteSelected() },
                        enabled = uiState.selectedItems.isNotEmpty() && uiState.isProUser,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val buttonText = if (!uiState.isProUser) "PRO Feature" else "Delete Selected (${formatFileSize(uiState.totalSelectedSize)})"
                        Text(buttonText)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(group: List<JunkItem>, selectedIds: Set<String>, isProUser: Boolean, onToggle: (JunkItem) -> Unit) {
    // Sort by path length and creation time to find the most likely original
    val sortedGroup = group.sortedWith(compareBy<JunkItem> { it.path.length }.thenBy { it.path })
    val original = sortedGroup.first()
    val duplicates = sortedGroup.drop(1)
    
    // Smart pre-selection: select all but the shortest path (likely original)
    val preSelectedDuplicates = duplicates.map { it.id }.toSet()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with file info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("📁 ${original.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${group.size} copies found", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatFileSize(group.sumOf { it.sizeInBytes }), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Total size", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Smart selection chip
            if (duplicates.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡 Smart Selection: Keep original, remove ${duplicates.size} copies", 
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.primary)
                    TextButton(
                        onClick = { 
                            duplicates.forEach { duplicate ->
                                if (duplicate.id !in selectedIds) onToggle(duplicate)
                            }
                        },
                        enabled = isProUser
                    ) {
                        Text("Select All Duplicates")
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Original file (never selectable)
            FileRowEnhanced(item = original, isSelected = false, isEnabled = false, isOriginal = true, onToggle = {})

            // Duplicate files
            duplicates.forEach { duplicate ->
                FileRowEnhanced(
                    item = duplicate, 
                    isSelected = duplicate.id in selectedIds, 
                    isEnabled = isProUser, 
                    isOriginal = false,
                    onToggle = { onToggle(duplicate) }
                )
            }
        }
    }
}

@Composable
private fun FileRowEnhanced(item: JunkItem, isSelected: Boolean, isEnabled: Boolean, isOriginal: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected, 
            onCheckedChange = { if (isEnabled) onToggle() }, 
            enabled = isEnabled
        )
        
        // File icon based on extension
        val fileIcon = when (item.name.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg", "png", "gif", "webp" -> "🖼️"
            "mp4", "avi", "mov", "mkv" -> "🎥"
            "mp3", "wav", "m4a", "flac" -> "🎵"
            "pdf" -> "📄"
            "doc", "docx" -> "📝"
            "zip", "rar", "7z" -> "📦"
            else -> "📄"
        }
        
        Text(fileIcon, modifier = Modifier.padding(end = 8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.name, 
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isOriginal) FontWeight.Bold else FontWeight.Normal
                )
                if (isOriginal) {
                    Text(
                        " (Original)", 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                item.path, 
                style = MaterialTheme.typography.bodySmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            if (!isOriginal) {
                Text(
                    "Can be safely deleted", 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(formatFileSize(item.sizeInBytes), fontWeight = FontWeight.Medium)
            if (!isOriginal) {
                Text("Duplicate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun FileRow(item: JunkItem, isSelected: Boolean, isEnabled: Boolean, onToggle: () -> Unit) {
    // Legacy component for compatibility
    FileRowEnhanced(item, isSelected, isEnabled, false, onToggle)
}
