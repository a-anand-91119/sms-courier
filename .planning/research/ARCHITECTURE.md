# Architecture Patterns for Settings Feature

**Domain:** Android Settings Screen
**Project:** SMS Courier (v0.0.63)
**Researched:** 2026-01-18

## Recommended Architecture

The Settings feature should follow the existing Clean Architecture with MVVM pattern already established in SMS Courier, extended with a dedicated settings data layer using Jetpack DataStore.

```
+------------------+     +------------------+     +------------------+
|   SettingsScreen |---->| SettingsViewModel|---->| SettingsRepository|
|   (Compose UI)   |<----|   (StateFlow)    |<----|    (DataStore)   |
+------------------+     +------------------+     +------------------+
                                                           |
                                                           v
                                   +------------------------------------------+
                                   |              MasterService               |
                                   | (Observes settings via SettingsRepository)|
                                   +------------------------------------------+
                                                           |
                                                           v
                                   +------------------------------------------+
                                   |             BootReceiver                 |
                                   | (Reads autoStart from SettingsRepository)|
                                   +------------------------------------------+
```

### Component Boundaries

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| SettingsScreen | Render settings UI, handle user input | SettingsViewModel |
| SettingsViewModel | Manage UI state, validate input | SettingsRepository |
| SettingsRepository | Abstract DataStore operations, expose Flows | DataStore, consumers |
| SettingsDataStore | Persist/retrieve settings values | Android DataStore |
| MasterService | Observe relevant settings, apply at runtime | SettingsRepository |
| BootReceiver | Check autoStart setting, start service | SettingsRepository |
| ThemeWrapper | Apply theme based on settings | SettingsRepository |

### Data Flow

**Settings Write Flow:**
```
1. User toggles setting in SettingsScreen
2. SettingsScreen calls ViewModel.updateSetting(key, value)
3. ViewModel calls SettingsRepository.setSetting(key, value)
4. Repository writes to DataStore (suspending, on IO dispatcher)
5. DataStore emits new value via Flow
6. All observers (ViewModel, Service) receive update
7. UI recomposes with new state
```

**Settings Read Flow (UI):**
```
1. SettingsScreen created
2. ViewModel collects SettingsRepository.settings as StateFlow
3. Compose observes StateFlow with collectAsState()
4. Initial values render immediately (from cache or defaults)
5. When DataStore emits, UI automatically updates
```

**Settings Read Flow (Service):**
```
1. MasterService.onCreate() collects relevant settings
2. SettingsRepository emits current values
3. Service applies settings (e.g., notification behavior)
4. When settings change, Flow emits, service updates behavior
5. No need for restart - reactive updates
```

## Patterns to Follow

### Pattern 1: DataStore Repository

Encapsulate DataStore behind a repository for testability and abstraction.

**What:** Single repository class that owns the DataStore instance and exposes typed Flows for each setting.

**When:** Always. Never access DataStore directly from ViewModels or Services.

**Example:**
```kotlin
class SettingsRepository(
    private val dataStore: DataStore<Preferences>
) {
    val autoStartOnBoot: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[AUTO_START_KEY] ?: false }
        .distinctUntilChanged()

    val defaultForwardingDuration: Flow<Int> = dataStore.data
        .map { prefs -> prefs[DEFAULT_DURATION_KEY] ?: 30 }
        .distinctUntilChanged()

    val themeMode: Flow<ThemeMode> = dataStore.data
        .map { prefs ->
            ThemeMode.fromString(prefs[THEME_KEY] ?: ThemeMode.SYSTEM.name)
        }
        .distinctUntilChanged()

    val notificationPersistence: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[NOTIFICATION_PERSIST_KEY] ?: true }
        .distinctUntilChanged()

    suspend fun setAutoStartOnBoot(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[AUTO_START_KEY] = enabled }
    }

    suspend fun setDefaultForwardingDuration(minutes: Int) {
        dataStore.edit { prefs -> prefs[DEFAULT_DURATION_KEY] = minutes }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[THEME_KEY] = mode.name }
    }

    suspend fun setNotificationPersistence(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[NOTIFICATION_PERSIST_KEY] = enabled }
    }

    companion object {
        private val AUTO_START_KEY = booleanPreferencesKey("auto_start_on_boot")
        private val DEFAULT_DURATION_KEY = intPreferencesKey("default_forwarding_duration")
        private val THEME_KEY = stringPreferencesKey("theme_mode")
        private val NOTIFICATION_PERSIST_KEY = booleanPreferencesKey("notification_persistence")
    }
}
```

