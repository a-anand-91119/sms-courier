# Milestone v0.0.64: Device Management & Visibility

**Status:** ✅ SHIPPED 2026-02-09
**Phases:** 15-23
**Total Plans:** 29

## Overview

Enhanced device management with session history, message-level storage, bidirectional forwarding visibility, and configurable history retention. This milestone transformed SMS Courier from a simple forwarding app into a comprehensive communication management tool with full history tracking and export capabilities.

## Phases

### Phase 15: Database Foundation & Migration

**Goal:** Database schema supports message-level storage and device archiving with validated migration
**Depends on:** Phase 14 (Settings shipped)
**Plans:** 2 plans

Plans:
- [x] 15-01: Schema v6: ForwardedMessage entity, PairedDevice soft delete columns, ForwardingSession tracking columns, Migration 5->6
- [x] 15-02: Migration testing: MigrationTestHelper tests for data preservation, CASCADE delete, and index verification

**Key accomplishments:**
- ForwardedMessage table with foreign key CASCADE
- Soft delete pattern for PairedDevice (isArchived, archivedAt, archivalInitiatedBy)
- Aggregate statistics columns (totalSessions, totalMessagesForwarded)
- Schema v6 and v7 migrations with room-testing infrastructure

### Phase 16: Message Storage Integration

**Goal:** Messages are stored during forwarding with real-time session statistics
**Depends on:** Phase 15
**Plans:** 2 plans

Plans:
- [x] 16-01: Schema v7: Add destinationNumber to ForwardedMessage, DAO counter methods, Migration 6->7
- [x] 16-02: Repository + integration: ForwardedMessageRepository, SmsCommandHandler integration, crash recovery

**Key accomplishments:**
- Transaction-based message storage with atomic counter updates
- TARGET device stores full message content; SOURCE stores metadata only
- Non-blocking storage failures (graceful degradation)
- Real-time session statistics updates

### Phase 17: Device History UI

**Goal:** Users can view device list with active/removed sections and aggregate statistics
**Depends on:** Phase 16
**Plans:** 4 plans

Plans:
- [x] 17-01: Data layer: DAO methods, repository wrappers, DeviceHistoryViewModel, navigation routes
- [x] 17-02: UI components: DeviceHistoryScreen, DeviceHistoryCard, badges, loading/empty states
- [x] 17-03: Navigation: Bottom sheet, removed device dialog, ArchiveManagement screen, NavGraph wiring
- [x] 17-04: Additional UI polish and navigation fixes

**Key accomplishments:**
- Device History screen accessible from Quick Actions
- Active/removed device sections with statistics
- DeviceDetailBottomSheet for device interactions
- ArchiveManagementScreen for removed device data

### Phase 18: Session History & Message Detail

**Goal:** Users can view session list and drill down to message-level details
**Depends on:** Phase 17
**Plans:** 4 plans

Plans:
- [x] 18-01: Data layer: Paging 3 dependency, DAO PagingSource queries, repository Pager wrappers
- [x] 18-02: Session History UI: SessionHistoryViewModel with paging, SessionHistoryScreen with tabs
- [x] 18-03: Message Detail: MessageDetailBottomSheet with expandable rows, pagination
- [x] 18-04: Navigation: SessionHistory route, NavGraph integration, bottom sheet wiring

**Key accomplishments:**
- Paging 3 integration for scalable message lists
- Session/contact view toggle
- MessageDetailBottomSheet with expandable message rows
- Real-time active session indicators

### Phase 19: Export Functionality

**Goal:** Users can export session/message history in multiple formats
**Depends on:** Phase 16
**Plans:** 7 plans

Plans:
- [x] 19-01: Export Types & Formatters: ExportFormat enum, CSV/JSON/TXT formatters
- [x] 19-02: Export Data Layer: DAO queries for bulk export, ExportManager
- [x] 19-03: Session History Export: ExportState, ExportFormatBottomSheet, SAF integration
- [x] 19-04: Archive Management Export: Export button for archived devices
- [x] 19-05: Navigation Wiring: ExportManager injection into ViewModels
- [x] 19-06: Single-Session Export: MessageDetailBottomSheet button, SessionCard long-press
- [x] 19-07: Device History Export: DeviceDetailBottomSheet export button

