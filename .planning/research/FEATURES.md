# Feature Research

**Domain:** Device Management & Bidirectional Forwarding Visibility for SMS Forwarding Apps
**Researched:** 2026-02-04
**Confidence:** MEDIUM

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist. Missing these = product feels incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Device list with active/removed states | Standard in all multi-device apps (WhatsApp, Telegram, Signal) - users expect to see which devices are connected | LOW | Soft delete pattern: devices marked as removed but kept for history. Microsoft Entra uses 30-day recovery window standard. |
| Session history per device | Users need audit trail for security/troubleshooting - when did forwarding happen, for how long | MEDIUM | Requires JOIN queries between sessions and devices. Session Messenger shows "last connection date" as minimum. |
| Active session count with status | Users need at-a-glance visibility into what's happening now | LOW | Currently implemented (homeState.activeSessionCount), just needs directional breakdown. |
| Export to CSV | Industry standard for message history backup - SMS Import/Export, SMS Backup & Restore all support CSV | MEDIUM | Android SMS backup apps universally support CSV. Format: timestamp, device, direction, sender, message. |
| History retention settings | Users want control over storage/privacy - standard in messaging platforms | MEDIUM | Slack: 90 days standard, Google Chat: 30 days, configurable 7-90 days is industry norm. |
| Search within history | Expected for any list >50 items - users need to find specific sessions/messages | MEDIUM | Mobile search patterns: prominent search bar, filter by date range/device, real-time results. |
| Session duration display | Users need to know "how long was it forwarding" for billing/security tracking | LOW | Already tracked in ForwardingSession.durationMinutes, just needs display. |
| Message count per session | Audit trail - "X messages forwarded" gives users confidence system worked | LOW | Already tracked in ForwardingSession.messagesForwarded. |

### Differentiators (Competitive Advantage)

Features that set the product apart. Not required, but valuable.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Bidirectional indicators (↑↓⇅) | Unique to SMS forwarding - clearly shows who's sending TO vs receiving FROM whom | MEDIUM | File sync apps (Dropbox, Resilio) use upload/download arrows. Adapt for SOURCE (↓ receiving) vs TARGET (↑ sending). Bidirectional = ⇅ when same device both ways. |
| Message-level history storage | Most SMS forwarders only track session metadata - storing actual message content enables forensic audit | HIGH | Requires new ForwardedMessage entity with foreign key to session. Privacy concern: encrypted storage needed. Security risk if not handled properly. |
| Export to JSON | Developer-friendly format - rare in consumer SMS apps but valuable for power users | LOW | JSON structure: sessions array with nested messages, metadata (export date, app version). |
| Export to TXT (human readable) | Accessibility - plain text format for email/printing, unlike CSV/JSON | LOW | Format: session headers with indented messages, similar to email thread export. |
| Archive management UI | Most apps just hide removed devices - dedicated archive view gives users control | MEDIUM | Pattern: swipe to archive, "Show archived" toggle, restore option. Prevents cluttered device list. |
| Auto-cleanup with configurable days | Privacy-focused feature - auto-delete old history reduces storage/risk | MEDIUM | Requires background worker (WorkManager). Run daily, delete records older than threshold. |
| Session breakdown by device | Goes beyond "X active sessions" to show "Forwarding TO: Alice (2h left), Bob (1h left)" | MEDIUM | Differentiates from competitors who show aggregate only. Requires grouping by device role and joining with device display names. |
| Time remaining countdown | Real-time visibility into session expiration - reduces "why did it stop?" support questions | LOW | Calculate expiresAt - currentTime, format as "Xh Ym remaining". Update every minute in UI. |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good but create problems.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Real-time message sync across all screens | Users want instant updates everywhere | Battery drain, complexity explosion. WebSocket/polling required. Most messaging apps don't do true real-time in history views. | Refresh on screen focus with pull-to-refresh option. Most users view history retrospectively, not live. |
| Unlimited history retention | "Never lose data" appeal | Storage bloat, privacy risk, performance degradation with large datasets. Google Chat limits to 30 days default for reason. | Configurable with sensible defaults (30-90 days). Require explicit opt-in for >90 days with warning. |
| Delete individual messages from history | Granular control feels empowering | Breaks audit trail integrity - defeats purpose of history. Can't verify "did this message forward?" if user can delete evidence. | Session-level deletion only. Clear messaging: history is for audit, not selective editing. |
| Inline message editing/resend | Fix mistakes in forwarded messages | SMS protocol doesn't support message editing. Creating false history is dangerous for security app. | Display messages as-is, note "forwarded at [time]" to indicate immutability. |
| Cross-device history sync | Users want history available everywhere | Privacy nightmare - syncing message content to cloud violates security model. Signal deliberately doesn't sync history for privacy. | Per-device history only. Use export/import for device migration. |
| Notification for every forwarded message | Users want to know each message arrived | Notification fatigue. If forwarding 50 messages, 50 notifications is overwhelming. | Session-level notifications: "Started forwarding TO Alice" / "Forwarded 50 messages FROM Alice (session ended)". |

