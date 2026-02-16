---
phase: 24-reproduce-uat-issues
plan: 01
subsystem: testing
tags: [junit, mockk, tdd, uat, test-infrastructure]

# Dependency graph
requires:
  - phase: 23-session-history
    provides: ViewModels and data layer for session tracking
provides:
  - UATTest marker interface for JUnit 4 @Category grouping
  - uatTest Gradle task for grouped UAT test execution
  - HOME-01 failing test proving TARGET device direction label bug
  - SESS-03 failing test proving missing SMS notification on session stop

affects: [25-home-direction-fix, 26-session-visibility-fix]

# Tech tracking
tech-stack:
  added: []
  patterns: ["JUnit @Category for test grouping", "TDD Red phase with actively failing tests"]

key-files:
  created:
    - app/src/test/java/dev/notyouraverage/smscourier/UATTest.kt
  modified:
    - app/build.gradle.kts
    - app/src/test/java/dev/notyouraverage/smscourier/viewmodels/HomeViewModelTest.kt
    - app/src/test/java/dev/notyouraverage/smscourier/viewmodels/ForwardingControlViewModelTest.kt

key-decisions:
  - "Used JUnit 4 @Category (not @Tag) since project uses JUnit 4"
  - "Tests actively fail (not @Ignore) for immediate feedback - TDD Red phase"
  - "Tests written to pass after Phase 25-26 fixes without modification"

patterns-established:
  - "UAT tests use @Category(UATTest::class) for grouped execution via ./gradlew uatTest"
  - "Test names include UAT issue IDs for traceability"

# Metrics
duration: 8 min
completed: 2026-02-16
---

# Phase 24 Plan 01: Reproduce UAT Issues Summary

**HOME-01 and SESS-03 failing tests with UATTest grouping infrastructure - TDD Red phase for UAT bug reproduction**

## Performance

- **Duration:** 8 min
- **Started:** 2026-02-16T07:14:57Z
- **Completed:** 2026-02-16T07:22:23Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- Created UATTest marker interface and uatTest Gradle task for grouped UAT test execution
- Added HOME-01 failing test that proves TARGET device shows wrong direction label (RECEIVING_FROM instead of FORWARDING_TO)
- Added SESS-03 failing test that proves stopForwarding doesn't send STOP_FORWARD SMS notification
- All existing tests (486) continue to pass - only UAT tests fail as expected
- Tests written to pass after production fixes in Phases 25-26 without test modifications

## Task Commits

Each task was committed atomically:

1. **Task 1: Create UATTest marker interface and Gradle uatTest task** - `9d4a2a1` (chore)
2. **Task 2: Add HOME-01 and SESS-03 failing tests** - Already present in HEAD from previous execution

**Plan metadata:** Will be added after SUMMARY creation

## Files Created/Modified

- `app/src/test/java/dev/notyouraverage/smscourier/UATTest.kt` - JUnit 4 @Category marker interface for UAT test grouping
- `app/build.gradle.kts` - Added uatTest Gradle task that runs tests with @Category(UATTest::class)
- `app/src/test/java/dev/notyouraverage/smscourier/viewmodels/HomeViewModelTest.kt` - Added HOME-01 test for direction label bug
- `app/src/test/java/dev/notyouraverage/smscourier/viewmodels/ForwardingControlViewModelTest.kt` - Added SESS-03 test for missing SMS notification

## Decisions Made

- **Used JUnit 4 @Category instead of @Tag:** Project uses JUnit 4 (not JUnit 5), so @Category is the correct annotation for test grouping
- **Active failures instead of @Ignore:** Tests fail actively to provide immediate feedback on what's broken - standard TDD Red phase approach
- **Test durability:** Tests written to assert correct behavior so they'll pass after production fixes without modification

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - all infrastructure existed (JUnit, MockK, Turbine, test patterns).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

**Tests prove bugs exist:**
- HOME-01: Fails with "expected: FORWARDING_TO but was: RECEIVING_FROM"
- SESS-03: Fails with "Verification failed: SmsSender.sendStopForward(...) was not called"

**Ready for Phase 25-26 fixes:** Tests will turn green once production code is corrected.

**UAT test infrastructure:**
- Run all UAT tests: `./gradlew uatTest`
- Currently 4 UAT tests (HOME-01, SESS-03, DEVH-01, DEVH-02) with 3 failing as expected

---
*Phase: 24-reproduce-uat-issues*
*Completed: 2026-02-16*
