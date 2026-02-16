---
phase: 25-home-screen-fixes
plan: 01
subsystem: ui
tags: [kotlin, jetpack-compose, viewmodel, direction-enum, android]

# Dependency graph
requires:
  - phase: 24-reproduce-uat-issues
    provides: UAT HOME-01 failing test for TARGET device direction
provides:
  - Corrected Direction enum semantic mapping (TARGET -> FORWARDING_TO, SOURCE -> RECEIVING_FROM)
  - Fixed HomeViewModel.calculateDirectionalStatus() direction logic
  - Fixed PairedDevicesViewModel.getDirectionForDevice() direction logic
  - Updated DirectionalStatusTest helper and all assertions to match new mapping
affects:
  - 25-02 (UI labels and terminology)
  - home-screen (direction indicators)
  - paired-devices-screen (direction arrows)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Direction enum semantic mapping: TARGET=FORWARDING_TO, SOURCE=RECEIVING_FROM"

key-files:
  created: []
  modified:
    - "app/src/main/java/dev/notyouraverage/smscourier/data/Direction.kt"
    - "app/src/main/java/dev/notyouraverage/smscourier/viewmodels/HomeViewModel.kt"
    - "app/src/main/java/dev/notyouraverage/smscourier/viewmodels/PairedDevicesViewModel.kt"
    - "app/src/test/java/dev/notyouraverage/smscourier/data/DirectionalStatusTest.kt"

key-decisions:
  - "Enum value names (FORWARDING_TO, RECEIVING_FROM) kept the same - only mapping from DeviceRole swapped"
  - "Fixed pre-existing Spotless issues in unrelated test files to unblock commit"

patterns-established:
  - "Direction mapping pattern: TARGET role forwards TO others (arrow up), SOURCE role receives FROM others (arrow down)"

# Metrics
duration: 5min
completed: 2026-02-16
---

# Phase 25 Plan 01: Direction Mapping Fix Summary

**Swapped Direction enum mapping to fix HOME-01: TARGET devices now correctly map to FORWARDING_TO direction, SOURCE devices to RECEIVING_FROM**

## Performance

- **Duration:** 5 minutes
- **Started:** 2026-02-16T10:40:01Z
- **Completed:** 2026-02-16T10:45:46Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments
- Fixed core semantic mapping: TARGET -> FORWARDING_TO, SOURCE -> RECEIVING_FROM
- UAT HOME-01 test now passes without any test modification
- All 15+ DirectionalStatusTest tests updated and passing
- No regressions in existing test suite

## Task Commits

Each task was committed atomically:

1. **Task 1: Swap Direction mapping in Direction.kt docs, HomeViewModel, and PairedDevicesViewModel** - `8157704` (fix)
2. **Task 2: Update DirectionalStatusTest helper and assertions to match swapped mapping** - `3a392d0` (test)

_Note: Final metadata commit will be created after SUMMARY.md and STATE.md updates_

## Files Created/Modified
- `Direction.kt` - Updated doc comments to reflect new semantic mapping (FORWARDING_TO = TARGET role, RECEIVING_FROM = SOURCE role)
- `HomeViewModel.kt` - Swapped calculateDirectionalStatus() mapping (SOURCE -> RECEIVING_FROM, TARGET -> FORWARDING_TO)
- `PairedDevicesViewModel.kt` - Swapped getDirectionForDevice() mapping (SOURCE -> RECEIVING_FROM, TARGET -> FORWARDING_TO)
- `DirectionalStatusTest.kt` - Updated test helper function and all 15+ test assertions to match new mapping
- `ForwardingSessionRepositoryTest.kt` - Fixed pre-existing Spotless inline comment issue
- `HomeViewModelTest.kt` - Spotless import ordering fix
- `ForwardingControlViewModelTest.kt` - Spotless import ordering fix

## Decisions Made

**Keep enum value names unchanged:**
The Direction enum values (FORWARDING_TO, RECEIVING_FROM) kept their names. Only the mapping from DeviceRole to Direction was swapped. This minimized code churn and kept the semantic meaning in the enum value names clear.

**Fix pre-existing Spotless issues:**
Encountered Spotless formatting errors in unrelated test files (inline comment in value argument list, import ordering). Applied Rule 3 (blocking issue) - fixed immediately to unblock commit.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed pre-existing Spotless formatting issues**
- **Found during:** Task 2 (committing test updates)
- **Issue:** Spotless failing on ForwardingSessionRepositoryTest.kt (inline comment in value argument list) and import ordering in two test files
- **Fix:** Moved inline comment to separate line, ran spotlessApply for import ordering
- **Files modified:** ForwardingSessionRepositoryTest.kt, HomeViewModelTest.kt, ForwardingControlViewModelTest.kt
- **Verification:** `./gradlew spotlessCheck` passes
- **Committed in:** 3a392d0 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (blocking issue)
**Impact on plan:** Essential to unblock commit. No scope creep.

## Issues Encountered
None - plan executed smoothly.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness

**Phase 25-02 ready:**
- Core direction mapping fixed at data/ViewModel layer
- Next: Update UI labels and terminology to match new semantic mapping
- HOME-02 (active badge layout) can also be addressed in 25-02

**Test status:**
- UAT HOME-01: PASSING (fixed in this plan)
- Expected failing UAT tests (from Phase 24): NOTF-01, DEVH-02, SESS-01/02/03 (will be fixed in later phases)
- No regressions in existing test suite (487 tests passing, 5 expected UAT failures)

---
*Phase: 25-home-screen-fixes*
*Completed: 2026-02-16*
