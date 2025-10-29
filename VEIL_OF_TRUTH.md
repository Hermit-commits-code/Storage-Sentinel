# VEIL OF TRUTH — Storage Sentinel (Canonical Plan)

This file is the single source of truth combining the prior roadmap and improvement notes.
It defines the canonical plan for rebuilding the app as a pure Android native application using AndroidX, Kotlin, Hilt, WorkManager, Room and Jetpack Compose.
All work from this point will follow the rules and milestones in this document.

## High-level goal

- Build a robust, maintainable Android native app in Kotlin using AndroidX and Jetpack libraries.
- Use Jetpack Compose for all UI surfaces. Keep heavy OS-level scanning, hashing, deletion, scheduling and enforcement in Kotlin native modules.
- Enforce PRO gating on the native side (server of truth) before any destructive operations.

## Success criteria

- Feature parity with previous releases for scanning, duplicates, ignore list, export, and scheduling.
- PRO gating enforced by native code before destructive operations.
- Deterministic, auditable deletion with reliable reporting and an undo/quarantine path where feasible.

## Canonical Milestones

M0 — Scaffold & contracts (1 week)

- Create a clean Android Studio project branch `kotlin-rebuild` with Gradle, app module, and Compose/Hilt/WorkManager setup.
- Define `ScannerApi` and data models (`JunkItem`, `JunkType`) and a stub `ScannerService` that streams mocked batches via `kotlinx.coroutines.Flow`.

M1 — Core scanning + Results UI (2–3 weeks)

- Implement `ScannerService` real scanning skeleton: traversal, streaming batches, hashing (coroutine-based, backpressure-friendly).
- Implement Compose-based Results UI, category cards, totals, and selection models backed by `ViewModel` + `StateFlow`.

M2 — Duplicates UI + PRO enforcement (2 weeks)

- Implement grouping and DuplicateFilesScreen, selection checkboxes, multi-select flows.
- Implement native PRO enforcement in `ScannerService`/ViewModel: before any deletion, verify entitlement (Play Billing and local entitlement state; optional server-side validation later).

M3 — Settings, Ignore list, Scheduling, Export (2 weeks)

- Settings screen (Compose) persisted in Room/SharedPreferences via a `SettingsRepository` injected by Hilt.
- Use WorkManager for scheduled scans/auto-cleaning; ensure OEM battery optimizations handled gracefully.
- Implement export/report generation (file writing with permissions) and an export UI.

M4 — Billing, telemetry, accessibility & polish (2–3 weeks)

- Integrate Play Billing (official libraries), entitlement storage, restore purchases flow.
- Add analytics and crash reporting as privacy-respecting opt-in options.

M5 — Tests & CI (1–2 weeks)

- Unit tests for Kotlin scanner logic, Room interactions, ViewModel behavior.
- Compose UI tests (robot tests) and integration smoke tests.
- CI: GitHub Actions to build the Android app, run unit tests, and run a subset of UI tests on emulators or Firebase Test Lab.

## Priorities (gold standard)

1. Correctness & paywall enforcement (native double-checks destructive ops).
2. Selection UX & totals correctness.
3. Fast, streaming scanner (coroutines, backpressure, Room cache).
4. Safe delete (quarantine/preview + logs + undo where possible).

## Internal communication and architecture

- The app will be a single Android application with modular packages. Inter-module communication will use Kotlin interfaces, coroutines, Flow/SharedFlow, and Room for persistence.
- Expose a small well-documented `ScannerApi` interface that higher-level components (UI/ViewModels) use. Scanner events are emitted via Kotlin `Flow` or `SharedFlow` to support streaming batches and backpressure.
- Payload shapes will be Kotlin data classes (e.g., `JunkItem`, `JunkType`) serialized to JSON only when exporting or logging. For internal IPC, use typed classes and Flows.

## Milestones (detailed)

M0 — Scaffold & contracts (1 week)

