---
phase: 21-history-retention-settings
plan: 01
subsystem: database
tags: [datastore, room, preferences, cleanup, retention]

# Dependency graph
requires:
  - phase: 13-settings-architecture
    provides: SettingsRepository with DataStore, PreferenceKeys, SettingsDefaults
  - phase: 15-database-foundation
    provides: ForwardingSessionDao, ForwardingSessionRepository
provides:
  - HISTORY_RETENTION_DAYS preference key and default (30 days)
  - SettingsRepository.historyRetentionDays Flow for reading retention
  - SettingsRepository.setHistoryRetentionDays with preset validation
  - ForwardingSessionDao cleanup queries (count/get/delete old sessions)
  - ForwardingSessionRepository.cleanupOldSessions with CleanupResult
affects: [21-02-settings-ui, 21-03-cleanup-trigger]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "CleanupResult data class for returning multi-value cleanup stats"
    - "Preset validation in setters (only 0, 7, 30, 90 accepted)"
    - "Threshold-based DAO queries for age-based cleanup"

key-files:
  created: []
  modified:
    - app/src/main/java/dev/notyouraverage/smscourier/data/settings/PreferenceKeys.kt
    - app/src/main/java/dev/notyouraverage/smscourier/data/settings/SettingsDefaults.kt
    - app/src/main/java/dev/notyouraverage/smscourier/repository/SettingsRepository.kt
    - app/src/main/java/dev/notyouraverage/smscourier/data/dao/ForwardingSessionDao.kt
    - app/src/main/java/dev/notyouraverage/smscourier/repository/ForwardingSessionRepository.kt

key-decisions:
  - "Preset values only (0, 7, 30, 90) for retention days - no custom input"
  - "0 = forever, skips cleanup entirely"
  - "CleanupResult returns both session and message counts for UI feedback"
  - "Cleanup only targets inactive sessions (is_active = 0)"
  - "Message counts from ForwardingSession.messageCount field (no separate query)"

patterns-established:
  - "CleanupResult: Multi-value return type for cleanup operations"
  - "Preset validation: require() in setter with explicit list of valid values"
  - "Threshold cleanup: Calculate threshold from days, query for older items"

# Metrics
duration: 4min
completed: 2026-02-08
---

# Phase 21 Plan 01: Retention Data Layer Summary

**History retention settings with DataStore persistence and ForwardingSessionRepository cleanup methods for age-based session deletion**

## Performance

- **Duration:** 4 min
- **Started:** 2026-02-08T13:05:32Z
- **Completed:** 2026-02-08T13:09:39Z
- **Tasks:** 3
- **Files modified:** 5

## Accomplishments
- HISTORY_RETENTION_DAYS preference key with 30-day default
- SettingsRepository Flow and setter with preset validation
- ForwardingSessionDao with count/get/delete queries for old sessions
- CleanupResult data class for cleanup operation feedback
- cleanupOldSessions repository method with retention logic

## Task Commits

Each task was committed atomically:

1. **Task 1: Add retention preference key and default** - `90b3c81` (feat)
2. **Task 2: Add SettingsRepository retention methods** - `8ae6df1` (feat)
3. **Task 3: Add DAO cleanup queries and Repository cleanup method** - `cb5c1e5` (feat)

## Files Created/Modified
- `app/src/main/java/dev/notyouraverage/smscourier/data/settings/PreferenceKeys.kt` - Added HISTORY_RETENTION_DAYS key
- `app/src/main/java/dev/notyouraverage/smscourier/data/settings/SettingsDefaults.kt` - Added 30-day default
- `app/src/main/java/dev/notyouraverage/smscourier/repository/SettingsRepository.kt` - Added historyRetentionDays Flow and setter
- `app/src/main/java/dev/notyouraverage/smscourier/data/dao/ForwardingSessionDao.kt` - Added cleanup queries
- `app/src/main/java/dev/notyouraverage/smscourier/repository/ForwardingSessionRepository.kt` - Added CleanupResult and cleanupOldSessions

## Decisions Made
- Preset values only (0, 7, 30, 90 days) enforced in setter to match planned UI picker
- CleanupResult data class placed at file level (not inner class) for reusability
- Message count from session's messageCount field avoids extra ForwardedMessage query
- Only inactive sessions cleaned up (active sessions never deleted regardless of age)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Data layer complete for settings UI (21-02)
- CleanupResult ready for ViewModel integration
- Cleanup can be triggered from Settings screen or WorkManager

---
*Phase: 21-history-retention-settings*
*Completed: 2026-02-08*
