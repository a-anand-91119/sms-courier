# Phase 12: Settings Data Layer - Research

**Researched:** 2026-01-18
**Domain:** Android DataStore Preferences, Repository Pattern, Kotlin Flows
**Confidence:** HIGH

## Summary

This phase implements a settings persistence layer using Android Jetpack DataStore Preferences with a SettingsRepository that exposes typed Flows. The codebase already includes `androidx.datastore:datastore-preferences:1.1.1` as a dependency, so no new dependencies are required.

The standard approach is to use the `preferencesDataStore` property delegate at the top level to create a singleton DataStore instance, wrap it in a `SettingsRepository` class that exposes individual `Flow<T>` properties for each setting with suspend functions for writes. The repository follows the same pattern as existing `PairedDeviceRepository` and `ForwardingSessionRepository` - constructor injection of dependencies, suspend functions for writes, Flow returns for reads.

**Primary recommendation:** Create a single `SettingsRepository` class in the existing `repository/` package that wraps DataStore with typed Flow properties for each setting category (main settings, advanced/security settings). Use `preferencesDataStore` delegate with `ApplicationContext` similar to how `SmsCourierDatabase.getDatabase(context)` is used. Match the existing ViewModel Factory pattern for instantiation.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.datastore:datastore-preferences` | 1.1.1 | Key-value persistence with Flow | Already in project, official Android solution |
| Kotlin Coroutines + Flow | (bundled) | Async data streams | Standard for reactive data layer |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Turbine | 1.0.0 | Flow testing | Already in project for Flow assertions |
| MockK | 1.13.8 | Repository mocking in tests | Already in project |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Preferences DataStore | Proto DataStore | More type-safe but requires protobuf setup - overkill for simple settings |
| Preferences DataStore | Room | Supports complex queries but heavyweight for key-value pairs |
| Preferences DataStore | SharedPreferences | Deprecated, no Flow support, ANR risk |

**Installation:** Already present in `build.gradle.kts`:
```kotlin
implementation(libs.androidx.datastore.preferences)
```

## Architecture Patterns

### Recommended Project Structure
```
app/src/main/java/dev/notyouraverage/smscourier/
├── repository/
│   ├── PairedDeviceRepository.kt        # Existing
│   ├── ForwardingSessionRepository.kt   # Existing
│   └── SettingsRepository.kt            # NEW
├── data/
│   └── settings/
│       ├── SettingsDefaults.kt          # NEW - default value constants
│       └── PreferenceKeys.kt            # NEW - DataStore key definitions
```

### Pattern 1: Top-Level DataStore Delegate
**What:** Use `preferencesDataStore` property delegate at file top-level to ensure singleton
**When to use:** Always - DataStore must be singleton per file
**Example:**
```kotlin
// Source: Official Android Documentation
// At the top level of SettingsRepository.kt:
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)
```

### Pattern 2: Repository with Typed Flows
**What:** Repository class wraps DataStore, exposes individual Flow<T> for each setting
**When to use:** Standard pattern for settings access throughout app
**Example:**
```kotlin
// Source: Official Android Documentation + Codebase Pattern
class SettingsRepository(
    private val context: Context,
) {
    private val dataStore = context.settingsDataStore

    // Read as Flow
    val notificationPersistence: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.NOTIFICATION_PERSISTENCE] ?: SettingsDefaults.NOTIFICATION_PERSISTENCE
        }

    // Write with suspend function
    suspend fun setNotificationPersistence(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATION_PERSISTENCE] = enabled
        }
    }
}
```

### Pattern 3: Enum Handling via String Storage
**What:** Store enums as string name, convert via Flow map
**When to use:** For theme selection and any enum-based settings
**Example:**
```kotlin
// Source: Official Android Documentation
val theme: Flow<AppTheme> = dataStore.data
    .map { preferences ->
        val themeName = preferences[PreferenceKeys.THEME] ?: SettingsDefaults.THEME.name
        AppTheme.valueOf(themeName)
    }

suspend fun setTheme(theme: AppTheme) {
    dataStore.edit { preferences ->
        preferences[PreferenceKeys.THEME] = theme.name
    }
}

enum class AppTheme { LIGHT, DARK, SYSTEM }
```

### Pattern 4: Duration as Minutes (Int)
**What:** Store duration settings as integer minutes
**When to use:** For forwarding duration, lockout duration, timeout settings
**Example:**
```kotlin
val defaultForwardingDurationMinutes: Flow<Int> = dataStore.data
    .map { preferences ->
        preferences[PreferenceKeys.DEFAULT_FORWARDING_DURATION] ?: SettingsDefaults.DEFAULT_FORWARDING_DURATION
    }

