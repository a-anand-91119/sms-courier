# Roadmap: SMS Courier

## Overview

SMS Courier is an Android app for forwarding SMS messages between two paired devices using secure SMS-based commands. The v1.0 milestone focuses on completing Play Store requirements, finalizing the automated CI/CD deployment pipeline, conducting thorough testing across internal/alpha/beta tracks, and ultimately launching publicly on Google Play Store.

## Domain Expertise

None (Android app development patterns already established in codebase)

## Milestones

- 🚧 **v1.0 Play Store Launch** - Phases 1-4 (in progress)
- 📋 **v1.1 Feature Improvements** - Phases 5-7 (planned)

## Phases

### 🚧 v1.0 Play Store Launch (In Progress)

**Milestone Goal:** Complete all Play Store requirements, test deployment pipeline, conduct multi-track testing, and successfully launch SMS Courier on Google Play Store.

#### Phase 1: Play Store Submission Requirements

**Goal**: Complete all Google Play Console requirements for app submission
**Depends on**: Nothing (first phase)
**Research**: Unlikely (established Play Store guidelines)
**Status**: In Progress
**Plans**: TBD

Key deliverables:
- Complete SMS and foreground service permissions declarations
- Create and submit demonstration video for reviewers
- Finalize and link privacy policy
- Submit app for Play Store review
- Address any reviewer feedback

Plans:
- [ ] 01-01: TBD (run /gsd:plan-phase 1 to break down)

#### Phase 2: CI/CD Pipeline Refinement

**Goal**: Validate and refine automated deployment pipeline from tag to production
**Depends on**: Phase 1 (need app approved for testing tracks)
**Research**: Unlikely (pipeline already built)
**Status**: In Progress
**Plans**: TBD

Key deliverables:
- Test full deployment flow: tag → internal → alpha → beta → production
- Verify semantic version code extraction (v1.2.3 → 10203)
- Validate draft release creation for unpublished app
- Test manual promotion workflow between tracks
- Document release process for future versions

Plans:
- [ ] 02-01: TBD

#### Phase 3: Pre-Launch Testing

**Goal**: Conduct thorough testing across all Play Store tracks before public release
**Depends on**: Phase 2 (need working deployment pipeline)
**Research**: Unlikely (standard testing practices)
**Status**: In Progress
**Plans**: TBD

Key deliverables:
- Internal testing validation (permissions, core features)
- Alpha testing with closed test group
- Beta testing with open testers
- Incorporate feedback and fix critical issues
- Performance and stability verification
- Final QA before production

Plans:
- [ ] 03-01: TBD

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
- Plan v1.1 improvements based on initial feedback

Plans:
- [ ] 04-01: TBD

### 📋 v1.1 Feature Improvements (Planned)

**Milestone Goal:** Address critical bugs and add key features discovered during development - bidirectional pairing, improved pending state management, and UI polish for production readiness.

**Note:** v1.1 features can ship to internal/alpha/beta tracks while v1.0 awaits Play Store production approval.

#### Phase 5: Bidirectional Pairing Architecture

**Goal**: Enable same phone number to exist in both "Forwarded to me" and "I forward to" sections simultaneously
**Depends on**: Phase 1 (basic app functionality must be stable)
**Research**: Likely (database schema changes, architectural implications)
**Research topics**: Database migration strategy, relationship modeling for bidirectional pairs, UI state management for dual roles
**Plans**: TBD

Key deliverables:
- Analyze current database schema and constraints preventing bidirectional pairing
- Design new relationship model supporting dual roles
- Implement database migration for bidirectional support
- Update PairedDeviceRepository to handle bidirectional relationships
- Modify UI to display and manage bidirectional pairs
- Test edge cases (both devices initiate simultaneously, role conflicts)

Plans:
- [ ] 05-01: Bidirectional Pairing Architecture Implementation

#### Phase 6: Pending State & Pairing UX

**Goal**: Improve pairing workflow with resend capability, unpair acknowledgment, and abuse prevention
**Depends on**: Phase 5 (bidirectional architecture changes may affect this)
**Research**: Likely (UI patterns for multi-action gestures, abuse prevention strategies)
**Research topics**: Material Design patterns for contextual actions, rate limiting for resend requests, unpair workflow best practices
**Status**: In Progress
**Plans**: 1/3 complete

Key deliverables:
- Resend pairing request from pending state with abuse prevention (rate limiting, max attempts)
- Research and implement UI pattern for multiple actions on long-press (resend + delete)
- Interview stakeholder on unpair acknowledgment requirements (needed? optional? always?)
- Implement chosen unpair acknowledgment workflow
- Add visual feedback for pending operations
- Test scenarios: rapid resend attempts, network failures, stale pending states

Plans:
- [x] 06-01: Resend pairing request with rate limiting (database + ViewModel)
- [ ] 06-02: UI for resend action
- [ ] 06-03: Unpair acknowledgment workflow

#### Phase 7: UI/UX Polish

**Goal**: Fix UI bugs and improve user experience for production quality
**Depends on**: Phase 6 (all functional changes complete)
**Research**: Unlikely (standard Android UI fixes)
**Plans**: TBD

Key deliverables:
- Fix button text overflow on smaller devices (Samsung S25 and similar)
- Improve service toggle UX with loading states during foreground service startup
- Add visual feedback/spinner while service is starting
- Ensure all UI elements are responsive across device sizes
- Polish animations and transitions
- Accessibility audit and fixes

Plans:
- [ ] 07-01: TBD

## Progress

| Phase | Milestone | Plans | Status | Completed |
|-------|-----------|-------|--------|-----------|
| 1. Play Store Submission | v1.0 | 0/? | In Progress | - |
| 2. CI/CD Pipeline | v1.0 | 0/? | In Progress | - |
| 3. Pre-Launch Testing | v1.0 | 0/? | In Progress | - |
| 4. Public Release | v1.0 | 0/? | Not Started | - |
| 5. Bidirectional Pairing | v1.1 | 0/1 | Not Started | - |
| 6. Pending State & Pairing UX | v1.1 | 1/3 | In Progress | - |
| 7. UI/UX Polish | v1.1 | 0/? | Not Started | - |
