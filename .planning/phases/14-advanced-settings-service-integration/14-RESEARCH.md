# Phase 14: Advanced Settings & Service Integration - Research

**Researched:** 2026-01-19
**Domain:** Android Settings UI, Service Flow Observation, SecurityManager Refactoring
**Confidence:** HIGH

## Summary

This phase exposes 6 hardcoded security parameters to user configuration and makes services reactive to settings changes. The infrastructure is largely in place: SettingsRepository already has all 6 security settings with Flows, PreferenceKeys, and SettingsDefaults defined. What's missing is the UI to expose them and the refactoring of SecurityManager and MasterService to consume settings from the repository instead of hardcoded constants.

The main technical challenge is refactoring SecurityManager from using static companion object constants to instance properties that can receive settings values. MasterService already uses SettingsRepository for notification persistence, so the pattern for Flow observation exists. PairedDevicesViewModel also has hardcoded constants for pairing resend that need to consume settings.

**Primary recommendation:** Extend the existing SettingsViewModel to include security settings, add an "Advanced Settings" section to SettingsScreen (collapsible or separate screen), and inject SettingsRepository into SecurityManager constructor for reactive settings consumption.

## Current Implementation Analysis

### SecurityManager Constants (Lines 19-21)

Three hardcoded constants that need to become configurable:

```kotlin
// app/src/main/java/dev/notyouraverage/smscourier/security/SecurityManager.kt
companion object {
    const val MAX_FAILED_ATTEMPTS = 5
    const val LOCKOUT_DURATION_MS = 15 * 60 * 1000L // 15 minutes
    private const val CHALLENGE_EXPIRY_MS = 2 * 60 * 1000L // 2 minutes
}
```

**Usage locations:**
- `MAX_FAILED_ATTEMPTS`: Used in `recordFailedAttempt()` (line 99) to determine when to lock, and in `validateChallengeAndAuthenticate()` (line 205) for attempts remaining calculation
- `LOCKOUT_DURATION_MS`: Used in `recordFailedAttempt()` (line 100) to set `lockedUntil` timestamp
- `CHALLENGE_EXPIRY_MS`: Used in `generateChallenge()` (lines 123, 125) for challenge expiration

**Refactoring approach:** SecurityManager already takes `PairedDeviceRepository` in constructor. Add SettingsRepository as second parameter. Methods that use these constants will need to become suspend functions or receive values synchronously from cached settings.

### MasterService Auth Request Timeout (Line 456)

One hardcoded constant:

```kotlin
// app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt
if (age > 5 * 60 * 1000L) {  // AUTH_REQUEST_TIMEOUT
    Log.w(TAG, "Pending auth request for $senderPhone has expired")
    pendingAuthRequests.remove(senderPhone)
    return
}
```

**Refactoring approach:** MasterService already has `settingsRepository` instance (line 61, initialized line 130). Can collect `authRequestTimeoutMinutes` Flow and cache the value, or read synchronously when checking.

### PairedDevicesViewModel Pairing Resend Constants (Lines 32-33)

Two hardcoded constants:

```kotlin
// app/src/main/java/dev/notyouraverage/smscourier/viewmodels/PairedDevicesViewModel.kt
companion object {
    private const val MAX_RESEND_ATTEMPTS = 5
    private const val RESEND_COOLDOWN_MS = 60_000L // 1 minute
}
```

**Usage:** `canResendPairingRequest()` method uses both to determine if resend is allowed

**Refactoring approach:** Add SettingsRepository to ViewModel constructor, collect `maxPairingResendAttempts` and `pairingResendCooldownMinutes` Flows as StateFlows.

### SettingsRepository State (Complete)

All 6 security settings are already implemented with Flows and setters:

| Setting | Flow | Setter | Validation Range | Default |
|---------|------|--------|------------------|---------|
| lockoutDurationMinutes | Yes | Yes | 1-60 minutes | 15 |
| maxFailedAttempts | Yes | Yes | 1-10 attempts | 5 |
| challengeExpiryMinutes | Yes | Yes | 1-10 minutes | 2 |
| maxPairingResendAttempts | Yes | Yes | 1-10 attempts | 5 |
| pairingResendCooldownMinutes | Yes | Yes | 1-10 minutes | 1 |
| authRequestTimeoutMinutes | Yes | Yes | 1-30 minutes | 5 |