**Key accomplishments:**
- CSV, JSON, and TXT export formats
- Storage Access Framework integration (API 29-35 compatible)
- Multiple export entry points (device, session, single-session)
- ExportFormatBottomSheet with metadata toggle

### Phase 20: Bidirectional Visibility Indicators

**Goal:** Home screen shows directional forwarding status with smart indicators
**Depends on:** Phase 16
**Plans:** 4 plans

Plans:
- [x] 20-01: Data models and ViewModel: Direction enum, DirectionalStatus data class
- [x] 20-02: Home screen indicators: DirectionalStatusCard with BadgedBox
- [x] 20-03: Session breakdown sheet: SessionBreakdownBottomSheet with grouped sessions
- [x] 20-04: Device list directions: PairedDevicesViewModel session tracking, DeviceCard arrows

**Key accomplishments:**
- ↑ (forwarding TO), ↓ (receiving FROM), ⇅ (bidirectional) indicators
- DirectionalStatusCard on Home screen
- SessionBreakdownBottomSheet with stop actions
- Device cards show directional status

### Phase 21: History Retention Settings

**Goal:** Users can configure history retention and manually clean old data
**Depends on:** Phase 16
**Plans:** 2 plans

Plans:
- [x] 21-01: Data layer: Retention preference, SettingsRepository, DAO cleanup queries
- [x] 21-02: ViewModel + UI: CleanupState, retention dropdown, cleanup button

**Key accomplishments:**
- History retention option (7-90 days or Forever)
- Default 30-day retention
- Manual "Clean up now" button
- CleanupResult feedback with counts

### Phase 22: Auto-Cleanup with WorkManager

**Goal:** History cleanup runs automatically on schedule based on retention settings
**Depends on:** Phase 21
**Plans:** 2 plans

Plans:
- [x] 22-01: Data layer + CleanupWorker: Preference keys, CleanupWorker, WorkManagerHelper
- [x] 22-02: Settings UI + App Launch: Toggle, last cleaned display, scheduling

**Key accomplishments:**
- WorkManager periodic cleanup (7-day interval)
- Battery-not-low constraint for efficiency
- Auto-cleanup toggle (hidden when retention is Forever)
- Last cleanup timestamp display

### Phase 23: UAT Bug Fixes

**Goal:** Fix critical bugs found during manual verification
**Depends on:** Phase 22
**Plans:** 3 plans

Plans:
- [x] 23-01: Fix combinedClickable crashes in PairedDevicesScreen and SessionHistoryComponents
- [x] 23-02: Fix unpair button: confirmation dialog and archiveDevice wiring
- [x] 23-03: Consolidate active forwarding UI into DirectionalStatusCard

**Key accomplishments:**
- Fixed combinedClickable crash (explicit interactionSource/indication)
- Unpair confirmation dialog with active session warning
- Consolidated active forwarding display

---

## Milestone Summary

**Key Decisions:**
- Soft delete pattern for PairedDevice (preserves history after unpair)
- Foreign key CASCADE (automatic message cleanup when session deleted)
- Paging 3 for message lists (scalability)
- Storage Access Framework for export (cross-API compatibility)
- Non-blocking storage failures (graceful degradation)
- Auto-archive on unpair (ARCH-05 simplification)

**Issues Resolved:**
- Message history foundation complete
- Device management fully operational
- Export functionality working across API levels
- Directional visibility indicators on Home screen

**Issues Deferred:**
- Contact name lookup (ENH-01) - deferred for user demand gauge
- Message content encryption (SEC-02) - deferred unless compliance required
- Export date range selection (ADV-01) - deferred to future milestone

**Technical Debt Incurred:**
- ARCH-05: Auto-archives without choice dialog (documented UX simplification)
- Dual message count columns in ForwardingSession (legacy compatibility)

---

_For current project status, see .planning/ROADMAP.md_
