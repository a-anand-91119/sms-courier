# Project Research Summary

**Project:** SMS Courier v0.0.64 - Device Management & Visibility
**Domain:** Android SMS Forwarding App Enhancement
**Researched:** 2026-02-04
**Confidence:** HIGH

## Executive Summary

SMS Courier v0.0.64 aims to add device history tracking, message-level storage, and bidirectional visibility indicators to the existing SMS forwarding system. The research reveals this is achievable with minimal stack changes - upgrading Room (2.6.1 → 2.8.4) and WorkManager (2.9.1 → 2.11.1) is sufficient, with NO new third-party libraries required for CSV/JSON export or UI components.

The recommended approach follows Android's established patterns: soft delete for device archiving (preserving history while maintaining referential integrity), Room foreign keys with CASCADE for automatic cleanup, Paging 3 for scalable message lists, and Storage Access Framework (SAF) for universal export compatibility across Android versions. The existing architecture (Foreground Service + Room + Jetpack Compose) extends cleanly with a new ForwardedMessage entity and enhanced ViewModel calculations for bidirectional status.

Critical risks center on database migration quality (composite primary key constraints with foreign keys), foreground service blocking from synchronous DB writes, and unbounded database growth without proper indexing and pagination. These are all preventable with proper schema migration testing, explicit Dispatchers.IO usage, mandatory indexes on session_id/timestamp columns, and WorkManager-based periodic cleanup. The bidirectional visibility logic requires careful mental model alignment (SOURCE receives, TARGET sends) to avoid inverting directional indicators.

## Key Findings

### Recommended Stack

**No major stack additions required.** The existing codebase (Room 2.6.1, WorkManager 2.9.1, Material 3, DataStore 1.1.1) covers all functionality. Only version upgrades needed:

**Core technologies:**
- **Room 2.8.4** (upgrade from 2.6.1): Better migration support, prepared statement cache for performance, required for proper foreign key constraint handling in v5→v6 migration
- **WorkManager 2.11.1** (upgrade from 2.9.1): Fixes network constraint bugs on Android 15+, improved reliability for periodic cleanup tasks
- **Kotlin stdlib** (existing): CSV/JSON export via string manipulation — external libraries (kotlinx-serialization, Apache Commons CSV) are overkill for simple flat data structures
- **Material 3 BOM** (existing): ModalBottomSheet already available for message detail sheets
- **DataStore Preferences** (existing): History retention settings using existing pattern
- **Storage Access Framework** (Android SDK): Universal export across all API levels (29-35) without permission fragmentation

**What NOT to add:** kotlinx-serialization-json, Apache Commons CSV, Gson, Moshi — all add dependency overhead for zero functional benefit given simple export data structures.

### Expected Features

**Must have (table stakes):**
- Device list with active/removed states — users expect to see which devices are connected (WhatsApp, Telegram, Signal pattern)
- Session history per device — audit trail for security/troubleshooting (when forwarding happened, duration, message count)
- Active session count with directional breakdown — at-a-glance visibility into current forwarding state
- Export to CSV — industry standard for message history backup (SMS Backup & Restore pattern)
- History retention settings — user control over storage/privacy (Slack 90 days, Google Chat 30 days standard)
- Search within history — expected for any list >50 items (date range, device name filters)

**Should have (competitive differentiators):**
- Bidirectional indicators (↑↓⇅) — unique to SMS forwarding, clearly shows SOURCE (receiving ↓) vs TARGET (sending ↑) vs bidirectional (⇅)
- Message-level history storage — most SMS forwarders only track session metadata, storing content enables forensic audit
- Export to JSON/TXT — developer-friendly format (JSON) + human-readable format (TXT) beyond standard CSV
- Archive management UI — dedicated archive view with restore option, prevents cluttered device list
- Auto-cleanup with configurable days — privacy-focused, auto-delete old history reduces storage/risk
- Session breakdown by device — "Forwarding TO: Alice (2h left), Bob (1h left)" vs aggregate "3 active sessions"

**Defer (v2+):**
- Real-time message sync across all screens — battery drain, complexity explosion, not needed for retrospective history viewing
- Delete individual messages from history — breaks audit trail integrity (defeats purpose of history)
- Cross-device history sync — privacy nightmare, Signal deliberately avoids for security
- Notification for every forwarded message — notification fatigue with high volume

