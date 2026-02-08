# Phase 22: Auto-Cleanup with WorkManager - Context

**Gathered:** 2026-02-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Scheduled background task that automatically deletes old session/message history based on user's retention setting. Uses WorkManager for reliable periodic execution. Manual cleanup (Phase 21) remains available independently.

</domain>

<decisions>
## Implementation Decisions

### Scheduling behavior
- 7-day interval between cleanup runs
- Flex window timing (WorkManager picks optimal time within window for battery efficiency)
- Retry with exponential backoff on failure (database locked, etc.)

### User feedback
- Display last cleanup timestamp as relative time ("Last cleaned 2 days ago")
- Show "Never run" before first cleanup has occurred
- No notifications for cleanup results (silent background operation)
- No cleanup statistics displayed (just timestamp, keep UI minimal)

### Cleanup constraints
- No network required (cleanup is local database operation)
- No charging required (cleanup is lightweight)
- No idle requirement — Claude's discretion on whether to add

### Toggle interaction
- Default state: ON for new installations
- Hide toggle when retention is "Forever" (no cleanup needed)
- On disable: Cancel all scheduled work immediately
- On enable with cleanup behavior: Let user decide whether to run immediately or wait

### Claude's Discretion
- Initial cleanup behavior when toggle is enabled (immediate vs wait for schedule)
- Whether to require device idle constraint
- Whether to require battery not low constraint

</decisions>

<specifics>
## Specific Ideas

- User wants auto-cleanup ON by default — assumes most users want automatic maintenance
- Weekly interval chosen to minimize battery impact while keeping history manageable
- No notifications — cleanup should be invisible background maintenance

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 22-auto-cleanup-workmanager*
*Context gathered: 2026-02-08*
