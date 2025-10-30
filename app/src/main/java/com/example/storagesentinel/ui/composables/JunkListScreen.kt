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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.storagesentinel.model.JunkItem
import com.example.storagesentinel.utils.formatFileSize
import com.example.storagesentinel.viewmodel.JunkListViewModel
import com.example.storagesentinel.viewmodel.JunkListViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JunkListScreen(onNavigateBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    val factory = JunkListViewModelFactory(application)
    val viewModel: JunkListViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.title) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isScanning) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.items.isEmpty()) {
                Text("No items found in this category.", modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.items, key = { it.id }) { item ->
                            JunkItemRow(item = item, isSelected = item.id in uiState.selectedItems, onToggle = { viewModel.toggleSelection(item) })
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.deleteSelected() },
                        enabled = uiState.selectedItems.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Selected (${formatFileSize(uiState.totalSelectedSize)})")
                    }
                    Spacer(modifier = Modifier.height(16.dp)) // Add some padding at the bottom
                }
            }
        }
    }
}

@Composable
private fun JunkItemRow(item: JunkItem, isSelected: Boolean, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name)
            Text(item.path, style = MaterialTheme.typography.bodySmall)
        }
        Text(formatFileSize(item.sizeInBytes))
    }
}
