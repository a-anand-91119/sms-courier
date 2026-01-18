# Feature Landscape: Android Settings Screen

**Domain:** Android Settings Screen for SMS Courier App
**Researched:** 2026-01-18
**Confidence:** HIGH (verified with official Android documentation)

## Table Stakes

Features users expect in any modern Android settings screen. Missing these makes the app feel incomplete or amateurish.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **Grouped settings with section headers** | Users expect logical organization; Android guidelines mandate grouping for 7+ items | Low | Use section dividers between groups, not between individual items |
| **Clear, impersonal labels** | "Notifications" not "Notify me"; convention across Android ecosystem | Low | Labels can wrap to multiple lines if necessary |
| **Secondary text showing current state** | Users need to see current values without opening dialogs | Low | Show "15 minutes" not "Tap to change lockout duration" |
| **Toggle switches for binary options** | Preferred over checkboxes per Material Design | Low | Use for notification persistence, auto-start on boot |
| **Dialog/radio selection for multiple choice** | Drop-down menus discouraged; dialogs preferred for single selection | Medium | Use for default forwarding duration, theme selection |
| **Subscreens for related settings** | Mandatory for 15+ settings; helps focus user attention | Medium | Advanced security settings should be a subscreen |
| **Permission status display** | Users expect visibility into what access the app has | Medium | Show granted/denied state with action to fix denied permissions |
| **About section with version info** | Standard practice; helps support troubleshooting | Low | Version, build number; keep licensing out of main settings |
| **Theme selection (light/dark/system)** | Expected since Android 10; Material You users assume this | Medium | Follow system default initially; let users override |
| **48dp minimum touch targets** | Accessibility requirement; Android guideline | Low | Never go below this |
| **8dp spacing increments** | Material Design consistency; increases task completion 16% | Low | Apply uniformly throughout |

## Differentiators

Features that would set SMS Courier apart from typical Android settings screens. Not expected, but valued by users.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Settings search** | Quick access in complex hierarchies; recommended by Android for deep settings | High | Only needed if settings exceed 15 items; uses Material 3 SearchBar |
| **Reset to defaults with confirmation** | Power user feature; allows experimentation without fear | Medium | Confirmation dialog required; consider per-section reset vs global |
| **Settings export/import** | Device migration, backup recovery, multi-device sync | High | JSON format; encrypted with user password; defer to post-MVP |
| **Live preview of theme changes** | Immediate feedback; reduces save-check-revert cycles | Medium | Show sample UI elements in dialog |
| **Permission troubleshooter** | Guide users through fixing denied permissions step-by-step | Medium | Deep-link to system settings with contextual instructions |
| **Security audit summary** | Show overview of security posture (lockout config, auth timeouts) | Medium | One-glance view of current protection level |
| **Contextual help tooltips** | In-place explanations for complex settings | Low | Info icons that expand to show explanation |
| **Recent changes highlight** | Show which settings were changed recently | Medium | Helpful for troubleshooting; "Why did behavior change?" |

## Anti-Features

Features to explicitly NOT build. Common mistakes in Android settings screens that degrade UX.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **Frequently accessed actions in settings** | Per Android guidelines: "Don't include frequently accessed actions" | Keep actions contextual to their feature screens |
| **Account management in settings** | Android explicitly advises against this | Use dedicated profile/account screen if needed |
| **Full-screen blocking permission dialogs** | "Don't block the user interface" per Android guidelines | Show inline message where feature is limited |
| **Dividers between every setting** | Creates visual noise; Android guidelines say "avoid using dividers between each individual setting" | Use dividers only between groups |
| **Generic "Other" or "Miscellaneous" groups** | Ambiguous; violates Android naming guidelines | Name groups specifically or flatten into main list |
| **Synonyms like "Options" or "Preferences"** | Android specifically says don't use these | Use "Settings" consistently |
| **Describing what setting does instead of showing state** | Secondary text should show status, not explain the setting | "15 minutes" not "How long to lock device after failed attempts" |
| **Links within settings** | "Using links in settings is not recommended" per Android | Use subscreens or dialogs instead |
| **Excessive settings on one screen** | "Showing more than 10-15 items can be overwhelming" | Group into subscreens |
| **Pogosticking navigation** | List items that don't show enough info, forcing constant drilling in/out | Show current value inline |
| **Overriding system settings** | "Don't override settings provided by the system, as they can be personal accessibility needs" | Respect system dark mode, font sizes, etc. |
| **Auto-saving without feedback** | Users unsure if changes applied | Show toast/snackbar confirming save |
| **Non-standard selection controls** | Checkboxes where switches expected; custom pickers | Use Material 3 standard components |

## Feature Dependencies

```
Theme Selection
    |
    v
Settings Infrastructure (DataStore, ViewModel) <--- Required first
    |
    +---> Basic Settings (toggles, simple values)
    |         |
    |         v
    |     Permission Status Display
    |
    +---> Advanced Settings Subscreen
    |         |
    |         v
    |     Security Timeout Configuration
    |     Pairing Rate Limit Configuration
    |
    +---> About Screen
              |
              v
          Open Source Licenses (if using external libs)
```

