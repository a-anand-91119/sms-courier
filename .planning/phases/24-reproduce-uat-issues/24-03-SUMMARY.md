---
phase: 24-reproduce-uat-issues
plan: 03
subsystem: testing
tags: [junit, mockk, robolectric, tdd, unit-tests]

# Dependency graph
requires:
  - phase: 24-01
    provides: UAT test infrastructure (@Category(UATTest::class), ./gradlew uatTest task)
provides:
  - SESS-01 failing test: TARGET devices with active sessions are invisible
  - SESS-02 failing test: stopForwarding uses wrong DeviceRole for encryption key
  - NOTF-01 failing test: notification approve action doesn't trigger approval
affects: [26-session-bidirectional-support, 28-notification-actions]

# Tech tracking
tech-stack:
  added: []
  patterns: [robolectric-receiver-test-pattern, mockk-verify-pattern]

key-files:
  created:
    - app/src/test/java/dev/notyouraverage/smscourier/receivers/PairingActionReceiverTest.kt
  modified:
    - app/src/test/java/dev/notyouraverage/smscourier/viewmodels/ForwardingControlViewModelTest.kt

key-decisions:
  - "SESS-01 test verifies ViewModel queries TARGET devices (not just SOURCE)"
  - "SESS-02 test verifies correct DeviceRole passed to updateEncryptionKey"
  - "NOTF-01 test verifies service started (not activity) for approve action"

patterns-established:
  - "Robolectric receiver test pattern: RuntimeEnvironment.getApplication() + shadowOf() for service verification"
  - "MockK verify pattern: verify { repository.method(expectedRole) } for role-based calls"

# Metrics
duration: 5min
completed: 2026-02-16
---

# Phase 24 Plan 03: Session & Notification Tests Summary

**Three failing UAT tests proving SESS-01 (session visibility), SESS-02 (wrong role in stopForwarding), and NOTF-01 (notification approve broken) bugs exist**

## Performance

- **Duration:** 5 min
- **Started:** 2026-02-16T12:12:29Z
- **Completed:** 2026-02-16T12:17:34Z
- **Tasks:** 2
- **Files modified:** 2 (1 created, 1 modified)

## Accomplishments
- SESS-01 test proves TARGET devices with active sessions are not queried by ForwardingControlViewModel
- SESS-02 test proves stopForwarding hardcodes DeviceRole.SOURCE instead of using actual device role
- NOTF-01 test proves notification approve action starts Activity (not Service) and doesn't trigger approval
- All three tests are @Category(UATTest::class) and discoverable via `./gradlew uatTest`

## Task Commits

Each task was committed atomically:

1. **Task 1: Add SESS-01 and SESS-02 failing tests** - `c6eed38` (test)
2. **Task 2: Add NOTF-01 failing test** - `fb0509a` (test)

## Files Created/Modified
- `app/src/test/java/dev/notyouraverage/smscourier/receivers/PairingActionReceiverTest.kt` - New test file for NOTF-01 (notification approve action)
- `app/src/test/java/dev/notyouraverage/smscourier/viewmodels/ForwardingControlViewModelTest.kt` - Added SESS-01 and SESS-02 tests

## Decisions Made

**SESS-01 test approach:**
- Verify ViewModel constructor calls `getDevicesByRoleAndStatus(DeviceRole.TARGET, PairingStatus.APPROVED)`
- This proves the bug: ViewModel only queries SOURCE devices (line 34-36 of ForwardingControlViewModel.kt)
- Test FAILS because verify { ... TARGET ... } doesn't match actual call with SOURCE

**SESS-02 test approach:**
- Mock updateEncryptionKey and verify it's called with DeviceRole.TARGET
- This proves the bug: stopForwarding hardcodes DeviceRole.SOURCE (line 107 of ForwardingControlViewModel.kt)
- Test FAILS because coVerify expects TARGET but SOURCE was passed

**NOTF-01 test approach:**
- Use Robolectric's shadowOf().nextStartedService to verify Service was started (not Activity)
- This proves the bug: approve action starts MainActivity (line 38-43 of PairingActionReceiver.kt) instead of calling MasterService like reject does
- Test FAILS because nextStartedService returns null (no service was started)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

**Initial Robolectric import issue:** First version used `androidx.test.core.app.ApplicationProvider` which isn't available in test classpath. Fixed by switching to `RuntimeEnvironment.getApplication()` pattern from SmsReceiverTest.kt (existing pattern in codebase).

## Next Phase Readiness

**Phase 26 (Session Bidirectional Support):**
- SESS-01 and SESS-02 tests ready
- Tests will pass after ForwardingControlViewModel queries both SOURCE and TARGET devices
- Tests will pass after stopForwarding determines actual device role before calling updateEncryptionKey

**Phase 28 (Notification Actions):**
- NOTF-01 test ready
- Test will pass after PairingActionReceiver.ACTION_APPROVE sends to MasterService.PAIRING_APPROVE_REQUESTED (like reject does)

**UAT test suite:**
- 6 failing UAT tests total (HOME-01, SESS-01/02/03, DEVH-02, NOTF-01)
- All discoverable via `./gradlew uatTest`
- All will pass after fixes in Phases 26, 27, 28

---
*Phase: 24-reproduce-uat-issues*
*Completed: 2026-02-16*
