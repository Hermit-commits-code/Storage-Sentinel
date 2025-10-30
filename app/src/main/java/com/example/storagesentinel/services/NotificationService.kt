package com.example.storagesentinel.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.storagesentinel.R
import com.example.storagesentinel.utils.formatFileSize

class NotificationService(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "storage_alerts"
        private const val CHANNEL_NAME = "Storage Alerts"
        private const val NOTIFICATION_ID_SMART_ALERT = 1001
        private const val NOTIFICATION_ID_CLEAN_COMPLETE = 1002
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for storage cleaning recommendations and completion"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showSmartRecommendation(cleanableSize: Long, fileCount: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("💡 Storage Optimization Available")
            .setContentText("You can free up ${formatFileSize(cleanableSize)} by cleaning $fileCount files")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Storage Sentinel found ${formatFileSize(cleanableSize)} worth of junk files that can be safely removed. Tap to start cleaning and optimize your device storage."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
            
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_SMART_ALERT, notification)
            } catch (e: SecurityException) {
                // Handle permission not granted
            }
        }
    }
    
    fun showCleaningComplete(freedSize: Long, filesRemoved: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("✅ Cleaning Complete")
            .setContentText("Freed ${formatFileSize(freedSize)} by removing $filesRemoved files")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Storage Sentinel successfully cleaned your device! Removed $filesRemoved files and freed up ${formatFileSize(freedSize)} of storage space."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
            
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_CLEAN_COMPLETE, notification)
            } catch (e: SecurityException) {
                // Handle permission not granted
            }
        }
    }
    
    fun showScheduledCleanReminder(estimatedCleanableSize: Long) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔄 Scheduled Cleaning Ready")
            .setContentText("Ready to clean approximately ${formatFileSize(estimatedCleanableSize)}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
            
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_SMART_ALERT, notification)
            } catch (e: SecurityException) {
                // Handle permission not granted
            }
        }
    }
}