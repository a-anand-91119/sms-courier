---
phase: 17-device-history-ui
plan: 04
subsystem: data
tags: [room, dao, repository, soft-delete, archive]

# Dependency graph
requires:
  - phase: 17-device-history-ui/03
    provides: DeviceHistoryScreen expecting archived devices in Removed section
provides:
  - archiveDevice DAO method for soft delete
  - archiveDevice Repository wrapper with timestamp
  - handleUnpair uses archive instead of hard delete
  - deleteDevice uses archive instead of hard delete
affects: [device-history-ui, pairing-flow, unpair-flow]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Soft delete pattern with is_archived, archived_at, archival_initiated_by
    - Query filtering to exclude archived records from active device lookups
    - Initiator tracking (LOCAL vs REMOTE) for archive operations

key-files:
  created: []
  modified:
    - app/src/main/java/dev/notyouraverage/smscourier/data/dao/PairedDeviceDao.kt
    - app/src/main/java/dev/notyouraverage/smscourier/repository/PairedDeviceRepository.kt
    - app/src/main/java/dev/notyouraverage/smscourier/handlers/SmsCommandHandler.kt
    - app/src/main/java/dev/notyouraverage/smscourier/viewmodels/PairedDevicesViewModel.kt
    - app/src/test/java/dev/notyouraverage/smscourier/handlers/SmsCommandHandlerTest.kt
    - app/src/test/java/dev/notyouraverage/smscourier/viewmodels/PairedDevicesViewModelTest.kt

key-decisions:
  - "getDeviceByPhoneNumber and getDeviceByPhoneNumberAndRole exclude archived devices to maintain semantic consistency"
  - "REMOTE initiator for SMS-received UNPAIR commands, LOCAL for user-initiated unpair from UI"

patterns-established:
  - "Soft delete (archive) pattern for preserving device history while removing from active queries"

# Metrics
duration: 8min
completed: 2026-02-06
---

# Phase 17 Plan 04: Gap Closure - Archive UNPAIR Summary

**Fixed UNPAIR to archive devices instead of hard delete, enabling Removed Devices section to display unpaired devices**

## Performance

- **Duration:** ~8 minutes
- **Started:** 2026-02-06
- **Completed:** 2026-02-06
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- Added archiveDevice DAO method with UPDATE query setting is_archived, archived_at, archival_initiated_by
- Added archiveDevice Repository wrapper with automatic timestamp generation
- Updated getDeviceByPhoneNumber and getDeviceByPhoneNumberAndRole queries to exclude archived devices
- Updated SmsCommandHandler.handleUnpair to archive with "REMOTE" initiator
- Updated PairedDevicesViewModel.deleteDevice to archive with "LOCAL" initiator
- Updated unit tests and integration tests to verify archive behavior

## Task Commits

Each task was committed atomically:

1. **Task 1: Add DAO and Repository archive methods** - `b60b718` (feat)
2. **Task 2: Update UNPAIR to archive instead of hard delete** - `43fde4d` (feat)

## Files Modified
- `app/src/main/java/dev/notyouraverage/smscourier/data/dao/PairedDeviceDao.kt` - Added archiveDevice method, updated getDevice queries to filter is_archived=0
- `app/src/main/java/dev/notyouraverage/smscourier/repository/PairedDeviceRepository.kt` - Added archiveDevice wrapper
- `app/src/main/java/dev/notyouraverage/smscourier/handlers/SmsCommandHandler.kt` - Changed deleteByPhoneNumberAndRole to archiveDevice with "REMOTE"
- `app/src/main/java/dev/notyouraverage/smscourier/viewmodels/PairedDevicesViewModel.kt` - Changed deleteByPhoneNumberAndRole to archiveDevice with "LOCAL"
- `app/src/test/java/dev/notyouraverage/smscourier/handlers/SmsCommandHandlerTest.kt` - Updated test expectations for archive
- `app/src/test/java/dev/notyouraverage/smscourier/viewmodels/PairedDevicesViewModelTest.kt` - Updated test expectations for archive

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Active device queries returned archived devices**
- **Found during:** Task 1 verification
- **Issue:** Integration tests failed because getDeviceByPhoneNumberAndRole returned archived devices
- **Fix:** Added `AND is_archived = 0` filter to getDeviceByPhoneNumber and getDeviceByPhoneNumberAndRole queries
- **Files modified:** PairedDeviceDao.kt
- **Verification:** All 343 tests pass
- **Committed in:** b60b718 (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (bug)
**Impact on plan:** Query filtering added for correctness. No scope creep.

## Issues Encountered
None - all tasks compiled and tests passed.

## Gap Closure Verification
- UAT gap: "Unpaired devices should appear in Removed Devices section with archived status"
- Root cause: handleUnpair() called deleteByPhoneNumberAndRole() (hard delete)
- Fix: Changed to archiveDevice() with appropriate initiator
- Verification: Tests pass, archived devices now visible via getArchivedDevices()

## Next Steps
- Re-run UAT to verify Removed Devices section now shows unpaired devices
- Complete Phase 17 verification

---
*Phase: 17-device-history-ui*
*Gap Closure: 2026-02-06*
