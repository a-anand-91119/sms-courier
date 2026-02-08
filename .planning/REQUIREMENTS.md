# Requirements: SMS Courier v0.0.64 Device Management & Visibility

**Defined:** 2026-02-04
**Core Value:** Reliable, secure SMS forwarding between paired devices with minimal user intervention

## v1 Requirements

Requirements for v0.0.64 milestone.

### Data Layer

- [x] **DATA-01**: ForwardedMessage table stores individual SMS (sessionId FK, senderNumber, messageContent, timestamp)
- [x] **DATA-02**: PairedDevice soft delete columns (isArchived, archivedAt, archivalInitiatedBy)
- [x] **DATA-03**: ForwardingSession message count tracking (messageCount, updatedAt)
- [x] **DATA-04**: PairedDevice aggregate statistics (totalSessions, totalMessagesForwarded)
- [x] **DATA-05**: Database migration 5 → 6 preserves all existing data
- [x] **DATA-06**: Indexes on ForwardedMessage (session_id, timestamp) for performance
- [x] **DATA-07**: Foreign key CASCADE (delete session → delete messages)

### Message Storage

- [x] **MSG-01**: TARGET device stores full message content on forward
- [x] **MSG-02**: SOURCE device only stores session metadata (no message content)
- [x] **MSG-03**: Message count updates in real-time during active sessions
- [x] **MSG-04**: Session statistics update on session end (totalSessions increment)
- [x] **MSG-05**: Messages stored asynchronously (Dispatchers.IO, no service blocking)

### Device History UI

- [x] **HIST-01**: Device History screen accessible from Quick Actions
- [x] **HIST-02**: Active devices section shows paired devices with statistics
- [x] **HIST-03**: Removed devices section (collapsed by default)
- [x] **HIST-04**: Device cards show total sessions and total messages
- [x] **HIST-05**: Device cards show last session date or active status
- [x] **HIST-06**: Tap active device navigates to Session History
- [x] **HIST-07**: Tap removed device navigates to Archive Management

### Session History UI

- [x] **SESS-01**: Session History screen shows all sessions for selected device
- [x] **SESS-02**: Session cards display date, duration, message count
- [x] **SESS-03**: Toggle between session view and contact view
- [x] **SESS-04**: View preference opens to Sessions by default (no persistence per CONTEXT.md decision)
- [x] **SESS-05**: Tap session opens message detail bottom sheet
- [x] **SESS-06**: Active sessions show real-time message count with [Active] badge
- [x] **SESS-07**: Paging 3 integration for scrolling large session lists

### Message Detail UI

- [x] **DETAIL-01**: Bottom sheet shows message list for selected session
- [x] **DETAIL-02**: Messages display sender number, timestamp, content
- [x] **DETAIL-03**: Messages grouped by session with metadata at top
- [x] **DETAIL-04**: Read-only display (no long-press actions)
- [x] **DETAIL-05**: Paging 3 integration for scrolling large message lists

### Archive Management

- [ ] **ARCH-01**: Archive Management screen for removed devices (read-only history)
- [ ] **ARCH-02**: Shows removal reason ("You initiated" or "Other side initiated")
- [ ] **ARCH-03**: Export history button with format dropdown (CSV, JSON, TXT)
- [ ] **ARCH-04**: Delete all data button with confirmation dialog
- [ ] **ARCH-05**: Soft delete on unpair presents archive choice (keep/delete history)

### Export Functionality

- [x] **EXP-01**: Export to CSV format (session + message data)
- [x] **EXP-02**: Export to JSON format (structured data)
- [x] **EXP-03**: Export to plain text format (human-readable)
- [x] **EXP-04**: Storage Access Framework integration (ACTION_CREATE_DOCUMENT)
- [x] **EXP-05**: Export works on API 29-35 without permission fragmentation
- [x] **EXP-06**: Export accessible from Archive Management and Session History screens

### Bidirectional Visibility

