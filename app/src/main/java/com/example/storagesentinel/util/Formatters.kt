package com.example.storagesentinel.util

import androidx.compose.runtime.Composable
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / 1024.0.pow(digitGroups), units[digitGroups])
}
