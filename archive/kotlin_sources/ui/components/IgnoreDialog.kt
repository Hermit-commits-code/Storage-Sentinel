package com.example.storagesentinel.ui.components

import androidx.compose.runtime.Composable
import com.example.storagesentinel.JunkItem

@Composable
fun IgnoreItemDialog(item: JunkItem, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    com.example.storagesentinel.legacy_ui.ui.components.IgnoreItemDialog(item, onConfirm, onDismiss)
}
