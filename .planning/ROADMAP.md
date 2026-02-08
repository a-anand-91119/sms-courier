# Roadmap: SMS Courier

## Overview

SMS Courier is an Android app for forwarding SMS messages between two paired devices using secure SMS-based commands. The roadmap focuses on completing Play Store requirements, finalizing the automated CI/CD deployment pipeline, conducting thorough testing, and launching publicly on Google Play Store.

## Domain Expertise

None (Android app development patterns already established in codebase)

## Milestones

- [**v0.0.7-v0.0.10 Feature Improvements**](milestones/v0.1-ROADMAP.md) - Phases 5-7 (shipped 2026-01-15)
- **v0.0.60-v0.0.61 CI/CD Optimizations** - Phase 8 (shipped 2026-01-16)
- [**v0.0.62 Testing**](milestones/v0.0.62-ROADMAP.md) - Phases 9-11 (shipped 2026-01-18)
- [**v0.0.63 Settings**](milestones/v0.0.63-settings/ROADMAP.md) - Phases 12-14 (shipped 2026-01-19)
- **Play Store Launch** - Phases 1-4 (parallel)
- 🚧 **v0.0.64 Device Management & Visibility** - Phases 15-22 (in progress)

## Completed Milestones

<details>
<summary>v0.0.7-v0.0.10 Feature Improvements (Phases 5-7) - SHIPPED 2026-01-15</summary>

- [x] Phase 5: Bidirectional Pairing Architecture - completed 2026-01-15
- [x] Phase 6: Pending State & Pairing UX (3/3 plans) - completed 2026-01-15
- [x] Phase 7: UI/UX Polish (1/1 plan + fix) - completed 2026-01-15

See [milestones/v0.1-ROADMAP.md](milestones/v0.1-ROADMAP.md) for full details.

</details>

<details>
<summary>v0.0.60-v0.0.61 CI/CD Optimizations (Phase 8) - COMPLETE 2026-01-16</summary>

- [x] Phase 8: CI/CD Pipeline Optimization (3/3 plans) - completed 2026-01-16

**Key accomplishments:**
- Upload-only Fastlane lanes (fix rebuild issue)
- Integrated fastlane-plugin-changelog for proper changelog generation
- GitLab release automation with artifact attachment

</details>

<details>
<summary>v0.0.62 Testing (Phases 9-11) - SHIPPED 2026-01-18</summary>

- [x] Phase 9: SmsReceiver Unit Testing (2/2 plans) - completed 2026-01-18
- [x] Phase 10: Integration Test Infrastructure (3/3 plans) - completed 2026-01-18
- [x] Phase 11: End-to-End Flow Tests (3/3 plans) - completed 2026-01-18

**Key accomplishments:**
- SmsReceiver unit tests (28 tests) with Robolectric
- Integration test infrastructure (IntegrationTestBase, CapturingSmsSender, ScenarioBuilders)
- E2E flow tests (40 tests) for pairing, forwarding, and security
- 75 new tests added, 301 total tests passing

See [milestones/v0.0.62-ROADMAP.md](milestones/v0.0.62-ROADMAP.md) for full details.

</details>

<details>
<summary>v0.0.63 Settings (Phases 12-14) - SHIPPED 2026-01-19</summary>

- [x] Phase 12: Settings Data Layer (1/1 plans) - completed 2026-01-18
- [x] Phase 13: Settings Screen & Main Settings (3/3 plans) - completed 2026-01-19
- [x] Phase 14: Advanced Settings & Service Integration (3/3 plans) - completed 2026-01-19

**Key accomplishments:**
- DataStore-backed settings persistence with SettingsRepository
- Settings screen with navigation, preferences, permission status, and about section
- 6 advanced security settings (lockout, attempts, challenge expiry, pairing limits, auth timeout)
- SecurityManager and MasterService refactored for reactive settings via Flow

See [milestones/v0.0.63-settings/ROADMAP.md](milestones/v0.0.63-settings/ROADMAP.md) for full details.

</details>

## Phases

### Play Store Launch (Parallel)