suspend fun setDefaultForwardingDuration(minutes: Int) {
    require(minutes in 1..60) { "Duration must be between 1 and 60 minutes" }
    dataStore.edit { preferences ->
        preferences[PreferenceKeys.DEFAULT_FORWARDING_DURATION] = minutes
    }
}
```

### Anti-Patterns to Avoid
- **Creating multiple DataStore instances for same file:** Throws `IllegalStateException`, breaks all functionality
- **Using runBlocking for reads:** Causes ANRs on UI thread
- **Caching DataStore values separately:** Invalidates consistency guarantees
- **Mutating data types:** DataStore requires immutable types

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Key-value persistence | Custom file/JSON storage | DataStore Preferences | Handles concurrency, corruption, transactions |
| Reactive settings updates | Manual listeners/callbacks | DataStore Flow | Built-in change observation |
| Thread-safe writes | Synchronized blocks | DataStore edit {} | Atomic read-modify-write transactions |
| Default value handling | Null checks everywhere | Flow map with elvis operator | Clean, centralized defaults |

**Key insight:** DataStore handles all the hard parts (thread safety, corruption recovery, transactions). The repository just provides typed access.

## Common Pitfalls

### Pitfall 1: Multiple DataStore Instances
**What goes wrong:** App crashes with `IllegalStateException` or silent data corruption
**Why it happens:** Creating DataStore in ViewModel/Activity instead of singleton pattern
**How to avoid:** Use top-level property delegate, pass repository via constructor injection
**Warning signs:** DataStore-related crashes, inconsistent settings values

### Pitfall 2: Blocking UI Thread with runBlocking
**What goes wrong:** ANRs when reading settings synchronously
**Why it happens:** Needing a setting value before Flow collection starts
**How to avoid:** Preload settings in `Application.onCreate()` using `lifecycleScope.launch`, or use Flow with sensible defaults until real values arrive
**Warning signs:** ANR reports mentioning DataStore, slow app startup

### Pitfall 3: Not Handling IOExceptions
**What goes wrong:** App crashes when storage fails
**Why it happens:** Forgetting to catch exceptions in Flow collection
**How to avoid:** Use `.catch { emit(defaultValue) }` on Flows or `ReplaceFileCorruptionHandler`
**Warning signs:** Crash reports with IOException from DataStore

### Pitfall 4: Exposing MutableStateFlow Instead of DataStore Flow
**What goes wrong:** UI updates don't reflect in persistence, race conditions
**Why it happens:** Trying to cache values for performance
**How to avoid:** Always return `dataStore.data.map {}` directly, let consumers use `stateIn()` if needed
**Warning signs:** Settings appear to save but revert on restart

### Pitfall 5: Validating on Read Instead of Write
**What goes wrong:** Invalid values can be persisted, validation spread across codebase
**Why it happens:** Not thinking about data integrity at write time
**How to avoid:** Validate in suspend write functions, throw on invalid input (per CONTEXT.md decision)
**Warning signs:** Invalid values appearing in settings, defensive code throughout consumers

## Code Examples

Verified patterns from official sources and codebase conventions:

### Complete SettingsRepository Structure
```kotlin
// Source: Official Android Documentation + Codebase patterns
package dev.notyouraverage.smscourier.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

