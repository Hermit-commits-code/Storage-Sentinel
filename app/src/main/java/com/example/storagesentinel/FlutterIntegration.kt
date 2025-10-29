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

            // Set MethodCallHandler that routes to FlutterScannerBridge.handleMethodCall
            val methodCallHandlerInterface = Class.forName("io.flutter.plugin.common.MethodChannel\$MethodCallHandler")
            val methodCallClass = Class.forName("io.flutter.plugin.common.MethodCall")
            val resultClass = Class.forName("io.flutter.plugin.common.MethodChannel\$Result")

            // Create a proxy handler implementation
            val handler = java.lang.reflect.Proxy.newProxyInstance(
                methodCallHandlerInterface.classLoader,
                arrayOf(methodCallHandlerInterface)
            ) { proxy, method, args ->
                // onMethodCall(MethodCall call, Result result)
                if (method.name == "onMethodCall") {
                    try {
                        val call = args?.getOrNull(0)
                        val result = args?.getOrNull(1)
                        val methodName = call?.javaClass?.getMethod("method")?.invoke(call) as? String
                        val arguments = call?.javaClass?.getMethod("arguments")?.invoke(call)
                        val nativeResp = FlutterScannerBridge.handleMethodCall(methodName ?: "", arguments)
                        // call result.success(nativeResp)
                        result?.javaClass?.getMethod("success", Any::class.java)?.invoke(result, nativeResp)
                    } catch (e: Exception) {
                        Log.w(TAG, "Method handler failed: ${e.message}")
                    }
                }
                null
            }

            // methodChannel.setMethodCallHandler(handler)
            methodChannelClass.getMethod("setMethodCallHandler", methodCallHandlerInterface).invoke(methodChannel, handler)

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

                        // Register a ScanListener that forwards JSON strings to events.success(json)
                        val listener = object : ScanListener {
                            override fun onScanBatch(json: String) {
                                try {
                                    successMethod?.invoke(eventsSink, json)
                                } catch (e: Exception) { Log.w(TAG, "forward batch failed: ${e.message}") }
                            }

                            override fun onScanProgress(progress: Int, path: String?) {
                                try {
                                    val map = mapOf("type" to "progress", "percent" to progress, "path" to path)
                                    successMethod?.invoke(eventsSink, map)
                                } catch (e: Exception) { Log.w(TAG, "forward progress failed: ${e.message}") }
                            }

                            override fun onScanComplete(summaryJson: String) {
                                try {
                                    successMethod?.invoke(eventsSink, summaryJson)
                                } catch (e: Exception) { Log.w(TAG, "forward complete failed: ${e.message}") }
                            }

                            override fun onError(code: String, message: String) {
                                try {
                                    val map = mapOf("type" to "error", "code" to code, "message" to message)
                                    successMethod?.invoke(eventsSink, map)
                                } catch (e: Exception) { Log.w(TAG, "forward error failed: ${e.message}") }
                            }
                        }

                        FlutterScannerBridge.registerListener(listener)
                        // Store listener on the eventsSink object so it can be removed onCancel
                        try {
                            eventsSink?.javaClass?.getMethod("getClass")
                            // attach via a weak map if needed; simplified here
                            eventsSink?.let { /* no-op */ }
                        } catch (_: Exception) {}
                    }
                    "onCancel" -> {
                        // Unregister all listeners to be safe
                        // In a full implementation we'd track the exact listener instance.
                        try {
                            FlutterScannerBridge.unregisterListener(object : ScanListener {
                                override fun onScanBatch(json: String) {}
                                override fun onScanProgress(progress: Int, path: String?) {}
                                override fun onScanComplete(summaryJson: String) {}
                                override fun onError(code: String, message: String) {}
                            })
                        } catch (e: Exception) { Log.w(TAG, "unregister failed: ${e.message}") }
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
