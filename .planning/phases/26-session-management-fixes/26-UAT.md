---
status: testing
phase: 26-session-management-fixes
source: 26-01-SUMMARY.md, 26-02-SUMMARY.md
started: 2026-02-16T16:00:00Z
updated: 2026-02-16T16:00:00Z
---

## Current Test

number: 1
name: Both device roles visible in session list
expected: |
  Open the Forwarding Control screen. Both SOURCE and TARGET paired devices appear in the list — not just one role. If you have a device paired as TARGET and another as SOURCE, both show up.
awaiting: user response

## Tests

### 1. Both device roles visible in session list
expected: Open the Forwarding Control screen. Both SOURCE and TARGET paired devices appear in the list — not just one role. If you have a device paired as TARGET and another as SOURCE, both show up.
result: [pending]

### 2. Role-based session sections
expected: Active sessions are grouped into two sections: "Forwarding to" (with tertiary/teal color) and "Receiving from" (with primary/purple color). Each section only shows devices of that role.
result: [pending]

### 3. Session initiation labels
expected: Each active session entry shows who started it — "Started by you" for sessions you initiated, or "Started by other device" for sessions the remote device started.
result: [pending]

### 4. Stop session confirmation dialog
expected: Tapping the Stop button on an active session shows a confirmation dialog asking if you want to stop. The dialog has Confirm and Cancel buttons. Nothing happens until you confirm.
result: [pending]

### 5. Bidirectional stop dialog text
expected: If the same phone number has active sessions in both directions (forwarding AND receiving), the stop dialog says "Stop all forwarding with [number]? This will stop forwarding in both directions." For unidirectional sessions, it just says "Stop forwarding to [number]?"
result: [pending]

### 6. Stop session sends SMS to remote device
expected: After confirming stop on an active session, the session ends locally AND an SMS notification (STOP_FORWARD) is sent to the other device. If the SMS fails to send, the session still stops locally and you see an error message about the remote device not being notified.
result: [pending]

### 7. Session stopped notification from remote device
expected: When the OTHER device stops a forwarding session, your device receives a notification indicating the session was stopped (on the low-priority forwarding channel, not the high-priority message channel).
result: [pending]

## Summary

total: 7
passed: 0
issues: 0
pending: 7
skipped: 0

## Gaps

[none yet]
