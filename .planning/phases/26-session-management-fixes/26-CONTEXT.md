# Phase 26: Session Management Fixes - Context

**Gathered:** 2026-02-16
**Status:** Ready for planning

<domain>
## Phase Boundary

Fix session management so both SOURCE and TARGET devices have full visibility into active sessions and can stop them, with cross-device SMS notification on stop. Addresses SESS-01 (visibility), SESS-02 (wrong role key), SESS-03 (no stop SMS).

</domain>

<decisions>
## Implementation Decisions

### Session list display
- Two separate sections: "Forwarding to" and "Receiving from" (consistent with Phase 25 home screen terminology)
- Bidirectional devices appear in BOTH sections as separate entries
- Each session entry shows who initiated it ("Started by you" / "Started by other device")
- Stopped sessions show the reason: "Stopped by you" / "Stopped by other device" / "Timed out"

### Stop confirmation UX
- Confirmation dialog before stopping: "Stop forwarding to +1234567890? The other device will be notified." with Stop/Cancel buttons
- For bidirectional sessions, dialog mentions both directions: "Stop all forwarding with +1234? This will stop forwarding in both directions. The other device will be notified."
- Brief loading state (spinner/disabled button) while SMS is being sent, then update to "Stopped"
- If STOP_FORWARD SMS fails to send, end the local session anyway and show a warning that the remote device wasn't notified

### Remote stop notification
- System notification (separate one-time notification, not foreground service update) when remote device stops a session
- Tapping the notification opens the home screen
- Auto-dismissable notification

### Bidirectional session handling
- Stopping a session stops ALL active sessions between the two devices (both directions)
- Single STOP_FORWARD SMS suffices — the receiving handler ends all sessions with that device
- handleStopForward() should end all active sessions for the sender device, not just one direction

### Claude's Discretion
- Exact notification channel configuration and notification content wording
- Loading state implementation details (spinner vs disabled button)
- How to determine the device's role at runtime for encryption key clearing (SESS-02)
- Error/warning toast wording when SMS fails to send

</decisions>

<specifics>
## Specific Ideas

- Use same terminology as home screen (Phase 25): "Forwarding to" (tertiary color), "Receiving from" (primary color)
- Informative confirmation dialogs that tell the user the other device will be notified
- The "stopped by" data already exists in ForwardingSession entity (`stoppedBy` field: USER/TIMEOUT/REMOTE) — surface it in the UI

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 26-session-management-fixes*
*Context gathered: 2026-02-16*