## Feature Dependencies

```
[Device History Screen]
    └──requires──> [Soft Delete for PairedDevice]
                      └──requires──> [PairedDevice.deletedAt column]

[Message-Level History]
    └──requires──> [ForwardedMessage entity]
    └──requires──> [Encrypted message storage]
                      └──requires──> [SQLCipher or encryption layer]

[Bidirectional Visibility]
    └──requires──> [Session direction tracking]
    └──requires──> [Device role display logic]
    └──enhances──> [Session Breakdown]

[Export History]
    └──requires──> [Message-Level History] (for detailed exports)
    └──requires──> [File provider setup]

[Auto-Cleanup]
    └──requires──> [WorkManager background task]
    └──requires──> [Retention settings from DataStore]
    └──conflicts──> [Unlimited retention setting]

[Archive Management]
    └──requires──> [Soft Delete for PairedDevice]
    └──enhances──> [Device History Screen]

[Search/Filter]
    └──requires──> [Device History Screen]
    └──requires──> [Room FTS (full-text search)] (optional performance)
```

### Dependency Notes

- **Device History requires Soft Delete:** Can't show removed devices if they're hard-deleted. Need `deletedAt` timestamp column, null = active, non-null = archived.
- **Message-Level History requires Encryption:** Storing SMS content long-term is privacy-sensitive. Either use SQLCipher for database encryption or per-field encryption with user password.
- **Export requires Message Storage:** Can't export detailed history without ForwardedMessage entity. Session-only exports (CSV with metadata) are possible but less valuable.
- **Auto-Cleanup conflicts with Unlimited retention:** These are mutually exclusive settings. If retention = "Never", disable auto-cleanup worker.
- **Bidirectional Visibility enhances Session Breakdown:** Showing direction (↑↓⇅) makes per-device session breakdown much more useful.

## MVP Definition

### Launch With (v0.0.64)

Minimum viable product - what's needed to validate the concept.

- [x] **Device History Screen** - Table stakes. View all devices (active + archived) with basic info.
- [x] **Soft Delete for Devices** - Required for history. Add `deletedAt` column to PairedDevice.
- [x] **Session History Per Device** - Table stakes. List past sessions with date, duration, message count.
- [x] **Bidirectional Indicators on Home** - Key differentiator. Show ↑ (sending TO), ↓ (receiving FROM), ⇅ (both).
- [x] **Session Breakdown by Device** - Differentiator. Replace "X active sessions" with per-device list.
- [ ] **Export to CSV** - Table stakes. Basic export format for session history.
- [x] **History Retention Settings** - Table stakes. Configurable 7-90 days + "Never".
- [x] **Basic Search/Filter** - Table stakes for >10 sessions. Search by device name/number, filter by date range.

### Add After Validation (v1.x)

Features to add once core is working.

- [ ] **Message-Level Storage** - Differentiator, but complex. Add after session history validates user need.
- [ ] **Export to JSON/TXT** - Differentiator. Easy wins after CSV export works.
- [ ] **Archive Management UI** - Differentiator. Dedicated "Archived Devices" screen with restore option.
- [ ] **Auto-Cleanup Worker** - Differentiator. Scheduled task to delete old history based on retention setting.
- [ ] **Time Remaining Countdown** - Differentiator. Real-time countdown in session breakdown.
- [ ] **Advanced Search** - Filter by direction (TO vs FROM), message count threshold, duration range.

### Future Consideration (v2+)

Features to defer until product-market fit is established.

