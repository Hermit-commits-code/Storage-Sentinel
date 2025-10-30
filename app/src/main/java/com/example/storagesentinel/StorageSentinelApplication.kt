package com.example.storagesentinel

import android.app.Application
import com.example.storagesentinel.billing.BillingManager

class StorageSentinelApplication : Application() {
    
    val billingManager by lazy { BillingManager(this) }
    
    override fun onCreate() {
        super.onCreate()
    }
    
    override fun onTerminate() {
        super.onTerminate()
        billingManager.disconnect()
    }
}