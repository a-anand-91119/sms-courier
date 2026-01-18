# Roadmap: SMS Courier

## Overview

SMS Courier is an Android app for forwarding SMS messages between two paired devices using secure SMS-based commands. The roadmap focuses on completing Play Store requirements, finalizing the automated CI/CD deployment pipeline, conducting thorough testing, and launching publicly on Google Play Store.

## Domain Expertise

None (Android app development patterns already established in codebase)

## Milestones

- [**v0.0.7-v0.0.10 Feature Improvements**](milestones/v0.1-ROADMAP.md) - Phases 5-7 (shipped 2026-01-15)
- **v0.0.60-v0.0.61 CI/CD Optimizations** - Phase 8 (shipped 2026-01-16)
- [**v0.0.62 Testing**](milestones/v0.0.62-ROADMAP.md) - Phases 9-11 (shipped 2026-01-18)
- [**v0.0.63 Settings**](milestones/v0.0.63-settings/ROADMAP.md) - Phases 12-14 (in progress)
- **Play Store Launch** - Phases 1-4 (parallel)

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

## Phases

### v0.0.63 Settings (In Progress)

**Milestone Goal:** Add settings screen with user-configurable preferences for notifications, service behavior, security, and app appearance.

See [milestones/v0.0.63-settings/ROADMAP.md](milestones/v0.0.63-settings/ROADMAP.md) for full phase details.

#### Phase 12: Settings Data Layer

**Goal**: Settings persist reliably and are accessible via typed Flows throughout the app
**Depends on**: Nothing (first phase of milestone)
**Requirements**: INFRA-01, INFRA-02
**Plans:** 1 plan
**Status**: Complete (2026-01-18)

Plans:
- [x] 12-01-PLAN.md — DataStore setup and SettingsRepository implementation

#### Phase 13: Settings Screen & Main Settings

**Goal**: Users can access and modify main settings and view app information
**Depends on**: Phase 12
**Requirements**: MAIN-01, MAIN-02, MAIN-03, MAIN-04, INFO-01, INFO-02
**Status**: Not Started

Plans:
- [ ] 13-01: Settings screen navigation and layout
- [ ] 13-02: Main settings preferences (notification, duration, theme)
- [ ] 13-03: Information sections (permissions, about)

#### Phase 14: Advanced Settings & Service Integration

**Goal**: Users can configure security parameters and services react to settings changes
**Depends on**: Phase 13
**Requirements**: ADV-01, ADV-02, ADV-03, ADV-04, ADV-05, ADV-06, INFRA-03, INFRA-04
**Status**: Not Started

Plans:
- [ ] 14-01: Advanced settings UI (security configuration)
- [ ] 14-02: Service integration (MasterService observes settings)
- [ ] 14-03: SecurityManager refactor (inject settings, remove hardcoded constants)

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
**Status**: In Progress (12 testers enrolled, waiting 14 days)

**Play Store Status (as of 2026-01-18):**
- App approved for closed testing
- 12 testers enrolled (requirement met)
- 14-day waiting period in progress
- After 14 days: Open testing (beta) access granted

Key focus:
- Daily personal use of the app
- Fix reliability issues - no missed messages
- Polish UX rough edges
- No new features or major refactors

**Approach**: Ad-hoc issue-driven. Issues discovered during dogfooding get addressed directly without formal PLAN.md files.

#### Phase 4: Open Testing & Production Release

**Goal**: Promote to open testing (beta), then to production
**Depends on**: Phase 3 (14-day closed testing complete)
**Status**: Not Started
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

## Progress

| Phase | Milestone | Plans | Status | Completed |
|-------|-----------|-------|--------|-----------|
| 1. Play Store Submission | Play Store Launch | - | Complete | 2026-01-15 |
| 2. CI/CD Pipeline | Play Store Launch | - | Complete | 2026-01-15 |
| 3. Closed Testing Period | Play Store Launch | ad-hoc | In Progress | - |
| 4. Open Testing & Production | Play Store Launch | 0/2 | Not Started | - |
| 5. Bidirectional Pairing | v0.0.7-v0.0.10 | 2/2 | Complete | 2026-01-15 |
| 6. Pending State & Pairing UX | v0.0.7-v0.0.10 | 3/3 | Complete | 2026-01-15 |
| 7. UI/UX Polish | v0.0.7-v0.0.10 | 2/2 | Complete | 2026-01-15 |
| 8. CI/CD Pipeline Optimization | v0.0.60-v0.0.61 | 3/3 | Complete | 2026-01-16 |
| 9. SmsReceiver Unit Testing | v0.0.62 | 2/2 | Complete | 2026-01-18 |
| 10. Integration Test Infrastructure | v0.0.62 | 3/3 | Complete | 2026-01-18 |
| 11. End-to-End Flow Tests | v0.0.62 | 3/3 | Complete | 2026-01-18 |
| 12. Settings Data Layer | v0.0.63 | 1/1 | Complete | 2026-01-18 |
| 13. Settings Screen & Main Settings | v0.0.63 | 0/3 | Not Started | - |
| 14. Advanced Settings & Service Integration | v0.0.63 | 0/3 | Not Started | - |
