# Project State: SMS Courier

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-16)

**Core value:** Reliable, secure SMS forwarding between paired devices with minimal user intervention
**Current focus:** v0.0.65 UAT Fixes -- Phase 24 Reproduce UAT Issues

## Current Position

Phase: 25 (second of 5 in v0.0.65) — Home Screen Fixes
Plan: 1 of 2 complete (Direction mapping fixed)
Status: In progress
Last activity: 2026-02-16 — Completed 25-01-PLAN.md (Direction enum mapping fix)

Progress: [█████░░░░░] 50% (1/2 Phase 25 plans complete)

## Performance Metrics

**Velocity:**
- Total plans completed: 40+ (all milestones)
- v0.0.64 plans: 29 (Phases 15-23)
- Average duration: ~4-5 minutes per plan

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.

**Architecture (stable):**
- SMS-based command protocol with SMSC prefix
- Foreground service (MasterService) for reliable forwarding
- Room database v7 with ForwardedMessage storage
- Jetpack Compose UI with Navigation Compose
- DataStore Preferences for settings
- WorkManager for background cleanup

**v0.0.65 approach:**
- Test-driven: write failing tests first (Phase 24), then fix in subsequent phases
- HOME-02 and HIST-01 are UI/icon issues not reproducible in unit/integration tests -- manual verification only

**Phase 25 Direction mapping (established):**
- TARGET devices map to FORWARDING_TO direction (forwards messages TO others, arrow up)
- SOURCE devices map to RECEIVING_FROM direction (receives messages FROM others, arrow down)
- Enum value names unchanged, only DeviceRole → Direction mapping swapped

**Phase 24 test patterns (established):**
- Repository-layer test pattern with mocked DAO and Flow assertions (Turbine)
- Stub extension function pattern for missing ViewModel methods in TDD red phase
- @Category(UATTest::class) for grouped UAT test execution via `./gradlew uatTest`
- Robolectric receiver test pattern: RuntimeEnvironment + shadowOf() for service verification

**UAT fixes (v0.0.65 progress):**
- HOME-01: ✓ FIXED (Phase 25-01) - Direction mapping corrected at data/ViewModel layer
- HOME-02: Pending (Phase 25-02) - Active badge layout for small screens
- SESS-01: Pending (Phase 26) - TARGET devices with active sessions not visible
- SESS-02: Pending (Phase 26) - stopForwarding uses wrong DeviceRole for encryption key
- SESS-03: Pending (Phase 26) - stopForwarding doesn't send STOP_FORWARD SMS
- DEVH-01: Pending (Phase 27) - Archived device history visibility
- DEVH-02: Pending (Phase 27) - No archiveDevice method exists
- HIST-01: Pending (Phase 28) - Wrong icon in Session History
- NOTF-01: Pending (Phase 28) - Notification approve action broken

### Pending Todos

11 todos tracked in `.planning/todos/pending/`

**UAT Issues (v0.0.65) -- this milestone:**
- HOME-01: ✓ Fixed (Direction mapping)
- HOME-02: Active badge layout broken on small screens
- SESS-01/02/03: Session visibility and stop for both devices
- DEVH-01/02: Removed device history and archive action
- HIST-01: Wrong icon in Session History
- NOTF-01: Notification Approve action broken

### Blockers/Concerns

None.

## Session Continuity

Last session: 2026-02-16T10:45:46Z
Stopped at: Completed 25-01-PLAN.md (Direction mapping fix)
Resume file: None

**Next actions:**
- Continue Phase 25: Plan 25-02 (UI labels and responsive layout)
- Parallel: Play Store Launch Phase 4 awaiting Google review
