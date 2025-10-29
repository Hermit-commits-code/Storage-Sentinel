package com.example.storagesentinel

interface ScanListener {
    fun onScanEvent(payloadJson: String)
}

interface ScannerApi {
    fun addListener(listener: ScanListener)
    fun removeListener(listener: ScanListener)
    fun startScan()
    fun stopScan()
    fun deleteItems(ids: List<String>)
}
