# Phase 17: Device History UI - Context

**Gathered:** 2026-02-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Users can view device list with active/removed sections and aggregate statistics. Accessible from Quick Actions menu on HomeScreen. Tapping devices leads to detail sheets or dialogs for further actions.

</domain>

<decisions>
## Implementation Decisions

### Device list layout
- Two-line card design: phone number + role badge on top, labeled stats below
- Stats format: "Sessions: 12  •  Messages: 847"
- Role shown as badge (SOURCE/TARGET) next to phone number
- Active sessions indicated with green [Active] badge

### Active vs removed sections
- Removed devices section collapsed by default (tap header to expand)
- Prominent divider section headers with full-width contrasting background
- Removed device cards are muted/grayed (lower opacity)
- Tapping removed device shows confirmation dialog: View history, Restore, Delete

### Navigation & entry points
- Entry point: Quick Actions menu on HomeScreen
- Menu item labeled "Device History"
- Tapping active device opens device detail bottom sheet
- Bottom sheet shows: aggregate stats, "View Sessions" link, unpair button

### Empty & edge states
- No devices: illustration + "No devices paired yet" text
- Loading state: skeleton card placeholders with shimmer
- Devices with 0 sessions still appear in list (always show paired devices)

### Claude's Discretion
- Whether to hide Removed section header when empty (0 removed devices)
- Exact wording for devices with no activity
- Skeleton card styling and animation
- Detail sheet layout and styling

</decisions>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 17-device-history-ui*
*Context gathered: 2026-02-06*
