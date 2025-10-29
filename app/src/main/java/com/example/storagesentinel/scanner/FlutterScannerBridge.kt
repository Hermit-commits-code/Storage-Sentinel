package com.example.storagesentinel.scanner

/**
 * A lightweight, Flutter-agnostic bridge helper.
 *
 * This class intentionally avoids compile-time references to Flutter SDK types so
 * the host can remain free of Flutter compile-time dependencies. The runtime
 * `FlutterIntegration` (already present) can call `handleMethod` and provide an
 * event sink lambda to receive events.
 */
class FlutterScannerBridge(private val scanner: ScannerApi) {
    private var eventSink: ((Any?) -> Unit)? = null

    fun handleMethod(method: String, arguments: Any?): Any? {
        return when (method) {
            "startScan" -> { scanner.startScan(); null }
            "stopScan" -> { scanner.stopScan(); null }
            "deleteItems" -> {
                val ids = when (arguments) {
                    is List<*> -> arguments.filterIsInstance<String>()
                    else -> emptyList()
                }
                scanner.deleteItems(ids)
            }
            else -> throw UnsupportedOperationException("Method not implemented: $method")
        }
    }

    fun setEventSink(sink: ((Any?) -> Unit)?) {
        if (sink == null) {
            scanner.unregisterListener(listener)
            eventSink = null
        } else {
            eventSink = sink
            scanner.registerListener(listener)
        }
    }

    private val listener = object : ScannerApi.ScannerListener {
        override fun onBatch(items: List<Map<String, Any?>>) {
            eventSink?.invoke(mapOf("type" to "batch", "items" to items))
        }

        override fun onProgress(percent: Int) {
            eventSink?.invoke(mapOf("type" to "progress", "percent" to percent))
        }

        override fun onComplete() {
            eventSink?.invoke(mapOf("type" to "complete"))
        }

        override fun onError(code: String, message: String) {
            eventSink?.invoke(mapOf("type" to "error", "code" to code, "message" to message))
        }
    }
}
