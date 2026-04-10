---
phase: 27
slug: device-history-fixes
status: active
nyquist_compliant: true
wave_0_complete: true
created: 2026-04-10
updated: 2026-04-10
---

# Phase 27 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + MockK + Robolectric (SDK 34) + kotlinx-coroutines-test |
| **Config file** | `app/build.gradle.kts` |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "*DeviceHistoryViewModelTest*"` |
| **Full suite command** | `./gradlew test` |
| **Build check** | `./gradlew :app:assembleDebug` (for Compose changes in Plan 27-02) |
| **Estimated runtime** | ~30s quick / ~3 min full / ~90s assemble |

---

## Sampling Rate

- **After every task commit:** Run the quick command for the touched test/module
- **After every wave:** Run full suite + spotlessCheck
- **Before `/gsd:verify-work`:** `./gradlew test spotlessCheck` green, plus manual UAT walkthrough per Plan 27-02 Task 2
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 27-01-T1 | 27-01 | 1 | DEVH-02 | unit (RED) | `./gradlew :app:testDebugUnitTest --tests "dev.notyouraverage.smscourier.viewmodels.DeviceHistoryViewModelTest"` | ✅ existing (+5 new tests added by this task) | ⬜ pending |
| 27-01-T2 | 27-01 | 1 | DEVH-02 | unit (GREEN) | `./gradlew :app:testDebugUnitTest --tests "dev.notyouraverage.smscourier.viewmodels.DeviceHistoryViewModelTest"` | ✅ existing | ⬜ pending |
| 27-02-T1 | 27-02 | 2 | DEVH-01, DEVH-02 | build + regression unit | `./gradlew :app:assembleDebug && ./gradlew :app:testDebugUnitTest --tests "*DeviceHistoryViewModelTest*" && ./gradlew spotlessCheck` | ✅ build tooling existing | ⬜ pending |
| 27-02-T2 | 27-02 | 2 | DEVH-01, DEVH-02 | manual UAT | Physical device walkthrough (6 scenarios in Plan 27-02) | manual only | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

**Nyquist continuity:** No run of 3 consecutive tasks lacks an automated verify. Task 27-02-T2 is the only manual-only task and it directly follows an automated build+test gate (27-02-T1).

---

## Wave 0 Requirements

Wave 0 gaps are handled **inline inside Plan 27-01 Task 1** rather than as a separate wave, because the test scaffold already exists and the only action is (a) delete a test stub and (b) add 5 new test cases.

- [x] **Delete test stub** at `DeviceHistoryViewModelTest.kt` lines 157–161 (`private fun DeviceHistoryViewModel.archiveDevice(device: PairedDevice)` throwing `NotImplementedError`) — assigned to Plan 27-01 Task 1.
- [x] **Add new tests** to `DeviceHistoryViewModelTest.kt` — assigned to Plan 27-01 Task 1:
  - `archiveDevice ends active session before archiving` (coVerifyOrder assertion)
  - `archiveDevice does NOT send UNPAIR SMS for APPROVED status`
  - `archiveDevice does NOT send UNPAIR SMS for PENDING_RECEIVED status`
  - `archiveDevice does NOT send UNPAIR SMS for REJECTED status`
  - `archiveDevice passes initiatedBy=LOCAL to repository`
- [x] **No new test files** required.
- [x] **No framework install** — JUnit4/MockK/Robolectric already on classpath.

Existing Phase 24 failing test (`UAT DEVH-02`) remains unchanged and serves as the primary contract assertion.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions | Gated By |
|----------|-------------|------------|-------------------|----------|
| Bottom sheet action order on real device | DEVH-02 | UI affordance verification — Compose previews don't assert order semantics in regression | Open Device History → tap active device → verify: View Sessions → Export History → Archive Device → Unpair Device | Plan 27-02 Task 2 (scenario 2) |
| Archive dialog copy (both variants) | DEVH-02 | Text content verification across session states | Archive a PENDING device (no session) AND an APPROVED device with active session; verify dialog text matches CONTEXT.md | Plan 27-02 Task 2 (scenarios 2 & 3) |
| Remote device receives NO SMS when archiving | DEVH-02 | Cross-device integration — can't assert on a physical second device from unit tests | Pair two devices, start session, archive from device A, confirm device B receives no UNPAIR notification | Plan 27-02 Task 2 (scenario 3) |
| Unpair dialog cross-references Archive | DEVH-02 | Text content verification | Open Unpair dialog in both variants; verify "To keep history without notifying, use Archive instead." sentence | Plan 27-02 Task 2 (scenario 4) |
| Removed device → SessionHistoryScreen with populated list | DEVH-01 | Integration + UX verification — the whole bug IS the navigation destination | Tap an archived device in Removed Devices section → verify lands on SessionHistoryScreen with populated session list | Plan 27-02 Task 2 (scenario 5) |

---

## Validation Sign-Off

- [x] All tasks have automated verify or explicit manual gate (27-02-T2)
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covered by Plan 27-01 Task 1 (tests added before production method)
- [x] No watch-mode flags
- [x] Feedback latency < 180s for all automated verifications
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** ready for execution
