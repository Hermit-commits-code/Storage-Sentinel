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
            val emptyFolders = mutableListOf<JunkItem>()
            val zeroByteFiles = mutableListOf<JunkItem>()
            scanDirectory(rootDirectory, emptyFolders, zeroByteFiles)
            val residualFiles = findResidualData()

            mapOf(
                emptyFolderType to emptyFolders,
                zeroByteFileType to zeroByteFiles,
                residualDataType to residualFiles
            )
        }
    }

    /**
     * Deletes the files and folders specified in the junkItems list.
     */
    suspend fun deleteJunkItems(junkItems: List<JunkItem>): Boolean {
        return withContext(Dispatchers.IO) {
            var allSucceeded = true
            for (item in junkItems) {
                try {
                    val file = File(item.path)
                    if (file.exists() && !file.deleteRecursively()) {
                        println("Failed to delete: ${item.path}")
                        allSucceeded = false
                    }
                } catch (e: Exception) {
                    println("Error deleting ${item.path}: ${e.message}")
                    allSucceeded = false
                }
            }
            allSucceeded
        }
    }

    private fun scanDirectory(
        directory: File,
        emptyFoldersList: MutableList<JunkItem>,
        zeroByteFilesList: MutableList<JunkItem>
    ) {
        val files = directory.listFiles() ?: return
        if (files.isEmpty()) {
            emptyFoldersList.add(JunkItem(directory.absolutePath, 0))
            return
        }
        for (file in files) {
            if (file.isDirectory) {
                scanDirectory(file, emptyFoldersList, zeroByteFilesList)
            } else {
                if (file.length() == 0L) {
                    zeroByteFilesList.add(JunkItem(file.absolutePath, 0L))
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
                residualItems.add(JunkItem(folder.absolutePath, folderSize))
            }
        }
        return residualItems
    }
}
