# Phase 25: Home Screen Fixes - Context

**Gathered:** 2026-02-16
**Status:** Ready for planning

<domain>
## Phase Boundary

Fix two bugs on the Home screen: (1) forwarding direction labels show wrong text for TARGET devices (HOME-01), and (2) active session badge/card overflows on small screens (HOME-02). Also align direction terminology across all screens for consistency.

</domain>

<decisions>
## Implementation Decisions

### Direction label wording
- TARGET devices show "Forwarding to X device(s)" with proper singular/plural ("1 device" / "3 devices")
- SOURCE devices show "Receiving from X device(s)" with proper singular/plural
- DirectionalStatusCard icon labels updated to match: "Receiving from" / "Forwarding to" (not just "Receiving" / "Forwarding")
- Bidirectional concept removed from UI — show as two separate entries: "Forwarding & Receiving" instead of a merged "Bidirectional" indicator

### Small screen badge layout
- Below a breakpoint (Claude's discretion), switch DirectionalStatusCard from horizontal row to vertical compact list
- Compact list: text-only rows like "Forwarding to: 2" without large icons
- Hide inactive directions (zero count) on small screens to save space
- Large screens continue showing all three directions with icons, grayed out when inactive

### Status text consistency
- All screens use the same "Forwarding to" / "Receiving from" terminology — not just Home screen
- Direction enum values renamed to match their actual meaning in the UI (current naming is inverted/confusing)
- SessionBreakdownBottomSheet (detail view) shows actual phone numbers since user tapped for details
- Home screen status text uses device counts for the summary level

### Claude's Discretion
- Exact breakpoint width for horizontal-to-vertical layout switch
- New Direction enum value names (as long as they clearly map to "Forwarding to" = TARGET sending, "Receiving from" = SOURCE receiving)
- Compact list visual styling (spacing, font size, color treatment)

</decisions>

<specifics>
## Specific Ideas

- Status text format: "Forwarding to 1 device" / "Receiving from 3 devices" (count-based with proper grammar)
- Detail view (bottom sheet) shows phone numbers, summary view (card) shows counts
- Bidirectional indicator replaced with separate "Forwarding" and "Receiving" entries — simpler mental model for users

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 25-home-screen-fixes*
*Context gathered: 2026-02-16*
