package com.example.storagesentinel

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.CopyOnWriteArrayList

class ScannerService : ScannerApi {
    private val listeners = CopyOnWriteArrayList<ScanListener>()
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    override fun addListener(listener: ScanListener) = listeners.add(listener)
    override fun removeListener(listener: ScanListener) = listeners.remove(listener)

    override fun startScan() {
        if (running) return
        running = true
        simulate()
    }

    override fun stopScan() { running = false }

    override fun deleteItems(ids: List<String>) {
        val o = JSONObject().apply {
            put("type", "deleteResult")
            put("deletedCount", ids.size)
        }
        emit(o.toString())
    }

    private fun emit(json: String) { listeners.forEach { it.onScanEvent(json) } }

    private fun simulate() {
        var progress = 0
        fun tick() {
            if (!running) return
            progress += 25
            val p = JSONObject().put("type", "progress").put("progress", progress.coerceAtMost(100))
            emit(p.toString())

            val item = JSONObject().put("id", "item_$progress").put("size", 123)
            val batch = JSONArray().put(item)
            val b = JSONObject().put("type", "batch").put("items", batch)
            emit(b.toString())

            if (progress >= 100) {
                emit(JSONObject().put("type", "complete").toString())
                running = false
            } else {
                handler.postDelayed({ tick() }, 300)
            }
        }
        handler.post { tick() }
    }
}
