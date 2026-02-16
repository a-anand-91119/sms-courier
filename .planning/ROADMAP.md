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
- [**v0.0.64 Device Management & Visibility**](milestones/v0.0.64-device-management/ROADMAP.md) - Phases 15-23 (shipped 2026-02-09)
- **v0.0.65 UAT Fixes** - Phases 24-28 (in progress)

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

<details>
<summary>v0.0.64 Device Management & Visibility (Phases 15-23) -- SHIPPED 2026-02-09</summary>

- [x] Phase 15: Database Foundation & Migration (2/2 plans) -- completed 2026-02-05
- [x] Phase 16: Message Storage Integration (2/2 plans) -- completed 2026-02-06
- [x] Phase 17: Device History UI (4/4 plans) -- completed 2026-02-06
- [x] Phase 18: Session History & Message Detail (4/4 plans) -- completed 2026-02-06
- [x] Phase 19: Export Functionality (7/7 plans) -- completed 2026-02-06
- [x] Phase 20: Bidirectional Visibility Indicators (4/4 plans) -- completed 2026-02-08
- [x] Phase 21: History Retention Settings (2/2 plans) -- completed 2026-02-08
- [x] Phase 22: Auto-Cleanup with WorkManager (2/2 plans) -- completed 2026-02-08
- [x] Phase 23: UAT Bug Fixes (3/3 plans) -- completed 2026-02-08

**Key accomplishments:**
- ForwardedMessage table with FK CASCADE for automatic message cleanup
- Transaction-based message storage with real-time counters
- Device History screen with active/removed sections and statistics
- Session History with Paging 3 and session/contact toggle
- Export functionality (CSV/JSON/TXT) via Storage Access Framework
- Bidirectional visibility indicators on Home screen
- Configurable history retention with auto-cleanup via WorkManager

See [milestones/v0.0.64-device-management/ROADMAP.md](milestones/v0.0.64-device-management/ROADMAP.md) for full details.

</details>

### v0.0.65 UAT Fixes (In Progress)

**Milestone Goal:** Fix UAT issues discovered during testing -- forwarding direction labels, session visibility for both devices, notification actions, device history bugs, and icon corrections. Test-driven: write failing tests first, then fix.

- [x] **Phase 24: Reproduce UAT Issues** - Write failing tests for all 7 reported UAT issues (completed 2026-02-16)
- [x] **Phase 25: Home Screen Fixes** - Fix forwarding direction labels and active badge layout (completed 2026-02-16)
- [ ] **Phase 26: Session Management Fixes** - Both devices see/stop sessions with SMS notification
- [ ] **Phase 27: Device History Fixes** - Archived device history visibility and explicit archive action
- [ ] **Phase 28: UI & Notification Quick Fixes** - Export icon swap and notification approve action

## Phase Details

### Phase 24: Reproduce UAT Issues

**Goal**: Every reported UAT issue has a failing test that proves the bug exists before any code is changed
**Depends on**: Nothing (first phase of milestone)
**Requirements**: HOME-01, HOME-02, SESS-01, SESS-02, SESS-03, DEVH-01, DEVH-02, HIST-01, NOTF-01
**Success Criteria** (what must be TRUE):
  1. A test exists that asserts TARGET device status text contains "Forwarding to" (not "Receiving") and currently FAILS (HOME-01)
  2. Tests exist that assert both SOURCE and TARGET devices can see and stop active sessions, and these tests currently FAIL (SESS-01, SESS-02)
  3. A test exists that asserts stopping a session triggers an SMS notification to the other device, and it currently FAILS (SESS-03)
  4. A test exists that asserts querying history for a removed device returns data (not empty), and it currently FAILS (DEVH-01)
  5. A test exists that asserts the archive action on a paired device works, and it currently FAILS (DEVH-02)
  6. A test exists that asserts the notification approve action triggers pairing approval, and it currently FAILS (NOTF-01)

**Notes on non-testable issues:**
- HOME-02 (active badge layout on small screens): UI layout issue -- not reproducible with unit/integration tests. Will be verified manually in Phase 25.
- HIST-01 (export icon vs share icon): Icon resource reference issue -- not reproducible with unit/integration tests. Will be verified manually in Phase 28.

**Status**: Complete (2026-02-16)
**Plans:** 3/3 complete

Plans:
- [x] 24-01-PLAN.md — HOME-01 and SESS-03 failing tests (direction label + SMS stop notification)
- [x] 24-02-PLAN.md — DEVH-01 and DEVH-02 failing tests (archived device history + archive action)
- [x] 24-03-PLAN.md — SESS-01, SESS-02, and NOTF-01 failing tests (session visibility/stop + notification approve)

### Phase 25: Home Screen Fixes

