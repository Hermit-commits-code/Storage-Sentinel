package com.example.storagesentinel.ui.scanner

import androidx.compose.runtime.Composable
import com.example.storagesentinel.JunkItem
import com.example.storagesentinel.JunkType

@Composable
fun DetailScreen(
    junkType: JunkType,
    items: List<JunkItem>,
    onBack: () -> Unit,
    onItemSelectionChanged: (JunkItem, Boolean) -> Unit,
    onAddToIgnoreList: (JunkItem) -> Unit
) {
    com.example.storagesentinel.legacy_ui.ui.scanner.DetailScreen(
        junkType = junkType,
        items = items,
        onBack = onBack,
        onItemSelectionChanged = onItemSelectionChanged,
        onAddToIgnoreList = onAddToIgnoreList
    )
}

@Composable
fun DetailItem(item: JunkItem, onSelectionChange: (Boolean) -> Unit, onLongPress: () -> Unit) {
    com.example.storagesentinel.legacy_ui.ui.scanner.DetailItem(
        item = item,
        onSelectionChange = onSelectionChange,
        onLongPress = onLongPress
    )
}
