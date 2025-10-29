package com.example.storagesentinel.ui.scanner

import androidx.compose.runtime.Composable
import com.example.storagesentinel.JunkItem

@Composable
fun ReadyStateView(onScanClick: () -> Unit, onCreateDummyFilesClick: () -> Unit) {
    com.example.storagesentinel.legacy_ui.ui.scanner.ReadyStateView(onScanClick, onCreateDummyFilesClick)
}

@Composable
fun ScanningStateView(currentlyScanningPath: String?) {
    com.example.storagesentinel.legacy_ui.ui.scanner.ScanningStateView(currentlyScanningPath)
}

@Composable
fun CleaningStateView(category: String?) {
    com.example.storagesentinel.legacy_ui.ui.scanner.CleaningStateView(category)
}

@Composable
fun PostCleanSummary(cleanedItems: List<JunkItem>, onFinish: () -> Unit, onExportReport: () -> Unit) {
    com.example.storagesentinel.legacy_ui.ui.scanner.PostCleanSummary(cleanedItems, onFinish, onExportReport)
}
