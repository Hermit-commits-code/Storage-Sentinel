# Storage Sentinel — Flutter + Kotlin Hybrid Roadmap

Last updated: 2025-10-28

This document outlines a practical roadmap to migrate the Storage Sentinel Android app to a hybrid architecture: Flutter for UI and Kotlin for native services. It expands on the earlier roadmap and provides deliverables, contracts, milestones, risks, and next steps.

## High-level goal

- Use Flutter for all UI surfaces for faster iteration and cross-platform potential, keep heavy OS-level scanning and deletion in Kotlin (Android).
- Preserve existing native scanning, hashing, WorkManager scheduling, and PRO gating while replacing UI surfaces with Flutter screens.

## Success criteria

- Feature parity with the existing app (scan, duplicates grouping, ignore list, report export, scheduled cleaning).
- Smooth, correct selection UX (tap/long-press selection, category sync, accurate "Found vs Selected" totals).
- PRO gating enforced in both UI and native layers (no bypass possible).
- Buildable, testable project with CI that builds both Kotlin and Flutter parts reliably.

## Architecture

- Host: Android app (single-activity) that loads the Flutter module via Add-to-App.
- Flutter: UI screens, selection UX, PRO dialogs, IAP (recommended in Flutter for cross-platform support).
- Kotlin: ScannerService, hashing/duplicate detection, deletion, WorkManager scheduling, SettingsManager.

### Communication

- MethodChannel (Flutter -> Kotlin): commands like `startScan`, `stopScan`, `requestPermissions`, `deleteItems`, `getSettings`, `setSetting`, `markProPurchased`.
- EventChannel (Kotlin -> Flutter): streaming events like `scanProgress`, `scanBatch` (category + items), `scanComplete`, `cleaningProgress`, `error`.
- Payloads: JSON for quick start; protobuf recommended later for schema/versioning.

## Data model (JSON example)

- JunkItem:

```json
{
  "id": "<path or uuid>",
  "path": "/storage/emulated/0/Download/file.jpg",
  "sizeBytes": 12345,
  "label": "Duplicate Files",
  "contentHash": "hex-or-null",
  "lastModified": 169xxx,
  "isSelected": true
}
```

- JunkType:

```json
{ "key": "duplicate_files", "label": "Duplicate Files", "locked": false }
```

## Milestones (M0..M5)

- M0 (Scaffold) — 1 week
  - Create Flutter module: `flutter create -t module flutter_ui`
  - Add module to Android project and wire a basic MethodChannel/EventChannel with mock events.
  - Deliverable: host app shows Flutter screen and receives mock scan events.

- M1 (Core scanning + Results UI) — 2–3 weeks
  - Native: expose `startScan` and stream batches via EventChannel.
  - Flutter: implement Results UI, category cards, totals, and selection model.
  - Deliverable: scan -> results -> selection flows.

- M2 (Duplicates UI + PRO gating) — 2 weeks
  - Flutter DuplicateFilesScreen, group UI, per-file checkboxes.
  - UI-level PRO gating and native-side enforcement before deletion.

- M3 (Settings, Ignore list, Scheduling, Export) — 2 weeks
  - Flutter Settings UI; native methods to persist settings and update WorkManager.
  - Report export method from native with proper permissions.

- M4 (IAP, telemetry, polish) — 2–3 weeks
  - Implement IAP (Flutter `in_app_purchase` recommended), analytics, accessibility improvements.

- M5 (Tests & CI) — 1–2 weeks
  - Unit tests for Kotlin logic and Dart UI, integration tests, and a GitHub Actions workflow to build both parts.

## UX rules & enforcement

- Interaction model:
  - Tap category card → open details.
  - Long-press category card → toggle category selection.
  - File detail: tap to preview; checkbox toggles selection.
- Totals: UI always shows "Found" (sum of items) and "Selected" (sum of current selection) updated on every change.
- PRO enforcement:
  - Flutter disables UI actions and shows a Pro dialog for locked categories.
  - Kotlin double-checks `isProUser` before destructive operations and returns explicit errors if blocked.

## Testing strategy

- Kotlin unit tests: hashing, duplicate grouping, preselection logic, WorkManager scheduling.
- Dart widget tests: ResultsDisplay, DuplicateFilesScreen, selection sync.
- Integration tests: end-to-end mock scans and cleaning flows.
- CI: matrix on GitHub Actions to build Kotlin and Flutter, run tests and produce artifacts.

## Security & privacy

- Never upload file paths/content to remote analytics.
- Consider `EncryptedSharedPreferences` for sensitive settings.
- Require explicit consent for writing reports to public directories and document the export path.
- For monetization, consider server-side receipt validation for increased fraud resistance.

## Migration plan (practical steps)

1. Prepare Kotlin API: extract a minimal `ScannerApi` interface with `startScan()`, `stopScan()`, `getSettings()`, `deleteItems()`.
2. Scaffold Flutter module and add-platform-channels with stubbed responses.
3. Replace Results UI with Flutter using mock data.
4. Wire real scan events from Kotlin to Flutter; iterate on selection UX.
5. Implement DuplicateFilesScreen + PRO gating.
6. Implement Settings syncing and WorkManager connections.
7. Add IAP, analytics, tests, and CI.

## CI & release notes

- CI should run both `./gradlew assembleDebug` (Android) and `flutter analyze && flutter test` (Dart).
- For release, use `flutter build apk` (if Flutter UI contains assets) then standard Android bundle signing.

## Risks & mitigations

- Tooling mismatches (Kotlin 2.x + Compose lint): pin versions or use compatible toolchain; add lint baselines when necessary.
- Large result payloads over channels: stream batches; avoid huge in-memory arrays.
- Selection sync mismatches: authoritative native checks on destructive actions.

## Next action checklist (concrete tasks)

- [ ] Create Flutter module and add it to the Android project (M0).
- [ ] Define MethodChannel & EventChannel method names and JSON schemas (quick draft).
- [ ] Implement Kotlin `ScannerApi` stubs that call `EventChannel` with mocked scan batches.
- [ ] Implement minimal Flutter ResultsDisplay to render streamed scan batches.
- [ ] Iterate on selection UX and PRO gating and add native enforcement.

---
If you want I can now:

- A: scaffold the Flutter module in-repo and add a minimal MethodChannel & EventChannel with mock events (I can patch the repo), or
- B: produce the exact MethodChannel JSON schema and a Kotlin `ScannerApi.kt` interface + stub implementation, or
- C: create a GitHub Actions CI draft that builds both Kotlin and Flutter parts.

Tell me A, B, or C and I will proceed and apply the changes to the repository.
