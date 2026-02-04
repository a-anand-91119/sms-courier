# Requirements: SMS Courier v0.0.64 Device Management & Visibility

**Defined:** 2026-02-04
**Core Value:** Reliable, secure SMS forwarding between paired devices with minimal user intervention

## v1 Requirements

Requirements for v0.0.64 milestone.

### Data Layer

- [ ] **DATA-01**: ForwardedMessage table stores individual SMS (sessionId FK, senderNumber, messageContent, timestamp)
- [ ] **DATA-02**: PairedDevice soft delete columns (isArchived, archivedAt, archivalInitiatedBy)
- [ ] **DATA-03**: ForwardingSession message count tracking (messageCount, updatedAt)
- [ ] **DATA-04**: PairedDevice aggregate statistics (totalSessions, totalMessagesForwarded)
- [ ] **DATA-05**: Database migration 5 → 6 preserves all existing data
- [ ] **DATA-06**: Indexes on ForwardedMessage (session_id, timestamp) for performance
- [ ] **DATA-07**: Foreign key CASCADE (delete session → delete messages)

### Message Storage

- [ ] **MSG-01**: TARGET device stores full message content on forward
- [ ] **MSG-02**: SOURCE device only stores session metadata (no message content)
- [ ] **MSG-03**: Message count updates in real-time during active sessions
- [ ] **MSG-04**: Session statistics update on session end (totalSessions increment)
- [ ] **MSG-05**: Messages stored asynchronously (Dispatchers.IO, no service blocking)

### Device History UI

- [ ] **HIST-01**: Device History screen accessible from Quick Actions
- [ ] **HIST-02**: Active devices section shows paired devices with statistics
- [ ] **HIST-03**: Removed devices section (collapsed by default)
- [ ] **HIST-04**: Device cards show total sessions and total messages
- [ ] **HIST-05**: Device cards show last session date or active status
- [ ] **HIST-06**: Tap active device navigates to Session History
- [ ] **HIST-07**: Tap removed device navigates to Archive Management

### Session History UI

- [ ] **SESS-01**: Session History screen shows all sessions for selected device
- [ ] **SESS-02**: Session cards display date, duration, message count
- [ ] **SESS-03**: Toggle between session view and contact view
- [ ] **SESS-04**: View preference (session/contact) persists across app restarts
- [ ] **SESS-05**: Tap session opens message detail bottom sheet
- [ ] **SESS-06**: Active sessions show real-time message count with [Active] badge
- [ ] **SESS-07**: Paging 3 integration for scrolling large session lists

### Message Detail UI

- [ ] **DETAIL-01**: Bottom sheet shows message list for selected session
- [ ] **DETAIL-02**: Messages display sender number, timestamp, content
- [ ] **DETAIL-03**: Messages grouped by session with metadata at top
- [ ] **DETAIL-04**: Read-only display (no long-press actions)
- [ ] **DETAIL-05**: Paging 3 integration for scrolling large message lists

### Archive Management

- [ ] **ARCH-01**: Archive Management screen for removed devices (read-only history)
- [ ] **ARCH-02**: Shows removal reason ("You initiated" or "Other side initiated")
- [ ] **ARCH-03**: Export history button with format dropdown (CSV, JSON, TXT)
- [ ] **ARCH-04**: Delete all data button with confirmation dialog
- [ ] **ARCH-05**: Soft delete on unpair presents archive choice (keep/delete history)

### Export Functionality

- [ ] **EXP-01**: Export to CSV format (session + message data)
- [ ] **EXP-02**: Export to JSON format (structured data)
- [ ] **EXP-03**: Export to plain text format (human-readable)
- [ ] **EXP-04**: Storage Access Framework integration (ACTION_CREATE_DOCUMENT)
- [ ] **EXP-05**: Export works on API 29-35 without permission fragmentation
- [ ] **EXP-06**: Export accessible from Archive Management and Session History screens

### Bidirectional Visibility

- [ ] **BIDIR-01**: Home screen calculates active session status (forwarding TO, receiving FROM, bidirectional)
- [ ] **BIDIR-02**: Smart status indicator shows ↑ (forwarding TO count)
- [ ] **BIDIR-03**: Smart status indicator shows ↓ (receiving FROM count)
- [ ] **BIDIR-04**: Smart status indicator shows ⇅ (bidirectional count)
- [ ] **BIDIR-05**: Status indicator only shows active directions (hides if zero)
- [ ] **BIDIR-06**: Tap status indicator opens session breakdown bottom sheet
- [ ] **BIDIR-07**: Session breakdown groups by direction (Forwarding To, Receiving From, Bidirectional)
- [ ] **BIDIR-08**: Paired devices list shows directional arrows (↑ forwarding, ↓ receiving, ⇅ bidirectional)

### History Retention