**Confidence:** HIGH - verified by reading SettingsRepository.kt

### SettingsDefaults (Complete)

```kotlin
// app/src/main/java/dev/notyouraverage/smscourier/data/settings/SettingsDefaults.kt
object SettingsDefaults {
    const val LOCKOUT_DURATION = 15 // minutes
    const val MAX_FAILED_ATTEMPTS = 5
    const val CHALLENGE_EXPIRY = 2 // minutes
    const val MAX_PAIRING_RESEND_ATTEMPTS = 5
    const val PAIRING_RESEND_COOLDOWN = 1 // minute
    const val AUTH_REQUEST_TIMEOUT = 5 // minutes
}
```

### PreferenceKeys (Complete)

All 6 keys defined in PreferenceKeys.kt - no additions needed.

## Technical Approach

### Adding Security Settings to UI

**Current SettingsScreen structure:**
1. Preferences section (notification persistence, default duration, theme)
2. Information section (permissions, about)

**Recommended approach for Advanced Settings:**
- Add collapsible "Advanced" section between Preferences and Information
- Section starts collapsed by default (per CONTEXT.md decision)
- Show header warning: "These settings affect security. Change with care."
- Use same ListItem patterns as existing settings

**UI Components needed:**
- `SettingsNumberInputItem` - new composable for freeform numeric input with validation
- Expand/collapse animation for section
- Error state display for invalid input

**SettingsViewModel extensions:**
- Add StateFlows for all 6 security settings
- Add setter functions with try/catch for validation errors
- Consider exposing validation error state

### Service Flow Observation Pattern

MasterService already demonstrates the pattern for settings observation (notification persistence):

```kotlin
// Current pattern in MasterService (lines 200-208)
private fun handleRecreateNotification() {
    serviceScope.launch {
        val shouldPersist = settingsRepository.notificationPersistence.first()
        if (shouldPersist) {
            recreateForegroundNotification()
        }
    }
}
```

For reactive settings in services, two approaches:

**Option A: On-demand reading (current pattern)**
```kotlin
serviceScope.launch {
    val timeout = settingsRepository.authRequestTimeoutMinutes.first()
    // use timeout
}
```

**Option B: Cached StateFlow collection**
```kotlin
private var authTimeoutMinutes: Int = SettingsDefaults.AUTH_REQUEST_TIMEOUT

init {
    serviceScope.launch {
        settingsRepository.authRequestTimeoutMinutes.collect { value ->
            authTimeoutMinutes = value
        }
    }
}
```

**Recommendation:** Option A for MasterService (settings checked infrequently). Option B for SecurityManager if methods need synchronous access without suspension.

### SecurityManager Refactor Strategy

Current SecurityManager has a fundamental issue: it uses hardcoded companion object constants but needs configurable values. Two approaches:

**Approach 1: Inject settings as constructor parameters (Simplest)**
```kotlin
class SecurityManager(
    private val deviceRepository: PairedDeviceRepository,
    private val maxFailedAttempts: Int = SettingsDefaults.MAX_FAILED_ATTEMPTS,
    private val lockoutDurationMs: Long = SettingsDefaults.LOCKOUT_DURATION * 60 * 1000L,
    private val challengeExpiryMs: Long = SettingsDefaults.CHALLENGE_EXPIRY * 60 * 1000L,
)
```
- Pro: Simple, testable, no async complications
- Con: Service must recreate SecurityManager when settings change (not truly reactive)

