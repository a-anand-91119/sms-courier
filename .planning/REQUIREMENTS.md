# Requirements: SMS Courier v0.0.63 Settings

**Defined:** 2026-01-18
**Core Value:** User-configurable preferences for notifications, service behavior, security, and app appearance.

## v1 Requirements

Requirements for v0.0.63 Settings milestone.

### Infrastructure

- [ ] **INFRA-01**: Settings persist across app restarts using DataStore
- [ ] **INFRA-02**: SettingsRepository exposes typed Flows for each setting
- [ ] **INFRA-03**: MasterService observes settings changes via Flow
- [ ] **INFRA-04**: SecurityManager uses injected settings instead of hardcoded constants

### Main Settings

- [ ] **MAIN-01**: User can access Settings screen from home navigation
- [ ] **MAIN-02**: User can toggle notification persistence (recreate on dismiss vs stay dismissed)
- [ ] **MAIN-03**: User can select default forwarding duration (15min, 30min, 1hr, 2hr)
- [ ] **MAIN-04**: User can select theme (Light, Dark, System)

### Information

- [ ] **INFO-01**: User can view permission status with links to system settings to fix
- [ ] **INFO-02**: User can view About section with app version, privacy policy link, support contact

### Advanced Settings

- [ ] **ADV-01**: User can configure device lockout duration (default: 15 minutes)
- [ ] **ADV-02**: User can configure max failed auth attempts before lockout (default: 5)
- [ ] **ADV-03**: User can configure challenge expiry timeout (default: 2 minutes)
- [ ] **ADV-04**: User can configure max pairing resend attempts (default: 5)
- [ ] **ADV-05**: User can configure pairing resend cooldown (default: 1 minute)
- [ ] **ADV-06**: User can configure auth request timeout (default: 5 minutes)

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
| INFRA-01 | TBD | Pending |
| INFRA-02 | TBD | Pending |
| INFRA-03 | TBD | Pending |
| INFRA-04 | TBD | Pending |
| MAIN-01 | TBD | Pending |
| MAIN-02 | TBD | Pending |
| MAIN-03 | TBD | Pending |
| MAIN-04 | TBD | Pending |
| INFO-01 | TBD | Pending |
| INFO-02 | TBD | Pending |
| ADV-01 | TBD | Pending |
| ADV-02 | TBD | Pending |
| ADV-03 | TBD | Pending |
| ADV-04 | TBD | Pending |
| ADV-05 | TBD | Pending |
| ADV-06 | TBD | Pending |

**Coverage:**
- v1 requirements: 16 total
- Mapped to phases: 0
- Unmapped: 16

---
*Requirements defined: 2026-01-18*
*Last updated: 2026-01-18 after initial definition*