**Milestone Goal:** Complete all Play Store requirements, test deployment pipeline, conduct multi-track testing, and successfully launch SMS Courier on Google Play Store.

#### Phase 1: Play Store Submission Requirements

**Goal**: Complete all Google Play Console requirements for app submission
**Depends on**: Nothing (first phase)
**Status**: Complete (2026-01-15)

All submission requirements completed:
- SMS and foreground service permissions declarations done
- Demonstration video submitted
- Privacy policy linked
- App approved for closed testing

#### Phase 2: CI/CD Pipeline Refinement

**Goal**: Validate and refine automated deployment pipeline from tag to production
**Depends on**: Phase 1
**Status**: Complete (2026-01-15)

Pipeline validated and working:
- Full deployment flow: tag -> internal -> alpha working
- Semantic version code extraction working
- Manual promotion workflow tested

#### Phase 3: Closed Testing Period

**Goal**: Complete 14-day closed testing period with 12+ testers to unlock open testing
**Depends on**: Phase 2 (complete)
**Status**: Complete (2026-02-01)

**Play Store Status:**
- Closed testing completed successfully
- 14-day waiting period fulfilled
- Applied for beta testing and production access on 2026-02-01

#### Phase 4: Open Testing & Production Release

**Goal**: Promote to open testing (beta), then to production
**Depends on**: Phase 3 (complete)
**Status**: In Progress — applied for beta/production access, awaiting Google review
**Plans**: TBD

**Play Store Progression:**
1. Closed testing (current) -> 14 days + 12 testers
2. Open testing (beta) -> broader audience, public opt-in
3. Production -> full public release

Key deliverables:
- Promote from closed to open testing (beta)
- Monitor crash reports and beta feedback
- Promote from beta to production
- Respond to user reviews
- Prepare user documentation and FAQs

Plans:
- [ ] 04-01: TBD (promote to open testing)
- [ ] 04-02: TBD (promote to production)

### 🚧 v0.0.64 Device Management & Visibility (In Progress)

**Milestone Goal:** Enhanced device management with session history, message-level storage, bidirectional forwarding visibility, and configurable history retention.

- [x] **Phase 15: Database Foundation & Migration** - Add ForwardedMessage table, soft delete columns, and migration testing
- [x] **Phase 16: Message Storage Integration** - Populate ForwardedMessage during forwarding sessions
- [x] **Phase 17: Device History UI** - Active/removed device list with statistics
- [x] **Phase 18: Session History & Message Detail** - Session list with message-level bottom sheets
- [x] **Phase 19: Export Functionality** - CSV/JSON/TXT export via Storage Access Framework
- [x] **Phase 20: Bidirectional Visibility Indicators** - Home screen directional status (↑↓⇅)
- [x] **Phase 21: History Retention Settings** - Configurable retention with manual cleanup
- [ ] **Phase 22: Auto-Cleanup with WorkManager** - Periodic background cleanup

#### Phase 15: Database Foundation & Migration
**Goal**: Database schema supports message-level storage and device archiving with validated migration
**Depends on**: Phase 14 (Settings shipped)
**Requirements**: DATA-01, DATA-02, DATA-03, DATA-04, DATA-05, DATA-06, DATA-07
**Status**: Complete (2026-02-05)
**Success Criteria** (what must be TRUE):
  1. ForwardedMessage table exists with sessionId foreign key, sender, content, timestamp columns
  2. PairedDevice table has isArchived, archivedAt, archivalInitiatedBy columns
  3. ForwardingSession table tracks messageCount and updatedAt
  4. PairedDevice table tracks totalSessions and totalMessagesForwarded
  5. Migration 5 to 6 completes successfully preserving all existing data (verified by MigrationTestHelper)
  6. Indexes exist on ForwardedMessage (session_id, timestamp) for query performance
  7. Foreign key CASCADE deletes messages when session is deleted
**Plans**: 2 plans

Plans:
- [x] 15-01-PLAN.md — Schema v6: ForwardedMessage entity, PairedDevice soft delete columns, ForwardingSession tracking columns, Migration 5->6
- [x] 15-02-PLAN.md — Migration testing: MigrationTestHelper tests for data preservation, CASCADE delete, and index verification

