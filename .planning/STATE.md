# Project State: SMS Courier

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-16)

**Core value:** Reliable, secure SMS forwarding between paired devices with minimal user intervention
**Current focus:** v0.0.65 UAT Fixes -- Phase 25 Home Screen Fixes

## Current Position

Phase: 25 (second of 5 in v0.0.65) — Home Screen Fixes
Plan: 2 of 2 complete (all plans done)
Status: Phase complete
Last activity: 2026-02-16 — Completed 25-02-PLAN.md (UI labels and responsive layout)

Progress: [██████████] 100% (2/2 Phase 25 plans complete)

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

**UAT fixes (v0.0.65 progress):**
- HOME-01: ✓ FIXED (Phase 25-01) - Direction mapping corrected at data/ViewModel layer
- HOME-02: ✓ FIXED (Phase 25-02) - Responsive layout with 400dp breakpoint
- SESS-01: Pending (Phase 26) - SOURCE device doesn't see active sessions (no protocol ack)
- SESS-02: Pending (Phase 26) - stopForwarding uses wrong DeviceRole for encryption key
- SESS-03: Pending (Phase 26) - stopForwarding doesn't send STOP_FORWARD SMS
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

Last session: 2026-02-16T13:15:00Z
Stopped at: Completed Phase 25 (both plans + verification)
Resume file: None

**Next actions:**
- Begin Phase 26: Session Management Fixes (SESS-01/02/03)
- Parallel: Play Store Launch Phase 4 awaiting Google review
