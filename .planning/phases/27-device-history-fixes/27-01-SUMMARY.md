---
phase: 27-device-history-fixes
plan: 01
subsystem: ui
tags: [android, kotlin, viewmodel, mockk, junit, tdd]

requires:
  - phase: 24-uat-failing-tests
    provides: Phase 24 UAT DEVH-02 failing test encoding the archiveDevice contract
provides:
  - DeviceHistoryViewModel.archiveDevice(device) public member method
  - Local-only device archival flow without remote UNPAIR SMS
  - Unit tests covering session-end ordering and no-SMS invariant across all pairing statuses
affects: [archive-management, device-history, phase-28]

tech-stack:
  added: []
  patterns:
    - Local-only archive flow (ends active session with stoppedBy=ARCHIVE, then archives with initiatedBy=USER)
    - Stub-delete TDD transition (test stub extension removed, method resolves to real member)

key-files:
  created: []
  modified:
    - app/src/main/java/dev/notyouraverage/smscourier/viewmodels/DeviceHistoryViewModel.kt
    - app/src/test/java/dev/notyouraverage/smscourier/viewmodels/DeviceHistoryViewModelTest.kt

key-decisions:
  - "Used initiatedBy=USER (not LOCAL) to satisfy Phase 24 UAT DEVH-02 locked assertion and match unpairDevice convention"
  - "Used stoppedBy=ARCHIVE to distinguish archive-initiated session termination from UNPAIR in session history"
  - "archiveDevice never calls smsSender.sendUnpair, independent of pairing status (APPROVED/PENDING_RECEIVED/REJECTED)"

patterns-established:
  - "Local archival vs unpair: archiveDevice mirrors unpairDevice minus the SMS step; both use initiatedBy=USER"
  - "No Parameterized runner in Robolectric tests: duplicate per-status tests instead"

requirements-completed: [DEVH-02]

duration: ~5min
completed: 2026-04-10
---

# Phase 27 Plan 01: DEVH-02 archiveDevice Implementation Summary

**Added a real `archiveDevice(device)` member to `DeviceHistoryViewModel` that ends active sessions locally and archives without sending an UNPAIR SMS, making the Phase 24 UAT DEVH-02 test pass untouched.**

## Performance

- **Duration:** ~5 min
- **Completed:** 2026-04-10
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Closes DEVH-02: users can retire a device locally without notifying the remote side
- Phase 24 UAT DEVH-02 test transitions FAIL -> PASS with zero modifications to the test body
- Deleted the private test-file stub extension that was shadowing the (missing) real method
- Added 4 new unit tests covering ordering and no-SMS invariants across pairing statuses

## Task Commits

1. **Task 1: Delete test stub and add new DEVH-02 test cases** - `1b66325` (test)
2. **Task 2: Implement archiveDevice() in DeviceHistoryViewModel** - `08f6918` (feat)

## Files Created/Modified

- `app/src/main/java/dev/notyouraverage/smscourier/viewmodels/DeviceHistoryViewModel.kt` - Added public `archiveDevice(device)` member function
- `app/src/test/java/dev/notyouraverage/smscourier/viewmodels/DeviceHistoryViewModelTest.kt` - Removed stub extension, added 4 tests, removed unused `PairedDevice` import

## Implementation

```kotlin
/**
 * Archives a device locally without sending an UNPAIR SMS to the remote device.
 * - Ends any active session for the device (local only, no SMS).
 * - Archives the device so it appears in the Removed Devices section.
 * - The remote device is NOT notified; the pairing remains valid on their side.
 *
 * Use [unpairDevice] if you want to notify the other device.
 */
fun archiveDevice(device: PairedDevice) {
    viewModelScope.launch {
        // End any active session locally (idempotent -- no-op if none)
        sessionRepository.endSessionForDevice(device.phoneNumber, "ARCHIVE")

        // Archive the device. initiatedBy = "USER" matches the existing unpairDevice()
        // convention AND the Phase 24 UAT DEVH-02 test's locked assertion.
        deviceRepository.archiveDevice(
            phoneNumber = device.phoneNumber,
            role = device.role,
            initiatedBy = "USER",
        )
    }
}
```

## Stub Removal