#### Phase 16: Message Storage Integration
**Goal**: Messages are stored during forwarding with real-time session statistics
**Depends on**: Phase 15
**Requirements**: MSG-01, MSG-02, MSG-03, MSG-04, MSG-05
**Status**: Complete (2026-02-06)
**Success Criteria** (what must be TRUE):
  1. TARGET device stores full message content (sender, content, timestamp, destinationNumber) when forwarding SMS
  2. SOURCE device stores only session metadata without message content
  3. Session message count updates in real-time during active forwarding
  4. Session statistics (totalSessions, totalMessagesForwarded) update immediately (per session start and per message)
  5. Message storage operations run on Dispatchers.IO without blocking MasterService
  6. Storage failures do not crash MasterService or block SMS forwarding
**Plans**: 2 plans

Plans:
- [x] 16-01-PLAN.md — Schema v7: Add destinationNumber to ForwardedMessage, DAO counter methods, Migration 6->7
- [x] 16-02-PLAN.md — Repository + integration: ForwardedMessageRepository, SmsCommandHandler integration, crash recovery

#### Phase 17: Device History UI
**Goal**: Users can view device list with active/removed sections and aggregate statistics
**Depends on**: Phase 16
**Requirements**: HIST-01, HIST-02, HIST-03, HIST-04, HIST-05, HIST-06, HIST-07
**Status**: Complete (2026-02-06)
**Success Criteria** (what must be TRUE):
  1. Device History screen is accessible from Quick Actions menu on HomeScreen
  2. Active devices section displays all paired devices with total sessions and message counts
  3. Removed devices section displays archived devices (collapsed by default)
  4. Device cards show last session date or "[Active]" badge for ongoing sessions
  5. Tapping active device navigates to Session History screen for that device
  6. Tapping removed device navigates to Archive Management screen for that device
**Plans**: 3 plans

Plans:
- [x] 17-01-PLAN.md — Data layer: DAO methods, repository wrappers, DeviceHistoryViewModel, navigation routes
- [x] 17-02-PLAN.md — UI components: DeviceHistoryScreen, DeviceHistoryCard, badges, loading/empty states
- [x] 17-03-PLAN.md — Navigation: Bottom sheet, removed device dialog, ArchiveManagement screen, NavGraph wiring

#### Phase 18: Session History & Message Detail
**Goal**: Users can view session list and drill down to message-level details
**Depends on**: Phase 17
**Requirements**: SESS-01, SESS-02, SESS-03, SESS-04, SESS-05, SESS-06, SESS-07, DETAIL-01, DETAIL-02, DETAIL-03, DETAIL-04, DETAIL-05
**Status**: Complete (2026-02-06)
**Plans**: 4 plans

Plans:
- [x] 18-01-PLAN.md — Data layer: Add Paging 3 dependency, DAO PagingSource queries, repository Pager wrappers, DateTimeFormatters utility
- [x] 18-02-PLAN.md — Session History UI: SessionHistoryViewModel with paging, SessionHistoryScreen with tabs, session/contact cards
- [x] 18-03-PLAN.md — Message Detail: MessageDetailBottomSheet with expandable rows, pagination, session metadata
- [x] 18-04-PLAN.md — Navigation: SessionHistory route, NavGraph integration, DeviceHistoryScreen connection, bottom sheet wiring

**Success Criteria** (what must be TRUE):
  1. Session History screen shows all sessions for selected device with pagination
  2. Session cards display start date, duration, and message count
  3. User can toggle between session view and contact view (grouped by sender)
  4. Session view is default on screen open (no persistence of toggle state per CONTEXT.md)
  5. Tapping session opens message detail bottom sheet
  6. Active sessions show real-time message count with "[Active]" badge
  7. Message detail bottom sheet displays sender number, timestamp, and content with pagination for large lists
  8. Messages are read-only (no delete/edit actions)
**Plans**: 4 plans

