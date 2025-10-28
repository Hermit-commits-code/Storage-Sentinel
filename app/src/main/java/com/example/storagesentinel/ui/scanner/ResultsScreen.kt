package com.example.storagesentinel.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.storagesentinel.JunkItem
import com.example.storagesentinel.JunkType
import com.example.storagesentinel.util.formatBytes

@Composable
fun ResultsDisplay(
    results: Map<JunkType, List<JunkItem>>,
    selectedCategories: Set<JunkType>,
    onCleanClick: () -> Unit,
    onCategoryClick: (JunkType) -> Unit,
    onCategorySelectionChanged: (JunkType, Boolean) -> Unit
) {
    val totalJunkCount = results.values.sumOf { list -> list.count { it.isSelected } }
    val totalJunkSize = results.values.flatten().filter { it.isSelected }.sumOf { it.sizeBytes }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Scan Complete!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Selected for Cleaning: ${formatBytes(totalJunkSize)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Across $totalJunkCount items", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            val filtered = results.filter { it.value.isNotEmpty() }
            items(filtered.keys.toList()) { key ->
                JunkCategorySummary(
                    categoryName = key.label,
                    items = filtered[key] ?: emptyList(),
                    totalSize = (filtered[key] ?: emptyList()).sumOf { it.sizeBytes },
                    isSelected = selectedCategories.contains(key),
                    onSelectionChange = { isSelected -> onCategorySelectionChanged(key, isSelected) },
                    onCardClick = { onCategoryClick(key) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (results.values.any { it.isNotEmpty() }) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCleanClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = totalJunkCount > 0
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
    onSelectionChange: (Boolean) -> Unit,
    onCardClick: () -> Unit
) {
    val itemCount = items.size

    Card(
        onClick = onCardClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isSelected, onCheckedChange = onSelectionChange)
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(text = categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = "$itemCount items found", style = MaterialTheme.typography.bodySmall)
                }
                Text(text = formatBytes(totalSize), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
