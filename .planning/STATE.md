# Project State: SMS Courier

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-16)

**Core value:** Reliable, secure SMS forwarding between paired devices with minimal user intervention
**Current focus:** v0.0.65 UAT Fixes -- Phase 25 Home Screen Fixes

## Current Position

Phase: 26 (third of 5 in v0.0.65) — Session Management Fixes
Plan: 1 of 2 complete
Status: In progress
Last activity: 2026-02-16 — Completed 26-01-PLAN.md (backend session management fixes)

Progress: [█████░░░░░] 50% (1/2 Phase 26 plans complete)

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

**Phase 25 patterns (established):**
- TARGET devices map to FORWARDING_TO direction (forwards messages TO others, arrow up)
- SOURCE devices map to RECEIVING_FROM direction (receives messages FROM others, arrow down)
- Enum value names unchanged, only DeviceRole → Direction mapping swapped
- BoxWithConstraints responsive layout pattern at 400dp breakpoint
- Bidirectional merged into both counts (no separate UI indicator)
- Consistent terminology: "Forwarding to" (tertiary), "Receiving from" (primary), "Forwarding & Receiving" (secondary)

**Phase 26 patterns (established):**
- Query both roles pattern: combine(SOURCE flow, TARGET flow) for bidirectional session visibility
- Graceful SMS degradation: try SMS, always clean up locally, warn user on failure
- Idempotent session termination: endSessionForDevice handles all sessions, always notify
- Confirmation state: nullable String in UiState (null = no dialog, non-null = show dialog)
- Session notifications use CHANNEL_FORWARDING (low priority) not CHANNEL_MESSAGES (high priority)

**UAT fixes (v0.0.65 progress):**
- HOME-01: ✓ FIXED (Phase 25-01) - Direction mapping corrected at data/ViewModel layer
- HOME-02: ✓ FIXED (Phase 25-02) - Responsive layout with 400dp breakpoint
- SESS-01: ✓ FIXED (Phase 26-01) - ViewModel now queries both SOURCE and TARGET devices
- SESS-02: ✓ FIXED (Phase 26-01) - stopForwarding clears encryption keys for both roles
- SESS-03: ✓ FIXED (Phase 26-01) - stopForwarding sends STOP_FORWARD SMS with graceful degradation
- DEVH-01: Pending (Phase 27) - Archived device history visibility
- DEVH-02: Pending (Phase 27) - No archiveDevice method exists
- HIST-01: Pending (Phase 28) - Wrong icon in Session History
- NOTF-01: Pending (Phase 28) - Notification approve action broken

### Pending Todos

12 todos tracked in `.planning/todos/pending/`

**New todo from Phase 25:**
- Device History small-screen layout (three-row layout for S22/S24)

### Blockers/Concerns

None.

## Session Continuity

Last session: 2026-02-16T14:30:21Z
Stopped at: Completed 26-01-PLAN.md (backend session management fixes)
Resume file: None

**Next actions:**
- Continue Phase 26: Plan 26-02 (UI session list with confirmation dialogs)
- Parallel: Play Store Launch Phase 4 awaiting Google review