Plans:
- [x] 18-01-PLAN.md — Data layer: Add Paging 3 dependency, DAO PagingSource queries, repository Pager wrappers, DateTimeFormatters utility
- [x] 18-02-PLAN.md — Session History UI: SessionHistoryViewModel with paging, SessionHistoryScreen with tabs, session/contact cards
- [x] 18-03-PLAN.md — Message Detail: MessageDetailBottomSheet with expandable rows, pagination, session metadata
- [x] 18-04-PLAN.md — Navigation: SessionHistory route, NavGraph integration, DeviceHistoryScreen connection, bottom sheet wiring

#### Phase 19: Export Functionality
**Goal**: Users can export session/message history in multiple formats to external storage
**Depends on**: Phase 16
**Requirements**: EXP-01, EXP-02, EXP-03, EXP-04, EXP-05, EXP-06
**Status**: Complete (2026-02-06)
**Success Criteria** (what must be TRUE):
  1. User can export session and message data to CSV format with proper column headers
  2. User can export session and message data to JSON format with structured schema
  3. User can export session and message data to plain text format (human-readable)
  4. Export uses Storage Access Framework (ACTION_CREATE_DOCUMENT) for file creation
  5. Export works on API 29-35 without permission fragmentation or failures
  6. Export is accessible from both Archive Management and Session History screens
**Plans**: 7 plans

Plans:
- [x] 19-01-PLAN.md — Export Types & Formatters: ExportFormat enum, data classes, CSV/JSON/TXT formatters with proper escaping
- [x] 19-02-PLAN.md — Export Data Layer: DAO queries for bulk export, repository wrappers, ExportManager with data loading
- [x] 19-03-PLAN.md — Session History Export: ExportState, ExportFormatBottomSheet, SessionHistoryViewModel export methods, SAF integration
- [x] 19-04-PLAN.md — Archive Management Export: Export button and SAF integration for archived devices
- [x] 19-05-PLAN.md — Navigation Wiring: ExportManager injection into ViewModels via NavGraph
- [x] 19-06-PLAN.md — Single-Session Export: MessageDetailBottomSheet button, SessionCard long-press, single-session export logic
- [x] 19-07-PLAN.md — Device History Export: DeviceDetailBottomSheet export button, per-device export from Device History screen

#### Phase 20: Bidirectional Visibility Indicators
**Goal**: Home screen shows directional forwarding status with smart indicators
**Depends on**: Phase 16
**Requirements**: BIDIR-01, BIDIR-02, BIDIR-03, BIDIR-04, BIDIR-05, BIDIR-06, BIDIR-07, BIDIR-08
**Status**: Complete (2026-02-08)
**Success Criteria** (what must be TRUE):
  1. Home screen calculates active session status (forwarding TO, receiving FROM, bidirectional)
  2. Smart status indicator shows ↑ with count when user is forwarding TO other devices
  3. Smart status indicator shows ↓ with count when user is receiving FROM other devices
  4. Smart status indicator shows ⇅ with count for bidirectional sessions
  5. Status indicator only shows active directions (hides if count is zero)
  6. Tapping status indicator opens session breakdown bottom sheet
  7. Session breakdown groups sessions by direction (Forwarding To, Receiving From, Bidirectional)
  8. Paired devices list shows directional arrows next to each device (↑ forwarding, ↓ receiving, ⇅ bidirectional)
**Plans**: 4 plans

Plans:
- [x] 20-01-PLAN.md — Data models and ViewModel: Direction enum, DirectionalStatus data class, HomeViewModel directional calculation
- [x] 20-02-PLAN.md — Home screen indicators: DirectionalStatusCard composable with BadgedBox, semantic colors, integration
- [x] 20-03-PLAN.md — Session breakdown sheet: SessionBreakdownBottomSheet with grouped sessions, stop actions, NavGraph wiring
- [x] 20-04-PLAN.md — Device list directions: PairedDevicesViewModel session tracking, DeviceCard directional subtitle

