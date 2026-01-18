# Roadmap: v0.0.63 Settings

**Jira Epic:** [SC-38](https://etcapp.atlassian.net/browse/SC-38)

## Overview

Add a settings screen with user-configurable preferences for notifications, service behavior, security, and app appearance. The milestone builds data layer foundation first, then the settings UI with main preferences, and finally advanced security settings with service integration.

## Phases

- [x] **Phase 12: Settings Data Layer** - DataStore persistence and SettingsRepository with typed Flows (completed 2026-01-18)
- [ ] **Phase 13: Settings Screen & Main Settings** - Navigation, main preferences, and information sections
- [ ] **Phase 14: Advanced Settings & Service Integration** - Security configuration and service/SecurityManager integration

## Phase Details

### Phase 12: Settings Data Layer

**Jira:** [SC-39](https://etcapp.atlassian.net/browse/SC-39)
**Goal**: Settings persist reliably and are accessible via typed Flows throughout the app
**Depends on**: Nothing (first phase of milestone)
**Requirements**: INFRA-01, INFRA-02
**Success Criteria** (what must be TRUE):
  1. App remembers all settings after restart (DataStore persists)
  2. Settings changes are immediately available to consumers via Flow
  3. Default values are used for unset settings
**Plans**: TBD

Plans:
- [x] 12-01: DataStore setup and SettingsRepository implementation ([SC-42](https://etcapp.atlassian.net/browse/SC-42)) ✓

### Phase 13: Settings Screen & Main Settings

**Jira:** [SC-40](https://etcapp.atlassian.net/browse/SC-40)
**Goal**: Users can access and modify main settings and view app information
**Depends on**: Phase 12 (data layer must exist)
**Requirements**: MAIN-01, MAIN-02, MAIN-03, MAIN-04, INFO-01, INFO-02
**Success Criteria** (what must be TRUE):
  1. User can navigate to Settings from home screen
  2. User can toggle notification persistence and selection is remembered
  3. User can select default forwarding duration from options (15min, 30min, 1hr, 2hr)
  4. User can select theme (Light, Dark, System) and app appearance changes
  5. User can view current permission status with links to fix missing permissions
  6. User can view app version, privacy policy, and support contact
**Plans**: TBD

Plans:
- [ ] 13-01: Settings screen navigation and layout ([SC-43](https://etcapp.atlassian.net/browse/SC-43))
- [ ] 13-02: Main settings preferences (notification, duration, theme) ([SC-44](https://etcapp.atlassian.net/browse/SC-44))
- [ ] 13-03: Information sections (permissions, about) ([SC-45](https://etcapp.atlassian.net/browse/SC-45))

### Phase 14: Advanced Settings & Service Integration

**Jira:** [SC-41](https://etcapp.atlassian.net/browse/SC-41)
**Goal**: Users can configure security parameters and services react to settings changes
**Depends on**: Phase 13 (main settings UI exists)
**Requirements**: ADV-01, ADV-02, ADV-03, ADV-04, ADV-05, ADV-06, INFRA-03, INFRA-04
**Success Criteria** (what must be TRUE):
  1. User can configure lockout duration and max failed attempts
  2. User can configure challenge expiry and auth request timeouts
  3. User can configure pairing rate limits (max resend attempts, cooldown)
  4. MasterService responds to settings changes without restart
  5. SecurityManager uses configured values instead of hardcoded constants
**Plans**: TBD

Plans:
- [ ] 14-01: Advanced settings UI (security configuration) ([SC-46](https://etcapp.atlassian.net/browse/SC-46))
- [ ] 14-02: Service integration (MasterService observes settings) ([SC-47](https://etcapp.atlassian.net/browse/SC-47))
- [ ] 14-03: SecurityManager refactor (inject settings, remove hardcoded constants) ([SC-48](https://etcapp.atlassian.net/browse/SC-48))

## Progress

| Phase | Plans | Status | Completed |
|-------|-------|--------|-----------|
| 12. Settings Data Layer | 1/1 | Complete | 2026-01-18 |
| 13. Settings Screen & Main Settings | 0/3 | Not Started | - |
| 14. Advanced Settings & Service Integration | 0/3 | Not Started | - |
