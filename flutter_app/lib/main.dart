import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Storage Sentinel (Flutter UI)',
      theme: ThemeData(primarySwatch: Colors.blue),
      home: const ScannerHome(),
    );
  }
}

class ScannerHome extends StatefulWidget {
  const ScannerHome({super.key});

  @override
  State<ScannerHome> createState() => _ScannerHomeState();
}

class _ScannerHomeState extends State<ScannerHome> {
  static const methodChannel =
      MethodChannel('com.storagesentinel/scan_commands');
  static const eventChannel = EventChannel('com.storagesentinel/scan_events');

  final List<Map<String, dynamic>> _events = [];
  StreamSubscription? _eventsSub;

  @override
  void initState() {
    super.initState();
    // Try to listen to platform EventChannel; if no events arrive within a short
    // timeout we fall back to a local mock stream so the Flutter UI can be
    // developed independently of the Android host.
    bool gotFirstEvent = false;
    final timeout = Duration(seconds: 2);
    _eventsSub = eventChannel.receiveBroadcastStream().listen((e) {
      gotFirstEvent = true;
      _onEvent(e);
    }, onError: _onError);

    // If no platform events are received within timeout, start local mock emitter.
    Future.delayed(timeout, () {
      if (!gotFirstEvent) {
        _eventsSub?.cancel();
        _eventsSub = _mockEventStream().listen(_onEvent);
      }
    });
  }

  void _onEvent(dynamic e) {
    setState(() {
      try {
        final m = e is String
            ? jsonDecode(e) as Map<String, dynamic>
            : Map<String, dynamic>.from(e);
        _events.insert(0, m);
      } catch (err) {
        _events.insert(0, {'type': 'raw', 'payload': e.toString()});
      }
    });
  }

  void _onError(Object error) {
    setState(() {
      _events.insert(0, {'type': 'error', 'message': error.toString()});
    });
  }

  Future<void> _startScan() async {
    try {
      final resp = await methodChannel.invokeMethod('startScan',
          {'scanId': DateTime.now().millisecondsSinceEpoch.toString()});
      debugPrint('startScan resp: $resp');
    } catch (e) {
      debugPrint('startScan error: $e');
    }
  }

  Future<void> _stopScan() async {
    try {
      final resp = await methodChannel.invokeMethod('stopScan');
      debugPrint('stopScan resp: $resp');
    } catch (e) {
      debugPrint('stopScan error: $e');
    }
  }

  @override
  void dispose() {
    _eventsSub?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Scanner (Prototype)')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12.0),
            child: Row(
              children: [
                ElevatedButton(
                    onPressed: _startScan, child: const Text('Start Scan')),
                const SizedBox(width: 12),
                ElevatedButton(
                    onPressed: _stopScan, child: const Text('Stop Scan')),
              ],
            ),
          ),
          Expanded(
            child: ListView.builder(
              itemCount: _events.length,
              itemBuilder: (context, idx) {
                final e = _events[idx];
                return ListTile(
                  title: Text(e['type']?.toString() ?? 'event'),
                  subtitle: Text(e.containsKey('message')
                      ? e['message']
                      : (e['category']?.toString() ?? e.toString())),
                );
              },
            ),
          )
        ],
      ),
    );
  }
}

// Local mock event stream that mirrors the JSON shape emitted by the Kotlin stub.
Stream<Map<String, dynamic>> _mockEventStream() async* {
  int batch = 0;
  while (batch < 5) {
    final items = List.generate(
        3,
        (i) => {
              'id': 'item-${batch}-$i',
              'path': '/storage/emulated/0/Download/file_${batch}_$i.jpg',
              'sizeBytes': (i + 1) * 1024
            });
    yield {'type': 'scanBatch', 'batch': batch, 'items': items};
    yield {
      'type': 'progress',
      'scanId': 'local',
      'percent': ((batch + 1) * 20).clamp(0, 100)
    };
    await Future.delayed(Duration(seconds: 1));
    batch++;
  }
  yield {'type': 'scanComplete', 'totalFound': 5};
}