- Create a clean Android Studio project branch `kotlin-rebuild` with Gradle, app module, and Compose/Hilt/WorkManager setup.
- Define `ScannerApi` and data models (`JunkItem`, `JunkType`) and a stub `ScannerService` that streams mocked batches via `kotlinx.coroutines.Flow`.
- Deliverable: a buildable app that shows a Compose placeholder screen and consumes `ScannerService` mock streams.

M1 — Core scanning + Results UI (2–3 weeks)

- Implement `ScannerService` real scanning skeleton: traversal, streaming batches, hashing (coroutine-based, backpressure-friendly).
- Implement Compose-based Results UI, category cards, totals, and selection models backed by `ViewModel` + `StateFlow`.
- Deliverable: startScan -> stream batches -> UI displays categories and totals.

M2 — Duplicates UI + PRO enforcement (2 weeks)

- Implement grouping and DuplicateFilesScreen, selection checkboxes, multi-select flows.
- Implement native PRO enforcement in `ScannerService`/ViewModel: before any deletion, verify entitlement (local Play Billing state + optional server validation).

M3 — Settings, Ignore list, Scheduling, Export (2 weeks)

- Settings screen (Compose) persisted in Room/SharedPreferences via a `SettingsRepository` injected by Hilt.
- Use WorkManager for scheduled scans/auto-cleaning; ensure OEM battery optimizations handled gracefully.
- Implement export/report generation (file writing with permissions) and an export UI.

M4 — Billing, telemetry, accessibility & polish (2–3 weeks)

- Integrate Play Billing (official libraries), entitlement storage, restore purchases flow.
- Add analytics and crash reporting as privacy-respecting opt-in options.
- Accessibility and UI polish across form factors.

M5 — Tests & CI (1–2 weeks)

- Unit tests for Kotlin scanner logic, Room interactions, ViewModel behavior.
- Compose UI tests (robot tests) and integration smoke tests.
- CI: GitHub Actions to build the Android app, run unit tests, and run a subset of UI tests on emulators or Firebase Test Lab.

## UX rules & enforcement

- Interaction model:
 	- Tap category card → open details.
 	- Long-press category card → toggle category selection (or use explicit multi-select controls).
 	- File detail: tap to preview; checkbox toggles selection.
- Totals: UI must always show "Found" (sum of discovered items) and "Selected" (current selection) and update atomically.
- PRO enforcement:
 	- UI will visually gate features; native code (scanner/service/ViewModel) is the final authority and will reject destructive actions if not entitled.

## Testing strategy

- Kotlin unit tests: hashing, duplicate grouping, selection logic, WorkManager scheduling.
- Compose UI tests: unit + screenshot/assertion tests for important screens.
- Integration/smoke tests: end-to-end mock scans and cleaning flows on emulator.
- CI: GitHub Actions to build, run unit tests, and optionally run emulator-based UI smoke tests or upload to Firebase Test Lab.

## Migration / Rebuild plan (practical steps)

1. Create `ScannerApi` interface and typed data models; design event streaming via `Flow`.
2. Scaffold the Android-native Compose app skeleton (Hilt + WorkManager + Room).
3. Implement `ScannerService` stub that emits batches via `Flow` and wire it to a `ScannerViewModel`.
4. Implement Compose Results screen to consume ViewModel state and demonstrate selection flows.
5. Replace stub `ScannerService` with real scanning logic (file traversal, hashing, Room caching).
6. Implement deletion workflows with native PRO enforcement, safe-delete, report export.
7. Add tests and CI.

## Next immediate actions (concrete tasks)

- [ ] Create branch `kotlin-rebuild` from `kotlin-rebuild-backup` and scaffold a clean Android Studio project skeleton with Compose + Hilt + WorkManager.
- [ ] Add `ScannerApi`, data models, and a stub `ScannerService` that emits `Flow` batches. Verify build.
- [ ] Implement Compose Results UI and `ScannerViewModel` backed by Hilt-injected services. Verify build + manual smoke test.

---

Keep this file updated — it is the ground truth for the rebuild.
