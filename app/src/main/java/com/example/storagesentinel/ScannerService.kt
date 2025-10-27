package com.example.storagesentinel

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ScannerService(private val rootDirectory: File, private val context: Context) {

    private val emptyFolderType = JunkType("Empty Folders")
    private val zeroByteFileType = JunkType("Zero-Byte Files")

    suspend fun startFullScan(): Map<JunkType, List<JunkItem>> {
        return withContext(Dispatchers.IO) {
            val emptyFolders = mutableListOf<JunkItem>()
            val zeroByteFiles = mutableListOf<JunkItem>()

            scanDirectory(rootDirectory, emptyFolders, zeroByteFiles)

            mapOf(
                emptyFolderType to emptyFolders,
                zeroByteFileType to zeroByteFiles
            )
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
}