class SettingsRepository(
    private val context: Context,
) {
    private val dataStore = context.settingsDataStore

    // Main Settings
    val notificationPersistence: Flow<Boolean> = dataStore.data
        .map { it[PreferenceKeys.NOTIFICATION_PERSISTENCE] ?: SettingsDefaults.NOTIFICATION_PERSISTENCE }

    val defaultForwardingDurationMinutes: Flow<Int> = dataStore.data
        .map { it[PreferenceKeys.DEFAULT_FORWARDING_DURATION] ?: SettingsDefaults.DEFAULT_FORWARDING_DURATION }

    val theme: Flow<AppTheme> = dataStore.data
        .map {
            val name = it[PreferenceKeys.THEME] ?: SettingsDefaults.THEME.name
            AppTheme.valueOf(name)
        }

    // Advanced/Security Settings
    val lockoutDurationMinutes: Flow<Int> = dataStore.data
        .map { it[PreferenceKeys.LOCKOUT_DURATION] ?: SettingsDefaults.LOCKOUT_DURATION }

    val maxFailedAttempts: Flow<Int> = dataStore.data
        .map { it[PreferenceKeys.MAX_FAILED_ATTEMPTS] ?: SettingsDefaults.MAX_FAILED_ATTEMPTS }

    val challengeExpiryMinutes: Flow<Int> = dataStore.data
        .map { it[PreferenceKeys.CHALLENGE_EXPIRY] ?: SettingsDefaults.CHALLENGE_EXPIRY }

    val maxPairingResendAttempts: Flow<Int> = dataStore.data
        .map { it[PreferenceKeys.MAX_PAIRING_RESEND_ATTEMPTS] ?: SettingsDefaults.MAX_PAIRING_RESEND_ATTEMPTS }

    val pairingResendCooldownMinutes: Flow<Int> = dataStore.data
        .map { it[PreferenceKeys.PAIRING_RESEND_COOLDOWN] ?: SettingsDefaults.PAIRING_RESEND_COOLDOWN }

    val authRequestTimeoutMinutes: Flow<Int> = dataStore.data
        .map { it[PreferenceKeys.AUTH_REQUEST_TIMEOUT] ?: SettingsDefaults.AUTH_REQUEST_TIMEOUT }

    // Write methods with validation
    suspend fun setNotificationPersistence(enabled: Boolean) {
        dataStore.edit { it[PreferenceKeys.NOTIFICATION_PERSISTENCE] = enabled }
    }

    suspend fun setDefaultForwardingDuration(minutes: Int) {
        require(minutes in 1..60) { "Duration must be between 1 and 60 minutes" }
        dataStore.edit { it[PreferenceKeys.DEFAULT_FORWARDING_DURATION] = minutes }
    }

    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { it[PreferenceKeys.THEME] = theme.name }
    }

    suspend fun setLockoutDuration(minutes: Int) {
        require(minutes in 1..60) { "Lockout duration must be between 1 and 60 minutes" }
        dataStore.edit { it[PreferenceKeys.LOCKOUT_DURATION] = minutes }
    }

    suspend fun setMaxFailedAttempts(attempts: Int) {
        require(attempts in 1..10) { "Max attempts must be between 1 and 10" }
        dataStore.edit { it[PreferenceKeys.MAX_FAILED_ATTEMPTS] = attempts }
    }

    // Additional setters follow same pattern...
}
```

### Preference Keys Definition
```kotlin
// Source: Official Android Documentation
package dev.notyouraverage.smscourier.data.settings

import androidx.datastore.preferences.core.*

object PreferenceKeys {
    // Main Settings
    val NOTIFICATION_PERSISTENCE = booleanPreferencesKey("notification_persistence")
    val DEFAULT_FORWARDING_DURATION = intPreferencesKey("default_forwarding_duration")
    val THEME = stringPreferencesKey("theme")

    // Advanced/Security Settings
    val LOCKOUT_DURATION = intPreferencesKey("lockout_duration")
    val MAX_FAILED_ATTEMPTS = intPreferencesKey("max_failed_attempts")
    val CHALLENGE_EXPIRY = intPreferencesKey("challenge_expiry")
    val MAX_PAIRING_RESEND_ATTEMPTS = intPreferencesKey("max_pairing_resend_attempts")
    val PAIRING_RESEND_COOLDOWN = intPreferencesKey("pairing_resend_cooldown")
    val AUTH_REQUEST_TIMEOUT = intPreferencesKey("auth_request_timeout")
}
```

### Settings Defaults (Conservative per CONTEXT.md)
```kotlin
// Source: Codebase (SecurityManager.kt) + CONTEXT.md decisions
package dev.notyouraverage.smscourier.data.settings

object SettingsDefaults {
    // Main Settings
    const val NOTIFICATION_PERSISTENCE = false  // Disabled by default
    const val DEFAULT_FORWARDING_DURATION = 15  // 15 minutes (minimum)
    val THEME = AppTheme.SYSTEM

