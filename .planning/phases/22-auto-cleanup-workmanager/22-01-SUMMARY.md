---
phase: 22-auto-cleanup-workmanager
plan: 01
subsystem: database
tags: [workmanager, coroutineworker, datastore, room, periodic-task]

# Dependency graph
requires:
  - phase: 21-history-retention-settings
    provides: cleanupOldSessions method, historyRetentionDays setting
provides:
  - AUTO_CLEANUP_ENABLED and LAST_CLEANUP_TIMESTAMP DataStore preferences
  - CleanupWorker CoroutineWorker for scheduled cleanup
  - WorkManagerHelper for scheduling/canceling periodic cleanup
affects: [22-02 UI integration, settings-screen]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - CoroutineWorker with Room repository integration
    - WorkManager periodic scheduling with flex interval
    - enqueueUniquePeriodicWork for toggle management

key-files:
  created:
    - app/src/main/java/dev/notyouraverage/smscourier/workers/CleanupWorker.kt
    - app/src/main/java/dev/notyouraverage/smscourier/utils/WorkManagerHelper.kt
  modified:
    - app/src/main/java/dev/notyouraverage/smscourier/data/settings/PreferenceKeys.kt
    - app/src/main/java/dev/notyouraverage/smscourier/data/settings/SettingsDefaults.kt
    - app/src/main/java/dev/notyouraverage/smscourier/repository/SettingsRepository.kt

key-decisions:
  - "Battery not low constraint for background cleanup (lenient but protective)"
  - "7-day interval with 1-day flex window for battery-efficient scheduling"
  - "AUTO_CLEANUP_ENABLED defaults to true for new installations"
  - "LAST_CLEANUP_TIMESTAMP = 0L means never run"

patterns-established:
  - "CleanupWorker pattern: get retention setting, call cleanup, update timestamp"
  - "WorkManagerHelper object for scheduling/canceling unique periodic work"

# Metrics
duration: 5min
completed: 2026-02-08
---

# Phase 22 Plan 01: Auto-Cleanup Data Layer Summary

**CleanupWorker with WorkManager 7-day periodic scheduling, DataStore preferences for toggle and timestamp, using existing cleanupOldSessions method**

## Performance

- **Duration:** 5 min
- **Started:** 2026-02-08T18:00:00Z
- **Completed:** 2026-02-08T18:05:00Z
- **Tasks:** 3
- **Files modified:** 5

## Accomplishments
- AUTO_CLEANUP_ENABLED and LAST_CLEANUP_TIMESTAMP DataStore preferences
- CleanupWorker calling existing cleanupOldSessions with retry on failure
- WorkManagerHelper with 7-day interval, 1-day flex, battery constraint
- scheduleCleanup and cancelCleanup functions for toggle management

## Task Commits

Each task was committed atomically:

1. **Task 1: Add auto-cleanup preference keys and defaults** - `8c11de9` (feat)
2. **Task 2: Add SettingsRepository auto-cleanup methods** - `427a5b8` (feat)
3. **Task 3: Create CleanupWorker and WorkManagerHelper** - `ff2bed3` (feat)

## Files Created/Modified
- `app/src/main/java/dev/notyouraverage/smscourier/data/settings/PreferenceKeys.kt` - Added AUTO_CLEANUP_ENABLED and LAST_CLEANUP_TIMESTAMP keys
- `app/src/main/java/dev/notyouraverage/smscourier/data/settings/SettingsDefaults.kt` - Added default values (true, 0L)
- `app/src/main/java/dev/notyouraverage/smscourier/repository/SettingsRepository.kt` - Added Flow properties and setter methods
- `app/src/main/java/dev/notyouraverage/smscourier/workers/CleanupWorker.kt` - CoroutineWorker for scheduled cleanup
- `app/src/main/java/dev/notyouraverage/smscourier/utils/WorkManagerHelper.kt` - Scheduling utilities

## Decisions Made
- Battery not low constraint chosen (protective but lenient, per CONTEXT.md "no charging required")
- No device idle constraint (cleanup is lightweight database operation)
- ExistingPeriodicWorkPolicy.KEEP to avoid rescheduling already-scheduled work
- Exponential backoff on failure with MIN_BACKOFF_MILLIS

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed database singleton method name**
- **Found during:** Task 3 (CleanupWorker creation)
- **Issue:** Plan used SmsCourierDatabase.getInstance but actual method is getDatabase
- **Fix:** Changed to SmsCourierDatabase.getDatabase(applicationContext)
- **Files modified:** CleanupWorker.kt
- **Verification:** Build compiles successfully
- **Committed in:** ff2bed3 (part of task commit)

**2. [Rule 3 - Blocking] Fixed MIN_BACKOFF_MILLIS reference**
- **Found during:** Task 3 (WorkManagerHelper creation)
- **Issue:** PeriodicWorkRequest.MIN_BACKOFF_MILLIS not found, constant is on WorkRequest
- **Fix:** Changed to WorkRequest.MIN_BACKOFF_MILLIS
- **Files modified:** WorkManagerHelper.kt
- **Verification:** Build compiles successfully
- **Committed in:** ff2bed3 (part of task commit)

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Minor API reference corrections. No scope creep.

## Issues Encountered
None beyond the auto-fixed blocking issues above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- CleanupWorker and WorkManagerHelper ready for UI integration in Plan 02
- SettingsViewModel needs autoCleanupEnabled and lastCleanupTimestamp state
- SettingsScreen needs toggle (hidden when retention = Forever) and last cleanup display
- App launch needs to schedule cleanup if toggle is ON

---
*Phase: 22-auto-cleanup-workmanager*
*Completed: 2026-02-08*