### Pattern 2: ViewModel Factory Injection

Match existing pattern for ViewModel creation.

**What:** Use ViewModelProvider.Factory to inject SettingsRepository.

**When:** Creating SettingsViewModel in NavGraph.

**Example:**
```kotlin
class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.autoStartOnBoot,
        settingsRepository.defaultForwardingDuration,
        settingsRepository.themeMode,
        settingsRepository.notificationPersistence
    ) { autoStart, duration, theme, notifPersist ->
        SettingsUiState(
            autoStartOnBoot = autoStart,
            defaultForwardingDuration = duration,
            themeMode = theme,
            notificationPersistence = notifPersist
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState()
    )

    fun updateAutoStart(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoStartOnBoot(enabled)
        }
    }

    // ... other update methods

    class Factory(
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository) as T
        }
    }
}
```

### Pattern 3: Service Settings Observation

Services should observe settings reactively rather than reading once.

**What:** Collect settings Flows in ServiceScope and react to changes.

**When:** MasterService needs to change behavior based on settings.

**Example:**
```kotlin
// In MasterService
private fun observeSettings() {
    serviceScope.launch {
        settingsRepository.notificationPersistence.collect { persist ->
            notificationPersistenceEnabled = persist
            // If notification was dismissed and persistence is re-enabled, recreate it
            if (persist && !notificationVisible) {
                recreateForegroundNotification()
            }
        }
    }
}
```

### Pattern 4: Boot Receiver with Settings Check

BootReceiver should read settings before starting service.

**What:** Check autoStartOnBoot setting in BootReceiver.onReceive().

**When:** Device boots and app needs to decide whether to start service.

**Example:**
```kotlin
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Use runBlocking carefully here - boot receiver has limited time
            val autoStart = runBlocking {
                val dataStore = context.settingsDataStore
                dataStore.data.first()[AUTO_START_KEY] ?: false
            }

            if (autoStart) {
                Intent(context, MasterService::class.java).apply {
                    action = MasterService.START_SELF
                    context.startForegroundService(this)
                }
            }
        }
    }
}
```

### Pattern 5: Theme Application at App Level

Theme settings should be applied at the top level of the UI tree.

**What:** Observe theme setting in MainActivity or a wrapper composable.

**When:** Theme setting changes should affect entire app immediately.

**Example:**
```kotlin
// In MainActivity
@Composable
fun SmsCourierApp(settingsRepository: SettingsRepository) {
    val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    smscourierTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            val navController = rememberNavController()
            SmsCourierNavGraph(navController = navController)
        }
    }
}
```

## Anti-Patterns to Avoid

### Anti-Pattern 1: Direct DataStore Access from UI

**What:** Accessing DataStore directly from Composables or ViewModels without repository.

**Why bad:**
- Violates separation of concerns
- Makes testing difficult
- Creates multiple DataStore instances (potential corruption)

**Instead:** Always go through SettingsRepository.

### Anti-Pattern 2: Synchronous DataStore Reads on UI Thread

**What:** Using `runBlocking` to read DataStore in UI code.

**Why bad:**
- Can cause ANRs
- Blocks UI thread
- Defeats purpose of Flow-based API

**Instead:** Use `collectAsState()` with sensible defaults for initial state.

### Anti-Pattern 3: Creating Multiple DataStore Instances

**What:** Creating DataStore in multiple places (Activity, Service, etc.).

**Why bad:**
- Can cause data corruption
- Inconsistent state across components
- Memory waste

**Instead:** Single DataStore instance, created as extension property on Context:
```kotlin
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)
```

### Anti-Pattern 4: Caching DataStore Values

**What:** Creating local cache variables that mirror DataStore state.

**Why bad:**
- Invalidates DataStore's consistency guarantees
- Creates stale data bugs
- Adds unnecessary complexity

**Instead:** Always read from Flow; use `stateIn()` for caching in ViewModels.

### Anti-Pattern 5: Ignoring Settings Changes in Service

**What:** Reading settings once at service start and never updating.

