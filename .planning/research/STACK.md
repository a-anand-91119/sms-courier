# Technology Stack: Settings Screen

**Project:** SMS Courier Settings Screen
**Researched:** 2026-01-18
**Confidence:** HIGH (verified with official sources and existing codebase)

## Executive Summary

The project already has all required dependencies for building a Settings screen. **No new libraries needed.** Build custom Settings UI using existing Material 3 components and DataStore Preferences (already in `build.gradle.kts`).

## Recommended Stack

### Already Available (No Changes Needed)

| Technology | Version | Purpose | Status |
|------------|---------|---------|--------|
| Jetpack Compose | BOM 2024.08.00 | UI Framework | In project |
| Material 3 | 1.2.1 (via BOM) | Design system | In project |
| DataStore Preferences | 1.1.1 | Settings persistence | In project |
| Navigation Compose | 2.7.7 | Screen navigation | In project |
| ViewModel Compose | 2.7.0 | State management | In project |

### Core Pattern: Custom Settings UI + DataStore

**Why custom over library:**
1. **Existing UI consistency** - HomeScreen already uses custom Card-based UI patterns (ServiceStatusCard, QuickActionCard). Settings should match.
2. **Material 3 alignment** - Project uses dynamic colors via `dynamicDarkColorScheme()`/`dynamicLightColorScheme()`. Third-party libraries may not integrate cleanly.
3. **Minimal overhead** - Settings screens are straightforward Compose; no library abstraction needed.
4. **DataStore already integrated** - Project has `androidx.datastore:datastore-preferences:1.1.1` in dependencies.

## Third-Party Libraries Considered (Not Recommended)

