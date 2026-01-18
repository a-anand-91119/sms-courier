# Roadmap: SMS Courier

## Overview

SMS Courier is an Android app for forwarding SMS messages between two paired devices using secure SMS-based commands. The roadmap focuses on completing Play Store requirements, finalizing the automated CI/CD deployment pipeline, conducting thorough testing, and launching publicly on Google Play Store.

## Domain Expertise

None (Android app development patterns already established in codebase)

## Milestones

- [**v0.0.7-v0.0.10 Feature Improvements**](milestones/v0.1-ROADMAP.md) - Phases 5-7 (shipped 2026-01-15)
- **v0.0.60-v0.0.61 CI/CD Optimizations** - Phase 8 (shipped 2026-01-16)
- [**v0.0.62 Testing**](milestones/v0.0.62-ROADMAP.md) - Phases 9-11 (shipped 2026-01-18)
- **Play Store Launch** - Phases 1-4 (in progress)

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

### Play Store Launch (In Progress)

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

#### Phase 3: Pre-Launch Testing

**Goal**: Dogfood the app daily while waiting for beta eligibility (12 testers + 14 days)
**Depends on**: Phase 2 (complete)
**Status**: Ongoing monitoring (no structured plans)
**Context**: [03-CONTEXT.md](phases/03-prelaunch-testing/03-CONTEXT.md)

Key focus:
- Daily personal use of the app
- Fix reliability issues - no missed messages
- Polish UX rough edges
- No new features or major refactors

**Approach**: Ad-hoc issue-driven. This phase uses reactive fixes rather than predefined plans. Issues discovered during dogfooding get addressed directly without formal PLAN.md files.

#### Phase 4: Public Release

**Goal**: Launch SMS Courier publicly on Google Play Store
**Depends on**: Phase 3 (testing complete)
**Research**: Unlikely (standard release process)
**Status**: Not Started
**Plans**: TBD

Key deliverables:
- Promote from beta to production track
- Monitor crash reports and analytics
- Respond to user reviews
- Prepare user documentation and FAQs
- Plan v1.2 improvements based on initial feedback

Plans:
- [ ] 04-01: TBD

## Progress

| Phase | Milestone | Plans | Status | Completed |
|-------|-----------|-------|--------|-----------|
| 1. Play Store Submission | Play Store Launch | - | Complete | 2026-01-15 |
| 2. CI/CD Pipeline | Play Store Launch | - | Complete | 2026-01-15 |
| 3. Pre-Launch Testing | Play Store Launch | ad-hoc | In Progress | - |
| 4. Public Release | Play Store Launch | 0/? | Not Started | - |
| 5. Bidirectional Pairing | v0.0.7-v0.0.10 | 2/2 | Complete | 2026-01-15 |
| 6. Pending State & Pairing UX | v0.0.7-v0.0.10 | 3/3 | Complete | 2026-01-15 |
| 7. UI/UX Polish | v0.0.7-v0.0.10 | 2/2 | Complete | 2026-01-15 |
| 8. CI/CD Pipeline Optimization | v0.0.60-v0.0.61 | 3/3 | Complete | 2026-01-16 |
| 9. SmsReceiver Unit Testing | v0.0.62 | 2/2 | Complete | 2026-01-18 |
| 10. Integration Test Infrastructure | v0.0.62 | 3/3 | Complete | 2026-01-18 |
| 11. End-to-End Flow Tests | v0.0.62 | 3/3 | Complete | 2026-01-18 |
