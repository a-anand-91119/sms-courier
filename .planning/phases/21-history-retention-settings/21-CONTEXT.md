# Phase 21: History Retention Settings - Context

**Gathered:** 2026-02-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Users can configure how long message and session history is retained, and manually trigger cleanup of old data. Retention setting uses a dropdown with preset values. Manual cleanup with confirmation dialog and summary feedback.

</domain>

<decisions>
## Implementation Decisions

### Retention picker UI
- Dropdown menu (not slider or radio buttons)
- Preset values: 7 days, 30 days, 90 days, Forever
- "Forever" label (not "Never delete" or "Keep all")
- Default: 30 days for new installations

### Cleanup confirmation
- Confirmation dialog before cleanup (not immediate action)
- Button shows loading state (disabled + spinner) while cleanup runs
- Dialog with summary after cleanup completes showing what was deleted

### Data scope
- Cleanup deletes both messages AND sessions older than retention period
- Active sessions cannot hit retention (max 30 min session vs min 7 day retention)
- Session duration capped at 30 minutes maximum if custom entry ever added

### Setting placement
- New "Data & Storage" section in Settings screen
- Positioned after Preferences section, before Advanced Settings
- Auto-cleanup toggle (Phase 22) will also go in this section

### Claude's Discretion
- Whether to show message count as helper text below retention dropdown
- Confirmation dialog format: count-based preview vs age-based message
- Behavior of "Clean up now" button when Forever is selected (hide vs disable vs show "nothing to clean")
- Whether archived device data is exempt from cleanup or included

</decisions>

<specifics>
## Specific Ideas

- Follow Android Settings convention for "Data & Storage" naming
- Cleanup button becomes disabled with spinner during operation, then shows dialog result

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 21-history-retention-settings*
*Context gathered: 2026-02-08*