**Approach 2: Inject SettingsRepository, read synchronously**
```kotlin
class SecurityManager(
    private val deviceRepository: PairedDeviceRepository,
    private val settingsRepository: SettingsRepository,
) {
    // Cache values, update via Flow collection
    private var maxFailedAttempts = SettingsDefaults.MAX_FAILED_ATTEMPTS
    private var lockoutDurationMs = SettingsDefaults.LOCKOUT_DURATION * 60 * 1000L
    private var challengeExpiryMs = SettingsDefaults.CHALLENGE_EXPIRY * 60 * 1000L

    suspend fun startObservingSettings(scope: CoroutineScope) {
        scope.launch {
            settingsRepository.maxFailedAttempts.collect { maxFailedAttempts = it }
        }
        // ... other flows
    }
}
```
- Pro: Truly reactive, settings apply immediately
- Con: Requires lifecycle management, more complex testing

**Recommendation:** Approach 2 - inject SettingsRepository, cache values. MasterService already has CoroutineScope to manage observation. This matches CONTEXT.md decision: "Apply timing: Immediately - new values apply right away via Flow"

### Change Behavior Implementation

Per CONTEXT.md decisions:
- **Existing lockouts honored:** `lockedUntil` timestamp in database is authoritative. New lockout duration only affects NEW lockouts. No code change needed - this is already how it works.
- **Max attempts mid-lockout:** Device stays locked until original expiry. No code change needed.
- **Pending operations use old values:** AUTH_CHALLENGE expiry is computed at generation time, not validation time. No code change needed.
- **Rate limit counters persist:** `resendAttemptCount` in database not affected by setting change. No code change needed.

## Architecture Patterns

### Recommended Settings Section Structure

```
Settings Screen
├── Preferences (existing)
│   ├── Persistent notification
│   ├── Default forwarding duration
│   └── Theme
├── Advanced (NEW - collapsible)
│   ├── Warning header
│   ├── Security
│   │   ├── Lockout duration
│   │   ├── Max failed attempts
│   │   └── Challenge expiry
│   └── Pairing
│       ├── Max pairing resend attempts
│       ├── Pairing resend cooldown
│       └── Auth request timeout
└── Information (existing)
    ├── Permissions
    └── About
```

### Component Dependencies

```
┌─────────────────────┐
│   SettingsScreen    │
│   (Composable)      │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│  SettingsViewModel  │
│  (Extended)         │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│ SettingsRepository  │───── DataStore Preferences
│  (Already complete) │
└──────────┬──────────┘
           │
    ┌──────┴──────┬─────────────────────┐
    │             │                     │
┌───▼───┐   ┌─────▼─────┐   ┌───────────▼───────────┐
│Master │   │Security   │   │PairedDevicesViewModel │
│Service│   │Manager    │   │(Refactored)           │
└───────┘   └───────────┘   └───────────────────────┘
```

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Number input validation | Custom regex parsing | `require()` in SettingsRepository | Already implemented with proper ranges |
| Collapsible section | Custom expand/collapse | `AnimatedVisibility` | Built into Compose, handles animation |
| Dropdown selections | Custom dialog | `DropdownMenu` | Already used for duration in SettingsScreen |
| Flow observation in Service | Manual coroutine management | `serviceScope.launch` | Already established pattern in MasterService |

## Common Pitfalls

### Pitfall 1: Synchronous Settings Access in Non-Suspend Functions

**What goes wrong:** SecurityManager methods like `recordFailedAttempt()` use constants. Converting to Flow requires either making them suspend or caching.

**Why it happens:** Flow values can only be collected in suspend context.

**How to avoid:** Cache settings values in instance variables, update via Flow collection on CoroutineScope provided by MasterService.

**Warning signs:** Compilation errors about "Suspend function X should only be called from coroutine or another suspend function"

### Pitfall 2: Settings Not Applied to Existing Operations

**What goes wrong:** User changes lockout duration, expects existing locked device to be affected.

**Why it happens:** `lockedUntil` is an absolute timestamp stored in database, not computed from current settings.

**How to avoid:** Document clearly in UI that changes only affect NEW lockouts/operations. Per CONTEXT.md: "Existing lockouts: Honored"

**Warning signs:** User bug reports about settings "not working"

### Pitfall 3: Flow Collection Memory Leaks

**What goes wrong:** Starting Flow collection without cancellation on service destroy.

**Why it happens:** Forgetting to scope collections to `serviceScope` which is cancelled in `onDestroy()`.