Removed lines 157-161 of `DeviceHistoryViewModelTest.kt` (pre-Phase-27 numbering):

```kotlin
private fun DeviceHistoryViewModel.archiveDevice(device: PairedDevice) {
    throw NotImplementedError("DEVH-02: archiveDevice not yet implemented on DeviceHistoryViewModel")
}
```

The unused `import ...data.entities.PairedDevice` line was also removed (Rule 3 - blocking: unused-import would have failed Kotlin compile warnings-as-errors on some configs; harmless cleanup here).

## Tests Added

1. `archiveDevice ends active session before archiving` - uses `coVerifyOrder` to assert `sessionRepository.endSessionForDevice(phone, "ARCHIVE")` is called before `deviceRepository.archiveDevice(...)`
2. `archiveDevice does NOT send UNPAIR SMS for APPROVED status` - `coVerify(exactly = 0) { smsSender.sendUnpair(any(), any()) }`
3. `archiveDevice does NOT send UNPAIR SMS for PENDING_RECEIVED status` - same assertion, different pairing status
4. `archiveDevice does NOT send UNPAIR SMS for REJECTED status` - same assertion, different pairing status

**Phase 24 UAT DEVH-02 test: UNCHANGED.** Lines 120-154 (now 120-154 in the updated file) are byte-identical. Git diff confirms only deletions below line 155 and the additions at top for `coVerifyOrder` import.

## Test Run Output

```
./gradlew :app:testDebugUnitTest --tests "dev.notyouraverage.smscourier.viewmodels.DeviceHistoryViewModelTest"
BUILD SUCCESSFUL
```

All DeviceHistoryViewModelTest cases pass including:
- 3 pre-existing `unpairDevice` tests (no regressions)
- Phase 24 `UAT DEVH-02` test (now PASSING without modification)
- 4 new `archiveDevice` tests

Spotless check also green.

## Decisions Made

- **`initiatedBy = "USER"` (not `"LOCAL"`):** The Phase 24 UAT DEVH-02 test explicitly asserts `initiatedBy = "USER"` and Phase 27 Success Criterion 4 requires that test to pass WITHOUT modification. Also matches existing `unpairDevice()` convention.
- **`stoppedBy = "ARCHIVE"`:** Distinguishes archive-initiated termination from UNPAIR in session history audit trail.
- **No status check:** Archive works uniformly for PENDING/REJECTED/APPROVED, unlike unpair which gates SMS on APPROVED.

## Deviations from Plan

None - plan executed exactly as written. (One minor cleanup: removed an unused `PairedDevice` import orphaned by stub deletion. Strictly required for clean compile.)

## Issues Encountered

None. TDD cycle ran cleanly: Task 1 produced expected compile failure (stub removed, real method not yet present), Task 2 added the real method and all tests turned green on first run.

## Follow-up TODO

**Audit `initiatedBy` string convention across `unpairDevice`/`archiveDevice` vs `ArchiveManagementViewModel.getRemovalReason()`** - Research Open Question 3 flagged that `getRemovalReason()` may expect `LOCAL`/`REMOTE` sentinel values while both `unpairDevice` and `archiveDevice` pass `"USER"`. This may cause the UI to fall through to the "Removal reason unknown" branch for all user-initiated removals. Out of scope for Phase 27 (it's a latent pre-existing issue affecting `unpairDevice` equally). Candidate for a small follow-up phase.

## Next Phase Readiness

- Plan 27-02 (DEVH-01 archived device history visibility) can proceed independently
- No blockers

## Self-Check: PASSED

- FOUND: app/src/main/java/dev/notyouraverage/smscourier/viewmodels/DeviceHistoryViewModel.kt (contains `fun archiveDevice(`)
- FOUND: app/src/test/java/dev/notyouraverage/smscourier/viewmodels/DeviceHistoryViewModelTest.kt (stub removed, 4 new tests present)
- FOUND commit: 1b66325 (test stub removal + new tests)
- FOUND commit: 08f6918 (archiveDevice implementation)
- Phase 24 UAT DEVH-02 test body UNCHANGED (verified by diff)

---
*Phase: 27-device-history-fixes*
*Completed: 2026-04-10*
