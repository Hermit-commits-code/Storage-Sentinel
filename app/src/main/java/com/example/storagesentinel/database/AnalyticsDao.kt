package com.example.storagesentinel.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CleaningSessionDao {
    @Insert
    suspend fun insertSession(session: CleaningSession)
    
    @Query("SELECT SUM(storageFreed) FROM cleaning_sessions")
    fun getTotalStorageFreed(): Flow<Long?>
    
    @Query("SELECT SUM(filesRemoved) FROM cleaning_sessions")
    fun getTotalFilesRemoved(): Flow<Int?>
    
    @Query("SELECT * FROM cleaning_sessions ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSessions(): Flow<List<CleaningSession>>
    
    @Query("SELECT SUM(storageFreed) FROM cleaning_sessions WHERE timestamp >= :since")
    suspend fun getStorageFreedSince(since: String): Long?
    
    @Query("SELECT SUM(filesRemoved) FROM cleaning_sessions WHERE timestamp >= :since")
    suspend fun getFilesRemovedSince(since: String): Int?
}

@Dao
interface StorageSnapshotDao {
    @Insert
    suspend fun insertSnapshot(snapshot: StorageSnapshot)
    
    @Query("SELECT * FROM storage_snapshots ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSnapshot(): StorageSnapshot?
    
    @Query("SELECT * FROM storage_snapshots ORDER BY timestamp DESC LIMIT 30")
    fun getRecentSnapshots(): Flow<List<StorageSnapshot>>
}