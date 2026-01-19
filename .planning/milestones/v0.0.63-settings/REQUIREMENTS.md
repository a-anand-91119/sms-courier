# Requirements: SMS Courier v0.0.63 Settings

**Defined:** 2026-01-18
**Core Value:** User-configurable preferences for notifications, service behavior, security, and app appearance.

## v1 Requirements

Requirements for v0.0.63 Settings milestone.

### Infrastructure

- [x] **INFRA-01**: Settings persist across app restarts using DataStore
- [x] **INFRA-02**: SettingsRepository exposes typed Flows for each setting
- [x] **INFRA-03**: MasterService observes settings changes via Flow
- [x] **INFRA-04**: SecurityManager uses injected settings instead of hardcoded constants

### Main Settings

- [x] **MAIN-01**: User can access Settings screen from home navigation
- [x] **MAIN-02**: User can toggle notification persistence (recreate on dismiss vs stay dismissed)
- [x] **MAIN-03**: User can select default forwarding duration (15min, 30min, 1hr, 2hr)
- [x] **MAIN-04**: User can select theme (Light, Dark, System)

### Information

- [x] **INFO-01**: User can view permission status with links to system settings to fix
- [x] **INFO-02**: User can view About section with app version, privacy policy link, support contact

### Advanced Settings

- [x] **ADV-01**: User can configure device lockout duration (default: 15 minutes)
- [x] **ADV-02**: User can configure max failed auth attempts before lockout (default: 5)
- [x] **ADV-03**: User can configure challenge expiry timeout (default: 2 minutes)
- [x] **ADV-04**: User can configure max pairing resend attempts (default: 5)
- [x] **ADV-05**: User can configure pairing resend cooldown (default: 1 minute)
- [x] **ADV-06**: User can configure auth request timeout (default: 5 minutes)

## v2 Requirements

Deferred to future release.

### Auto-Start

- **BOOT-01**: User can enable auto-start on device boot
- **BOOT-02**: App handles vendor-specific auto-start restrictions (Xiaomi, Huawei, Samsung)

### Enhanced Features

- **ENH-01**: User can customize forwarded message format
- **ENH-02**: User can configure notification grouping behavior
- **ENH-03**: User can view and export logs for troubleshooting

## Out of Scope

Explicitly excluded from this milestone.

| Feature | Reason |
|---------|--------|
| Auto-start on boot | Vendor complexity (Xiaomi/Huawei/Samsung quirks), defer to dedicated milestone |
| Message format customization | Lower priority, requires protocol changes |
| Data management (clear history) | Defer until device/SMS history feature |
| Notification grouping | Lower priority UX enhancement |
| In-app notification channel controls | Android system settings handles this natively |
| Debug/logging options | Not needed for typical users |
| Open source licenses display | Adds complexity, version/privacy/support sufficient |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| INFRA-01 | Phase 12 | Complete |
| INFRA-02 | Phase 12 | Complete |
| INFRA-03 | Phase 14 | Complete |
| INFRA-04 | Phase 14 | Complete |
| MAIN-01 | Phase 13 | Complete |
| MAIN-02 | Phase 13 | Complete |
| MAIN-03 | Phase 13 | Complete |
| MAIN-04 | Phase 13 | Complete |
| INFO-01 | Phase 13 | Complete |
| INFO-02 | Phase 13 | Complete |
| ADV-01 | Phase 14 | Complete |
| ADV-02 | Phase 14 | Complete |
| ADV-03 | Phase 14 | Complete |
| ADV-04 | Phase 14 | Complete |
| ADV-05 | Phase 14 | Complete |
| ADV-06 | Phase 14 | Complete |

**Coverage:**
- v1 requirements: 16 total
- Mapped to phases: 16
- Unmapped: 0

---
*Requirements defined: 2026-01-18*
*Last updated: 2026-01-19 after Phase 14 completion*