### Architecture Approach

**Extension of existing Room + Service + Compose architecture.** Add ForwardedMessage entity with foreign key to ForwardingSession, implement soft delete pattern for PairedDevice (isArchived + archivedAt columns), and calculate bidirectional status in HomeViewModel using Flow.combine(). Migration from v5 to v6 adds one table and two columns.

**Major components:**
1. **ForwardedMessage entity** — Stores individual SMS with session_id FK, enables message-level export and detailed audit trail
2. **Soft delete for PairedDevice** — Adds isArchived/archivedAt columns, preserves history when users unpair devices, separate query methods for active vs archived
3. **Bidirectional status calculation** — ViewModel computes ↑↓⇅ indicators by joining devices + sessions, cached in StateFlow to avoid recomposition overhead
4. **Export repository methods** — CSV/JSON/TXT generation using Kotlin stdlib, SAF integration for file sharing across API levels
5. **HistoryCleanupWorker** — WorkManager periodic task (24-hour interval) to delete old messages based on retention setting, lenient constraints (battery not low only)
6. **Enhanced integration points** — SmsCommandHandler stores messages after forwarding, PairedDevicesViewModel archives on delete instead of hard delete

**Key architectural decisions from research:**
- Foreign key with ON DELETE CASCADE auto-removes messages when session deleted
- Index on ForwardedMessage.session_id required to avoid full table scans
- Paging 3 for message lists (page size 50) to handle 10k+ records
- Storage Access Framework (SAF) exclusively for export (no permission fragmentation)
- Separate DAO methods (getActiveDevices, getArchivedDevices) to enforce soft delete filtering

### Critical Pitfalls

1. **Room Migration Failure from Composite Primary Key** — Foreign key to PairedDevice (phoneNumber, device_role) causes constraint violations if not properly defined. Add both columns to FK or use session-based reference only. Must test with MigrationTestHelper before shipping.

2. **Foreground Service Blocking from Synchronous DB Writes** — Storing messages during high volume (20+ msgs/min) blocks MasterService if not using Dispatchers.IO. All Room operations must use `withContext(Dispatchers.IO)` or batch inserts in separate coroutines.

3. **Unbounded Database Growth Without Indexes** — Database grows to 50MB+ with 10k messages, queries degrade from 50ms to 2000ms without indexes on session_id and timestamp. Indexes are mandatory, pagination is mandatory, auto-cleanup is mandatory.

4. **Soft Delete Query Fragmentation** — Forgetting `deleted_at IS NULL` filter in queries returns archived devices in active lists. Use separate DAO methods (getActiveDevices, getArchivedDevices) to enforce correctness, not boolean parameters.

5. **Scoped Storage Permission Fragmentation** — WRITE_EXTERNAL_STORAGE fails on API 30+ causing export failures on Android 11-13. Use Storage Access Framework (ACTION_CREATE_DOCUMENT) exclusively for universal compatibility.

6. **WorkManager Battery Optimization on Android 12+** — Cleanup jobs never run with RequiresDeviceIdle constraint due to aggressive battery optimization. Use lenient constraints (battery not low only) and provide manual cleanup option.

7. **LazyColumn Recomposition Storm** — Flow<List<Message>> causes full recomposition on every message insert, dropping to 10 FPS with 100+ items. Use Paging 3 with Flow<PagingData<Message>> and stable keys in items().

8. **Bidirectional Visibility Logic Inversion** — SOURCE receives (↓), TARGET sends (↑), but developers think "SOURCE = sending". Create centralized SessionDirectionCalculator to enforce correct semantics, test bidirectional pairing scenario.

## Implications for Roadmap

Based on research, suggested phase structure follows dependency order (data layer → integration → UI → enhancements):

