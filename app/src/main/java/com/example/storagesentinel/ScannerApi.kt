package com.example.storagesentinel

/**
 * Simple interface defining the scanner API used by UI code (or platform channels).
 * Implementations can be native real scanner or a stub for development.
 */
interface ScannerApi {
    fun startScan()
    fun stopScan()
    fun deleteItems(ids: List<String>)
    fun addListener(listener: ScanListener)
    fun removeListener(listener: ScanListener)
}

interface ScanListener {
    /**
     * Called when a new batch of scan results is available. Payload is JSON string.
     */
    fun onScanBatch(json: String)

    /**
     * Progress update 0..100 and optional currently-scanning path.
     */
    fun onScanProgress(progress: Int, path: String?)

    /**
     * Called when scan completes with a summary payload (JSON string).
     */
    fun onScanComplete(summaryJson: String)

    fun onError(code: String, message: String)
}
