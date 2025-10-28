package com.example.storagesentinel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.storagesentinel.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ScannerService(private val rootDirectory: File, private val context: Context) {

    private val emptyFolderType = JunkType("Empty Folders")
    private val zeroByteFileType = JunkType("Zero-Byte Files")
    private val residualDataType = JunkType("Residual App Data")
    private val largeFilesType = JunkType("Large Files")

    private val settingsManager = SettingsManager(context)

    suspend fun startFullScan(): Map<JunkType, List<JunkItem>> {
        return withContext(Dispatchers.IO) {
            val allItems = mutableListOf<JunkItem>()
            val ignoreList = settingsManager.getIgnoreList()

            scanDirectory(rootDirectory, allItems, ignoreList)
            allItems.addAll(findResidualData(ignoreList))
            allItems.addAll(findLargeFiles(rootDirectory, ignoreList))

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
    
    suspend fun createDummyJunkFiles() {
        withContext(Dispatchers.IO) {
            try {
                // Create Empty Folders
                File(rootDirectory, "EmptyFolder1").mkdir()
                File(rootDirectory, "My Documents/EmptySubFolder").mkdirs()

                // Create Zero-Byte Files
                File(rootDirectory, "zero_byte_file.tmp").createNewFile()
                File(rootDirectory, "Downloads/another_zero.dat").createNewFile()

                // Create a plausible Residual App Data folder
                File(rootDirectory, "Android/data/com.old.uninstalled.game").mkdirs()
                File(rootDirectory, "Android/data/com.old.uninstalled.game/cache").mkdirs()
                File(rootDirectory, "Android/data/com.old.uninstalled.game/files/save.dat").createNewFile()

                // Create a Large File ( > 100MB)
                val largeFile = File(rootDirectory, "large_test_file.bin")
                largeFile.outputStream().use { fos ->
                    repeat(101) { // Write 101 MB of dummy data
                        fos.write(ByteArray(1024 * 1024))
                    }
                }
            } catch (e: Exception) {
                println("Error creating dummy files: ${e.message}")
            }
        }
    }

    private fun scanDirectory(
        directory: File,
        foundItems: MutableList<JunkItem>,
        ignoreList: Set<String>
    ) {
        if (ignoreList.contains(directory.absolutePath)) return

        val files = directory.listFiles() ?: return
        if (files.isEmpty()) {
            foundItems.add(JunkItem(directory.absolutePath, 0, emptyFolderType))
            return
        }
        for (file in files) {
            if (ignoreList.contains(file.absolutePath)) continue
            if (file.isDirectory) {
                scanDirectory(file, foundItems, ignoreList)
            } else {
                if (file.length() == 0L) {
                    foundItems.add(JunkItem(file.absolutePath, 0L, zeroByteFileType))
                }
            }
        }
    }

    private fun findResidualData(ignoreList: Set<String>): List<JunkItem> {
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
            if (ignoreList.contains(folder.absolutePath)) continue
            val packageName = folder.name
            if (!installedPackageNames.contains(packageName)) {
                val folderSize = folder.walkTopDown().sumOf { it.length() }
                residualItems.add(JunkItem(folder.absolutePath, folderSize, residualDataType))
            }
        }
        return residualItems
    }

    private fun findLargeFiles(directory: File, ignoreList: Set<String>): List<JunkItem> {
        val largeFiles = mutableListOf<JunkItem>()
        val threshold = settingsManager.getLargeFileThreshold() * 1024 * 1024 // Convert MB to Bytes
        directory.walkTopDown().forEach {
            if (ignoreList.contains(it.absolutePath)) return@forEach
            if (it.isFile && it.length() > threshold) {
                largeFiles.add(JunkItem(it.absolutePath, it.length(), largeFilesType, isSelected = false))
            }
        }
        return largeFiles
    }
}
