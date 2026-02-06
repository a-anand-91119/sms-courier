# Phase 19: Export Functionality - Context

**Gathered:** 2026-02-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Export session and message history in CSV/JSON/TXT formats via Storage Access Framework. Users can export single sessions or all sessions for a device. This is about getting data OUT — not filtering, searching, or transforming it.

</domain>

<decisions>
## Implementation Decisions

### Export Scope
- Support both single-session export AND per-device bulk export
- User toggle for including metadata header (session start/end, duration, message count)
- Per-device export separates by role — user picks which role's sessions to export
- Archived (removed) devices can be exported from Archive Management screen

### Format Behavior
- CSV: RFC 4180 compliant — proper escaping with quotes for commas/newlines, works in Excel/Google Sheets
- JSON: Nested structure — `{ sessions: [{ metadata, messages: [...] }] }` — hierarchical
- Plain text: Chat log style — `[2026-02-06 10:30] +1234567890: Message content`

### Export Trigger Points
- Single-session export: Message detail bottom sheet button AND session card long-press menu
- Per-device export: Available from Device History screen AND Session History screen ("Export all")
- Archive Management screen: Prominent export button for archived devices (before permanent deletion)

### File Naming & Feedback
- Default filename: `smscourier_export_YYYYMMDD_HHMMSS.{ext}` — app name + timestamp
- Filename is fixed/generated — no user editing in SAF picker
- Progress spinner during export, then success/failure snackbar
- Success message shows file location — no open/share action offered

### Claude's Discretion
- Timestamp format per export type (local vs UTC with offset — pick appropriate per format)
- Format selection UX (bottom sheet picker vs inline menu — pick cleaner flow)
- Exact column ordering for CSV
- Error handling and retry behavior

</decisions>

<specifics>
## Specific Ideas

- Chat log style for TXT should feel like reading a conversation transcript
- RFC 4180 CSV compliance matters — users will open these in spreadsheet apps
- Export from Archive Management is important — users want to preserve data before permanent deletion

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 19-export-functionality*
*Context gathered: 2026-02-06*
