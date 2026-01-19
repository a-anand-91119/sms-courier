# Project State: SMS Courier

## Current Position

Phase: 14 of 14 (Advanced Settings & Service Integration)
Plan: 3 of 3 in current phase
Status: Phase complete
Last activity: 2026-01-19 - Completed 14-03-PLAN.md (Service Integration)

Progress: ██████████ 100% of v0.0.7-v0.0.10 | 100% of v0.0.60-v0.0.61 | 100% of v0.0.62 | 100% of v0.0.63

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
- Play Store app in closed testing (12 testers, 14-day wait)
- Database at version 5

**Completed Milestones:**
- v0.0.7-v0.0.10 Feature Improvements: SHIPPED 2026-01-15 (Phases 5-7)
- v0.0.60-v0.0.61 CI/CD Optimizations: SHIPPED 2026-01-16 (Phase 8)
- v0.0.62 Testing: SHIPPED 2026-01-18 (Phases 9-11)
- v0.0.63 Settings: COMPLETE 2026-01-19 (Phases 12-14)

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
Stopped at: Completed 14-03-PLAN.md (Service Integration)
Resume file: None

**Context for next session:**
- Phase 14 complete - Settings fully integrated with services and ViewModels
- All 9 settings are now reactive via Flow/StateFlow collection
- MasterService: auth request timeout configurable (default 5 min)
- PairedDevicesViewModel: pairing resend limits configurable (default 5 attempts, 1 min cooldown)
- SecurityManager: lockout, attempts, challenge expiry all configurable
- v0.0.63 Settings milestone complete - ready for release
- Next action: Tag v0.0.63 and deploy, or continue with Play Store launch phase 4