**How to avoid:** Always use `serviceScope.launch { }` for Flow collections in MasterService.

**Warning signs:** ANR or memory issues after service restart

### Pitfall 4: Test Breakage from Constant Removal

**What goes wrong:** SecurityManagerTest and other tests reference `SecurityManager.MAX_FAILED_ATTEMPTS` companion object constant.

**Why it happens:** Tests use constants for assertions.

**How to avoid:** Update tests to use `SettingsDefaults.MAX_FAILED_ATTEMPTS` instead, or inject known values via constructor.

**Warning signs:** Test compilation failures after refactor

### Pitfall 5: Freeform Input UX Issues

**What goes wrong:** User enters invalid values repeatedly, gets frustrated.

**Why it happens:** Poor error feedback or confusing validation messages.

**How to avoid:**
- Show validation range in supporting text (e.g., "1-60 minutes")
- Immediate inline error display
- Clear error message explaining valid range

**Warning signs:** User confusion in testing

## Code Examples

### Existing SettingsSwitchItem Pattern

```kotlin
// Source: app/src/main/java/dev/notyouraverage/smscourier/composables/screens/SettingsScreen.kt
@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    )
}
```

### Existing SettingsSelectionItem Pattern

```kotlin
// Source: app/src/main/java/dev/notyouraverage/smscourier/composables/screens/SettingsScreen.kt
@Composable
private fun SettingsSelectionItem(
    title: String,
    selectedValue: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(selectedValue) },
        trailingContent = {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
```

### Flow Collection Pattern in MasterService

```kotlin
// Source: app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt
private fun handleRecreateNotification() {
    serviceScope.launch {
        val shouldPersist = settingsRepository.notificationPersistence.first()
        if (shouldPersist) {
            recreateForegroundNotification()
        }
    }
}
```

### ViewModel Flow Pattern

```kotlin
// Source: app/src/main/java/dev/notyouraverage/smscourier/viewmodels/SettingsViewModel.kt
val theme: StateFlow<AppTheme> = settingsRepository.theme
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)
```

### Proposed Number Input Composable

```kotlin
@Composable
private fun SettingsNumberInputItem(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    minValue: Int,
    maxValue: Int,
    unit: String,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Column {
                Text("$minValue-$maxValue $unit")
                if (isError && errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        trailingContent = {
            OutlinedTextField(
                value = value.toString(),
                onValueChange = { newValue ->
                    newValue.toIntOrNull()?.let { onValueChange(it) }
                },
                modifier = Modifier.width(80.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = isError,
                singleLine = true,
            )
        },
    )
}
```

### Proposed SecurityManager Refactor