- [ ] **RETENTION-01**: Settings option for history retention days (range: 7-90, or 0 for forever)
- [ ] **RETENTION-02**: Default retention: 30 days
- [ ] **RETENTION-03**: Auto-cleanup toggle in Advanced Settings
- [ ] **RETENTION-04**: Manual "Clean up now" button in settings
- [ ] **RETENTION-05**: WorkManager periodic cleanup (24-hour interval)
- [ ] **RETENTION-06**: Cleanup respects retention setting (deletes messages older than N days)
- [ ] **RETENTION-07**: Lenient WorkManager constraints (battery not low only)
- [ ] **RETENTION-08**: Last cleanup timestamp displayed in settings

## v2 Requirements

Deferred to future release.

### Enhanced History

- [ ] **ENH-01**: Contact name lookup (match sender numbers to contacts)
- [ ] **ENH-02**: Search within message content
- [ ] **ENH-03**: Date range filter for sessions
- [ ] **ENH-04**: Delete individual messages from history
- [ ] **ENH-05**: Message delivery tracking (SMS delivery reports)

### Security

- [ ] **SEC-01**: PIN/fingerprint authentication to access history
- [ ] **SEC-02**: ForwardedMessage table encryption (SQLCipher or per-field)
- [ ] **SEC-03**: Export file encryption option

### Advanced Export

- [ ] **ADV-01**: Export date range selection
- [ ] **ADV-02**: Export chunking for large datasets (>10k messages)
- [ ] **ADV-03**: Scheduled export (auto-export every N days)

## Out of Scope

Explicitly excluded from this milestone.

| Feature | Reason |
|---------|--------|
| Real-time message sync across screens | Battery drain, complexity explosion, not needed for retrospective viewing |
| Delete individual messages | Breaks audit trail integrity (defeats purpose of history) |
| Cross-device history sync | Privacy nightmare, Signal deliberately avoids for security |
| Notification per forwarded message | Notification fatigue with high volume forwarding |
| Contact name lookup | Requires READ_CONTACTS permission, defer to gauge user demand first |
| Message content encryption at rest | High complexity, defer unless compliance required |

## Traceability

Which phases cover which requirements. Will be updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| DATA-01 | TBD | Pending |
| DATA-02 | TBD | Pending |
| DATA-03 | TBD | Pending |
| DATA-04 | TBD | Pending |
| DATA-05 | TBD | Pending |
| DATA-06 | TBD | Pending |
| DATA-07 | TBD | Pending |
| MSG-01 | TBD | Pending |
| MSG-02 | TBD | Pending |
| MSG-03 | TBD | Pending |
| MSG-04 | TBD | Pending |
| MSG-05 | TBD | Pending |
| HIST-01 | TBD | Pending |
| HIST-02 | TBD | Pending |
| HIST-03 | TBD | Pending |
| HIST-04 | TBD | Pending |
| HIST-05 | TBD | Pending |
| HIST-06 | TBD | Pending |
| HIST-07 | TBD | Pending |
| SESS-01 | TBD | Pending |
| SESS-02 | TBD | Pending |
| SESS-03 | TBD | Pending |
| SESS-04 | TBD | Pending |
| SESS-05 | TBD | Pending |
| SESS-06 | TBD | Pending |
| SESS-07 | TBD | Pending |
| DETAIL-01 | TBD | Pending |
| DETAIL-02 | TBD | Pending |
| DETAIL-03 | TBD | Pending |
| DETAIL-04 | TBD | Pending |
| DETAIL-05 | TBD | Pending |
| ARCH-01 | TBD | Pending |
| ARCH-02 | TBD | Pending |
| ARCH-03 | TBD | Pending |
| ARCH-04 | TBD | Pending |
| ARCH-05 | TBD | Pending |
| EXP-01 | TBD | Pending |
| EXP-02 | TBD | Pending |
| EXP-03 | TBD | Pending |
| EXP-04 | TBD | Pending |
| EXP-05 | TBD | Pending |
| EXP-06 | TBD | Pending |
| BIDIR-01 | TBD | Pending |
| BIDIR-02 | TBD | Pending |
| BIDIR-03 | TBD | Pending |
| BIDIR-04 | TBD | Pending |
| BIDIR-05 | TBD | Pending |
| BIDIR-06 | TBD | Pending |
| BIDIR-07 | TBD | Pending |
| BIDIR-08 | TBD | Pending |
| RETENTION-01 | TBD | Pending |
| RETENTION-02 | TBD | Pending |
| RETENTION-03 | TBD | Pending |
| RETENTION-04 | TBD | Pending |
| RETENTION-05 | TBD | Pending |
| RETENTION-06 | TBD | Pending |
| RETENTION-07 | TBD | Pending |
| RETENTION-08 | TBD | Pending |

**Coverage:**
- v1 requirements: 56 total
- Mapped to phases: 0
- Unmapped: 56

---
*Requirements defined: 2026-02-04*
*Last updated: 2026-02-04 after initial definition*
