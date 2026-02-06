# Phase 18: Session History & Message Detail - Context

**Gathered:** 2026-02-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Users can view session list for a selected device and drill down to message-level details. Includes session view and contact view toggle, message detail bottom sheet with pagination, and active session indicators. Messages are read-only (no delete/edit).

</domain>

<decisions>
## Implementation Decisions

### Session card design
- Duration displayed as relative format: "2h 15m", "45 min" — no time ranges
- Dates use relative format: "Today", "Yesterday", "3 days ago" for recent; full date for older
- Active sessions get green "Active" chip with pulsing dot indicator
- Cards show metadata only (date, duration, message count) — no message preview, tap to see messages

### View toggle behavior
- Toggle between "Sessions" and "Contacts" tabs
- Top bar tabs below screen title (underlined active tab style)
- Contact view grouping: Claude's discretion on best approach
- Scroll position: Claude's discretion based on UX best practices
- Always opens to Sessions view by default, no persistence of toggle state

### Message detail layout
- Messages displayed as compact list rows: sender | timestamp | message preview (truncated)
- Tapping a row expands it inline to show full message content
- No copy functionality — purely read-only view
- Bottom sheet opens at 50% height, draggable up to full screen

### Claude's Discretion
- Contact view grouping implementation (by sender phone number)
- Scroll position behavior when switching views
- Exact shimmer animation for skeleton loading
- Error state handling

</decisions>

<specifics>
## Specific Ideas

- Active session badge: green chip with pulsing dot to indicate ongoing session
- Relative dates similar to chat apps: "Today", "Yesterday", then actual dates
- List rows should feel scannable — sender and time at a glance, content on expand

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 18-session-history-message-detail*
*Context gathered: 2026-02-06*
