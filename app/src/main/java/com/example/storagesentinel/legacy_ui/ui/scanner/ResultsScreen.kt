package com.example.storagesentinel.legacy_ui.ui.scanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.storagesentinel.JunkItem
import com.example.storagesentinel.JunkType
import com.example.storagesentinel.ui.theme.FileCopy
import com.example.storagesentinel.ui.theme.Folder
import com.example.storagesentinel.ui.theme.Note
import com.example.storagesentinel.ui.theme.Science
import com.example.storagesentinel.util.formatBytes

@Composable
fun ResultsDisplay(
    results: Map<JunkType, List<JunkItem>>,
    selectedCategories: Set<JunkType>,
    isProUser: Boolean,
    onCleanClick: () -> Unit,
    onCategoryClick: (JunkType) -> Unit,
    onCategorySelectionChanged: (JunkType, Boolean) -> Unit,
    onProUpgradeClick: () -> Unit,
    onSaveDefaults: () -> Unit
) {
    val itemsToClean = results.filterKeys { it in selectedCategories }.values.flatten()
    val totalSelectedCount = itemsToClean.size
    val totalSelectedSize = itemsToClean.sumOf { it.sizeBytes }
    val totalFoundSize = results.values.flatten().sumOf { it.sizeBytes }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Scan Complete!", style = MaterialTheme.typography.headlineMedium)
        Text("Found ${formatBytes(totalFoundSize)} of junk", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Selected for Cleaning: ${formatBytes(totalSelectedSize)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Across $totalSelectedCount items", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            val filtered = results.filter { it.value.isNotEmpty() }
            items(filtered.keys.toList()) { key ->
                val proLabels = listOf("Residual App Data", "Duplicate Files", "Large Files")
                val isLockedForUser = (key.label in proLabels) && !isProUser
                    JunkCategorySummary(
                    categoryName = key.label,
                    items = filtered[key] ?: emptyList(),
                    totalSize = (filtered[key] ?: emptyList()).sumOf { it.sizeBytes },
                    isSelected = selectedCategories.contains(key),
                        isLocked = isLockedForUser,
                        onCardClick = {
                            if (isLockedForUser) onProUpgradeClick() else onCategoryClick(key)
                        },
                        onCardLongPress = {
                            if (isLockedForUser) onProUpgradeClick() else onCategorySelectionChanged(key, !selectedCategories.contains(key))
                        }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (results.values.any { it.isNotEmpty() }) {
            OutlinedButton(onClick = onSaveDefaults, modifier = Modifier.fillMaxWidth()) {
                Text("Save Current Selection as Defaults")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onCleanClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = totalSelectedCount > 0
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
    isLocked: Boolean,
    onCardClick: () -> Unit,
    onCardLongPress: () -> Unit
) {
    val itemCount = items.size
    val cardColors = if (isSelected) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    }

    val border = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    }

    val icon = when (categoryName) {
        "Empty Folders" -> Icons.Default.Folder
        "Zero-Byte Files" -> Icons.Default.Note
        "Residual App Data" -> Icons.Default.Science
        "Duplicate Files" -> Icons.Default.FileCopy
        else -> Icons.Default.Folder // Fallback icon
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onCardClick() },
                    onLongPress = { onCardLongPress() }
                )
            },
        shape = RoundedCornerShape(12.dp),
        colors = cardColors,
        border = border
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.padding(start = 16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Row {
                    Text(text = categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (isLocked) {
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text("(PRO)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                Text(text = "$itemCount items found", style = MaterialTheme.typography.bodySmall)
            }
            Text(text = formatBytes(totalSize), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
