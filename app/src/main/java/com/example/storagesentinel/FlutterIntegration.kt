package com.example.storagesentinel

import android.content.Context
import android.util.Log

/**
 * Lightweight, reflection-based glue that attempts to create a FlutterEngine and
 * register MethodChannel and EventChannel handlers if Flutter is available at runtime.
 *
 * This avoids adding a compile-time dependency on Flutter in the host app while
 * allowing a proper Add-to-App integration when the Flutter engine/artifacts are added.
 */
object FlutterIntegration {
    private const val TAG = "FlutterIntegration"

    fun setupFlutterIfAvailable(context: Context) {
        try {
            // Try to load FlutterEngine class
            val flutterEngineClass = Class.forName("io.flutter.embedding.engine.FlutterEngine")
            val dartExecutorClass = Class.forName("io.flutter.embedding.engine.dart.DartExecutor")
            val binaryMessengerClass = Class.forName("io.flutter.plugin.common.BinaryMessenger")

            // Create a new FlutterEngine(context)
            val constructor = flutterEngineClass.getConstructor(Context::class.java)
            val flutterEngine = constructor.newInstance(context)

            // Get dartExecutor = flutterEngine.getDartExecutor()
            val getDartExecutor = flutterEngineClass.getMethod("getDartExecutor")
            val dartExecutor = getDartExecutor.invoke(flutterEngine)

            // binaryMessenger = dartExecutor.getBinaryMessenger()
            val getBinaryMessenger = dartExecutorClass.getMethod("getBinaryMessenger")
            val binaryMessenger = getBinaryMessenger.invoke(dartExecutor)

            // Prepare MethodChannel and EventChannel via reflection
            val methodChannelClass = Class.forName("io.flutter.plugin.common.MethodChannel")
            val eventChannelClass = Class.forName("io.flutter.plugin.common.EventChannel")

            // Create MethodChannel(binaryMessenger, "com.storagesentinel/scan_commands")
            val methodCtor = methodChannelClass.getConstructor(binaryMessengerClass, String::class.java)
            val methodChannel = methodCtor.newInstance(binaryMessenger, "com.storagesentinel/scan_commands")

            // Set MethodCallHandler that routes to instance of scanner bridge
            val methodCallHandlerInterface = Class.forName("io.flutter.plugin.common.MethodChannel\$MethodCallHandler")

            // Create a scanner service and bridge instance (compile-time safe)
            val scannerService = com.example.storagesentinel.scanner.ScannerService()
            val scannerBridge = com.example.storagesentinel.scanner.FlutterScannerBridge(scannerService)

            val methodCallHandler = java.lang.reflect.Proxy.newProxyInstance(
                methodCallHandlerInterface.classLoader,
                arrayOf(methodCallHandlerInterface)
            ) { proxy, method, args ->
                if (method.name == "onMethodCall") {
                    try {
                        val call = args?.getOrNull(0)
                        val result = args?.getOrNull(1)
                        val methodName = call?.javaClass?.getMethod("method")?.invoke(call) as? String
                        val arguments = call?.javaClass?.getMethod("arguments")?.invoke(call)

                        // Delegate to the bridge instance
                        val nativeResp = scannerBridge.handleMethod(methodName ?: "", arguments)

                        // result.success(nativeResp) if present
                        result?.javaClass?.getMethod("success", Any::class.java)?.invoke(result, nativeResp)
                    } catch (e: Exception) {
                        Log.w(TAG, "Method handler failed: ${e.message}")
                    }
                }
                null
            }

            // methodChannel.setMethodCallHandler(methodCallHandler)
            methodChannelClass.getMethod("setMethodCallHandler", methodCallHandlerInterface).invoke(methodChannel, methodCallHandler)

            // Create EventChannel(binaryMessenger, "com.storagesentinel/scan_events")
            val eventCtor = eventChannelClass.getConstructor(binaryMessengerClass, String::class.java)
            val eventChannel = eventCtor.newInstance(binaryMessenger, "com.storagesentinel/scan_events")

            // StreamHandler interface
            val streamHandlerInterface = Class.forName("io.flutter.plugin.common.EventChannel\$StreamHandler")

            val streamHandler = java.lang.reflect.Proxy.newProxyInstance(
                streamHandlerInterface.classLoader,
                arrayOf(streamHandlerInterface)
            ) { proxy, method, args ->
                when (method.name) {
                    "onListen" -> {
                        // args[0] = arguments, args[1] = events sink
                        val eventsSink = args?.getOrNull(1)
                        val successMethod = eventsSink?.javaClass?.getMethod("success", Any::class.java)
                        val errorMethod = eventsSink?.javaClass?.getMethod("error", String::class.java, String::class.java, Any::class.java)

                        // Create a kotlin lambda that forwards bridge events to eventsSink.success
                        val sink: (Any?) -> Unit = { payload ->
                            try {
                                successMethod?.invoke(eventsSink, payload)
                            } catch (e: Exception) { Log.w(TAG, "forward to sink failed: ${e.message}") }
                        }

                        // Register the sink with the bridge so it will forward events
                        try {
                            scannerBridge.setEventSink(sink)
                        } catch (e: Exception) { Log.w(TAG, "setEventSink failed: ${e.message}") }
                    }
                    "onCancel" -> {
                        try {
                            scannerBridge.setEventSink(null)
                        } catch (e: Exception) { Log.w(TAG, "clearEventSink failed: ${e.message}") }
                    }
                }
                null
            }

            // eventChannel.setStreamHandler(streamHandler)
            eventChannelClass.getMethod("setStreamHandler", streamHandlerInterface).invoke(eventChannel, streamHandler)

            Log.i(TAG, "Flutter engine and channels registered via reflection")

        } catch (e: ClassNotFoundException) {
            Log.i(TAG, "Flutter not present on classpath; skipping Flutter engine setup")
        } catch (e: Exception) {
            Log.w(TAG, "FlutterIntegration failed: ${e.message}")
        }
    }
}
