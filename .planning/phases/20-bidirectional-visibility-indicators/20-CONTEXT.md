# Phase 20: Bidirectional Visibility Indicators - Context

**Gathered:** 2026-02-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Home screen shows directional forwarding status with smart indicators. Users can see at a glance whether they're forwarding TO other devices, receiving FROM other devices, or both (bidirectional). Tapping indicators opens a session breakdown sheet. Device list shows directional status per device.

</domain>

<decisions>
## Implementation Decisions

### Indicator design
- Place indicators in the existing status card on home screen (alongside paired/pending/active counts)
- Use icon chips with arrow icon and count badge
- Semantic colors: green for receiving, blue for forwarding, purple for bidirectional
- Show all directions but gray out/dim inactive ones (zero count)

### Session breakdown sheet
- Group sessions by direction: "Forwarding To", "Receiving From", "Bidirectional" sections
- Each session row shows: device phone number and message count only (simple)
- Empty state message when no active sessions: "No active forwarding sessions" with suggestion
- Sheet always opens when tapped, even with zero count

### Device list arrows
- Direction shown as subtitle text below device name (e.g., "Forwarding • Last active: 2 min ago")
- Devices with no active session show "Idle • Last active: 2 hours ago"
- Direction text uses semantic colors matching indicator chips
- Short verb labels: "Forwarding" / "Receiving" / "Bidirectional"

### Claude's Discretion
- Session breakdown sheet actions (view only vs quick stop) — Claude determines appropriate UX
- Transition animations when counts change
- Whether to show toast/snackbar on session state changes
- How UI refreshes when SMS commands change state while app is in foreground (auto vs wait for pull)

</decisions>

<specifics>
## Specific Ideas

- Existing home screen already has a status card with paired device counts — add directional indicators there
- Pull-to-refresh pattern for updating session data on home screen
- Consistency with existing chip/badge patterns in the app

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 20-bidirectional-visibility-indicators*
*Context gathered: 2026-02-08*
