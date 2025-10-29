Flutter UI prototype for Storage Sentinel.

This module is a minimal scaffold that connects to native code via MethodChannel and EventChannel:

- MethodChannel: `com.storagesentinel/scan_commands`
- EventChannel: `com.storagesentinel/scan_events`

To run locally (with Flutter installed):

```bash
cd flutter_app
flutter pub get
flutter run -d <device>
```

Note: The Android host must provide the platform channels (see `MIGRATION.md`).