- [ ] **Message-Level Export** - Depends on message storage. CSV/JSON with actual SMS content.
- [ ] **Contact Name Lookup** - Enhancement. Replace phone numbers with contact names (already deferred in PROJECT.md).
- [ ] **Session Statistics** - Analytics. Charts for messages/day, top devices, average session duration.
- [ ] **Encrypted Message Storage** - Security enhancement. SQLCipher or per-field encryption for message content.
- [ ] **Export Scheduling** - Automation. Weekly/monthly auto-export to email/drive.

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Device History Screen | HIGH | LOW | P1 |
| Soft Delete | HIGH | LOW | P1 |
| Session History Per Device | HIGH | MEDIUM | P1 |
| Bidirectional Indicators | HIGH | MEDIUM | P1 |
| Session Breakdown | HIGH | MEDIUM | P1 |
| Export to CSV | MEDIUM | MEDIUM | P1 |
| History Retention Settings | MEDIUM | MEDIUM | P1 |
| Basic Search/Filter | MEDIUM | MEDIUM | P1 |
| Archive Management UI | MEDIUM | MEDIUM | P2 |
| Auto-Cleanup Worker | MEDIUM | MEDIUM | P2 |
| Export to JSON/TXT | LOW | LOW | P2 |
| Time Remaining Countdown | MEDIUM | LOW | P2 |
| Message-Level Storage | HIGH | HIGH | P2 |
| Advanced Search | LOW | MEDIUM | P2 |
| Contact Name Lookup | MEDIUM | MEDIUM | P3 |
| Session Statistics | LOW | HIGH | P3 |
| Encrypted Message Storage | HIGH | HIGH | P3 |
| Export Scheduling | LOW | HIGH | P3 |

**Priority key:**
- P1: Must have for v0.0.64 launch (table stakes + key differentiators)
- P2: Should have, add when possible (remaining differentiators + nice-to-haves)
- P3: Nice to have, future consideration (requires validation or high complexity)

## Competitor Feature Analysis

| Feature | WhatsApp | Telegram | Signal | SMS Forwarders | Our Approach |
|---------|----------|----------|--------|----------------|--------------|
| Linked Device Management | Up to 4 devices, recent message sync only | Unlimited devices, full cloud sync | Up to 5 devices, no history sync (privacy) | N/A - typically 1:1 forwarding | Similar to Signal: paired devices list, no cloud sync |
| Session History | Not applicable | Not applicable | Shows "last connection date" only | Typically none - just current status | Full session history with duration, message counts (differentiator) |
| Message History | Unlimited cloud storage | Unlimited cloud storage | Per-device only, no sync | Not stored (privacy) | Optional message-level storage (v1.x), default session-only |
| Export | Chat export to email/cloud | Export chat to file | Not supported (privacy) | CSV export common (SMS Backup & Restore) | CSV (table stakes), JSON/TXT (differentiators) |
| Retention Policy | User controls local storage | Cloud-based, unlimited | User controls local storage | Typically unlimited or manual | Configurable 7-90 days with auto-cleanup (differentiator) |
| Archive/Remove Devices | Remove linked device (no recovery) | Log out device remotely | Remove linked device | Remove pairing (hard delete) | Soft delete with archive (differentiator) |
| Directional Indicators | Not applicable | Not applicable | Not applicable | Not used | ↑↓⇅ indicators for TO/FROM/BOTH (unique differentiator) |

## Implementation Notes

### Soft Delete Pattern

Based on Microsoft Entra's 30-day recovery window and industry standards:

```kotlin
// Add to PairedDevice entity
@ColumnInfo(name = "deleted_at")
val deletedAt: Long? = null  // null = active, timestamp = archived
```

**Query patterns:**
- Active devices: `WHERE deleted_at IS NULL`
- Archived devices: `WHERE deleted_at IS NOT NULL`
- All devices: no filter

**Re-pairing:** If user re-pairs an archived device, set `deletedAt = null` instead of creating new row.

### Bidirectional Visibility Logic

```kotlin
// On HomeScreen
data class SessionDirection {
    val devicesReceivingFrom: List<DeviceSession>  // TARGET role sessions (↓)
    val devicesSendingTo: List<DeviceSession>      // SOURCE role sessions (↑)
    val bidirectional: List<DeviceSession>         // Both roles active (⇅)
}

// Query: JOIN forwarding_sessions with paired_devices, group by device role
```

**Display:**
- "Forwarding TO 2 devices ↑"
- "Receiving FROM 1 device ↓"
- Tap to expand: per-device breakdown with time remaining

### Export Format (CSV)

```csv
Export Date,2026-02-04 15:30:00
App Version,0.0.64
Device,Alice (+1234567890)
Total Sessions,15

Session Start,Session End,Duration (min),Direction,Messages Forwarded,Stopped By
2026-02-01 10:00:00,2026-02-01 12:00:00,120,TO,45,USER
2026-02-02 14:30:00,2026-02-02 15:00:00,30,FROM,12,TIMEOUT
```

