# Project State: SMS Courier

## Current Position

Phase: Not started (defining requirements)
Plan: —
Status: Defining requirements for v0.0.64
Last activity: 2026-02-04 — Milestone v0.0.64 started: device management & visibility

Progress: ░░░░░░░░░░ Requirements phase | Play Store Launch (Phase 4) in parallel

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
- Cached settings with Flow collection pattern for MasterService auth timeout
- StateFlow.stateIn() pattern for PairedDevicesViewModel reactive settings
- SecurityManager receives settingsRepository in constructor with startObservingSettings() for lifecycle-managed collection

### Technical Context

**Current State:**
- App functionally complete with pairing, forwarding, and session management
- Settings fully integrated: data layer, UI, and service/ViewModel consumption
- All 9 settings from SettingsRepository wired to consumers:
  - Main: notification persistence, default forwarding duration, theme
  - Advanced: lockout duration, max failed attempts, challenge expiry, max pairing resend attempts, pairing resend cooldown, auth request timeout
- MasterService uses configurable auth request timeout
- PairedDevicesViewModel uses configurable pairing resend limits
- SecurityManager uses configurable lockout, attempts, and challenge expiry settings
- Theme observation wired at app level - theme changes apply immediately
- Comprehensive test coverage: 342+ tests
- CI/CD pipeline fully configured with parallel test jobs
- Play Store closed testing complete, applied for beta/production access (awaiting Google review)
- Database at version 5

**Completed Milestones:**
- v0.0.7-v0.0.10 Feature Improvements: SHIPPED 2026-01-15 (Phases 5-7)
- v0.0.60-v0.0.61 CI/CD Optimizations: SHIPPED 2026-01-16 (Phase 8)
- v0.0.62 Testing: SHIPPED 2026-01-18 (Phases 9-11)
- v0.0.63 Settings: SHIPPED 2026-01-19 (Phases 12-14)

**Parallel Milestone:**
- Play Store Launch (Phases 1-4)
  - Phase 3: Complete - Closed testing finished
  - Phase 4: In Progress - Applied for beta/production access, awaiting Google review

### Blockers/Concerns Carried Forward

None currently identified.

### Pending Todos

11 todos tracked in `.planning/todos/pending/`

**Feature Backlog:**
- Forward to email or chat apps
- Group messages by contact type
- Sync sent messages back to original device
- Smart filters for selective forwarding
- Message history and forwarding logs
- Scheduled and recurring forwarding sessions
- Multi-destination forwarding (one-to-many)

**Reliability:**
- Auto-start app on device boot
- Proper permission management flow

**Tech Debt:**
- Remove legacy background service code
- Settings export/import

## Session Continuity

Last session: 2026-02-04
Stopped at: Milestone initialization in progress
Resume file: None

**Context for next session:**
- Starting v0.0.64 milestone: Enhanced device management and bidirectional visibility
- MILESTONE-CONTEXT.md contains complete specification from interview
- Next: Define requirements and create roadmap
- Play Store Launch (Phase 4) continues in parallel - awaiting Google review