- [x] **BIDIR-01**: Home screen calculates active session status (forwarding TO, receiving FROM, bidirectional)
- [x] **BIDIR-02**: Smart status indicator shows ↑ (forwarding TO count)
- [x] **BIDIR-03**: Smart status indicator shows ↓ (receiving FROM count)
- [x] **BIDIR-04**: Smart status indicator shows ⇅ (bidirectional count)
- [x] **BIDIR-05**: Status indicator only shows active directions (hides if zero)
- [x] **BIDIR-06**: Tap status indicator opens session breakdown bottom sheet
- [x] **BIDIR-07**: Session breakdown groups by direction (Forwarding To, Receiving From, Bidirectional)
- [x] **BIDIR-08**: Paired devices list shows directional arrows (↑ forwarding, ↓ receiving, ⇅ bidirectional)

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

Which phases cover which requirements.

| Requirement | Phase | Status |
|-------------|-------|--------|
| DATA-01 | Phase 15 | Complete |
| DATA-02 | Phase 15 | Complete |
| DATA-03 | Phase 15 | Complete |
| DATA-04 | Phase 15 | Complete |
| DATA-05 | Phase 15 | Complete |
| DATA-06 | Phase 15 | Complete |
| DATA-07 | Phase 15 | Complete |
| MSG-01 | Phase 16 | Complete |
| MSG-02 | Phase 16 | Complete |
| MSG-03 | Phase 16 | Complete |
| MSG-04 | Phase 16 | Complete |
| MSG-05 | Phase 16 | Complete |
| HIST-01 | Phase 17 | Complete |
| HIST-02 | Phase 17 | Complete |
| HIST-03 | Phase 17 | Complete |
| HIST-04 | Phase 17 | Complete |
| HIST-05 | Phase 17 | Complete |
| HIST-06 | Phase 17 | Complete |
| HIST-07 | Phase 17 | Complete |
| SESS-01 | Phase 18 | Complete |
| SESS-02 | Phase 18 | Complete |
| SESS-03 | Phase 18 | Complete |
| SESS-04 | Phase 18 | Complete |
| SESS-05 | Phase 18 | Complete |
| SESS-06 | Phase 18 | Complete |
| SESS-07 | Phase 18 | Complete |
| DETAIL-01 | Phase 18 | Complete |
| DETAIL-02 | Phase 18 | Complete |
| DETAIL-03 | Phase 18 | Complete |
| DETAIL-04 | Phase 18 | Complete |
| DETAIL-05 | Phase 18 | Complete |
| ARCH-01 | Phase 17 | Pending |
| ARCH-02 | Phase 17 | Pending |
| ARCH-03 | Phase 19 | Complete |
| ARCH-04 | Phase 17 | Pending |
| ARCH-05 | Phase 17 | Pending |
| EXP-01 | Phase 19 | Complete |
| EXP-02 | Phase 19 | Complete |
| EXP-03 | Phase 19 | Complete |
| EXP-04 | Phase 19 | Complete |
| EXP-05 | Phase 19 | Complete |
| EXP-06 | Phase 19 | Complete |
| BIDIR-01 | Phase 20 | Complete |
| BIDIR-02 | Phase 20 | Complete |
| BIDIR-03 | Phase 20 | Complete |
| BIDIR-04 | Phase 20 | Complete |
| BIDIR-05 | Phase 20 | Complete |
| BIDIR-06 | Phase 20 | Complete |
| BIDIR-07 | Phase 20 | Complete |
| BIDIR-08 | Phase 20 | Complete |
| RETENTION-01 | Phase 21 | Pending |
| RETENTION-02 | Phase 21 | Pending |
| RETENTION-03 | Phase 21 | Pending |
| RETENTION-04 | Phase 21 | Pending |
| RETENTION-05 | Phase 22 | Pending |
| RETENTION-06 | Phase 22 | Pending |
| RETENTION-07 | Phase 22 | Pending |
| RETENTION-08 | Phase 22 | Pending |

**Coverage:**
- v1 requirements: 56 total
- Mapped to phases: 56
- Unmapped: 0

Coverage validation: 100% (56/56 requirements mapped)

---
*Requirements defined: 2026-02-04*
*Last updated: 2026-02-06 - Phase 19 complete (EXP-01 through EXP-06, ARCH-03)*
