# Project State: SMS Courier

## Current Position

Phase: 14 of 14 (Advanced Settings & Service Integration)
Plan: 1 of 3 in current phase
Status: In progress
Last activity: 2026-01-19 - Completed 14-01-PLAN.md (Advanced Settings UI)

Progress: ██████████ 100% of v0.0.7-v0.0.10 | 100% of v0.0.60-v0.0.61 | 100% of v0.0.62 | 78% of v0.0.63

## Accumulated Context

### Key Decisions

**Architecture:**
- SMS-based command protocol with `SMSC` prefix for device-to-device communication
- Foreground service (MasterService) for reliable message forwarding
- Room database for paired device management and session tracking
- Jetpack Compose UI with Navigation Compose
- Composite primary key `(phoneNumber, role)` for bidirectional pairing (v0.1)

**Security:**
- Bcrypt password hashing for device pairing
- Failed attempt tracking and device lockout
- Secure pairing workflow (request -> approve -> password)
- Role-specific UNPAIR command: `SMSC UNPAIR SOURCE` / `SMSC UNPAIR TARGET`

**CI/CD:**
- GitLab CI/CD with Kubernetes runners
- Semantic versioning with automatic version code calculation (v1.2.3 -> 10203)
- Automated deployment: tag -> internal, manual promotions to alpha -> beta -> production
- Fastlane for Play Store deployment automation
- Parallel test jobs: test:unit (fastlane) and test:integration (gradle filter)

**Testing (v0.0.62):**
- Reflection-based testing for private methods when PDU construction is impractical
- SmsSender made `open` class to enable test subclassing
- IntegrationTestBase pattern: abstract base with full dependency injection
- 342 total tests passing

**Settings (v0.0.63 - Phases 12-14):**
- DataStore Preferences for settings persistence
- SettingsRepository with typed Flow properties
- Conservative security defaults matching existing SecurityManager constants
- require() validation throws IllegalArgumentException on invalid input
- Two SettingsRepository instances (MainActivity and NavGraph) acceptable - DataStore is Context singleton
- App-level theme observation in MainActivity for immediate theme changes
- Duration options limited to 15, 30, 60 minutes (within SettingsRepository 1-60 validation)
- Material3 ListItem pattern for settings rows (SettingsSwitchItem, SettingsSelectionItem, SettingsNumberInputItem)
- BuildConfig generation enabled for VERSION_NAME access
- Lifecycle-aware permission refresh using DisposableEffect with LifecycleEventObserver
- About URLs externalized in Constants.kt (PRIVACY_POLICY_URL, SUPPORT_URL)
- Collapsible Advanced section with warning header for security emphasis
- SettingsNumberInputItem shows range in supporting text with helper text below
- Error handling via snackbar with LaunchedEffect trigger

### Technical Context

**Current State:**
- App functionally complete with pairing, forwarding, and session management
- Settings data layer complete: SettingsRepository with 9 typed Flows
- Settings screen complete: preferences, advanced, and information sections
- Advanced section with 6 security settings (lockout, attempts, challenge, pairing, cooldown, timeout)
- Permission status display with Fix action opening app settings
- About section with version, privacy policy, and support links
- Theme observation wired at app level - theme changes apply immediately
- Comprehensive test coverage: 342 tests passing
- CI/CD pipeline fully configured with parallel test jobs
- Play Store app in closed testing (12 testers, 14-day wait)
- Database at version 5

**Completed Milestones:**
- v0.0.7-v0.0.10 Feature Improvements: SHIPPED 2026-01-15 (Phases 5-7)
- v0.0.60-v0.0.61 CI/CD Optimizations: SHIPPED 2026-01-16 (Phase 8)
- v0.0.62 Testing: SHIPPED 2026-01-18 (Phases 9-11)

**Current Milestone:**
- v0.0.63 Settings (Phases 12-14)
  - Phase 12: Settings Data Layer - COMPLETE
  - Phase 13: Settings Screen & Main Settings - COMPLETE
  - Phase 14: Advanced Settings & Service Integration - In Progress (Plan 1/3 complete)

**Parallel Milestone:**
- Play Store Launch (Phases 1-4)
  - Phase 3: In Progress - Closed testing (12 testers, 14-day wait)
  - Phase 4: Not Started

### Blockers/Concerns Carried Forward

None currently identified.

### Pending Todos

4 todos tracked in `.planning/todos/pending/`

**Feature Backlog:**
- Forward to email or chat apps
- Group messages by contact type
- Sync sent messages back to original device
- Smart filters for selective forwarding

## Session Continuity

Last session: 2026-01-19
Stopped at: Completed 14-01-PLAN.md (Advanced Settings UI)
Resume file: None

**Context for next session:**
- Plan 14-01 complete - Advanced settings UI fully functional
- SettingsViewModel has 6 new security StateFlows with validation-aware setters
- SettingsScreen has collapsible Advanced section with warning header
- SettingsNumberInputItem composable available for numeric inputs
- Error snackbar displays validation errors
- All 342 tests passing
- Next action: Execute 14-02-PLAN.md (Service Integration)
