package com.example.storagesentinel.ui.scanner

import androidx.compose.runtime.Composable
import com.example.storagesentinel.JunkItem

@Composable
fun DuplicateFilesScreen(
    duplicateFiles: Map<String, List<JunkItem>>,
    onBack: () -> Unit,
    isProUser: Boolean,
    onProUpgradeClick: () -> Unit,
    onItemSelectionChanged: (JunkItem, Boolean) -> Unit
) {
    com.example.storagesentinel.legacy_ui.ui.scanner.DuplicateFilesScreen(
        duplicateFiles = duplicateFiles,
        onBack = onBack,
        isProUser = isProUser,
        onProUpgradeClick = onProUpgradeClick,
        onItemSelectionChanged = onItemSelectionChanged
    )
}

@Composable
fun DuplicateFileGroup(
    hash: String,
    items: List<JunkItem>,
    isProUser: Boolean,
    onProUpgradeClick: () -> Unit,
    onItemSelectionChanged: (JunkItem, Boolean) -> Unit
) {
    com.example.storagesentinel.legacy_ui.ui.scanner.DuplicateFileGroup(
        hash = hash,
        items = items,
        isProUser = isProUser,
        onProUpgradeClick = onProUpgradeClick,
        onItemSelectionChanged = onItemSelectionChanged
    )
}