### Phase 1: Database Foundation & Migration
**Rationale:** Everything depends on data layer working correctly. Migration v5→v6 is highest risk (composite PK + foreign keys). Must validate before building on top.
**Delivers:**
- Migration 5→6 with MigrationTestHelper tests
- ForwardedMessage entity with FK to ForwardingSession
- Enhanced PairedDevice with soft delete columns (isArchived, archivedAt)
- Indexes on session_id and timestamp
**Addresses:** Table stakes foundation for all features
**Avoids:** Pitfall #1 (migration failure), Pitfall #3 (missing indexes)
**Research flag:** Standard Room migration pattern, well-documented, skip research-phase

### Phase 2: Message Storage Integration
**Rationale:** Start populating database to validate schema and test integration with existing service layer.
**Delivers:**
- ForwardedMessageRepository with insert/query methods
- Enhanced SmsCommandHandler storing messages on forward (TARGET side)
- Enhanced SmsCommandHandler storing messages on receive (SOURCE side)
- Batch insert optimization with Dispatchers.IO
**Uses:** Room 2.8.4, Kotlin coroutines
**Implements:** Message storage component from architecture
**Avoids:** Pitfall #2 (service blocking)
**Research flag:** Standard DAO/Repository pattern, skip research-phase

### Phase 3: Device History UI with Soft Delete
**Rationale:** Users need master view (device-level) before detail view (session/message level). Soft delete must be visually clear.
**Delivers:**
- Enhanced PairedDeviceRepository with archive methods
- Enhanced PairedDevicesViewModel (archive on delete for APPROVED devices)
- DeviceHistoryScreen with active/archived tabs
- Search/filter by device name, date range
**Addresses:** Table stakes (device list, session history per device, search)
**Avoids:** Pitfall #4 (soft delete UX confusion)
**Research flag:** Needs lightweight research on mobile filter UX patterns (NOT full research-phase, just pattern verification)

### Phase 4: Session History Detail View
**Rationale:** Detail view after master view. Requires Paging 3 for scalability.
**Delivers:**
- SessionHistoryViewModel with Paging 3 integration
- SessionHistoryScreen with LazyColumn (stable keys, PagingData)
- Message list grouped by session with metadata display
- Navigation from device history to session detail
**Addresses:** Table stakes (session history per device)
**Avoids:** Pitfall #7 (recomposition storm), Pitfall #3 (pagination for large lists)
**Research flag:** Paging 3 pattern well-documented, skip research-phase

### Phase 5: Export Functionality
**Rationale:** Independent of other features, relies only on data layer. SAF must be tested across API levels.
**Delivers:**
- Export format generation (CSV, JSON, TXT) using Kotlin stdlib
- FileProvider setup in AndroidManifest
- Storage Access Framework integration (ACTION_CREATE_DOCUMENT)
- Export UI in DeviceHistoryScreen and SessionHistoryScreen
**Addresses:** Table stakes (CSV export), differentiators (JSON/TXT export)
**Avoids:** Pitfall #5 (scoped storage fragmentation)
**Research flag:** SAF pattern standard, skip research-phase, but must test on API 29, 30-32, 33+

### Phase 6: Bidirectional Visibility Indicators
**Rationale:** Can be developed in parallel with history UI, independent feature. Requires careful mental model.
**Delivers:**
- BidirectionalStatus enum (SENDING ↑, RECEIVING ↓, BIDIRECTIONAL ⇅, etc.)
- Enhanced HomeViewModel with status calculation (combine devices + sessions)
- Updated HomeScreen with directional indicators
- Session breakdown by device (replace "X active sessions" with per-device list)
**Addresses:** Key differentiator (bidirectional indicators), table stakes (session breakdown)
**Avoids:** Pitfall #8 (direction logic inversion)
**Research flag:** Domain-specific logic, needs careful implementation but skip research-phase

### Phase 7: History Retention Settings & Manual Cleanup
**Rationale:** Management UI before automation. Users need manual control before trusting auto-cleanup.
**Delivers:**
- History retention settings in DataStore (7, 14, 30, 60, 90 days, "Never")
- Settings UI in SettingsScreen
- Manual "Clean up now" button
- ArchiveManagementViewModel with cleanup logic
**Addresses:** Table stakes (history retention settings), differentiator (archive management UI)
**Research flag:** Standard DataStore pattern, skip research-phase

