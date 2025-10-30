package com.example.storagesentinel.utils

import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    if (digitGroups >= units.size) return "Large File"
    return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
}
