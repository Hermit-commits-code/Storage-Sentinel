package com.example.storagesentinel.scanner

import android.os.Handler
import android.os.Looper

/**
 * A tiny, non-blocking stub scanner implementation used as a placeholder while
 * migrating UI to Flutter. Emits simple progress and batch events.
 */
class ScannerService : ScannerApi {
    private val listeners = mutableSetOf<ScannerApi.ScannerListener>()
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    override fun startScan() {
        if (running) return
        running = true
        simulateScan()
    }

    override fun stopScan() {
        running = false
    }

    override fun deleteItems(ids: List<String>): Boolean {
        // Placeholder: pretend deletion always succeeds.
        return true
    }

    override fun registerListener(listener: ScannerApi.ScannerListener) {
        listeners.add(listener)
    }

    override fun unregisterListener(listener: ScannerApi.ScannerListener) {
        listeners.remove(listener)
    }

    private fun simulateScan() {
        var progress = 0
        fun tick() {
            if (!running) return
            progress += 20
            listeners.forEach { it.onProgress(progress.coerceAtMost(100)) }
            val batch = listOf(mapOf("id" to "item_$progress", "size" to 123))
            listeners.forEach { it.onBatch(batch) }
            if (progress >= 100) {
                listeners.forEach { it.onComplete() }
                running = false
            } else {
                handler.postDelayed({ tick() }, 300)
            }
        }
        handler.post { tick() }
    }
}
