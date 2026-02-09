# Project Milestones: SMS Courier

## v0.0.64 Device Management & Visibility (Shipped: 2026-02-09)

**Delivered:** Enhanced device management with session history, message-level storage, bidirectional visibility indicators, export functionality, and configurable history retention with auto-cleanup.

**Phases completed:** 15-23 (29 plans total)

**Key accomplishments:**
- Database foundation with ForwardedMessage table, soft delete for devices, and foreign key CASCADE
- Transaction-based message storage with real-time counter updates and non-blocking failure handling
- Device History UI with active/removed sections, statistics badges, and navigation to Session History
- Session History with Paging 3, session/contact toggle, and MessageDetailBottomSheet
- Export functionality (CSV/JSON/TXT) via Storage Access Framework for API 29-35 compatibility
- Bidirectional visibility indicators (↑↓⇅) on Home screen with session breakdown bottom sheet
- Configurable history retention (7-90 days or Forever) with manual cleanup button
- Auto-cleanup with WorkManager (7-day periodic schedule with lenient constraints)
- UAT bug fixes for combinedClickable crashes and unpair button functionality

**Stats:**
- 51 files modified
- +5,920 lines of Kotlin (net +5,525)
- 22,261 total lines of code
- 9 phases, 29 plans executed
- 5 days from start to ship (2026-02-05 → 2026-02-09)
- 161 commits

**Git range:** Phase 15 → Phase 23

**What's next:** Next milestone TBD

---

## v0.0.7–v0.0.10 Feature Improvements (Shipped: 2026-01-15)

**Delivered:** Bidirectional pairing architecture, improved pending state UX with rate limiting and contextual menus, and UI polish for production readiness.

**Phases completed:** 5-7 (6 main plans + 3 fix plans)

**Key accomplishments:**
- Bidirectional pairing: Same phone number can be SOURCE and TARGET simultaneously (composite PK architecture)
- Rate-limited resend: Max 5 attempts with 1-minute cooldown for pairing requests
- Contextual UX: Dropdown menus on long-press with status-aware actions
- Notification deep-linking: Approve button navigates to pairing screen
- Smart UNPAIR: Only sends SMS for approved devices (saves SMS charges)
- Service toggle loading state with polling mechanism

**Stats:**
- 30+ files created/modified
- Database migrations 3→4→5
- 228 unit tests passing
- 1 day from start to ship

**Git range:** v0.0.7 to v0.0.10

---

## v0.0.60–v0.0.61 CI/CD Optimizations (Complete: 2026-01-16)

**Delivered:** Optimized CI/CD pipeline with upload-only lanes, proper changelog generation, and GitLab release automation.

**Phases completed:** 8 (3 plans)

**Key accomplishments:**
- Upload-only Fastlane lanes that skip unnecessary rebuilds
- Integrated fastlane-plugin-changelog for proper changelog generation
- GitLab release automation with APK/AAB artifact attachment

**Git range:** v0.0.60 to v0.0.61

---

## v0.0.62 Testing (Shipped: 2026-01-18)

**Delivered:** Comprehensive automated testing infrastructure with SmsReceiver unit tests, integration test framework, and end-to-end flow tests for pairing, forwarding, and security scenarios.

**Phases completed:** 9-11 (8 plans)

**Key accomplishments:**
- SmsReceiver unit tests (28 tests) with Robolectric covering validation, command routing, and forwarding logic
- Integration test infrastructure: IntegrationTestBase, CapturingSmsSender, ScenarioBuilders
- Pairing flow integration tests (12 tests) covering request/approve/reject/unpair/bidirectional
- Forwarding flow integration tests (13 tests) covering auth/start/forward/stop
- Security integration tests (15 tests) covering lockout, failed auth, idempotency, error handling
- Bug discovered and fixed during testing: NPE in SmsReceiver.kt

**Stats:**
- 37 files created/modified
- ~2,400 lines of test code added
- 75 new tests (28 unit + 47 integration)
- 301 total tests passing
- 1 day from start to ship

**Git range:** v0.0.62

---

## v0.0.63 Settings (Shipped: 2026-01-19)

**Delivered:** Full settings screen with DataStore persistence, main preferences (notification, duration, theme), permission status, about section, and advanced security settings with reactive service integration.

**Phases completed:** 12-14 (7 plans)

**Key accomplishments:**
- DataStore-backed settings persistence with SettingsRepository singleton
- 9 typed Flow properties for reactive settings access
- Settings screen with gear icon navigation from HomeScreen
- Main settings: notification persistence toggle, default forwarding duration, theme selection
- Theme observation at app level for immediate changes
- Permission status with Fix action navigating to app settings
- About section with version, privacy policy, support links
- Collapsible Advanced section with 6 security settings
- SecurityManager refactored with SettingsRepository injection
- MasterService observes authRequestTimeoutMinutes via Flow
- PairedDevicesViewModel uses configurable pairing limits

**Stats:**
- 3 phases, 7 plans executed
- 41 SettingsRepositoryTest cases
- 343 total tests passing
- 2 days from start to ship

**Git range:** v0.0.63

---

_See [milestones/](milestones/) for archived milestone details._
