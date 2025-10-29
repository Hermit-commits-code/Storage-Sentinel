package com.example.storagesentinel.ui.components

import androidx.compose.runtime.Composable

@Composable
fun ProUpgradeDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    com.example.storagesentinel.legacy_ui.ui.components.ProUpgradeDialog(onConfirm, onDismiss)
}
