---
phase: 27
slug: device-history-fixes
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-10
---

# Phase 27 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + Robolectric (Android unit tests) |
| **Config file** | `app/build.gradle.kts` |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "*DeviceHistoryViewModelTest*"` |
| **Full suite command** | `./gradlew test` |
| **Estimated runtime** | ~30 seconds (quick) / ~3 minutes (full) |

---

## Sampling Rate

- **After every task commit:** Run quick command for the touched test file(s)
- **After every plan wave:** Run full suite
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

> Populated by gsd-planner during plan creation. Each task must map to an automated test or a Wave 0 stub file.

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| TBD | TBD | TBD | DEVH-01 | unit | `./gradlew :app:testDebugUnitTest --tests "*DeviceHistoryViewModelTest*"` | ✅ existing | ⬜ pending |
| TBD | TBD | TBD | DEVH-02 | unit | `./gradlew :app:testDebugUnitTest --tests "*DeviceHistoryViewModelTest*"` | ✅ existing | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing Phase 24 failing tests cover both requirements:
- `app/src/test/java/dev/notyouraverage/smscourier/viewmodels/DeviceHistoryViewModelTest.kt` — tests for DEVH-01 (navigation to session history) and DEVH-02 (`archiveDevice()` method)
- Test stub (lines 157-161) throwing `NotImplementedError` must be removed as part of the real implementation task.

*No new Wave 0 stubs needed — tests already exist and fail.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Bottom sheet action order on real device | DEVH-01/02 | UI affordance verification | Long-press device in Device History → confirm order: View Sessions → Export → Archive → Unpair |
| Navigation from Removed device tap → SessionHistoryScreen renders populated list | DEVH-01 | Integration/UX verification | Tap an archived device in Device History → confirm session list is populated (not empty) |
| Archive paired active device without unpair | DEVH-02 | End-to-end UX check | Select active device → Archive → confirm it moves to Removed section with history preserved |

---

## Validation Sign-Off

- [ ] All tasks have automated verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references (N/A — existing tests)
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter after planner populates task map

**Approval:** pending
