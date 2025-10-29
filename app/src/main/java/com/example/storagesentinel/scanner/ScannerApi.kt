package com.example.storagesentinel.scanner

/**
 * Minimal public scanner API used by the Flutter host.
 * Implementations should be lightweight and lifecycle-friendly.
 */
interface ScannerApi {
    fun startScan()
    fun stopScan()
    fun deleteItems(ids: List<String>): Boolean
    fun registerListener(listener: ScannerListener)
    fun unregisterListener(listener: ScannerListener)

    interface ScannerListener {
        fun onBatch(items: List<Map<String, Any?>>)
        fun onProgress(percent: Int)
        fun onComplete()
        fun onError(code: String, message: String)
    }
}
