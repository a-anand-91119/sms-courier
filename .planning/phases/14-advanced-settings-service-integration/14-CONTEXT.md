# Phase 14: Advanced Settings & Service Integration - Context

**Gathered:** 2026-01-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Expose 6 security parameters to user configuration (lockout, auth, pairing rate limits) and make MasterService and SecurityManager reactive to settings changes via Flow. No new security capabilities — just making existing hardcoded constants configurable.

</domain>

<decisions>
## Implementation Decisions

### Security settings UX
- Organization: Claude's discretion — group by function or flat list based on UI patterns
- Input control for durations: Claude's discretion — dropdown presets or other appropriate control
- Contextual warnings: Yes — show helper text explaining consequences (e.g., "Lower values = more frequent lockouts")
- Reset to defaults: Claude's discretion — include if it adds value

### Value constraints
- Input type: Freeform numeric entry — user types exact values
- Unit: Minutes for all duration settings (lockout, cooldown, expiry, timeout)
- Validation: Reject invalid input with error — show error message if non-numeric or out-of-range
- Range for max attempts: Claude's discretion — pick sensible range based on security norms
- Extreme values: Allow with warning — let user save very permissive values but show caution

### Change behavior
- Apply timing: Immediately — new values apply right away via Flow
- Existing lockouts: Honored — persisted lockoutUntil timestamp remains authoritative, setting change only affects NEW lockouts
- Max attempts mid-lockout: No effect — device stays locked until original expiry
- Pending pairing/auth: Use old values — in-flight requests continue with original timeouts
- Rate limit counters: Persist — if user changes max resend attempts, existing counter is NOT reset
- App upgrade: User values preserved by default — but architecture should support migration override for future if needed
- Confirmation dialogs: Claude's discretion — determine which changes warrant "Are you sure?"
- Logging: Claude's discretion — determine if audit logging adds value
- Service restart: Claude's discretion — determine if purely reactive or some settings need restart
- Visual feedback on save: Claude's discretion — determine if snackbar confirmation helps

### Advanced section visibility
- Access pattern: Claude's discretion — either hidden behind toggle (original thought) or separate screen
- Header warning: Yes — top of section: "These settings affect security. Change with care."
- Section state: Always starts collapsed — does not persist expanded state across visits
- Label: Generic — "Advanced" or "Advanced Settings"

### Claude's Discretion
- Security settings organization (grouped vs flat)
- Duration input control type (dropdown vs other)
- Reset to defaults button (yes/no)
- Range for max failed attempts
- Confirmation dialogs for sensitive changes
- Audit logging of security setting changes
- Service restart requirements
- Save feedback mechanism
- Toggle vs separate screen for advanced section

</decisions>

<specifics>
## Specific Ideas

No specific UI references — follow Phase 13 patterns and Material3 conventions.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 14-advanced-settings-service-integration*
*Context gathered: 2026-01-19*