**Why bad:**
- Settings changes don't take effect until service restart
- Poor user experience
- Inconsistent behavior

**Instead:** Observe settings Flows in ServiceScope and react to changes.

## Suggested Build Order

Dependencies between components determine the build order:

```
Phase 1: Data Layer (Foundation)
   |
   +-- 1.1 DataStore setup (extension property, keys)
   |
   +-- 1.2 SettingsRepository (with typed Flows)
   |
   +-- 1.3 Unit tests for SettingsRepository

Phase 2: ViewModel Layer
   |
   +-- 2.1 SettingsViewModel (combines Flows, exposes update methods)
   |
   +-- 2.2 Unit tests for SettingsViewModel

Phase 3: UI Layer
   |
   +-- 3.1 SettingsScreen (main settings)
   |
   +-- 3.2 Advanced settings section
   |
   +-- 3.3 About section

Phase 4: Integration Points
   |
   +-- 4.1 Theme integration in MainActivity
   |
   +-- 4.2 MasterService settings observation
   |
   +-- 4.3 BootReceiver for auto-start
   |
   +-- 4.4 Notification persistence in MasterService

Phase 5: NavGraph Integration
   |
   +-- 5.1 Replace placeholder SettingsScreen
   |
   +-- 5.2 Wire up SettingsRepository in NavGraph
```

### Phase Rationale

1. **Data Layer First:** Everything depends on SettingsRepository. Building this first enables parallel development of ViewModel and integration points.

2. **ViewModel Before UI:** ViewModel defines the contract that UI implements. Having stable ViewModel APIs enables UI iteration without churn.

3. **UI After Foundation:** With ViewModel complete, UI is purely presentational and can be designed/iterated independently.

4. **Integration Points Last:** These touch existing code and benefit from having the core settings infrastructure complete and tested.

5. **NavGraph Integration Final:** Small change that connects everything; should be done after all pieces are tested.

## File Structure

Recommended file locations following existing project conventions:

```
app/src/main/java/dev/notyouraverage/smscourier/
  |
  +-- data/
  |     +-- settings/
  |           +-- SettingsDataStore.kt    # DataStore setup, keys
  |           +-- SettingsRepository.kt   # Repository with Flows
  |           +-- ThemeMode.kt            # Enum for theme options
  |
  +-- viewmodels/
  |     +-- SettingsViewModel.kt          # ViewModel with Factory
  |
  +-- composables/
  |     +-- screens/
  |           +-- SettingsScreen.kt       # Main settings screen
  |           +-- SettingsComponents.kt   # Reusable setting items
  |
  +-- receivers/
  |     +-- BootReceiver.kt               # New receiver for auto-start
  |
  +-- navigation/
        +-- NavGraph.kt                   # Update to wire SettingsScreen

app/src/test/java/dev/notyouraverage/smscourier/
  |
  +-- data/
  |     +-- settings/
  |           +-- SettingsRepositoryTest.kt
  |
  +-- viewmodels/
        +-- SettingsViewModelTest.kt
```

## Scalability Considerations

| Concern | Current (10 settings) | Future (50+ settings) |
|---------|----------------------|----------------------|
| DataStore performance | Excellent | Still good - single file read |
| Repository complexity | Manageable | Consider grouping by category |
| ViewModel state | Simple data class | Nested state or multiple VMs |
| UI organization | Single scrollable list | Nested navigation or tabs |

**Recommendation:** Start simple. The current settings list (~10 items) doesn't need complex organization. Add structure only when complexity demands it.

## Sources

- [Android DataStore Documentation](https://developer.android.com/topic/libraries/architecture/datastore)
- [Mastering Jetpack DataStore in 2025](https://medium.com/design-bootcamp/mastering-jetpack-datastore-in-2025-replace-sharedpreferences-with-modern-apis-b065d2addd9e)
- [DataStore vs SharedPreferences in 2025](https://www.atipik.ch/en/blog/android-jetpack-datastore-vs-sharedpreferences)
- [Android Developer Documentation - DataStore](https://developer.android.com/codelabs/android-preferences-datastore)
- [GitHub - Android AutoStart App](https://github.com/PerfsolTech/Android-AutoStart-App)
- Project codebase analysis: `/Users/aanand/AndroidStudioProjects/SMSCourier/`

---

*Architecture research: 2026-01-18*
*Update when major patterns change*