**With message-level (v1.x):**
Add message rows under each session with indentation or parent session ID.

### History Retention Implementation

**DataStore settings:**
```kotlin
val RETENTION_DAYS = intPreferencesKey("history_retention_days")
// Values: 7, 14, 30, 60, 90, -1 (never)
```

**Auto-cleanup (WorkManager):**
```kotlin
// Run daily at 3 AM
PeriodicWorkRequest(1, TimeUnit.DAYS)

// Delete logic
if (retentionDays > 0) {
    val cutoff = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
    forwardingSessionDao.deleteSessionsOlderThan(cutoff)
    if (messageStorageEnabled) {
        forwardedMessageDao.deleteMessagesOlderThan(cutoff)
    }
}
```

### Search/Filter UX Pattern

Based on mobile search best practices from research:

**Search bar:**
- Prominent at top of Device History screen
- Real-time filtering (no submit button)
- Search scope: device name, phone number

**Filters (expandable panel):**
- Date range picker (start/end)
- Direction filter: All / TO / FROM / BOTH
- Session status: All / Active / Completed
- Sort: Recent first / Oldest first / Most messages

**Results:**
- Group by device
- Show match highlighting for search terms
- Display filter chips below search bar when active
- "Clear all filters" option

## Sources

### Messaging App Device Management
- [WhatsApp Web Login 2026: Device Limits & Management](https://chati.ai/blog/whatsapp-web-login-2026-new-methods-device-limits-fixes)
- [How to See and Manage Linked Devices in Signal](https://www.thefastcode.com/en-idr/article/how-to-see-and-manage-linked-devices-in-signal)
- [Session Messenger Review 2026](https://cyberinsider.com/secure-encrypted-messaging-apps/session/)

### Soft Delete vs Hard Delete Patterns
- [Microsoft Entra: Soft deletion and restoration](https://learn.microsoft.com/en-us/entra/architecture/recover-from-deletions)
- [Hard Delete vs Soft Delete Logic in Spring Boot](https://medium.com/@AlexanderObregon/hard-delete-vs-soft-delete-logic-in-spring-boot-services-747798a601f9)

### SMS Export Formats
- [SMS Import / Export - F-Droid](https://f-droid.org/packages/com.github.tmo1.sms_ie/)
- [SMS Backup, Print & Restore - Google Play](https://play.google.com/store/apps/details?id=com.gilapps.smsshare2&hl=en_US)
- [iMessage-Export: Archive to HTML, CSV or SQL](https://github.com/aaronpk/iMessage-Export)

### History Retention Policies
- [Slack Message Retention Policy](https://www.logikcull.com/blog/what-you-should-know-about-a-slack-messages-retention-policy)
- [Google Workspace: Auto-delete Chat messages](https://support.google.com/a/answer/13364888?hl=en)
- [Microsoft 365 retention settings](https://learn.microsoft.com/en-us/purview/retention-settings)

### SMS Forwarding Apps
- [8 Best SMS Forwarding Apps in 2026](https://www.quo.com/blog/sms-forwarding-app/)
- [SMS Forwarder - Google Play](https://play.google.com/store/apps/details?id=com.gawk.smsforwarder&hl=en_US)

### Bidirectional Sync Indicators
- [Reliable Bidirectional Sync with Resilio](https://www.resilio.com/blog/bidirectional-file-sync)
- [How to check Dropbox sync status](https://help.dropbox.com/sync/check-sync-status)

### Mobile Search/Filter UX
- [Master Search UX in 2026: Best Practices](https://www.designmonks.co/blog/search-ux-best-practices)
- [Best practices for mobile search filter UX](https://blog.logrocket.com/ux-design/best-practices-mobile-search-filter/)
- [Mobile Filter UX Design Patterns](https://www.pencilandpaper.io/articles/ux-pattern-analysis-mobile-filters)

### Session Timeout Patterns
- [Session Timeout Pattern - PatternFly](https://pf3.patternfly.org/v3/pattern-library/communication/session-timeout/)
- [Manage a session timeout - DWP Design System](https://design-system.dwp.gov.uk/patterns/manage-a-session-timeout)
- [Enhancing Android Security: Session Timeout](https://medium.com/@dugguRK/enhancing-android-security-ux-session-timeout-ba74ac653023)

---
*Feature research for: SMS Courier v0.0.64 Device Management & Visibility*
*Researched: 2026-02-04*
