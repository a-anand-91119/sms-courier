# Project Research Summary

**Project:** SMS Courier Settings Screen
**Domain:** Android Settings Screen with Foreground Service Integration
**Researched:** 2026-01-18
**Confidence:** HIGH

## Executive Summary

Building a Settings screen for SMS Courier is a straightforward implementation task with no new dependencies required. The project already includes Jetpack Compose, Material 3, DataStore Preferences, and Navigation Compose — everything needed for a production-quality settings experience. The recommended approach is to build custom settings UI components using existing Material 3 patterns rather than adopting third-party settings libraries, which would conflict with the app's established design language and add maintenance burden.

The architecture should follow the repository pattern already used throughout the codebase: DataStore behind SettingsRepository, exposed via Flows to SettingsViewModel, consumed by Compose UI. Critical integration points include MasterService (which must observe settings reactively, not read once at startup) and a new BootReceiver for auto-start functionality. The existing hardcoded values in SecurityManager (lockout duration, max attempts, challenge expiry) must be refactored to use injected settings.

The primary risks are: (1) Android notification channels cannot be modified after creation, requiring upfront design of a dual-channel strategy for the persistence toggle; (2) BOOT_COMPLETED receivers have significant vendor-specific quirks on Xiaomi/Samsung/Huawei devices plus Android 12+ background start restrictions; and (3) MasterService currently uses hardcoded security constants that won't see settings updates without architectural changes. All three are addressable with proper planning.

## Key Findings

### Recommended Stack

No new dependencies needed. All required technologies are already in `build.gradle.kts`:

**Core technologies:**
- **Jetpack Compose (BOM 2024.08.00):** UI framework — existing project standard
- **Material 3 (1.2.1):** Design system — consistent with HomeScreen patterns
- **DataStore Preferences (1.1.1):** Settings persistence — already in project, superior to SharedPreferences
- **Navigation Compose (2.7.7):** Screen navigation — existing navigation infrastructure
- **ViewModel Compose (2.7.0):** State management — matches existing ViewModels

Third-party settings libraries (ComposePrefs3, Compose-Settings, ComposePreference) were evaluated and rejected. They add external dependencies for functionality that is simple to build with existing tools, and their UI patterns conflict with SMS Courier's established card-based design.

### Expected Features

**Must have (table stakes):**
- Grouped settings with section headers — users expect logical organization
- Toggle switches for binary options (notification persistence, auto-start on boot)
- Dialog/radio selection for multiple choice (theme, default forwarding duration)
- Secondary text showing current state ("15 minutes" not "Tap to change")
- Permission status display with action to fix denied permissions
- About section with version info and open source licenses
- Theme selection (light/dark/system default)
- 48dp minimum touch targets and 8dp spacing increments

**Should have (competitive):**
- Advanced security settings subscreen (lockout duration, max failed attempts, challenge expiry)
- Contextual help tooltips for complex settings
- Reset to defaults with confirmation dialog

**Defer (v2+):**
- Settings search (only needed if settings exceed 15 items)
- Settings export/import (complex, low initial demand)
- Live preview of theme changes
- Recent changes highlight

### Architecture Approach

Follow the existing Clean Architecture with MVVM pattern: SettingsScreen -> SettingsViewModel -> SettingsRepository -> DataStore. The repository exposes typed Flows for each setting, ViewModel combines them into StateFlow, and Compose collects with collectAsState(). Critical: services must observe these Flows reactively, not read once at startup.

**Major components:**
1. **SettingsDataStore** — Extension property for singleton DataStore instance, preference key definitions
2. **SettingsRepository** — Wraps DataStore operations, provides Flow<T> per preference, handles validation
3. **SettingsViewModel** — Combines Flows into SettingsUiState StateFlow, exposes update methods
4. **SettingsScreen** — Main settings UI with Material 3 components matching existing app patterns
5. **BootReceiver** — New BroadcastReceiver checking autoStart setting on BOOT_COMPLETED
6. **ThemeWrapper** — App-level composable applying theme based on settings

### Critical Pitfalls

1. **Notification channel settings locked after creation** — Cannot change channel importance/sound programmatically. Design TWO channels (persistent and non-persistent) from the start and switch between them for the persistence toggle.

2. **BOOT_COMPLETED receiver failures** — Multiple failure modes: permission not declared, app never launched, Direct Boot restrictions, vendor-specific auto-launch blockers (Xiaomi, Huawei, Samsung), battery optimization, Android 12+ background start restrictions. Use WorkManager for Android 12+, consider AutoStarter library for vendor quirks.

3. **Service not seeing updated settings** — SecurityManager has hardcoded constants (MAX_FAILED_ATTEMPTS=5, LOCKOUT_DURATION_MS=15min). Must refactor to inject settings via repository and observe Flows in service scope. Settings changes should take effect without restart.

4. **DataStore lacks built-in encryption** — Not critical for SMS Courier (security timeouts are not sensitive data), but if storing password-related config, use Tink library or encrypted-datastore. Current bcrypt password hashes are in Room database, not DataStore.

5. **ViewModel scoping in Compose** — Same ViewModel instance shared across destinations unless each screen is a navigation destination. Make settings subscreens proper navigation destinations or use separate ViewModel types.

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Data Layer Foundation
**Rationale:** Everything depends on SettingsRepository. Building this first enables parallel development of UI and integration points. Must be done before any settings can be displayed or applied.
**Delivers:** DataStore setup, preference keys, SettingsRepository with typed Flows, unit tests
**Addresses:** Settings infrastructure prerequisite for all features
**Avoids:** DataStore concurrency issues (single instance pattern), migration complexity (design clean from start)