## SMS Courier-Specific Settings Analysis

Based on the project context provided, here's how to categorize the specific settings:

### Main Settings (First Screen)

| Setting | Type | Category | Implementation Notes |
|---------|------|----------|---------------------|
| Notification persistence | Toggle | Table Stakes | Whether notification stays visible; uses standard Switch |
| Auto-start on boot | Toggle | Table Stakes | RECEIVE_BOOT_COMPLETED permission; show rationale |
| Default forwarding duration | Selector | Table Stakes | Radio dialog: 30min, 1hr, 2hr, 4hr, "Until stopped" |
| Theme | Selector | Table Stakes | Radio dialog: Light, Dark, System default |
| About | Navigation | Table Stakes | Navigate to About subscreen |
| Permission status | Status card | Table Stakes | Show SMS, Phone permissions; action button for denied |

### Advanced Settings (Subscreen)

| Setting | Type | Category | Implementation Notes |
|---------|------|----------|---------------------|
| Lockout duration | Selector | Table Stakes | Currently hardcoded 15min; allow 5/15/30/60min |
| Max failed attempts | Selector | Table Stakes | Currently hardcoded 5; allow 3/5/10 |
| Challenge expiry | Selector | Differentiator | Currently 2min; allow 1/2/5min |
| Max pairing resend | Number input | Table Stakes | Prevent spam; 3-10 range |
| Pairing cooldown | Selector | Table Stakes | Time between pairing attempts |
| Auth request timeout | Selector | Table Stakes | How long to wait for challenge response |
| Reset to defaults | Action | Differentiator | Confirmation dialog required |

### About Subscreen

| Item | Type | Category | Implementation Notes |
|------|------|----------|---------------------|
| App version | Display | Table Stakes | Version name and code |
| Build info | Display | Table Stakes | Build type, date |
| Open source licenses | Navigation | Table Stakes | If using external libraries (BCrypt, etc.) |
| Privacy policy link | Link | Legal | May be required for Play Store |

## MVP Recommendation

For MVP settings screen, prioritize:

1. **Settings infrastructure** - DataStore, ViewModel, basic navigation
2. **Main toggles** - Notification persistence, auto-start on boot
3. **Theme selection** - Light/Dark/System
4. **Default forwarding duration** - Core functionality control
5. **Permission status display** - Critical for SMS app functionality
6. **About screen** - Version info, licenses

Defer to post-MVP:
- **Settings search** - Only needed at scale (15+ settings)
- **Settings export/import** - Complex; low user demand initially
- **Advanced security subscreens** - Can use sensible defaults initially
- **Reset to defaults** - Nice-to-have, not critical

## Implementation Pattern Recommendation

Based on research, the recommended pattern for SMS Courier:

```
Settings Screen Architecture:
├── SettingsScreen (Composable)
│   └── Uses SettingsViewModel
│       └── Uses SettingsRepository
│           └── Uses Preferences DataStore
│
├── SettingsViewModel
│   ├── Exposes StateFlow<SettingsState>
│   └── Provides update methods per setting
│
├── SettingsRepository
│   ├── Wraps DataStore operations
│   └── Provides Flow<T> per preference
│
└── DataStore (Preferences)
    └── Stores key-value pairs async
```

Key patterns:
- **Single source of truth**: DataStore, not ViewModel
- **Reactive updates**: Flow from DataStore -> StateFlow in ViewModel -> Compose UI
- **Singleton DataStore**: Avoid concurrent access exceptions
- **Immediate UI feedback**: Update StateFlow immediately, persist in background

## Sources

### HIGH Confidence (Official Documentation)
- [Android Settings Design Guidelines](https://developer.android.com/design/ui/mobile/guides/patterns/settings) - Official patterns
- [Material Design Settings](https://m2.material.io/design/platform-guidance/android-settings.html) - Design language
- [Android Accessibility Principles](https://developer.android.com/guide/topics/ui/accessibility/principles) - Accessibility requirements
- [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) - Persistence layer
- [App Permissions Best Practices](https://developer.android.com/training/permissions/usage-notes) - Permission UX

### MEDIUM Confidence (Verified Community Sources)
- [Implementing a Production-Ready Settings Screen in Jetpack Compose](https://medium.com/@santosh_yadav321/implementing-a-production-ready-settings-screen-in-jetpack-compose-9b4611c6f39f) - Implementation patterns
- [Compose-Settings Library](https://github.com/alorma/Compose-Settings) - Alternative to building from scratch
- [Settings UI Design: Usability Tips](https://www.setproduct.com/blog/settings-ui-design) - UX patterns

### LOW Confidence (Informational Only)
- SMS forwarding app feature comparisons - Market research for feature ideas
