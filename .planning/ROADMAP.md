# Roadmap: SMS Courier

## Overview

SMS Courier is an Android app for forwarding SMS messages between two paired devices using secure SMS-based commands. The v1.0 milestone focuses on completing Play Store requirements, finalizing the automated CI/CD deployment pipeline, conducting thorough testing across internal/alpha/beta tracks, and ultimately launching publicly on Google Play Store.

## Domain Expertise

None (Android app development patterns already established in codebase)

## Milestones

- 🚧 **v1.0 Play Store Launch** - Phases 1-4 (in progress)
- ✅ [**v1.1 Feature Improvements**](milestones/v1.1-ROADMAP.md) - Phases 5-7 (shipped 2026-01-15)

## Completed Milestones

<details>
<summary>✅ v1.1 Feature Improvements (Phases 5-7) — SHIPPED 2026-01-15</summary>

- [x] Phase 5: Bidirectional Pairing Architecture — completed 2026-01-15
- [x] Phase 6: Pending State & Pairing UX (3/3 plans) — completed 2026-01-15
- [x] Phase 7: UI/UX Polish (1/1 plan + fix) — completed 2026-01-15

See [milestones/v1.1-ROADMAP.md](milestones/v1.1-ROADMAP.md) for full details.

</details>

## Phases

### 🚧 v1.0 Play Store Launch (In Progress)

**Milestone Goal:** Complete all Play Store requirements, test deployment pipeline, conduct multi-track testing, and successfully launch SMS Courier on Google Play Store.

#### Phase 1: Play Store Submission Requirements ✅

**Goal**: Complete all Google Play Console requirements for app submission
**Depends on**: Nothing (first phase)
**Status**: Complete (2026-01-15)

All submission requirements completed:
- SMS and foreground service permissions declarations done
- Demonstration video submitted
- Privacy policy linked
- App approved for closed testing

#### Phase 2: CI/CD Pipeline Refinement ✅

**Goal**: Validate and refine automated deployment pipeline from tag to production
**Depends on**: Phase 1
**Status**: Complete (2026-01-15)

Pipeline validated and working:
- Full deployment flow: tag → internal → alpha working
- Semantic version code extraction working
- Manual promotion workflow tested

#### Phase 3: Pre-Launch Testing

**Goal**: Dogfood the app daily while waiting for beta eligibility (12 testers + 14 days)
**Depends on**: Phase 2 (complete)
**Status**: In Progress
**Context**: [03-CONTEXT.md](phases/03-prelaunch-testing/03-CONTEXT.md)

Key focus:
- Daily personal use of the app
- Fix reliability issues — no missed messages
- Polish UX rough edges
- No new features or major refactors

Plans:
- [ ] 03-01: TBD (run /gsd:plan-phase 3 to break down)

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
| 1. Play Store Submission | v1.0 | - | Complete | 2026-01-15 |
| 2. CI/CD Pipeline | v1.0 | - | Complete | 2026-01-15 |
| 3. Pre-Launch Testing | v1.0 | 0/? | In Progress | - |
| 4. Public Release | v1.0 | 0/? | Not Started | - |
| 5. Bidirectional Pairing | v1.1 | 2/2 | Complete | 2026-01-15 |
| 6. Pending State & Pairing UX | v1.1 | 3/3 | Complete | 2026-01-15 |
| 7. UI/UX Polish | v1.1 | 2/2 | Complete | 2026-01-15 |