| Library | Why Considered | Why NOT Recommended |
|---------|----------------|---------------------|
| [ComposePrefs3](https://github.com/JamalMulla/ComposePrefs3) (JamalMulla) | Material 3 preference composables | Last updated 2023; uses older Material 3; UI style may conflict with existing app design |
| [Compose-Settings](https://github.com/alorma/Compose-Settings) (alorma) | Multiplatform settings tiles | Adds external dependency for simple UI; project not as actively maintained |
| [ComposePreference](https://github.com/zhanghai/ComposePreference) (zhanghai) | Material 3 preferences, uses SharedPreferences by default | Uses SharedPreferences (DataStore already in project); adds complexity for simple use case |

**Key insight:** All these libraries essentially wrap basic Compose components (Switch, Checkbox, ListItem patterns) with DataStore/SharedPreferences integration. Since SMS Courier already has DataStore and a consistent UI pattern, building custom is simpler and maintains design consistency.

## Implementation Architecture

### DataStore Setup (Already Available)

```kotlin
// Extension property for single DataStore instance
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)
```

### Preference Keys Pattern

```kotlin
object SettingsKeys {
    // Notification settings
    val PERSISTENT_NOTIFICATION = booleanPreferencesKey("persistent_notification")

    // Auto-start
    val AUTO_START_ON_BOOT = booleanPreferencesKey("auto_start_on_boot")

    // Forwarding defaults
    val DEFAULT_FORWARDING_DURATION = intPreferencesKey("default_forwarding_duration")

    // Theme
    val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"

    // Security
    val REQUIRE_AUTH_FOR_SETTINGS = booleanPreferencesKey("require_auth_for_settings")
}
```

### Repository Pattern

```kotlin
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val persistentNotification: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[SettingsKeys.PERSISTENT_NOTIFICATION] ?: true }

    suspend fun setPersistentNotification(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.PERSISTENT_NOTIFICATION] = enabled
        }
    }
    // ... other settings
}
```

### ViewModel Pattern

```kotlin
class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val persistentNotification: StateFlow<Boolean> = settingsRepository
        .persistentNotification
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setPersistentNotification(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPersistentNotification(enabled)
        }
    }
}
```

## UI Component Patterns

### Reusable Settings Components to Build

Based on existing UI patterns in HomeScreen.kt:

| Component | Purpose | Based On |
|-----------|---------|----------|
| `SettingsSwitchItem` | Toggle settings (notifications, auto-start) | ServiceStatusCard Switch pattern |
| `SettingsClickItem` | Navigation items (About, Licenses) | QuickActionCard pattern |
| `SettingsSelectionItem` | Single-choice (Theme, Duration) | Dialog-based selection |
| `SettingsSectionHeader` | Group headers | Text with titleMedium style |

### Theme Selection Implementation

```kotlin
enum class ThemeMode(val label: String) {
    SYSTEM("Follow system"),
    LIGHT("Light"),
    DARK("Dark")
}

// In Theme.kt - modify to accept theme override
@Composable
fun smscourierTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // ... existing dynamic color logic
}
```

## Settings Categories Structure

| Category | Settings | UI Component |
|----------|----------|--------------|
| **General** | Auto-start on boot | Switch |
| | Default forwarding duration | Selection dialog (15m, 30m, 1h, 2h, unlimited) |
| **Notifications** | Persistent notification | Switch |
| **Appearance** | Theme | Selection (System, Light, Dark) |
| **Security** | Lockout duration | Selection |
| | Max failed attempts | Selection |
| **About** | Version info | Text display |
| | Open source licenses | Navigation |
| **Permissions** | SMS permission status | Status display + action |
| | Notification permission status | Status display + action |

## What NOT to Build

| Anti-Pattern | Why Avoid | Alternative |
|--------------|-----------|-------------|
| SharedPreferences wrapper | DataStore already in project | Use DataStore directly |
| Complex preference DSL | Over-engineering for simple settings | Simple Compose functions |
| XML-based PreferenceScreen | Legacy Views pattern | Pure Compose |
| Singleton preference manager | Violates dependency injection | Repository with DataStore injection |

## Version Compatibility Notes

| Dependency | Current Version | Notes |
|------------|-----------------|-------|
| DataStore Preferences | 1.1.1 | Latest stable is 1.1.2 (minor); 1.3.0-alpha04 is latest alpha. Current is fine. |
| Compose BOM | 2024.08.00 | Material 3 1.2.1. Consider upgrading to 2025.01.00+ for M3 1.3+ features if needed. |
| Navigation Compose | 2.7.7 | Stable, supports type-safe args. Current is adequate. |

## Upgrade Recommendations

**Optional (not required for Settings screen):**

```toml
# In libs.versions.toml - only if needed for newer M3 features
composeBom = "2025.01.00"  # Upgrades Material3 to 1.3.x
datastorePreferences = "1.1.2"  # Minor bug fixes
```

**Not recommended:**
- Upgrading to Kotlin 2.0 just for settings (breaking change, requires project-wide changes)
- Adding compose-material3-adaptive (overkill for phone-only settings screen)

## Files to Create

| File | Purpose |
|------|---------|
| `data/SettingsKeys.kt` | Preference key definitions |
| `repository/SettingsRepository.kt` | DataStore operations |
| `viewmodels/SettingsViewModel.kt` | Settings UI state |
| `composables/settings/SettingsSwitchItem.kt` | Reusable switch component |
| `composables/settings/SettingsClickItem.kt` | Reusable click item component |
| `composables/settings/SettingsSelectionDialog.kt` | Selection dialog component |
| `composables/screens/SettingsScreen.kt` | Main settings screen (replace placeholder in NavGraph.kt) |

## Files to Modify

| File | Change |
|------|--------|
| `ui/theme/Theme.kt` | Add ThemeMode parameter support |
| `activities/MainActivity.kt` | Wire theme preference to smscourierTheme |
| `navigation/NavGraph.kt` | Replace placeholder SettingsScreen with real implementation |
| `applications/MainApplication.kt` | Initialize SettingsRepository (or use Hilt if adding DI later) |

## Sources

### Official Documentation
- [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) - Official Android DataStore guide
- [Material Design 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3) - M3 component reference
- [Compose BOM Mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping) - Version compatibility

### Community References (MEDIUM confidence)
- [Production-Ready Settings Screen in Jetpack Compose](https://medium.com/@santosh_yadav321/implementing-a-production-ready-settings-screen-in-jetpack-compose-9b4611c6f39f) - Architecture patterns
- [Building Dark Mode & Dynamic Theming](https://dev.to/blamsa0mine/building-dark-mode-dynamic-theming-with-kotlin-jetpack-compose-advanced-settings-datastore--39d7) - Theme implementation
- [Mastering Material 3 in Jetpack Compose 2025](https://medium.com/@hiren6997/mastering-material-3-in-jetpack-compose-the-2025-guide-1c1bd5acc480) - M3 best practices

### Third-Party Libraries Evaluated (not recommended for this project)
- [ComposePrefs3](https://github.com/JamalMulla/ComposePrefs3) - M3 preference library
- [Compose-Settings](https://github.com/alorma/Compose-Settings) - Multiplatform settings tiles
- [ComposePreference](https://github.com/zhanghai/ComposePreference) - M3 preferences with SharedPreferences
