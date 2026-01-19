---
phase: 14-advanced-settings-service-integration
plan: 02
subsystem: security
tags: [flow-collection, settings, security-manager, service-integration]

# Dependency graph
requires:
  - phase: 14-01
    provides: SettingsRepository with security setting Flows, SettingsDefaults constants
provides:
  - SecurityManager with configurable lockout/attempts/challenge via SettingsRepository
  - Reactive settings observation via Flow collection
  - MasterService integration with SecurityManager settings
affects:
  - Any future security-related features that need configurable parameters
  - Testing patterns for mocking SettingsRepository in security tests

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Flow collection for reactive settings in SecurityManager
    - startObservingSettings(scope) pattern for lifecycle-aware observation

key-files:
  created: []
  modified:
    - app/src/main/java/dev/notyouraverage/smscourier/security/SecurityManager.kt
    - app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt
    - app/src/test/java/dev/notyouraverage/smscourier/security/SecurityManagerTest.kt
    - app/src/test/java/dev/notyouraverage/smscourier/integration/IntegrationTestBase.kt
    - app/src/test/java/dev/notyouraverage/smscourier/integration/ScenarioBuilders.kt
    - app/src/test/java/dev/notyouraverage/smscourier/integration/SecurityIntegrationTest.kt

key-decisions:
  - "Use instance properties with computed millisecond values instead of companion object constants"
  - "startObservingSettings takes CoroutineScope parameter for lifecycle management"
  - "Default values from SettingsDefaults ensure backwards compatibility"

patterns-established:
  - "Flow collection pattern: startObservingSettings(scope) with scope.launch per Flow"
  - "Mock SettingsRepository with flowOf(SettingsDefaults.X) for test setup"
  - "advanceUntilIdle() after startObservingSettings in tests to ensure Flow collection"

# Metrics
duration: 25min
completed: 2026-01-19
---

# Phase 14-02: Service Integration Summary

**SecurityManager now consumes lockout duration, max failed attempts, and challenge expiry from SettingsRepository with reactive Flow collection**

## Performance

- **Duration:** 25 min
- **Started:** 2026-01-19T11:00:00Z
- **Completed:** 2026-01-19T11:25:00Z
- **Tasks:** 3
- **Files modified:** 7

## Accomplishments
- Removed hardcoded security constants from SecurityManager companion object
- Added reactive settings observation via startObservingSettings(CoroutineScope)
- MasterService initializes SecurityManager with SettingsRepository
- All existing tests updated to mock SettingsRepository with defaults
- Added test verifying custom maxFailedAttempts setting works

## Task Commits

Tasks 1 and 2 were committed in a previous session (labeled 14-03 incorrectly):

1. **Task 1: Refactor SecurityManager** - `f5cf947` (feat)
   - Includes: constructor with SettingsRepository, instance properties, startObservingSettings
2. **Task 2: Update MasterService** - `f5cf947` (feat - same commit)
   - Includes: pass settingsRepository to SecurityManager, call startObservingSettings
3. **Task 3: Update SecurityManagerTest** - `cd04161` (test)

Note: Commit f5cf947 was labeled "14-03" but contains plan 14-02 implementation.

## Files Created/Modified
- `SecurityManager.kt` - Removed hardcoded constants, added SettingsRepository injection, Flow observation
- `MasterService.kt` - Pass settingsRepository to SecurityManager, call startObservingSettings in startSelf()
- `SecurityManagerTest.kt` - Mock SettingsRepository, add test for custom maxFailedAttempts
- `IntegrationTestBase.kt` - Create mock SettingsRepository with defaults
- `ScenarioBuilders.kt` - Use SettingsDefaults.MAX_FAILED_ATTEMPTS
- `SecurityIntegrationTest.kt` - Use SettingsDefaults.MAX_FAILED_ATTEMPTS
- `PairedDevicesViewModelTest.kt` - Mock SettingsRepository (auto-fixed by linter)

## Decisions Made
- **Instance properties with computed getters:** Use `private var maxFailedAttempts` updated via Flow, with `private val lockoutDurationMs: Long get() = lockoutDurationMinutes * 60 * 1000L` for millisecond conversion
- **Lifecycle-aware observation:** startObservingSettings takes CoroutineScope parameter to tie Flow collection to service lifecycle
- **Conservative defaults:** Initialize instance properties with SettingsDefaults values for immediate availability before Flow emits

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Updated PairedDevicesViewModelTest**
- **Found during:** Running all tests after Task 3
- **Issue:** PairedDevicesViewModel requires SettingsRepository but test didn't mock it
- **Fix:** Added SettingsRepository mock with maxPairingResendAttempts and pairingResendCooldownMinutes Flows
- **Files modified:** PairedDevicesViewModelTest.kt
- **Verification:** All tests pass
- **Committed in:** cd04161 (included with Task 3)

**2. [Rule 1 - Bug] Fixed test for custom settings timing**
- **Found during:** Task 3 test execution
- **Issue:** Test failed because Flow collection hadn't emitted before assertion
- **Fix:** Added `advanceUntilIdle()` after `startObservingSettings()` call
- **Files modified:** SecurityManagerTest.kt
- **Verification:** Test passes with correct verification
- **Committed in:** cd04161

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 bug)
**Impact on plan:** Both fixes necessary for test correctness. No scope creep.

## Issues Encountered
- Gradle build cache corruption caused ClassNotFoundException during test runs - resolved by cleaning build directories
- Multiple existing tests needed SettingsRepository mocking due to cascading dependency changes

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- SecurityManager fully integrated with SettingsRepository
- All security settings (lockout, attempts, challenge expiry) now configurable via UI
- Ready for plan 14-03: PairedDevicesViewModel/SmsCommandHandler settings integration

---
*Phase: 14-advanced-settings-service-integration*
*Completed: 2026-01-19*
