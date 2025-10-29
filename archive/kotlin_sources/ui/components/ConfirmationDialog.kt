package com.example.storagesentinel.ui.components

import androidx.compose.runtime.Composable

@Composable
fun ConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    com.example.storagesentinel.legacy_ui.ui.components.ConfirmationDialog(onConfirm, onDismiss)
}
