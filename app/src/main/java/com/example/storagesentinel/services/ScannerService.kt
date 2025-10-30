package com.example.storagesentinel.services

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.storagesentinel.api.ScannerApi
import com.example.storagesentinel.managers.AnalyticsManager
import com.example.storagesentinel.managers.SettingsManager
import com.example.storagesentinel.model.JunkCategory
import com.example.storagesentinel.model.JunkItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

class ScannerService(private val context: Context, private val settingsManager: SettingsManager) : ScannerApi {

    private val analyticsManager = AnalyticsManager(context)

    override fun scan(): Flow<List<JunkItem>> = flow {
        Log.d("ScannerService", "Scan started.")
        val junkItemsFound = mutableListOf<JunkItem>()
        val largeFileThresholdBytes = settingsManager.largeFileThresholdMb.first() * 1024 * 1024
        val rootDirectory = Environment.getExternalStorageDirectory()
        val androidDataDir = File(rootDirectory, "Android/data")
        val excludedPaths = listOf(androidDataDir.path)

        // Run all scans on the entire external storage
        junkItemsFound.addAll(findResidualAppData())
        junkItemsFound.addAll(findAppCaches())
        junkItemsFound.addAll(findDuplicateFiles(rootDirectory, excludedPaths))
        searchDirectory(rootDirectory, junkItemsFound, excludedPaths, largeFileThresholdBytes)

        delay(1000)
        Log.d("ScannerService", "Scan finished. Found ${junkItemsFound.size} items.")
        emit(junkItemsFound)
    }.flowOn(Dispatchers.IO)