    // Advanced/Security Settings (conservative defaults)
    const val LOCKOUT_DURATION = 15             // 15 minutes (matches current SecurityManager)
    const val MAX_FAILED_ATTEMPTS = 5           // (matches current SecurityManager)
    const val CHALLENGE_EXPIRY = 2              // 2 minutes (matches current SecurityManager)
    const val MAX_PAIRING_RESEND_ATTEMPTS = 5   // (matches current PairedDevicesViewModel)
    const val PAIRING_RESEND_COOLDOWN = 1       // 1 minute (matches current PairedDevicesViewModel)
    const val AUTH_REQUEST_TIMEOUT = 5          // 5 minutes
}

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}
```

### Testing Pattern (Matches Codebase)
```kotlin
// Source: Codebase test patterns (PairedDevicesViewModelTest.kt)
@ExperimentalCoroutinesApi
class SettingsRepositoryTest {
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Use a unique file name per test to avoid cross-test pollution
        settingsRepository = SettingsRepository(context)
    }

    @Test
    fun `default forwarding duration returns default when not set`() = runTest {
        settingsRepository.defaultForwardingDurationMinutes.test {
            assertEquals(SettingsDefaults.DEFAULT_FORWARDING_DURATION, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setDefaultForwardingDuration persists value`() = runTest {
        settingsRepository.defaultForwardingDurationMinutes.test {
            awaitItem() // Default

            settingsRepository.setDefaultForwardingDuration(30)

            assertEquals(30, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setDefaultForwardingDuration rejects invalid values`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { settingsRepository.setDefaultForwardingDuration(0) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { settingsRepository.setDefaultForwardingDuration(61) }
        }
    }
}
```

### Integration with Existing NavGraph Pattern
```kotlin
// Source: Codebase (NavGraph.kt)
@Composable
fun SmsCourierNavGraph(navController: NavHostController, ...) {
    val context = LocalContext.current

    // Create shared dependencies (existing pattern)
    val database = remember { SmsCourierDatabase.getDatabase(context) }
    val deviceRepository = remember { PairedDeviceRepository(database.pairedDeviceDao()) }
    val sessionRepository = remember { ForwardingSessionRepository(database.forwardingSessionDao()) }
    val smsSender = remember { SmsSender(context) }

    // Add settings repository (same pattern)
    val settingsRepository = remember { SettingsRepository(context) }

    // Pass to ViewModels that need it...
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| SharedPreferences | DataStore Preferences | 2020 (DataStore 1.0 stable 2021) | Async-first, Flow support, corruption handling |
| Sync reads | Flow-based reactive reads | DataStore 1.0 | No ANR risk, automatic UI updates |
| Manual listeners | Built-in Flow observation | DataStore 1.0 | Simpler, cleaner code |

**Deprecated/outdated:**
- SharedPreferences: Still works but discouraged for new code, no Flow support
- PreferenceManager: Deprecated in favor of DataStore or Jetpack Preference library

## Open Questions

Things that couldn't be fully resolved:

1. **DataStore file location for tests**
   - What we know: Need unique file per test to avoid pollution
   - What's unclear: Best practice for test cleanup
   - Recommendation: Use Robolectric with `ApplicationProvider.getApplicationContext()`, delete file in `@After`

2. **Combined settings Flow**
   - What we know: CONTEXT.md says "Claude's discretion" on whether to include
   - What's unclear: Will Phase 13/14 need all settings at once?
   - Recommendation: Start without it, add if needed. Individual flows are cleaner.

## Sources

### Primary (HIGH confidence)
- [Official Android DataStore Documentation](https://developer.android.com/topic/libraries/architecture/datastore) - Core patterns, API usage
- Codebase `PairedDeviceRepository.kt`, `ForwardingSessionRepository.kt` - Repository patterns
- Codebase `PairedDevicesViewModelTest.kt` - Testing patterns with Turbine
- Codebase `SecurityManager.kt`, `PairedDevicesViewModel.kt` - Current hardcoded constants to match

### Secondary (MEDIUM confidence)
- [All about Preferences DataStore (Android Developers Medium)](https://medium.com/androiddevelopers/all-about-preferences-datastore-cc7995679334) - Best practices
- [DataStore and Dependency Injection](https://medium.com/androiddevelopers/datastore-and-dependency-injection-ea32b95704e3) - DI patterns

### Tertiary (LOW confidence)
- Various blog posts on enum handling - patterns verified against official docs

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - DataStore already in project, official Android recommendation
- Architecture: HIGH - Matches existing repository patterns in codebase exactly
- Pitfalls: HIGH - From official documentation and common Android development issues
- Code examples: HIGH - Directly from official docs and adapted to codebase conventions

**Research date:** 2026-01-18
**Valid until:** 60 days (DataStore is stable, patterns unlikely to change)
