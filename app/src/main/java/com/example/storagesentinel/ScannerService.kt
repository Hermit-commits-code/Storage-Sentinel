package com.example.storagesentinel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ScannerService(private val rootDirectory: File, private val context: Context) {

    private val emptyFolderType = JunkType("Empty Folders")
    private val zeroByteFileType = JunkType("Zero-Byte Files")
    private val residualDataType = JunkType("Residual App Data")

    suspend fun startFullScan(): Map<JunkType, List<JunkItem>> {
        return withContext(Dispatchers.IO) {
            val allItems = mutableListOf<JunkItem>()

            // Run the recursive scan and add the results to a single list
            scanDirectory(rootDirectory, allItems)
            // Run the residual data scan and add the results
            allItems.addAll(findResidualData())

            // Group all the found items by their type to create the final map
            allItems.groupBy { it.type }
        }
    }

    suspend fun deleteJunkItems(junkItems: List<JunkItem>): List<String> {
        return withContext(Dispatchers.IO) {
            val errors = mutableListOf<String>()
            for (item in junkItems) {
                try {
                    val file = File(item.path)
                    if (file.exists() && !file.deleteRecursively()) {
                        val errorMsg = "Failed to delete: ${item.path}"
                        println(errorMsg)
                        errors.add(errorMsg)
                    }
                } catch (e: Exception) {
                    val errorMsg = "Error deleting ${item.path}: ${e.message}"
                    println(errorMsg)
                    errors.add(errorMsg)
                }
            }
            errors
        }
    }

    private fun scanDirectory(
        directory: File,
        foundItems: MutableList<JunkItem> // A single list to add all findings to
    ) {
        val files = directory.listFiles() ?: return
        if (files.isEmpty()) {
            foundItems.add(JunkItem(directory.absolutePath, 0, emptyFolderType))
            return
        }
        for (file in files) {
            if (file.isDirectory) {
                scanDirectory(file, foundItems)
            } else {
                if (file.length() == 0L) {
                    foundItems.add(JunkItem(file.absolutePath, 0L, zeroByteFileType))
                }
            }
        }
    }

    private fun findResidualData(): List<JunkItem> {
        val residualItems = mutableListOf<JunkItem>()
        val pm = context.packageManager
        val installedPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(PackageManager.GET_META_DATA)
        }
        val installedPackageNames = installedPackages.map { it.packageName }.toSet()
        val dataDir = File(rootDirectory, "Android/data")
        if (!dataDir.exists() || !dataDir.isDirectory) return emptyList()

        val appDataFolders = dataDir.listFiles { file -> file.isDirectory } ?: return emptyList()

        for (folder in appDataFolders) {
            val packageName = folder.name
            if (!installedPackageNames.contains(packageName)) {
                val folderSize = folder.walkTopDown().sumOf { it.length() }
                residualItems.add(JunkItem(folder.absolutePath, folderSize, residualDataType))
            }
        }
        return residualItems
    }
}
