---
phase: 20-bidirectional-visibility-indicators
plan: 01
subsystem: viewmodel
tags: [direction-enum, directional-status, homeviewmodel, stateflow, data-classes]

# Dependency graph
requires:
  - phase: 15-database-foundation-migration
    provides: PairedDevice entity with DeviceRole enum
  - phase: 16-message-storage-integration
    provides: ForwardingSession entity with devicePhoneNumber
provides:
  - Direction enum with FORWARDING_TO, RECEIVING_FROM, BIDIRECTIONAL values
  - DirectionalStatus data class with session counts and SessionWithDirection list
  - HomeState.directionalStatus field for UI consumption
  - calculateDirectionalStatus() logic for categorizing active sessions
affects: [20-02, 20-03, 20-04, HomeScreen, PairedDevicesScreen]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Direction enum separates UI semantics from DeviceRole data model"
    - "Flow.combine() with calculateDirectionalStatus for derived state"

key-files:
  created:
    - app/src/main/java/dev/notyouraverage/smscourier/data/Direction.kt
  modified:
    - app/src/main/java/dev/notyouraverage/smscourier/viewmodels/HomeViewModel.kt

key-decisions:
  - "Direction enum created separately from DeviceRole to preserve UI semantics"
  - "Bidirectional detection requires active sessions for both roles, not just paired devices"
  - "SOURCE device as primary device reference for bidirectional sessions"

patterns-established:
  - "Directional status calculation: group sessions by phone, check roles, categorize"
  - "SessionWithDirection links session, device, and direction for UI rendering"

# Metrics
duration: 5m 50s
completed: 2026-02-08
---

# Phase 20 Plan 01: Direction Data Models Summary

**Direction enum and DirectionalStatus data class for categorizing active forwarding sessions by message flow direction**

## Performance

- **Duration:** 5m 50s
- **Started:** 2026-02-08T09:02:32Z
- **Completed:** 2026-02-08T09:08:22Z
- **Tasks:** 2
- **Files modified:** 2 (plus 19 spotless fixes)

## Accomplishments
- Created Direction enum with FORWARDING_TO, RECEIVING_FROM, BIDIRECTIONAL values
- Implemented DirectionalStatus and SessionWithDirection data classes
- Added calculateDirectionalStatus() to HomeViewModel with correct role-to-direction mapping
- Extended HomeState with directionalStatus field for UI consumption

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Direction enum and data classes** - `a08e012` (feat)
2. **Task 2: Add directional status calculation to HomeViewModel** - `0c7e8f1` (feat)

**Additional commits:**
- `fec61e1` (style) - Fix ktlint violations from spotlessApply
- `ee6753a` (style) - Apply spotlessApply formatting fixes

## Files Created/Modified
- `app/src/main/java/dev/notyouraverage/smscourier/data/Direction.kt` - Direction enum, SessionWithDirection, DirectionalStatus data classes
- `app/src/main/java/dev/notyouraverage/smscourier/viewmodels/HomeViewModel.kt` - Extended with directionalStatus and calculateDirectionalStatus()

## Decisions Made
- **Direction enum separate from DeviceRole:** Created Direction.kt in data package to keep UI semantics separate from database model. Direction represents "message flow from this device's perspective" while DeviceRole represents "role in pairing relationship".
- **Bidirectional requires active sessions in both directions:** Not just both roles existing, but actual active sessions for both SOURCE and TARGET roles with same phone number.
- **SOURCE device as primary reference:** For bidirectional sessions, use SOURCE device as the primary device reference in SessionWithDirection.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed ktlint class naming violation**
- **Found during:** Verification (spotlessCheck)
- **Issue:** Pre-existing `Migration_5_6_Test.kt` violated ktlint class naming convention (underscores not allowed)
- **Fix:** Renamed to `Migration5To6Test.kt`
- **Files modified:** app/src/androidTest/.../migrations/Migration5To6Test.kt
- **Verification:** spotlessCheck passes
- **Committed in:** fec61e1

**2. [Rule 3 - Blocking] Fixed ktlint value-parameter-comment violations**
- **Found during:** Verification (spotlessCheck)
- **Issue:** Multiple files had inline parameter comments, violating ktlint value-parameter-comment rule
- **Fix:** Moved comments to separate lines above parameters
- **Files modified:** DeviceHistoryComponents.kt, DeviceHistoryScreen.kt, SmsCommandHandler.kt, HomeViewModel.kt
- **Verification:** spotlessCheck passes
- **Committed in:** fec61e1

**3. [Rule 3 - Blocking] Applied spotless formatting to 15 additional files**
- **Found during:** Verification (spotlessCheck)
- **Issue:** Trailing commas, unused imports, formatting violations in files from previous phases
- **Fix:** Ran spotlessApply
- **Files modified:** 15 files across composables, data, export, repository, viewmodels
- **Verification:** spotlessCheck passes
- **Committed in:** ee6753a

---

**Total deviations:** 3 auto-fixed (all blocking - required for build to pass)
**Impact on plan:** All fixes necessary for spotlessCheck to pass. No scope creep.

## Issues Encountered
- Nullable type issue in calculateDirectionalStatus when using boolean flags for null checks - resolved by using direct null checks in when expression for smart casting

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Direction enum and DirectionalStatus ready for UI consumption in 20-02
- HomeState.directionalStatus field available for HomeScreen status card
- SessionWithDirection provides all data needed for session breakdown bottom sheet in 20-03

---
*Phase: 20-bidirectional-visibility-indicators*
*Completed: 2026-02-08*