```kotlin
class SecurityManager(
    private val deviceRepository: PairedDeviceRepository,
    private val settingsRepository: SettingsRepository,
) {
    // Cached settings values with defaults
    private var maxFailedAttempts = SettingsDefaults.MAX_FAILED_ATTEMPTS
    private var lockoutDurationMinutes = SettingsDefaults.LOCKOUT_DURATION
    private var challengeExpiryMinutes = SettingsDefaults.CHALLENGE_EXPIRY

    private val lockoutDurationMs: Long get() = lockoutDurationMinutes * 60 * 1000L
    private val challengeExpiryMs: Long get() = challengeExpiryMinutes * 60 * 1000L

    /**
     * Start observing settings changes. Call from MasterService.onCreate()
     * with serviceScope.
     */
    fun startObservingSettings(scope: CoroutineScope) {
        scope.launch {
            settingsRepository.maxFailedAttempts.collect { maxFailedAttempts = it }
        }
        scope.launch {
            settingsRepository.lockoutDurationMinutes.collect { lockoutDurationMinutes = it }
        }
        scope.launch {
            settingsRepository.challengeExpiryMinutes.collect { challengeExpiryMinutes = it }
        }
    }

    // Rest of methods unchanged but use instance properties instead of companion object constants
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Hardcoded constants | DataStore + Flow | Phase 12-13 established pattern | Settings can now be reactive |
| Plain Service | CoroutineScope in Service | Already in MasterService | Enables Flow collection |
| LiveData for settings | StateFlow | Kotlin Flow maturity | Better lifecycle handling |

**Current stack:**
- Jetpack DataStore Preferences for persistence
- Kotlin Flow for reactive data
- StateFlow for UI state in ViewModels
- Material3 ListItem for settings rows
- AnimatedVisibility for show/hide animations (Compose)

## Risk Areas

### Medium Risk: SecurityManager Test Updates

16 tests in SecurityManagerTest.kt reference hardcoded constants. Tests will need updates to:
1. Inject mock SettingsRepository or known values
2. Update assertions to use SettingsDefaults instead of companion object constants

**Mitigation:** Plan a dedicated test update task after SecurityManager refactor.

### Low Risk: Freeform Input Validation UX

CONTEXT.md specifies "Freeform numeric entry" with "Reject invalid input with error". Need clear UX for:
- Non-numeric input handling
- Out-of-range value feedback
- Keyboard type (numeric only)

**Mitigation:** Follow Material3 TextField error state patterns.

### Low Risk: Flow Collection Lifecycle

MasterService uses `serviceScope` which is cancelled in `onDestroy()`. SecurityManager needs to receive this scope to safely collect Flows.

**Mitigation:** Pass scope explicitly via `startObservingSettings(scope)` method.

## Open Questions

1. **Collapsible vs Separate Screen for Advanced Settings**
   - What we know: CONTEXT.md leaves this to Claude's discretion
   - What's unclear: Which provides better UX for 6 settings
   - Recommendation: Collapsible section - simpler navigation, keeps all settings on one screen

2. **Reset to Defaults Button**
   - What we know: CONTEXT.md leaves to discretion, SettingsRepository has `clearAllSettings()`
   - What's unclear: Should reset ALL settings or just advanced?
   - Recommendation: Include reset button for advanced section only, with confirmation dialog

3. **Save Confirmation Feedback**
   - What we know: Changes apply immediately via Flow
   - What's unclear: Does user need visual confirmation?
   - Recommendation: No snackbar needed - immediate field update is sufficient feedback

## Sources

### Primary (HIGH confidence)
- `/Users/aanand/AndroidStudioProjects/SMSCourier/app/src/main/java/dev/notyouraverage/smscourier/security/SecurityManager.kt` - hardcoded constants, current implementation
- `/Users/aanand/AndroidStudioProjects/SMSCourier/app/src/main/java/dev/notyouraverage/smscourier/repository/SettingsRepository.kt` - all Flows already implemented
- `/Users/aanand/AndroidStudioProjects/SMSCourier/app/src/main/java/dev/notyouraverage/smscourier/data/settings/SettingsDefaults.kt` - default values
- `/Users/aanand/AndroidStudioProjects/SMSCourier/app/src/main/java/dev/notyouraverage/smscourier/services/foreground/MasterService.kt` - existing Flow observation pattern
- `/Users/aanand/AndroidStudioProjects/SMSCourier/app/src/main/java/dev/notyouraverage/smscourier/composables/screens/SettingsScreen.kt` - UI patterns
- `/Users/aanand/AndroidStudioProjects/SMSCourier/app/src/main/java/dev/notyouraverage/smscourier/viewmodels/PairedDevicesViewModel.kt` - pairing resend constants

### Secondary (MEDIUM confidence)
- [Android Developers - StateFlow and SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow) - Flow best practices
- [Android Developers - Kotlin flows on Android](https://developer.android.com/kotlin/flow) - Flow collection guidance
- [A safer way to collect flows from Android UIs](https://medium.com/androiddevelopers/a-safer-way-to-collect-flows-from-android-uis-23080b1f8bda) - Lifecycle-aware collection

## Metadata

**Confidence breakdown:**
- Current implementation analysis: HIGH - direct code inspection
- Technical approach: HIGH - based on existing patterns in codebase
- UI patterns: HIGH - following established SettingsScreen patterns
- Service integration: HIGH - MasterService already demonstrates pattern
- Risk areas: MEDIUM - test updates scope estimated

**Research date:** 2026-01-19
**Valid until:** 2026-02-19 (stable patterns, internal codebase)
