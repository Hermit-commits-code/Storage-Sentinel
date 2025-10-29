package com.example.storagesentinel.util

import com.example.storagesentinel.JunkItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.example.storagesentinel.util.formatBytes

class ReportGenerator @Inject constructor() {

    fun generateReport(cleanedItems: List<JunkItem>): String {
        val reportBuilder = StringBuilder()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        reportBuilder.append("*** Storage Sentinel Cleanup Report ***\n")
        reportBuilder.append("Report generated on: $timestamp\n\n")

        val totalReclaimed = cleanedItems.sumOf { it.sizeBytes }
        reportBuilder.append("Total space reclaimed: ${formatBytes(totalReclaimed)}\n")
        reportBuilder.append("Total items removed: ${cleanedItems.size}\n\n")

        reportBuilder.append("--- Summary by Category ---\n")
        cleanedItems.groupBy { it.type.label }.forEach { (category, items) ->
            val categorySize = items.sumOf { it.sizeBytes }
            reportBuilder.append("- $category: ${items.size} items (${formatBytes(categorySize)})\n")
        }

        reportBuilder.append("\n--- Detailed Log of Removed Items ---\n")
        cleanedItems.forEach {
            reportBuilder.append("${it.path} (${formatBytes(it.sizeBytes)})\n")
        }

        reportBuilder.append("\n*** End of Report ***")

        return reportBuilder.toString()
    }
}
