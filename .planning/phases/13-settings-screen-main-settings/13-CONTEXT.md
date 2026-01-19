# Phase 13: Settings Screen & Main Settings - Context

**Gathered:** 2026-01-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Build the settings UI with navigation from the home screen, implement main settings preferences (notification persistence, default forwarding duration, theme), and add information sections (permissions status, about). Covers requirements MAIN-01, MAIN-02, MAIN-03, MAIN-04, INFO-01, INFO-02.

</domain>

<decisions>
## Implementation Decisions

### Navigation & Access
- Gear icon in top app bar of home screen to access settings
- Settings changes apply immediately (standard Android pattern, no save button)

### Screen Layout
- Grouped sections with visual headers separated by spacing
- Section organization: Claude's discretion on breakdown (Main + Info only for this phase, Advanced in Phase 14)

### Theme Selection
- Three options: Light, Dark, System
- Default: System (follow device setting)
- UI presentation: Claude's discretion (appropriate Material Design pattern)
- Transition animation: Claude's discretion

### Notification Preferences
- Single setting: Foreground notification persistence toggle
- Controls whether dismissed foreground service notification auto-recreates (persistent) or stays dismissed (dismissable)
- Default: Persistent (auto-recreate when dismissed)
- Forwarded message notifications remain always visible and dismissable (no setting needed)

### Information Display
- Permissions status display: Claude's discretion on layout/pattern
- About section content (per requirements):
  - App version
  - Privacy policy link
  - Support contact: Link to GitLab issues page URL

### Claude's Discretion
- Section grouping within this phase
- Theme selection UI pattern (radio buttons, dropdown, etc.)
- Theme transition animation (if any)
- Permissions status layout/badges pattern
- Exact spacing, typography, visual styling

</decisions>

<specifics>
## Specific Ideas

- Support contact opens GitLab issues page URL (not email)
- Notification persistence setting specifically controls the foreground service notification behavior already implemented in ServiceNotificationReceiver (recreate on dismiss vs stay dismissed)

</specifics>

<deferred>
## Deferred Ideas

None - discussion stayed within phase scope

</deferred>

---

*Phase: 13-settings-screen-main-settings*
*Context gathered: 2026-01-19*