**Goal**: Home screen correctly reflects forwarding direction and renders cleanly on all device sizes
**Depends on**: Phase 24 (failing tests must exist first)
**Requirements**: HOME-01, HOME-02
**Success Criteria** (what must be TRUE):
  1. TARGET device Home screen shows "Forwarding to: [phone number(s)]" when a forwarding session is active
  2. SOURCE device Home screen continues to show "Receiving from: [phone number]" correctly (no regression)
  3. Active session badge on Home screen renders without text wrapping or overflow on a small screen device (e.g., 320dp width)
  4. Phase 24 test for HOME-01 now PASSES (no code change to the test)
**Status**: Complete (2026-02-16)
**Plans:** 2/2 complete

Plans:
- [x] 25-01-PLAN.md — Fix Direction enum mapping in data/ViewModel layer (HOME-01 root cause)
- [x] 25-02-PLAN.md — Update UI labels, terminology consistency, and responsive layout (HOME-01 labels + HOME-02)

### Phase 26: Session Management Fixes

**Goal**: Both SOURCE and TARGET devices have full visibility into active sessions and can stop them with cross-device notification
**Depends on**: Phase 24 (failing tests must exist first)
**Requirements**: SESS-01, SESS-02, SESS-03
**Success Criteria** (what must be TRUE):
  1. SOURCE device can see all its active forwarding sessions (not just TARGET)
  2. TARGET device can see all its active forwarding sessions (not just SOURCE)
  3. Either device (SOURCE or TARGET) can tap "Stop" on an active session and the session ends
  4. When one device stops a session, the other device receives an SMS notification that the session was stopped
  5. Session status updates to "Stopped" on both devices after either side stops it
  6. Phase 24 tests for SESS-01, SESS-02, SESS-03 now PASS (no code changes to the tests)
**Plans:** 2 plans

Plans:
- [ ] 26-01-PLAN.md — Fix ViewModel (SESS-01/02/03), handleStopForward, and session stopped notification
- [ ] 26-02-PLAN.md — Update ForwardingControlScreen UI for combined device list and improved stop dialog

### Phase 27: Device History Fixes

**Goal**: Users can view history for removed devices and explicitly archive paired devices
**Depends on**: Phase 24 (failing tests must exist first)
**Requirements**: DEVH-01, DEVH-02
**Success Criteria** (what must be TRUE):
  1. Tapping a removed/archived device in Device History shows its session history and message data (not empty)
  2. User can explicitly archive an active paired device from the device list (without needing to unpair first, or via a clear archive action)
  3. Archived device appears in the "Removed" section of Device History with its history preserved
  4. Phase 24 tests for DEVH-01, DEVH-02 now PASS (no code changes to the tests)
**Plans**: TBD

Plans:
- [ ] 27-01: TBD

### Phase 28: UI & Notification Quick Fixes

**Goal**: Correct the export icon in Session History and fix the notification approve action
**Depends on**: Phase 24 (failing tests must exist first)
**Requirements**: HIST-01, NOTF-01
**Success Criteria** (what must be TRUE):
  1. Session History screen shows an export/download icon (not a share icon) for the export action
  2. Tapping "Approve" on a pairing request notification actually approves the pairing (device appears as paired)
  3. After tapping "Approve" on the notification, the notification is dismissed and the pairing is reflected in the app UI
  4. Phase 24 test for NOTF-01 now PASSES (no code change to the test)
**Plans**: TBD

Plans:
- [ ] 28-01: TBD

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
| 17. Device History UI | v0.0.64 | 4/4 | Complete | 2026-02-06 |
| 18. Session History & Message Detail | v0.0.64 | 4/4 | Complete | 2026-02-06 |
| 19. Export Functionality | v0.0.64 | 7/7 | Complete | 2026-02-06 |
| 20. Bidirectional Visibility Indicators | v0.0.64 | 4/4 | Complete | 2026-02-08 |
| 21. History Retention Settings | v0.0.64 | 2/2 | Complete | 2026-02-08 |
| 22. Auto-Cleanup with WorkManager | v0.0.64 | 2/2 | Complete | 2026-02-08 |
| 23. UAT Bug Fixes | v0.0.64 | 3/3 | Complete | 2026-02-08 |
| 24. Reproduce UAT Issues | v0.0.65 | 3/3 | Complete | 2026-02-16 |
| 25. Home Screen Fixes | v0.0.65 | 2/2 | Complete | 2026-02-16 |
| 26. Session Management Fixes | v0.0.65 | 0/2 | Not started | - |
| 27. Device History Fixes | v0.0.65 | 0/TBD | Not started | - |
| 28. UI & Notification Quick Fixes | v0.0.65 | 0/TBD | Not started | - |
