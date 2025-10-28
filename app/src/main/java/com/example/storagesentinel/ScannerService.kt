package com.example.storagesentinel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import com.example.storagesentinel.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class ScannerService(
    private val rootDirectory: File,
    private val context: Context, // Context is still needed for PackageManager
    private val settingsManager: SettingsManager
) {

    private val emptyFolderType = JunkType("Empty Folders")
    private val zeroByteFileType = JunkType("Zero-Byte Files")
    private val residualDataType = JunkType("Residual App Data")
    private val largeFilesType = JunkType("Large Files")
    private val duplicateFilesType = JunkType("Duplicate Files")

    suspend fun startFullScan(onProgress: (String) -> Unit): Map<JunkType, List<JunkItem>> {
        return withContext(Dispatchers.IO) {
            val allItems = mutableListOf<JunkItem>()
            val ignoreList = settingsManager.getIgnoreList()

            scanDirectory(rootDirectory, allItems, ignoreList, onProgress)
            allItems.addAll(findResidualData(ignoreList, onProgress))
            allItems.addAll(findLargeFiles(rootDirectory, ignoreList, onProgress))
            allItems.addAll(findDuplicateFiles(rootDirectory, ignoreList, onProgress))

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
                File(rootDirectory, "EmptyFolder1").mkdir()
                File(rootDirectory, "My Documents/EmptySubFolder").mkdirs()
                File(rootDirectory, "zero_byte_file.tmp").createNewFile()
                File(rootDirectory, "Downloads/another_zero.dat").createNewFile()
                File(rootDirectory, "Android/data/com.old.uninstalled.game").mkdirs()
                File(rootDirectory, "Android/data/com.old.uninstalled.game/cache").mkdirs()
                File(rootDirectory, "Android/data/com.old.uninstalled.game/files/save.dat").createNewFile()
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
        ignoreList: Set<String>,
        onProgress: (String) -> Unit
    ) {
        if (ignoreList.contains(directory.absolutePath)) return
        onProgress(directory.absolutePath)

        val files = directory.listFiles() ?: return
        if (files.isEmpty()) {
            foundItems.add(JunkItem(directory.absolutePath, 0, emptyFolderType))
            return
        }
        for (file in files) {
            if (ignoreList.contains(file.absolutePath)) continue
            if (file.isDirectory) {
                scanDirectory(file, foundItems, ignoreList, onProgress)
            } else {
                if (file.length() == 0L) {
                    foundItems.add(JunkItem(file.absolutePath, 0L, zeroByteFileType))
                }
            }
        }
    }

    private fun findResidualData(ignoreList: Set<String>, onProgress: (String) -> Unit): List<JunkItem> {
        val residualItems = mutableListOf<JunkItem>()
        val pm = context.packageManager
        val installedPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(PackageManager.GET_META_DATA)
        }
        val installedPackageNames = installedPackages.map { it.packageName }.toSet()

        val directoriesToScan = listOf(
            File(rootDirectory, "Android/data"),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            rootDirectory
        ).distinct()

        directoriesToScan.forEach { dir ->
            onProgress(dir.absolutePath)
            if (dir.exists() && dir.isDirectory) {
                val appDataFolders = dir.listFiles { file -> file.isDirectory } ?: return@forEach
                for (folder in appDataFolders) {
                    if (ignoreList.contains(folder.absolutePath)) continue
                    val packageName = folder.name
                    if (installedPackageNames.none { name -> packageName.contains(name, ignoreCase = true) }) {
                        val folderSize = folder.walkTopDown().sumOf { it.length() }
                        if (folderSize > 0) {
                            residualItems.add(JunkItem(folder.absolutePath, folderSize, residualDataType))
                        }
                    }
                }
            }
        }

        return residualItems
    }

    private fun findLargeFiles(
        directory: File,
        ignoreList: Set<String>,
        onProgress: (String) -> Unit
    ): List<JunkItem> {
        val largeFiles = mutableListOf<JunkItem>()
        val threshold = settingsManager.getLargeFileThreshold() * 1024 * 1024 // Convert MB to Bytes
        directory.walkTopDown().onEnter { dir ->
            onProgress(dir.absolutePath)
            !ignoreList.contains(dir.absolutePath)
        }.forEach {
            if (ignoreList.contains(it.absolutePath)) return@forEach
            if (it.isFile && it.length() > threshold) {
                largeFiles.add(JunkItem(it.absolutePath, it.length(), largeFilesType, isSelected = false))
            }
        }
        return largeFiles
    }

    private fun findDuplicateFiles(
        directory: File,
        ignoreList: Set<String>,
        onProgress: (String) -> Unit
    ): List<JunkItem> {
        val allDuplicates = mutableListOf<JunkItem>()
        val filesBySize = mutableMapOf<Long, MutableList<File>>()

        directory.walkTopDown().onEnter { dir ->
            onProgress(dir.absolutePath)
            !ignoreList.contains(dir.absolutePath)
        }.forEach { file ->
            if (file.isFile && file.length() > 0 && !ignoreList.contains(file.absolutePath)) {
                filesBySize.getOrPut(file.length()) { mutableListOf() }.add(file)
            }
        }

        filesBySize.values.filter { it.size > 1 }.forEach { potentialDuplicates ->
            val filesByHash = potentialDuplicates.groupBy { getFileHash(it) }

            filesByHash.values.filter { it.size > 1 }.forEach { duplicateGroup ->
                val sortedGroup = duplicateGroup.sortedBy { it.lastModified() }
                val contentHash = getFileHash(sortedGroup.first())

                sortedGroup.forEachIndexed { index, file ->
                    val isSelected = index != 0 // Keep the oldest, select the rest
                    allDuplicates.add(
                        JunkItem(
                            path = file.absolutePath,
                            sizeBytes = file.length(),
                            type = duplicateFilesType,
                            isSelected = isSelected,
                            contentHash = contentHash
                        )
                    )
                }
            }
        }
        return allDuplicates
    }

    private fun getFileHash(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead = fis.read(buffer)
            while (bytesRead != -1) {
                md.update(buffer, 0, bytesRead)
                bytesRead = fis.read(buffer)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
