package com.example.storagesentinel.di

import android.content.Context
import android.os.Environment
import com.example.storagesentinel.ScannerService
import com.example.storagesentinel.data.SettingsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    fun provideScannerService(@ApplicationContext context: Context, settingsManager: SettingsManager): ScannerService {
        return ScannerService(Environment.getExternalStorageDirectory(), context, settingsManager)
    }
}
