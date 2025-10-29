package com.example.storagesentinel

/**
 * Template bridge for wiring Flutter MethodChannel / EventChannel to the native ScannerApi.
 *
 * This file intentionally contains a small runtime-safe stub to avoid requiring Flutter
 * embedding classes at compile time. It also documents how to wire channels once the
 * Flutter module is added to the Android project.
 */
object FlutterScannerBridge {
    // In local development we use a stub. Replace with real ScannerApi implementation as needed.
    val scanner: ScannerApi = ScannerApiStub()

    /**
     * Call this to register a listener that forwards events to a platform-specific EventChannel
     * or to any UI observer. In the real integration this function would create/attach a
     * FlutterEventChannel.StreamHandler and call eventSink.success(jsonString) when an event
     * arrives.
     */
    fun registerListener(listener: ScanListener) {
        scanner.addListener(listener)
    }

    fun unregisterListener(listener: ScanListener) {
        scanner.removeListener(listener)
    }

    /**
     * Example method to show how a MethodChannel could call into native code. In the actual
     * integration you would register a MethodChannel handler and map method names to these
     * functions.
     */
    fun handleMethodCall(method: String, args: Any?): Any? {
        return when (method) {
            "startScan" -> {
                scanner.startScan(); "ok"
            }
            "stopScan" -> {
                scanner.stopScan(); "ok"
            }
            "deleteItems" -> {
                val list = (args as? Map<String, Any?>)?.get("items") as? List<String> ?: emptyList()
                scanner.deleteItems(list); "ok"
            }
            else -> null
        }
    }

    /*
    Real integration sketch (non-compiling template):

    // In your Activity or Application:
    val flutterEngine = FlutterEngine(context)
    MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "com.storagesentinel/scan_commands").setMethodCallHandler { call, result ->
        when (call.method) {
            "startScan" -> { scanner.startScan(); result.success(null) }
            "stopScan" -> { scanner.stopScan(); result.success(null) }
            "deleteItems" -> { val ids = call.argument<List<String>>("items") ?: emptyList(); scanner.deleteItems(ids); result.success(null)}
            else -> result.notImplemented()
        }
    }

    val eventChannel = EventChannel(flutterEngine.dartExecutor.binaryMessenger, "com.storagesentinel/scan_events")
    eventChannel.setStreamHandler(object: EventChannel.StreamHandler {
        override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
            // add listener that calls events?.success(jsonString)
        }
        override fun onCancel(arguments: Any?) { }
    })
    */
}
