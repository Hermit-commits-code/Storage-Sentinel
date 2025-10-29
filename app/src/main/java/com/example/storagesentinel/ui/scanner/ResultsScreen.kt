package com.example.storagesentinel.ui.scanner

import androidx.compose.runtime.Composable
import com.example.storagesentinel.JunkItem
import com.example.storagesentinel.JunkType

/**
 * This file has been moved to `legacy_ui`. The original implementation remains there.
 * To keep existing imports working, this is a thin wrapper that delegates to the
 * legacy implementation.
 */
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
    com.example.storagesentinel.legacy_ui.ui.scanner.ResultsDisplay(
        results = results,
        selectedCategories = selectedCategories,
        isProUser = isProUser,
        onCleanClick = onCleanClick,
        onCategoryClick = onCategoryClick,
        onCategorySelectionChanged = onCategorySelectionChanged,
        onProUpgradeClick = onProUpgradeClick,
        onSaveDefaults = onSaveDefaults
    )
}

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
    com.example.storagesentinel.legacy_ui.ui.scanner.JunkCategorySummary(
        categoryName = categoryName,
        items = items,
        totalSize = totalSize,
        isSelected = isSelected,
        isLocked = isLocked,
        onCardClick = onCardClick,
        onCardLongPress = onCardLongPress
    )
}