    override suspend fun delete(items: List<JunkItem>) {
        withContext(Dispatchers.IO) {
            var totalFreed = 0L
            var filesDeleted = 0
            val categoriesCleaned = mutableSetOf<JunkCategory>()
            
            items.forEach { item ->
                try {
                    val file = File(item.path)
                    if (file.exists()) {
                        val fileSize = item.sizeInBytes
                        if (file.deleteRecursively()) {
                            totalFreed += fileSize
                            filesDeleted++
                            categoriesCleaned.add(item.category)
                            Log.d("ScannerService", "Deleted: ${item.path} (${fileSize} bytes)")
                        } else {
                            Log.e("ScannerService", "Failed to delete: ${item.path}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ScannerService", "Error deleting ${item.path}", e)
                }
            }
            
            // Record the cleaning session in analytics
            if (filesDeleted > 0) {
                analyticsManager.recordCleaningSession(
                    storageFreed = totalFreed,
                    filesRemoved = filesDeleted,
                    categoriesCleaned = categoriesCleaned,
                    sessionType = "manual"
                )
                Log.d("ScannerService", "Recorded cleaning session: ${totalFreed} bytes freed, ${filesDeleted} files deleted")
            }
        }
    }

    private fun findDuplicateFiles(root: File, excludedPaths: List<String>): List<JunkItem> {
        val duplicates = mutableListOf<JunkItem>()
        val filesBySize = mutableMapOf<Long, MutableList<File>>()

        try {
            root.walkTopDown()
                .onEnter { dir -> !excludedPaths.any { dir.path.startsWith(it) } }
                .filter { it.isFile && it.length() > 0 } // Include all non-empty files
                .forEach { file -> 
                    filesBySize.getOrPut(file.length()) { mutableListOf() }.add(file) 
                }

            // Process files that have the same size
            filesBySize.values.filter { it.size > 1 }.forEach { potentialDuplicates ->
                // For files larger than 1KB, use hash comparison for accuracy
                if (potentialDuplicates.first().length() > 1024) {
                    potentialDuplicates.groupBy { getFileHash(it) }
                        .filterKeys { it.isNotEmpty() }
                        .forEach { (hash, group) ->
                            if (group.size > 1) {
                                group.forEach { file ->
                                    duplicates.add(
                                        JunkItem(
                                            file.absolutePath,
                                            file.name,
                                            file.absolutePath,
                                            file.length(),
                                            JunkCategory.DUPLICATE_FILE,
                                            hash
                                        )
                                    )
                                }
                            }
                        }
                } else {
                    // For small files, use simple content comparison
                    val contentGroups = potentialDuplicates.groupBy { getFileContent(it) }
                    contentGroups.values.filter { it.size > 1 }.forEach { group ->
                        group.forEach { file ->
                            duplicates.add(
                                JunkItem(
                                    file.absolutePath,
                                    file.name,
                                    file.absolutePath,
                                    file.length(),
                                    JunkCategory.DUPLICATE_FILE,
                                    "small_file_${file.length()}"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ScannerService", "Error finding duplicate files", e)
        }
        
        return duplicates
    }

    private fun getFileHash(file: File): String {
        try {
            FileInputStream(file).use { fis ->
                val digest = MessageDigest.getInstance("MD5")
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
                return digest.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (e: Exception) {
            Log.e("ScannerService", "Error hashing file ${file.path}", e)
            return ""
        }
    }

    private fun getFileContent(file: File): String {
        return try {
            if (file.length() > 1024) {
                // For larger files, just read first 1KB
                file.readBytes().take(1024).toByteArray().contentToString()
            } else {
                // For small files, read entire content
                file.readText()
            }
        } catch (e: Exception) {
            Log.e("ScannerService", "Error reading file content ${file.path}", e)
            ""
        }
    }

    private fun searchDirectory(directory: File, junkItemsFound: MutableList<JunkItem>, excludedPaths: List<String>, largeFileThresholdBytes: Long) {
        if (excludedPaths.any { directory.path.startsWith(it) }) return

        val children = try { 
            directory.listFiles() 
        } catch (e: Exception) { 
            Log.w("ScannerService", "Cannot access directory: ${directory.path}")
            null 
        } ?: return

        // Check if directory is empty (excluding the root directory)
        if (children.isEmpty() && directory.path != Environment.getExternalStorageDirectory().path) {
            if (junkItemsFound.none { it.path == directory.path }) {
                junkItemsFound.add(
                    JunkItem(
                        directory.absolutePath,
                        directory.name,
                        directory.absolutePath,
                        0,
                        JunkCategory.EMPTY_FOLDER
                    )
                )
            }
            return
        }

        children.forEach { child ->
            try {
                if (child.isDirectory) {
                    searchDirectory(child, junkItemsFound, excludedPaths, largeFileThresholdBytes)
                } else if (child.isFile) {
                    // Avoid double-counting files already found by other scan methods
                    if (junkItemsFound.none { it.path == child.path }) {
                        when {
                            child.length() > largeFileThresholdBytes -> {
                                junkItemsFound.add(
                                    JunkItem(
                                        child.absolutePath,
                                        child.name,
                                        child.absolutePath,
                                        child.length(),
                                        JunkCategory.LARGE_FILE
                                    )
                                )
                            }
                            child.length() == 0L -> {
                                junkItemsFound.add(
                                    JunkItem(
                                        child.absolutePath,
                                        child.name,
                                        child.absolutePath,
                                        0,
                                        JunkCategory.ZERO_BYTE_FILE
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("ScannerService", "Error processing ${child.path}: ${e.message}")
            }
        }
    }

    private fun findAppCaches(): List<JunkItem> {
        val cacheItems = mutableListOf<JunkItem>()
        val rootDirectory = Environment.getExternalStorageDirectory()
        
        // Common cache file patterns
        val cachePatterns = listOf(".cache", ".tmp", ".temp", ".log")
        val cacheExtensions = listOf("tmp", "cache", "log", "temp")
        
        try {
            rootDirectory.walkTopDown()
                .filter { it.isFile }
                .filter { file ->
                    // Check if file is in a cache directory or has cache-like name
                    file.path.contains("cache", ignoreCase = true) ||
                    file.path.contains("temp", ignoreCase = true) ||
                    cachePatterns.any { pattern -> file.name.contains(pattern, ignoreCase = true) } ||
                    cacheExtensions.any { ext -> file.extension.equals(ext, ignoreCase = true) }
                }
                .forEach { file ->
                    cacheItems.add(
                        JunkItem(
                            file.absolutePath,
                            file.name,
                            file.absolutePath,
                            file.length(),
                            JunkCategory.TEMP_CACHE
                        )
                    )
                }
        } catch (e: Exception) {
            Log.e("ScannerService", "Error finding cache files", e)
        }
        
        return cacheItems
    }

    private fun findResidualAppData(): List<JunkItem> {
        val residualItems = mutableListOf<JunkItem>()
        val rootDirectory = Environment.getExternalStorageDirectory()
        
        try {
            // Look for common app data folders that might be leftover
            val commonAppFolders = listOf(
                "Android/obb",
                "Android/media",
                ".com.google.Chrome",
                ".facebook",
                ".instagram",
                ".whatsapp",
                ".telegram"
            )
            
            commonAppFolders.forEach { folderPath ->
                val folder = File(rootDirectory, folderPath)
                if (folder.exists() && folder.isDirectory) {
                    folder.walkTopDown()
                        .filter { it.isFile }
                        .forEach { file ->
                            residualItems.add(
                                JunkItem(
                                    file.absolutePath,
                                    file.name,
                                    file.absolutePath,
                                    file.length(),
                                    JunkCategory.RESIDUAL_APP_DATA
                                )
                            )
                        }
                }
            }
        } catch (e: Exception) {
            Log.e("ScannerService", "Error finding residual app data", e)
        }
        
        return residualItems
    }

    private fun getDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }

    suspend fun developer_createFakeJunk() {
        withContext(Dispatchers.IO) {
            val root = Environment.getExternalStorageDirectory()
            val junkDir = File(root, ".ST_dev_junk")
            if (!junkDir.exists()) junkDir.mkdirs()

            try {
                Log.d("ScannerService", "Creating fake junk files...")
                
                // CORRECTED: Create a 150MB file efficiently
                val largeFile = File(junkDir, "large_video.mp4")
                if (!largeFile.exists() || largeFile.length() < 150 * 1024 * 1024) {
                    FileOutputStream(largeFile).use { fos ->
                        val buffer = ByteArray(1024 * 1024) // 1MB buffer
                        repeat(150) { // Write 1MB 150 times
                            fos.write(buffer)
                        }
                    }
                }

                // CORRECTED: Create duplicate files larger than 1KB
                val docContent = "This is a test document for finding duplicates. ".repeat(50).toByteArray() // Content is now > 1KB
                File(junkDir, "duplicate_document.pdf").apply { if (!exists()) writeBytes(docContent) }
                File(junkDir, "duplicate_document_copy.pdf").apply { if (!exists()) writeBytes(docContent) }
                
                File(junkDir, "zero_byte_file.tmp").apply { if (!exists()) createNewFile() }
                File(junkDir, "empty_test_folder").apply { if (!exists()) mkdirs() }
                
                Log.d("ScannerService", "Fake junk creation complete.")
            } catch (e: Exception) {
                Log.e("ScannerService", "Failed to create fake junk", e)
            }
        }
    }
}
