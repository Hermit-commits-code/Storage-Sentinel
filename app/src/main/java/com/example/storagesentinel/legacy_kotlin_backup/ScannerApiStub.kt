package com.example.storagesentinel

import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Development stub that emits mock scan events on a schedule. Useful for local Flutter UI development
 * before wiring real platform channels.
 */
class ScannerApiStub : ScannerApi {
    private val listeners = CopyOnWriteArraySet<ScanListener>()
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var running = false

    override fun startScan() {
        if (running) return
        running = true
        var batch = 0
        scheduler.scheduleAtFixedRate({
            if (!running) return@scheduleAtFixedRate
            val json = makeMockBatch(batch++)
            listeners.forEach { it.onScanBatch(json) }
            listeners.forEach { it.onScanProgress((batch * 20).coerceAtMost(100), "/storage/emulated/0/Download") }
            if (batch >= 5) {
                val summary = "{\"type\":\"scanComplete\",\"totalFound\":5}"
                listeners.forEach { it.onScanComplete(summary) }
                running = false
            }
        }, 0, 1, TimeUnit.SECONDS)
    }

    override fun stopScan() {
        running = false
    }

    override fun deleteItems(ids: List<String>) {
        // simulate immediate success
        // In a real native implementation this would perform deletion and emit progress/errors
    }

    override fun addListener(listener: ScanListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: ScanListener) {
        listeners.remove(listener)
    }

    private fun makeMockBatch(batch: Int): String {
        // Simple JSON with type and items
        val items = (0 until 3).joinToString(",") { i ->
            "{\"id\":\"item-${batch}-$i\",\"path\":\"/storage/emulated/0/Download/file_${batch}_$i.jpg\",\"sizeBytes\":${(i+1)*1024}}"
        }
        return "{\"type\":\"scanBatch\",\"batch\":$batch,\"items\":[$items]}"
    }
}
