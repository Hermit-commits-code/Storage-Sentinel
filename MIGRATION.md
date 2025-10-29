# Migration Plan and Platform Channel Contract

This document defines the migration approach (Flutter UI + native Kotlin services), the platform-channel contract between Flutter and Android, data shapes for exchange, and acceptance criteria for the integration.

## Overview

- Host: Android `app/` module remains the host that starts the Flutter engine or attaches Flutter activity.
- UI: Full Flutter frontend located in `flutter_app/`.
- Native services: Kotlin scanning, deletion, and background workers remain in a small `scanner` service with a minimal API.
- Communication: Flutter <-> Kotlin via MethodChannel (commands) and EventChannel (streaming scan events).

## Channels

- MethodChannel (commands)
  - Name: `com.storagesentinel/scan_commands`
  - Purpose: RPC-style calls from Flutter to native to control scanning and destructive operations.

- EventChannel (events)
  - Name: `com.storagesentinel/scan_events`
  - Purpose: Stream scan progress and batch results from native to Flutter.

## JSON Contract

- All messages should be JSON objects. Fields not present are treated as null/optional.

### JunkItem

{
  "id": "string",            // stable id (path or generated UUID)
  "path": "string",          // absolute or content URI (string)
  "sizeBytes": 12345,         // numeric
  "contentHash": "sha256...", // content hash for dedupe
  "thumbnail": "base64?url?", // optional small preview
  "isSelected": false         // bool, selection state (Flutter may override)
}

### JunkType (category)

{
  "id": "string",           // e.g. "duplicates", "large_files"
  "label": "string",        // human friendly label
  "description": "string"   // optional
}

### MethodChannel RPCs (Flutter -> Native)

- startScan
  - method: `startScan`
  - args: { "scanId": "string", "includeTypes": ["string"], "excludePaths": ["string"] }
  - response: { "scanId": "string", "status": "started" }

- stopScan
  - method: `stopScan`
  - args: { "scanId": "string" }
  - response: { "scanId": "string", "status": "stopped" }

- deleteItems
  - method: `deleteItems`
  - args: { "items": [ { "id": "string", "path": "string" } ], "requirePro": true|false }
  - response: { "deleted": ["id"], "failed": [ {"id":"", "reason":""} ] }

- getScanStatus
  - method: `getScanStatus`
  - args: { "scanId": "string" }
  - response: { "scanId":"", "status":"running|idle|failed|completed", "progress": 0..100 }

- setProEntitlement
  - method: `setProEntitlement`
  - args: { "isPro": true|false }
  - response: { "ok": true }

### EventChannel messages (Native -> Flutter stream)

- progress
  - { "type": "progress", "scanId": "", "percent": 42, "message": "Scanning..." }

- batch
  - { "type": "batch", "scanId": "", "category": {"id":"", "label":""}, "items": [<JunkItem>], "batchComplete": true|false }

- error
  - { "type": "error", "scanId": "", "code": "string", "message": "string" }

## Error codes (examples)

- `PERMISSION_DENIED` -- missing runtime permission(s)
- `IO_ERROR` -- file IO failure
- `SCAN_FAILED` -- scanner internal error
- `DELETE_FAILED` -- deletion failed for some items

## Backpressure & Large Payloads

- For large result sets, native should send multiple `batch` events (pagination) and set `batchComplete` true on last batch.
- Flutter should be prepared to de-duplicate items by `contentHash`.

## Security & PRO gating

- UI gating is enforced client-side (Flutter) for UX, but native must double-check entitlement before performing destructive operations such as deletion.
- For Play Billing, validate receipts on server when possible; for local checks, persist entitlement securely and verify before deletes.

## Acceptance Criteria

1. Flutter can start a scan and receives `progress` and `batch` events in near real-time.
2. Flutter can request deletion; native performs entitlement check and returns per-item results.
3. Large batch pagination works: Flutter receives multiple `batch` messages and aggregates them without duplication.
4. Errors are surfaced via `error` event and appropriate UI is shown.
5. End-to-end smoke test: Start scan -> Receive at least one batch -> Select items -> Delete -> Confirm deleted items are removed.

## Next Steps

1. Implement the Flutter module scaffold and hook up a simple `ScannerStub` on the Kotlin side that emits sample events.
2. Implement the cleaned native `Scanner` module and wire it to real scanning logic.
3. Iterate Flutter UI and tests, then remove legacy Compose UI from compilation and finalise cleanup branch.
