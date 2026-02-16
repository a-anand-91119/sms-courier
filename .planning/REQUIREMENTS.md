# Requirements: SMS Courier v0.0.65

**Defined:** 2026-02-16
**Core Value:** Reliable, secure SMS forwarding between paired devices with minimal user intervention

## v0.0.65 Requirements

Requirements for UAT fixes milestone. Each maps to roadmap phases.

### Home Screen

- [ ] **HOME-01**: TARGET device shows "Forwarding to: [phone numbers]" instead of "Receiving" for active sessions
- [ ] **HOME-02**: Active badge layout handles small screen devices without vertical text wrapping

### Session Management

- [ ] **SESS-01**: Both SOURCE and TARGET devices can see active forwarding sessions
- [ ] **SESS-02**: Both SOURCE and TARGET devices can stop an active session
- [ ] **SESS-03**: When a session is stopped, the other device receives an SMS notification

### Session History UI

- [ ] **HIST-01**: Session History screen uses export icon instead of share icon

### Device History

- [ ] **DEVH-01**: Removed/archived devices show their session and message history when viewed
- [ ] **DEVH-02**: User can explicitly archive a paired device

### Notifications

- [ ] **NOTF-01**: Tapping "Approve" on pairing request notification approves the pairing

## Future Requirements

Deferred to later milestones.

### Reliability
- **REL-01**: Auto-start app on device boot
- **REL-02**: Proper permission management flow

### Features
- **FEAT-01**: Forward to email or chat apps
- **FEAT-02**: Smart filters for selective forwarding
- **FEAT-03**: Scheduled and recurring forwarding sessions
- **FEAT-04**: Multi-destination forwarding (one-to-many)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Message format customization | Deferred to future milestone |
| Notification grouping | Deferred to future milestone |
| Contact name lookup for message history | Requires READ_CONTACTS permission, gauge demand first |
| Cross-device history sync | Privacy concerns, intentionally avoided |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| HOME-01 | TBD | Pending |
| HOME-02 | TBD | Pending |
| SESS-01 | TBD | Pending |
| SESS-02 | TBD | Pending |
| SESS-03 | TBD | Pending |
| HIST-01 | TBD | Pending |
| DEVH-01 | TBD | Pending |
| DEVH-02 | TBD | Pending |
| NOTF-01 | TBD | Pending |

**Coverage:**
- v0.0.65 requirements: 9 total
- Mapped to phases: 0
- Unmapped: 9

---
*Requirements defined: 2026-02-16*
*Last updated: 2026-02-16 after initial definition*
