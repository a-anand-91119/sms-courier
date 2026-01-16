# Roadmap: SMS Courier

## Overview

SMS Courier is an Android app for forwarding SMS messages between two paired devices using secure SMS-based commands. The v1.0 milestone focuses on completing Play Store requirements, finalizing the automated CI/CD deployment pipeline, conducting thorough testing across internal/alpha/beta tracks, and ultimately launching publicly on Google Play Store.

## Domain Expertise

None (Android app development patterns already established in codebase)

## Milestones

- ✅ [**v0.1 Feature Improvements**](milestones/v0.1-ROADMAP.md) - Phases 5-7 (shipped 2026-01-15)
- 🚧 **v0.1.1 CI/CD Optimizations** - Phase 8 (in progress)
- 🚧 **v0.2 Play Store Launch** - Phases 1-4 (in progress)

## Completed Milestones

<details>
<summary>✅ v0.1 Feature Improvements (Phases 5-7) — SHIPPED 2026-01-15</summary>

- [x] Phase 5: Bidirectional Pairing Architecture — completed 2026-01-15
- [x] Phase 6: Pending State & Pairing UX (3/3 plans) — completed 2026-01-15
- [x] Phase 7: UI/UX Polish (1/1 plan + fix) — completed 2026-01-15

See [milestones/v0.1-ROADMAP.md](milestones/v0.1-ROADMAP.md) for full details.

</details>

## Phases

### 🚧 v0.1.1 CI/CD Optimizations (In Progress)

**Milestone Goal:** Optimize the CI/CD pipeline to eliminate unnecessary rebuilds, improve changelog generation, and automate GitLab release creation with artifact attachment.

#### Phase 8: CI/CD Pipeline Optimization

**Goal**: Fix rebuild inefficiency, integrate proper changelog generation, and add GitLab release automation
**Depends on**: Phase 7 (v0.1 complete)
**Research**: Likely (fastlane-plugin-changelog integration)
**Research topics**: fastlane-plugin-changelog setup, conventional commit parsing, GitLab release API
**Status**: Complete
**Plans**: 3/3 complete

Plans:
- [x] 08-01: Create upload-only Fastlane lanes (fix rebuild issue) — completed 2026-01-15
- [x] 08-02: Integrate fastlane-plugin-changelog for proper changelog generation — completed 2026-01-16
- [x] 08-03: Add GitLab release job with artifact attachment — completed 2026-01-16

**Key Changes Planned:**
- New `upload_internal` lane that skips gradle build, uses existing artifacts
- Replace `changelog_from_git_commits(commits_count: 20)` with proper changelog plugin
- Auto-generate changelog from conventional commits in Keep a Changelog format
- New CI job to create draft GitLab release after Play Store upload
- Attach APK + AAB + changelog to GitLab release
- Release naming: "SMS Courier vX.Y.Z"

---

### 🚧 v0.2 Play Store Launch (In Progress)

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
**Status**: Ongoing monitoring (no structured plans)
**Context**: [03-CONTEXT.md](phases/03-prelaunch-testing/03-CONTEXT.md)

Key focus:
- Daily personal use of the app
- Fix reliability issues — no missed messages
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
| 1. Play Store Submission | v0.2 | - | Complete | 2026-01-15 |
| 2. CI/CD Pipeline | v0.2 | - | Complete | 2026-01-15 |
| 3. Pre-Launch Testing | v0.2 | ad-hoc | Ongoing | - |
| 4. Public Release | v0.2 | 0/? | Not Started | - |
| 5. Bidirectional Pairing | v0.1 | 2/2 | Complete | 2026-01-15 |
| 6. Pending State & Pairing UX | v0.1 | 3/3 | Complete | 2026-01-15 |
| 7. UI/UX Polish | v0.1 | 2/2 | Complete | 2026-01-15 |
| 8. CI/CD Pipeline Optimization | v0.1.1 | 3/3 | Complete | 2026-01-16 |
