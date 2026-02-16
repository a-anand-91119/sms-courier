# Project State: SMS Courier

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-16)

**Core value:** Reliable, secure SMS forwarding between paired devices with minimal user intervention
**Current focus:** v0.0.65 UAT Fixes -- Phase 24 Reproduce UAT Issues

## Current Position

Phase: 24 (first of 5 in v0.0.65) — Reproduce UAT Issues
Plan: 2 of 3 complete (24-01 UAT infrastructure, 24-02 Device History tests)
Status: In progress
Last activity: 2026-02-16 — Completed 24-01-PLAN.md (HOME-01/SESS-03 failing tests)

Progress: [██████░░░░] 67% (2/3 Phase 24 plans)

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

**Phase 24 test patterns (established):**
- Repository-layer test pattern with mocked DAO and Flow assertions (Turbine)
- Stub extension function pattern for missing ViewModel methods in TDD red phase
- @Category(UATTest::class) for grouped UAT test execution via `./gradlew uatTest`

**DEVH bugs identified:**
- DEVH-01: Repository correctly returns session data for archived devices (bug is at UI/ViewModel layer)
- DEVH-02: No archiveDevice method exists on DeviceHistoryViewModel (users must unpair to archive)

### Pending Todos

11 todos tracked in `.planning/todos/pending/`

**UAT Issues (v0.0.65) -- this milestone:**
- HOME-01: Forwarding direction wrong for TARGET devices
- HOME-02: Active badge layout broken on small screens
- SESS-01/02/03: Session visibility and stop for both devices
- DEVH-01/02: Removed device history and archive action
- HIST-01: Wrong icon in Session History
- NOTF-01: Notification Approve action broken

### Blockers/Concerns

None.

## Session Continuity

Last session: 2026-02-16T07:22:23Z
Stopped at: Completed 24-01-PLAN.md (HOME-01/SESS-03 UAT test infrastructure)
Resume file: None

**Next actions:**
- Continue Phase 24: Plan 03 (SESS-01/02/03, NOTF-01 failing tests)
- Parallel: Play Store Launch Phase 4 awaiting Google review
