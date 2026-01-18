# Roadmap: SMS Courier

## Overview

SMS Courier is an Android app for forwarding SMS messages between two paired devices using secure SMS-based commands. The roadmap focuses on completing Play Store requirements, finalizing the automated CI/CD deployment pipeline, conducting thorough testing, and launching publicly on Google Play Store.

## Domain Expertise

None (Android app development patterns already established in codebase)

## Milestones

- [**v0.0.7-v0.0.10 Feature Improvements**](milestones/v0.1-ROADMAP.md) - Phases 5-7 (shipped 2026-01-15)
- **v0.0.60-v0.0.61 CI/CD Optimizations** - Phase 8 (complete)
- **v0.0.62 Testing** - Phases 9-11 (complete)
- **Play Store Launch** - Phases 1-4 (blocked on testing)

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

## Phases

### v0.0.62 Testing (Complete)

**Milestone Goal:** Establish comprehensive automated testing, focusing on the critical SmsReceiver component and integration test infrastructure.

#### Phase 9: SmsReceiver Unit Testing

**Goal**: Create comprehensive unit tests for SmsReceiver, the critical untested component
**Depends on**: None (standalone testing work)
**Research**: Unlikely (testing patterns established in codebase)
**Status**: Complete (2026-01-18)
**Plans**: 2/2

Key deliverables:
- SmsReceiver unit tests with Robolectric
- SMS intent simulation for command routing
- Coverage for SMSC command interception
- Coverage for regular SMS forwarding logic

Plans:
- [x] 09-01: Input validation and SMSC command routing tests
- [x] 09-02: Regular SMS forwarding logic tests

#### Phase 10: Integration Test Infrastructure

**Goal**: Set up infrastructure for integration testing of multi-component flows
**Depends on**: Phase 9
**Research**: Complete (10-RESEARCH.md)
**Status**: Complete (2026-01-18)
**Plans**: 3/3

Key deliverables:
- Test database setup with in-memory Room
- CapturingSmsSender for controlled testing (mock SMS output)
- Test fixtures and scenario builders for pairing/forwarding flows
- CI integration for integration test stage

Plans:
- [x] 10-01: Core infrastructure (IntegrationTestBase, CapturingSmsSender, annotation)
- [x] 10-02: Scenario builders for test states
- [x] 10-03: Infrastructure validation test and CI configuration

#### Phase 11: End-to-End Flow Tests

**Goal**: Create integration tests for complete pairing and forwarding flows
**Depends on**: Phase 10
**Research**: Complete (11-RESEARCH.md)
**Status**: Complete (2026-01-18)
**Plans**: 3/3

Key deliverables:
- Full pairing flow test (request -> approve -> reject -> unpair)
- Full forwarding flow test (auth -> start -> forward -> stop)
- Security and error scenario coverage (lockout, malformed commands, idempotency)

Plans:
- [x] 11-01: Pairing flow integration tests (12 tests)
- [x] 11-02: Forwarding flow integration tests (13 tests)
- [x] 11-03: Security and error handling tests (15 tests)

---

### Play Store Launch (Blocked on Testing)

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
| 3. Pre-Launch Testing | Play Store Launch | ad-hoc | Blocked | - |
| 4. Public Release | Play Store Launch | 0/? | Not Started | - |
| 5. Bidirectional Pairing | v0.0.7-v0.0.10 | 2/2 | Complete | 2026-01-15 |
| 6. Pending State & Pairing UX | v0.0.7-v0.0.10 | 3/3 | Complete | 2026-01-15 |
| 7. UI/UX Polish | v0.0.7-v0.0.10 | 2/2 | Complete | 2026-01-15 |
| 8. CI/CD Pipeline Optimization | v0.0.60-v0.0.61 | 3/3 | Complete | 2026-01-16 |
| 9. SmsReceiver Unit Testing | v0.0.62 | 2/2 | Complete | 2026-01-18 |
| 10. Integration Test Infrastructure | v0.0.62 | 3/3 | Complete | 2026-01-18 |
| 11. End-to-End Flow Tests | v0.0.62 | 3/3 | Complete | 2026-01-18 |