### Phase 8: Auto-Cleanup with WorkManager
**Rationale:** Automation last after manual flows tested. Highest risk for silent failure.
**Delivers:**
- HistoryCleanupWorker with lenient constraints (battery not low only)
- WorkManager scheduling in MainApplication
- "Last cleanup: X hours ago" indicator in settings
- Settings integration (auto-cleanup toggle)
**Addresses:** Differentiator (auto-cleanup)
**Avoids:** Pitfall #6 (WorkManager battery optimization)
**Research flag:** WorkManager constraint patterns well-documented, skip research-phase, but MUST test on real devices with battery saver

### Phase Ordering Rationale

- **Data layer first (Phase 1-2):** Migration quality is highest risk, must validate before building UI on top
- **UI follows data (Phase 3-4):** Master-detail pattern (device history → session history) matches user mental model
- **Export independent (Phase 5):** Can parallelize with UI development, only depends on data layer
- **Bidirectional visibility independent (Phase 6):** Can parallelize with export, enhances home screen
- **Manual before automated (Phase 7-8):** Cleanup settings with manual trigger before WorkManager automation reduces risk

### Research Flags

**Phases with standard patterns (skip research-phase):**
- Phase 1: Room migration pattern well-documented
- Phase 2: DAO/Repository/Dispatchers.IO standard
- Phase 4: Paging 3 established pattern
- Phase 5: SAF universally documented
- Phase 7: DataStore preferences existing pattern
- Phase 8: WorkManager constraints documented (but test on real devices)

**Phases needing lightweight pattern verification (NOT full research-phase):**
- Phase 3: Mobile filter UX patterns (quickly verify collapsible filter panels, date pickers)
- Phase 6: Bidirectional indicator semantics (verify ↑↓⇅ symbols align with user expectations)

**No phases need deep research** — domain is well-established (SMS apps, messaging history, device management), Android patterns are mature and documented.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All recommendations verified with official Android release notes, existing dependencies cover functionality |
| Features | MEDIUM | Table stakes identified from messaging app patterns (WhatsApp, Signal), differentiators validated against SMS forwarding apps, but user research needed to validate priority |
| Architecture | HIGH | All patterns verified against official Android documentation, integration points identified in existing codebase |
| Pitfalls | HIGH | All pitfalls sourced from official Android docs, Medium articles by Android team, and ProAndroidDev community (cross-verified) |

**Overall confidence:** HIGH

Research is based on official Android documentation (developer.android.com), Android team Medium articles, and established community patterns. The codebase already exists with clear architecture, so integration points are concrete rather than theoretical. The main uncertainty is feature priority (table stakes vs differentiators) which requires user validation.

### Gaps to Address

**During planning:**
- **Message encryption decision:** ForwardedMessage has `wasEncrypted` boolean, but should message content be encrypted at rest in database? This adds complexity (SQLCipher or per-field encryption). Recommendation: defer to v2+ unless explicitly required for security compliance.
- **Export file size limits:** Research covers pagination in UI but not export chunking. If session has 10k+ messages, single export file could be huge. Recommendation: add "Export date range" filter in UI to limit scope.
- **Contact name lookup integration:** Deferred to v2+ in PROJECT.md, but users may expect names instead of phone numbers in history. Recommendation: validate with real users before implementing.

**During implementation:**
- **Bidirectional pairing edge case:** Can same phone number be paired twice with different roles simultaneously? Research assumes yes, but needs validation in existing pairing logic. Test case required.
- **Soft delete re-pairing behavior:** If user archives device then re-pairs same phone number, should old history be restored or kept separate? Research suggests `archivedAt = null` to restore, but UX needs validation.
- **WorkManager execution on manufacturer-specific Android (Samsung, Xiaomi):** Battery optimization varies by manufacturer. Real device testing on top 3 manufacturers (Samsung, Google Pixel, OnePlus) required to validate cleanup runs.

## Sources

