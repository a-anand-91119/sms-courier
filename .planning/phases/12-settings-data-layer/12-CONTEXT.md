# Phase 12: Settings Data Layer - Context

**Gathered:** 2026-01-18
**Status:** Ready for planning

<domain>
## Phase Boundary

DataStore persistence and SettingsRepository with typed Flows for storing/retrieving user preferences. This phase builds the data foundation that Phases 13-14 will consume for UI and service integration.

</domain>

<decisions>
## Implementation Decisions

### Settings Organization
- Key naming convention: Claude's discretion
- Repository exposes both individual flows AND grouped convenience accessors
- Single vs multiple repositories: Claude's discretion based on complexity
- Validation on write: Reject invalid values (throw/return error)
- Default values defined in data classes as default parameters
- Reset to defaults capability: Claude's discretion on best UX
- Write API (suspend vs fire-and-forget): Claude's discretion, idiomatic approach
- Dependency injection: Match existing repository patterns in codebase

### Default Values
- Default forwarding duration: 15 minutes
- Forwarding duration range: 1 minute to 1 hour (user-configurable max)
- Duration picker UI: Slider (increments Claude's discretion)
- Default theme: System (follow device setting)
- Notification persistence: Disabled by default
- Security defaults: Conservative (stricter lockouts, fewer attempts, shorter timeouts)

### API Design
- Emission timing: Debounced (wait for input to stop before emitting)
- Combined all-settings flow: Claude's discretion
- Batch update API: Claude's discretion
- Testability approach: Match existing repository testing patterns in codebase
- Migration support: Versioned schema with migration logic
- Missing key behavior: Claude's discretion on safe approach
- DataStore encryption: Claude's discretion based on sensitivity assessment

### Claude's Discretion
- Exact key naming convention (flat vs prefixed)
- Single repository vs domain-split
- Reset API design (all vs per-section)
- Write method signature style
- Slider increment granularity for duration picker
- Whether to include allSettings combined flow
- Whether to include batch update capability
- DataStore encryption decision

</decisions>

<specifics>
## Specific Ideas

- User wants flexible forwarding duration with slider (1-60 minutes), not preset options
- Security settings should lean conservative by default
- Validation should be strict — reject invalid values rather than silently clamping

</specifics>

<deferred>
## Deferred Ideas

- Settings export/import for backup (JSON) — add to backlog for future version

</deferred>

---

*Phase: 12-settings-data-layer*
*Context gathered: 2026-01-18*