### Phase 2: Settings Screen UI
**Rationale:** With data layer complete, UI is purely presentational and can iterate independently. ViewModel defines contract that UI implements.
**Delivers:** SettingsViewModel, SettingsScreen composable, reusable settings components (SettingsSwitchItem, SettingsClickItem, SettingsSelectionDialog)
**Addresses:** Main toggles, theme selection, default forwarding duration, permission status display
**Avoids:** Input validation bypass (validate in ViewModel), ViewModel scoping issues (proper navigation destinations)

### Phase 3: Advanced Settings Subscreen
**Rationale:** Security settings are more complex and less frequently accessed. Separate subscreen reduces cognitive load and matches Android guidelines for 15+ settings.
**Delivers:** Advanced security settings subscreen with configurable lockout duration, max failed attempts, challenge expiry, pairing rate limits
**Addresses:** Table stakes security configuration, power user features
**Avoids:** Overwhelming users with too many settings on one screen

### Phase 4: Service Integration
**Rationale:** Integration points touch existing code and benefit from having core settings infrastructure complete and tested. MasterService and SecurityManager modifications require careful testing.
**Delivers:** MasterService observing settings via Flows, SecurityManager refactored to use injected settings, notification persistence toggle working
**Addresses:** Settings propagation to service, reactive updates without restart
**Avoids:** Service not seeing updated settings (Flow observation), notification channel modification (dual-channel strategy)

### Phase 5: Auto-Start and Boot Receiver
**Rationale:** BOOT_COMPLETED is complex with many failure modes. Isolate this complexity to its own phase after core settings are working. Requires testing across multiple OEM devices and Android versions.
**Delivers:** BootReceiver with proper permissions, vendor compatibility handling, WorkManager fallback for Android 12+
**Addresses:** Auto-start on boot feature
**Avoids:** BOOT_COMPLETED failures (comprehensive implementation), Android 12+ restrictions (WorkManager expedited work)

### Phase 6: About Screen and Polish
**Rationale:** Polish and informational screens come last. Low risk, can ship without if needed.
**Delivers:** About subscreen with version info, open source licenses, final UI polish, tablet layout constraints
**Addresses:** Table stakes about section, professional appearance on large screens
**Avoids:** Settings stretching on tablets (max-width constraint)

### Phase Ordering Rationale

- **Data layer first:** Repository is a dependency for ViewModel, Service, and BootReceiver. Cannot proceed without it.
- **UI before integration:** Better to have working UI to validate settings before wiring to services. Faster iteration on user-facing features.
- **Service integration after UI:** Modifying MasterService is higher risk. Want stable settings infrastructure before touching core service.
- **Boot receiver last:** Highest complexity and testing burden. Vendor quirks require device-specific testing. Can ship MVP without auto-start if needed.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 5 (Auto-Start):** Vendor-specific quirks require device testing matrix. May need AutoStarter library evaluation. Android 12+ restrictions need WorkManager integration research.
- **Phase 4 (Service Integration):** Notification channel strategy needs validation. Dual-channel approach may have edge cases.

Phases with standard patterns (skip research-phase):
- **Phase 1 (Data Layer):** DataStore patterns well-documented, official documentation covers all cases.
- **Phase 2 (Settings Screen):** Standard Compose UI, matches existing app patterns.
- **Phase 3 (Advanced Settings):** Simple subscreen, no novel patterns.
- **Phase 6 (About Screen):** Trivial implementation.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All dependencies already in project; official documentation verified |
| Features | HIGH | Based on official Android settings guidelines and Material Design docs |
| Architecture | HIGH | Follows existing project patterns; DataStore documentation comprehensive |
| Pitfalls | HIGH | Verified via official Android documentation; codebase-specific issues identified |

**Overall confidence:** HIGH

### Gaps to Address

- **Vendor auto-start compatibility:** Research showed Xiaomi/Huawei/Samsung have proprietary auto-launch managers. May need AutoStarter library or manual user guidance. Evaluate during Phase 5 planning.
- **Android 12+ background start:** WorkManager expedited work is recommended but not fully researched. Deep dive needed during Phase 5.
- **Notification persistence UX:** Dual-channel approach is architecturally sound but user experience (channel switching, system notification settings) needs validation during Phase 4.

## Sources

### Primary (HIGH confidence)
- [Android Settings Design Guidelines](https://developer.android.com/design/ui/mobile/guides/patterns/settings)
- [Jetpack DataStore Documentation](https://developer.android.com/topic/libraries/architecture/datastore)
- [Android Notification Channels](https://developer.android.com/develop/ui/views/notifications/channels)
- [Android Foreground Service Requirements](https://developer.android.com/about/versions/14/changes/fgs-types-required)
- [Material Design 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)

### Secondary (MEDIUM confidence)
- [Production-Ready Settings Screen in Jetpack Compose](https://medium.com/@santosh_yadav321/implementing-a-production-ready-settings-screen-in-jetpack-compose-9b4611c6f39f)
- [DataStore + Tink Migration Guide](https://www.droidcon.com/2025/12/16/goodbye-encryptedsharedpreferences-a-2026-migration-guide/)
- [BroadcastReceiver Boot Complete Issues](https://codingtechroom.com/question/-broadcastreceiver-boot-complete-issues)
- [ViewModel Scoping in Compose](https://developer.android.com/develop/ui/compose/migrate/other-considerations)

### Tertiary (LOW confidence)
- Third-party library evaluations (ComposePrefs3, Compose-Settings) — GitHub readmes only
- Vendor auto-start compatibility — community reports, not official documentation

---
*Research completed: 2026-01-18*
*Ready for roadmap: yes*