### Primary (HIGH confidence - Official Documentation)
- [Room 2.8.4 Release Notes](https://developer.android.com/jetpack/androidx/releases/room) - Version verification, migration support features
- [WorkManager 2.11.1 Release Notes](https://developer.android.com/jetpack/androidx/releases/work) - Network constraint fixes on Android 15+
- [Room Migration Guide](https://developer.android.com/training/data-storage/room/migrating-db-versions) - Manual migration patterns, schema export
- [Room Foreign Keys](https://developer.android.com/reference/android/arch/persistence/room/ForeignKey) - FK best practices, index requirements
- [Room Relationships](https://developer.android.com/training/data-storage/room/relationships) - One-to-many patterns, CASCADE behavior
- [WorkManager Periodic Tasks](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work) - Constraints, minimum intervals
- [Android Storage Use Cases](https://developer.android.com/training/data-storage/use-cases) - SAF recommendations
- [Scoped Storage Guide](https://source.android.com/docs/core/storage/scoped) - API 30+ permission changes
- [Storage Updates in Android 11](https://developer.android.com/about/versions/11/privacy/storage) - WRITE_EXTERNAL_STORAGE deprecation
- [Send Simple Data to Other Apps](https://developer.android.com/training/sharing/send) - FileProvider + share intent pattern
- [Material 3 Bottom Sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets) - ModalBottomSheet in Compose
- [State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state) - StateFlow + combine() patterns
- [Optimize Battery for Task Scheduling](https://developer.android.com/develop/background-work/background-tasks/optimize-battery) - WorkManager constraint best practices

### Secondary (MEDIUM confidence - Android Team Articles)
- [Understanding Migrations with Room - Medium](https://medium.com/androiddevelopers/understanding-migrations-with-room-f01e04b07929) - Migration testing patterns
- [Database Relations with Room - Medium](https://medium.com/androiddevelopers/database-relations-with-room-544ab95e4542) - Foreign key implementation examples
- [WorkManager Periodicity - Medium](https://medium.com/androiddevelopers/workmanager-periodicity-ff35185ff006) - Flex intervals, optimization
- [Jetpack Compose Performance - Codelab](https://developer.android.com/codelabs/jetpack-compose-performance) - Recomposition optimization
- [Jetpack Compose Best Practices](https://developer.android.com/develop/ui/compose/performance/bestpractices) - Stable keys, strong skipping mode

### Community Sources (Cross-Verified)
- [WhatsApp Web Login 2026 - Chati.ai](https://chati.ai/blog/whatsapp-web-login-2026-new-methods-device-limits-fixes) - Device management patterns
- [Signal Linked Devices - TheFastCode](https://www.thefastcode.com/en-idr/article/how-to-see-and-manage-linked-devices-in-signal) - No history sync for privacy
- [Session Messenger Review - CyberInsider](https://cyberinsider.com/secure-encrypted-messaging-apps/session/) - Session history patterns
- [Microsoft Entra Soft Deletion](https://learn.microsoft.com/en-us/entra/architecture/recover-from-deletions) - 30-day recovery window standard
- [Slack Message Retention - Logikcull](https://www.logikcull.com/blog/what-you-should-know-about-a-slack-messages-retention-policy) - 90-day retention industry norm
- [Google Workspace Auto-Delete](https://support.google.com/a/answer/13364888?hl=en) - 30-day default retention
- [SMS Import/Export - F-Droid](https://f-droid.org/packages/com.github.tmo1.sms_ie/) - CSV export format standard
- [8 Best SMS Forwarding Apps - Quo](https://www.quo.com/blog/sms-forwarding-app/) - Feature landscape
- [Bidirectional Sync Indicators - Resilio](https://www.resilio.com/blog/bidirectional-file-sync) - Upload/download arrow patterns
- [Mobile Search Filter UX - LogRocket](https://blog.logrocket.com/ux-design/best-practices-mobile-search-filter/) - Filter panel patterns
- [Avoiding Soft Delete Anti-Pattern - Cultured Systems](https://www.cultured.systems/2024/04/24/Soft-delete/) - Lifecycle state approach
- [Room Performance Hidden Dangers - ProAndroidDev](https://proandroiddev.com/the-hidden-dangers-of-room-database-performance-and-how-to-fix-them-ac93830885bd) - Index requirements
- [Android Room Hidden Costs - Krossovochkin](https://krossovochkin.com/posts/2020_12_18_android_room_hidden_costs/) - WAL mode, query optimization

---
*Research completed: 2026-02-04*
*Ready for roadmap: yes*
