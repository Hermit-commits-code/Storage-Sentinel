package com.example.storagesentinel.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.storagesentinel.model.JunkCategory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Entity(tableName = "cleaning_sessions")
@TypeConverters(Converters::class)
data class CleaningSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val storageFreed: Long, // bytes
    val filesRemoved: Int,
    val categoriesCleaned: Set<JunkCategory>,
    val sessionType: String = "manual" // "manual", "scheduled", "auto"
)

@Entity(tableName = "storage_snapshots")
@TypeConverters(Converters::class)
data class StorageSnapshot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val totalStorage: Long, // bytes
    val usedStorage: Long,  // bytes
    val availableStorage: Long // bytes
)

class Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(formatter)
    }
    
    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, formatter) }
    }
    
    @TypeConverter
    fun fromJunkCategorySet(value: Set<JunkCategory>): String {
        return value.joinToString(",") { it.name }
    }
    
    @TypeConverter
    fun toJunkCategorySet(value: String): Set<JunkCategory> {
        return if (value.isEmpty()) emptySet() 
        else value.split(",").mapNotNull { 
            try { JunkCategory.valueOf(it) } catch (e: Exception) { null }
        }.toSet()
    }
}