#### Phase 21: History Retention Settings
**Goal**: Users can configure history retention and manually clean old data
**Depends on**: Phase 16
**Requirements**: RETENTION-01, RETENTION-02, RETENTION-03, RETENTION-04
**Status**: Complete (2026-02-08)
**Success Criteria** (what must be TRUE):
  1. Settings screen has history retention option with range 7-90 days or "Forever" (0)
  2. Default retention is 30 days for new installations
  3. Advanced Settings section has auto-cleanup toggle (Phase 22)
  4. Settings screen has "Clean up now" button for manual cleanup
  5. Manual cleanup deletes messages and sessions older than retention setting
**Plans**: 2 plans

Plans:
- [x] 21-01-PLAN.md — Data layer: Retention preference key/default, SettingsRepository Flow/setter, DAO cleanup queries, Repository cleanup method
- [x] 21-02-PLAN.md — ViewModel + UI: CleanupState sealed class, SettingsViewModel retention state/actions, "Data & Storage" section with dropdown and cleanup button

#### Phase 22: Auto-Cleanup with WorkManager
**Goal**: History cleanup runs automatically on schedule based on retention settings
**Depends on**: Phase 21
**Requirements**: RETENTION-05, RETENTION-06, RETENTION-07, RETENTION-08
**Success Criteria** (what must be TRUE):
  1. WorkManager schedules periodic cleanup task with 24-hour interval
  2. Cleanup job respects retention setting (deletes messages older than N days)
  3. WorkManager uses lenient constraints (battery not low only, no idle requirement)
  4. Last cleanup timestamp is displayed in Settings screen
  5. Auto-cleanup only runs when toggle is enabled in settings
**Plans**: TBD

Plans:
- [ ] 22-01: TBD

## Progress

| Phase | Milestone | Plans | Status | Completed |
|-------|-----------|-------|--------|-----------|
| 1. Play Store Submission | Play Store Launch | - | Complete | 2026-01-15 |
| 2. CI/CD Pipeline | Play Store Launch | - | Complete | 2026-01-15 |
| 3. Closed Testing Period | Play Store Launch | ad-hoc | Complete | 2026-02-01 |
| 4. Open Testing & Production | Play Store Launch | 0/2 | In Progress | - |
| 5. Bidirectional Pairing | v0.0.7-v0.0.10 | 2/2 | Complete | 2026-01-15 |
| 6. Pending State & Pairing UX | v0.0.7-v0.0.10 | 3/3 | Complete | 2026-01-15 |
| 7. UI/UX Polish | v0.0.7-v0.0.10 | 2/2 | Complete | 2026-01-15 |
| 8. CI/CD Pipeline Optimization | v0.0.60-v0.0.61 | 3/3 | Complete | 2026-01-16 |
| 9. SmsReceiver Unit Testing | v0.0.62 | 2/2 | Complete | 2026-01-18 |
| 10. Integration Test Infrastructure | v0.0.62 | 3/3 | Complete | 2026-01-18 |
| 11. End-to-End Flow Tests | v0.0.62 | 3/3 | Complete | 2026-01-18 |
| 12. Settings Data Layer | v0.0.63 | 1/1 | Complete | 2026-01-18 |
| 13. Settings Screen & Main Settings | v0.0.63 | 3/3 | Complete | 2026-01-19 |
| 14. Advanced Settings & Service Integration | v0.0.63 | 3/3 | Complete | 2026-01-19 |
| 15. Database Foundation & Migration | v0.0.64 | 2/2 | Complete | 2026-02-05 |
| 16. Message Storage Integration | v0.0.64 | 2/2 | Complete | 2026-02-06 |
| 17. Device History UI | v0.0.64 | 3/3 | Complete | 2026-02-06 |
| 18. Session History & Message Detail | v0.0.64 | 4/4 | Complete | 2026-02-06 |
| 19. Export Functionality | v0.0.64 | 7/7 | Complete | 2026-02-06 |
| 20. Bidirectional Visibility Indicators | v0.0.64 | 4/4 | Complete | 2026-02-08 |
| 21. History Retention Settings | v0.0.64 | 2/2 | Complete | 2026-02-08 |
| 22. Auto-Cleanup with WorkManager | v0.0.64 | 0/TBD | Not started | - |
