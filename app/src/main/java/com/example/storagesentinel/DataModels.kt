package com.example.storagesentinel

// In Kotlin, a "data class" is a simple class used for holding data.
// The compiler automatically creates useful functions for these classes, like `equals()` and `toString()`.
// They are perfect for representing simple pieces of data in our app.

// Defines the categories of junk files we can find.
// For example, one instance of this class could be JunkType("Empty Folders").
// Using a class for this is safer than just using a String, as it prevents typos.
data class JunkType(val label: String)

// Represents a single junk file or empty folder found on the device.
// For example, JunkItem("/sdcard/empty_folder", 0)
// - path: The full location of the file/folder on the device.
// - sizeBytes: The size of the file in bytes. For folders, this will be 0.
data class JunkItem(val path: String, val sizeBytes: Long)
