package com.example.storagesentinel.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CleaningSession::class, StorageSnapshot::class],
    version = 1,
    exportSchema = false
)
abstract class AnalyticsDatabase : RoomDatabase() {
    abstract fun cleaningSessionDao(): CleaningSessionDao
    abstract fun storageSnapshotDao(): StorageSnapshotDao
    
    companion object {
        @Volatile
        private var INSTANCE: AnalyticsDatabase? = null
        
        fun getDatabase(context: Context): AnalyticsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnalyticsDatabase::class.java,
                    "analytics_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}