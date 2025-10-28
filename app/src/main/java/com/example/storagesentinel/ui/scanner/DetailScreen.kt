package com.example.storagesentinel.ui.scanner

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.storagesentinel.JunkItem
import com.example.storagesentinel.JunkType
import com.example.storagesentinel.ui.components.IgnoreItemDialog
import com.example.storagesentinel.util.formatBytes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    junkType: JunkType,
    items: List<JunkItem>,
    onBack: () -> Unit,
    onItemSelectionChanged: (JunkItem, Boolean) -> Unit,
    onAddToIgnoreList: (JunkItem) -> Unit
) {
    var itemToIgnore by remember { mutableStateOf<JunkItem?>(null) }

    itemToIgnore?.let { item ->
        IgnoreItemDialog(
            item = item,
            onConfirm = {
                onAddToIgnoreList(item)
                itemToIgnore = null
            },
            onDismiss = { itemToIgnore = null }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(junkType.label) }, navigationIcon = {
            TextButton(onClick = onBack) { Text("Back") }
        })
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            items(items) { item ->
                DetailItem(
                    item = item,
                    onSelectionChange = { isSelected ->
                        onItemSelectionChanged(item, isSelected)
                    },
                    onLongPress = { itemToIgnore = item }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun DetailItem(item: JunkItem, onSelectionChange: (Boolean) -> Unit, onLongPress: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onLongPress() })
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isSelected,
            onCheckedChange = onSelectionChange
        )
        Spacer(modifier = Modifier.padding(start = 16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.path, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            Text(text = formatBytes(item.sizeBytes), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}
