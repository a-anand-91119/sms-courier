# Phase 22: Auto-Cleanup with WorkManager - Research

**Researched:** 2026-02-08
**Domain:** Android WorkManager periodic tasks, CoroutineWorker with Room database, DataStore timestamp storage
**Confidence:** HIGH

## Summary

Phase 22 implements automatic cleanup of old session/message history using WorkManager's periodic task scheduling. This research covers three technical domains: (1) WorkManager's PeriodicWorkRequest API with flex intervals and constraints for battery-efficient scheduling, (2) CoroutineWorker integration with existing Room cleanup operations from Phase 21, and (3) DataStore Preferences for tracking last cleanup timestamp with relative time display.

**Key findings:**
- WorkManager 2.9.1 (already in project) provides PeriodicWorkRequest with 7-day intervals and flex windows for battery optimization
- CoroutineWorker defaults to Dispatchers.Default and works seamlessly with existing Room repository suspend functions
- WorkManager persists across device reboots and respects Doze mode, making it ideal for background maintenance tasks
- enqueueUniquePeriodicWork with ExistingPeriodicWorkPolicy manages toggle on/off behavior cleanly
- longPreferencesKey in DataStore stores millisecond timestamps for last cleanup tracking
- Android DateUtils.getRelativeTimeSpanString provides native relative time formatting ("2 days ago")

**Primary recommendation:** Use PeriodicWorkRequestBuilder with 7-day interval and 1-day flex window, CoroutineWorker calling existing ForwardingSessionRepository.cleanupOldSessions, DataStore Long preference for last cleanup timestamp, and enqueueUniquePeriodicWork with KEEP/CANCEL_AND_REENQUEUE policies for toggle management.

## Standard Stack

### Core Dependencies (Already in Project)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| androidx.work.runtime.ktx | 2.9.1 | Periodic background tasks | Official Google library for guaranteed background execution, replaces JobScheduler and AlarmManager |
| androidx.room | 2.6.1 | Database cleanup operations | Already used in Phase 21 for cleanupOldSessions method |
| androidx.datastore.preferences | 1.1.1 | Last cleanup timestamp storage | Already used for all settings, supports longPreferencesKey for milliseconds |
| kotlinx.coroutines | Latest | CoroutineWorker and suspend functions | WorkManager CoroutineWorker is coroutine-native |

### Supporting (Already Available)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| android.text.format.DateUtils | Platform API | Relative time formatting | Native Android utility for "2 days ago" display |
| androidx.lifecycle.viewmodel | Latest | ViewModel state management | Toggle state and last cleanup display in SettingsViewModel |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| PeriodicWorkRequest | AlarmManager | AlarmManager not guaranteed, requires more code, doesn't respect Doze mode |
| CoroutineWorker | Worker or RxWorker | Worker requires blocking code, RxWorker adds RxJava dependency |
| DataStore Long | Custom database table | Overkill for single timestamp, DataStore already in use |
| DateUtils | Manual relative time logic | DateUtils handles all edge cases (today, yesterday, plurals, localization) |

**Installation:**
All dependencies already present in project. No new dependencies needed.

## Architecture Patterns

### Recommended Project Structure

```
app/src/main/java/dev/notyouraverage/smscourier/
├── workers/
│   └── CleanupWorker.kt                    # CoroutineWorker for cleanup task
├── data/settings/
│   ├── PreferenceKeys.kt                   # Add AUTO_CLEANUP_ENABLED, LAST_CLEANUP_TIMESTAMP
│   └── SettingsDefaults.kt                 # Add AUTO_CLEANUP_ENABLED = true
├── repository/
│   ├── SettingsRepository.kt               # Add auto-cleanup toggle and timestamp methods
│   └── ForwardingSessionRepository.kt      # Already has cleanupOldSessions (Phase 21)
├── viewmodels/
│   └── SettingsViewModel.kt                # Add auto-cleanup toggle state and last cleanup display
├── composables/screens/
│   └── SettingsScreen.kt                   # Add auto-cleanup toggle (hide when retention = Forever)
└── utils/
    └── WorkManagerHelper.kt                # Utility for scheduling/canceling cleanup work
```

### Pattern 1: PeriodicWorkRequest with Flex Interval

**What:** 7-day periodic task with 1-day flex window for battery-efficient scheduling
**When to use:** Background maintenance tasks that don't need precise timing

**Example:**
```kotlin
// Source: Context7 androidx/androidx WorkManager API
val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(
    repeatInterval = 7,
    repeatIntervalTimeUnit = TimeUnit.DAYS,
    flexTimeInterval = 1,
    flexTimeIntervalUnit = TimeUnit.DAYS
)
    .setConstraints(
        Constraints.Builder()
            .setRequiresBatteryNotLow(true)  // User decision from CONTEXT.md
            .build()
    )
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
        TimeUnit.MILLISECONDS
    )
    .build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "cleanup_old_history",
    ExistingPeriodicWorkPolicy.KEEP,  // Don't replace if already scheduled
    cleanupRequest
)
```

**Key insight:** Flex window means WorkManager can run cleanup anytime in the last 24 hours of the 7-day interval, allowing it to batch with other background work for better battery efficiency.

### Pattern 2: CoroutineWorker with Room Repository

**What:** Worker that calls existing repository suspend functions
**When to use:** Background tasks that use Room database operations

**Example:**
```kotlin
// Source: Context7 androidx/androidx CoroutineWorker API + existing Phase 21 cleanup
class CleanupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = SmsCourierDatabase.getInstance(applicationContext)
            val sessionRepository = ForwardingSessionRepository(database.forwardingSessionDao())
            val settingsRepository = SettingsRepository(applicationContext)

            // Get retention setting
            val retentionDays = settingsRepository.historyRetentionDays.first()

            // Run cleanup (existing method from Phase 21)
            val result = sessionRepository.cleanupOldSessions(retentionDays)

            // Update last cleanup timestamp
            settingsRepository.setLastCleanupTimestamp(System.currentTimeMillis())

            Log.i(TAG, "Cleanup complete: ${result.sessionCount} sessions, ${result.messageCount} messages deleted")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
            // Exponential backoff will retry
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "CleanupWorker"
    }
}
```

**Key insight:** CoroutineWorker.doWork() is a suspend function that defaults to Dispatchers.Default. Using withContext(Dispatchers.IO) ensures database operations run on IO dispatcher, matching existing repository patterns.

### Pattern 3: Unique Work Name for Toggle Management

**What:** enqueueUniquePeriodicWork with unique name for enable/disable toggle
**When to use:** User-controlled periodic tasks that should have at most one instance

**Example:**
```kotlin
// Source: Context7 androidx/androidx WorkManager API + WebSearch verified patterns
const val CLEANUP_WORK_NAME = "cleanup_old_history"

// When toggle is enabled
fun scheduleCleanup(context: Context) {
    val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(7, TimeUnit.DAYS, 1, TimeUnit.DAYS)
        .setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
        )
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        CLEANUP_WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,  // If already scheduled, keep existing
        cleanupRequest
    )
}

// When toggle is disabled
fun cancelCleanup(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(CLEANUP_WORK_NAME)
}
```

**Key insight:** KEEP policy means enabling toggle multiple times won't reschedule work unnecessarily. CANCEL_AND_REENQUEUE is used if user changes settings and wants to reschedule immediately.

### Pattern 4: DataStore Long Preference for Timestamp

**What:** Store millisecond timestamp in DataStore with Flow for UI display
**When to use:** Tracking when background work last executed

**Example:**
```kotlin
// Source: Context7 androidx/androidx DataStore API + existing PreferenceKeys pattern
// PreferenceKeys.kt
object PreferenceKeys {
    // ... existing keys
    val AUTO_CLEANUP_ENABLED = booleanPreferencesKey("auto_cleanup_enabled")
    val LAST_CLEANUP_TIMESTAMP = longPreferencesKey("last_cleanup_timestamp")
}

// SettingsDefaults.kt
object SettingsDefaults {
    // ... existing defaults
    const val AUTO_CLEANUP_ENABLED = true
    const val LAST_CLEANUP_TIMESTAMP = 0L  // 0 = never run
}

// SettingsRepository.kt
val autoCleanupEnabled: Flow<Boolean> = dataStore.data
    .map { preferences ->
        preferences[PreferenceKeys.AUTO_CLEANUP_ENABLED]
            ?: SettingsDefaults.AUTO_CLEANUP_ENABLED
    }

val lastCleanupTimestamp: Flow<Long> = dataStore.data
    .map { preferences ->
        preferences[PreferenceKeys.LAST_CLEANUP_TIMESTAMP]
            ?: SettingsDefaults.LAST_CLEANUP_TIMESTAMP
    }

suspend fun setAutoCleanupEnabled(enabled: Boolean) {
    dataStore.edit { preferences ->
        preferences[PreferenceKeys.AUTO_CLEANUP_ENABLED] = enabled
    }
}

suspend fun setLastCleanupTimestamp(timestampMs: Long) {
    dataStore.edit { preferences ->
        preferences[PreferenceKeys.LAST_CLEANUP_TIMESTAMP] = timestampMs
    }
}
```

### Pattern 5: Relative Time Display with DateUtils

**What:** Native Android utility for "2 days ago" formatting
**When to use:** Displaying timestamps in user-friendly relative format

**Example:**
```kotlin
// Source: WebSearch verified Android DateUtils pattern
import android.text.format.DateUtils

fun formatLastCleanup(timestampMs: Long): String {
    if (timestampMs == 0L) {
        return "Never run"
    }

    return DateUtils.getRelativeTimeSpanString(
        timestampMs,
        System.currentTimeMillis(),
        DateUtils.DAY_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}

// Usage in Compose
val lastCleanup by viewModel.lastCleanupTimestamp.collectAsState()
Text(
    text = "Last cleaned: ${formatLastCleanup(lastCleanup)}",
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
```

**Key insight:** DateUtils handles all edge cases: "today", "yesterday", "2 days ago", "in 5 hours" (future), localization, and pluralization. No need for manual string formatting.

### Pattern 6: Toggle Visibility Based on Retention Setting

**What:** Hide auto-cleanup toggle when retention is "Forever" (no cleanup needed)
**When to use:** UI should reflect logical dependencies between settings

**Example:**
```kotlin
// Source: User decision from CONTEXT.md + existing SettingsScreen pattern
// SettingsScreen.kt
val historyRetentionDays by viewModel.historyRetentionDays.collectAsState()
val autoCleanupEnabled by viewModel.autoCleanupEnabled.collectAsState()

// Only show auto-cleanup toggle if retention is not Forever
if (historyRetentionDays != 0) {
    item {
        SettingsSwitchItem(
            title = "Auto-cleanup",
            subtitle = "Clean up old data automatically",
            checked = autoCleanupEnabled,
            onCheckedChange = { viewModel.setAutoCleanupEnabled(it) }
        )
    }

    // Show last cleanup time
    if (autoCleanupEnabled) {
        val lastCleanup by viewModel.lastCleanupTimestamp.collectAsState()
        item {
            SettingsTextItem(
                title = "Last cleaned",
                value = formatLastCleanup(lastCleanup)
            )
        }
    }
}
```

### Anti-Patterns to Avoid

- **Scheduling work on every app launch**: Check if work is already scheduled before calling enqueueUniquePeriodicWork
- **Using MIN_PERIODIC_INTERVAL_MILLIS (15 minutes) for cleanup**: Too frequent for cleanup tasks, wastes battery
- **Not using flex interval**: Without flex, WorkManager must wake device at exact time, losing battery optimization
- **Storing timestamp in database**: DataStore is simpler and already used for all settings
- **Custom retry logic in Worker**: WorkManager's setBackoffCriteria handles retries automatically
- **Blocking database calls in doWork()**: Always use withContext(Dispatchers.IO) for Room operations

## Don't Hand-Roll

Problems that have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Periodic task scheduling | AlarmManager with BroadcastReceiver | WorkManager PeriodicWorkRequest | WorkManager handles all edge cases: boot, Doze, battery optimization, constraints, retries |
| Retry with backoff | Manual retry counter in Worker | setBackoffCriteria with BackoffPolicy.EXPONENTIAL | WorkManager tracks attempts, calculates delays, respects constraints on retries |
| Timestamp storage | Custom database table for settings | DataStore longPreferencesKey | DataStore is type-safe, atomic, already used for all settings |
| Relative time formatting | Manual string concatenation | DateUtils.getRelativeTimeSpanString | Handles today/yesterday, plurals, localization, future times |
| Unique work enforcement | Manual flag checking | enqueueUniquePeriodicWork | WorkManager guarantees only one instance, handles concurrent enqueue attempts |
| Worker cancellation | Custom cancellation flag | WorkManager.cancelUniqueWork | Cancels pending and running work, handles cleanup automatically |

**Key insight:** WorkManager is mature (2.9.1 as of 2026) and handles all complexity of Android's battery optimization (Doze, App Standby), boot-time rescheduling, and constraint-based execution. Custom implementations consistently miss edge cases.

## Common Pitfalls

### Pitfall 1: Not Respecting Minimum Intervals

**What goes wrong:** Setting interval below 15 minutes throws IllegalArgumentException
**Why it happens:** WorkManager enforces MIN_PERIODIC_INTERVAL_MILLIS (900000ms = 15 minutes)
**How to avoid:** Use TimeUnit.DAYS for cleanup intervals (7 days = well above minimum)
**Warning signs:** Crash with "interval must be at least 900000" message

**Prevention:**
```kotlin
// WRONG - 5 minutes is below minimum
val request = PeriodicWorkRequestBuilder<CleanupWorker>(5, TimeUnit.MINUTES).build()

// RIGHT - 7 days is well above 15 minute minimum
val request = PeriodicWorkRequestBuilder<CleanupWorker>(7, TimeUnit.DAYS).build()
```

### Pitfall 2: Flex Interval Larger Than Repeat Interval

**What goes wrong:** IllegalArgumentException when flex interval >= repeat interval
**Why it happens:** Flex must be less than repeat interval (flex is window within interval)
**How to avoid:** Flex interval should be 10-20% of repeat interval
**Warning signs:** Crash with "flex interval must be less than interval" message

**Prevention:**
```kotlin
// WRONG - 7 day flex for 7 day interval
val request = PeriodicWorkRequestBuilder<CleanupWorker>(7, TimeUnit.DAYS, 7, TimeUnit.DAYS).build()

// RIGHT - 1 day flex for 7 day interval (14%)
val request = PeriodicWorkRequestBuilder<CleanupWorker>(7, TimeUnit.DAYS, 1, TimeUnit.DAYS).build()
```

### Pitfall 3: Not Handling Worker Result Properly

**What goes wrong:** Worker returns Result.success() even on failure, no retry happens
**Why it happens:** Forgetting to catch exceptions and return Result.retry()
**How to avoid:** Wrap cleanup in try-catch, return Result.retry() on failure
**Warning signs:** Cleanup silently fails, WorkManager doesn't retry, user sees "Never run"

**Prevention:**
```kotlin
// WRONG - exception crashes worker, no retry
override suspend fun doWork(): Result {
    val result = repository.cleanupOldSessions(retentionDays)
    return Result.success()
}

// RIGHT - catches exceptions, returns retry
override suspend fun doWork(): Result = try {
    val result = repository.cleanupOldSessions(retentionDays)
    settingsRepository.setLastCleanupTimestamp(System.currentTimeMillis())
    Result.success()
} catch (e: Exception) {
    Log.e(TAG, "Cleanup failed", e)
    Result.retry()  // WorkManager will retry with exponential backoff
}
```

### Pitfall 4: Updating Timestamp Before Cleanup Completes

**What goes wrong:** Last cleanup shows current time but cleanup failed
**Why it happens:** Setting timestamp before cleanup operation completes successfully
**How to avoid:** Update timestamp only after cleanup returns CleanupResult
**Warning signs:** UI shows "Last cleaned 5 minutes ago" but data still exists

**Prevention:**
```kotlin
// WRONG - sets timestamp before cleanup
override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    settingsRepository.setLastCleanupTimestamp(System.currentTimeMillis())
    val result = sessionRepository.cleanupOldSessions(retentionDays)
    Result.success()
}

// RIGHT - sets timestamp only after successful cleanup
override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    try {
        val result = sessionRepository.cleanupOldSessions(retentionDays)
        // Only update timestamp after successful cleanup
        settingsRepository.setLastCleanupTimestamp(System.currentTimeMillis())
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}
```

### Pitfall 5: Not Canceling Work on Toggle Disable

**What goes wrong:** Cleanup continues running even after user disables toggle
**Why it happens:** Forgetting to call cancelUniqueWork when toggle is disabled
**How to avoid:** Call WorkManager.cancelUniqueWork immediately in toggle change handler
**Warning signs:** User disables toggle but cleanup still happens, data gets deleted unexpectedly

**Prevention:**
```kotlin
// WRONG - only updates DataStore preference
fun setAutoCleanupEnabled(enabled: Boolean) {
    viewModelScope.launch {
        settingsRepository.setAutoCleanupEnabled(enabled)
        // Work keeps running!
    }
}

// RIGHT - cancels work when disabled, schedules when enabled
fun setAutoCleanupEnabled(enabled: Boolean) {
    viewModelScope.launch {
        settingsRepository.setAutoCleanupEnabled(enabled)
        if (enabled) {
            scheduleCleanup(getApplication<Application>())
        } else {
            cancelCleanup(getApplication<Application>())
        }
    }
}
```

### Pitfall 6: Forever Retention with Auto-Cleanup Enabled

**What goes wrong:** Toggle is ON but cleanup does nothing (retention = Forever means retentionDays = 0)
**Why it happens:** Not hiding toggle when retention is Forever
**How to avoid:** Only show toggle when historyRetentionDays != 0
**Warning signs:** User confused why toggle has no effect

**Prevention:**
```kotlin
// WRONG - shows toggle always
item {
    SettingsSwitchItem(
        title = "Auto-cleanup",
        checked = autoCleanupEnabled,
        onCheckedChange = { viewModel.setAutoCleanupEnabled(it) }
    )
}

// RIGHT - only shows toggle when retention is not Forever
if (historyRetentionDays != 0) {
    item {
        SettingsSwitchItem(
            title = "Auto-cleanup",
            checked = autoCleanupEnabled,
            onCheckedChange = { viewModel.setAutoCleanupEnabled(it) }
        )
    }
}
```

## Code Examples

Verified patterns from official sources and existing codebase:

### Complete CleanupWorker Implementation

```kotlin
// Source: Context7 CoroutineWorker API + Phase 21 cleanup + CONTEXT.md decisions
package dev.notyouraverage.smscourier.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.notyouraverage.smscourier.data.SmsCourierDatabase
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CleanupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting scheduled cleanup")

            // Initialize repositories
            val database = SmsCourierDatabase.getInstance(applicationContext)
            val sessionRepository = ForwardingSessionRepository(database.forwardingSessionDao())
            val settingsRepository = SettingsRepository(applicationContext)

            // Get current retention setting
            val retentionDays = settingsRepository.historyRetentionDays.first()

            // Run cleanup (uses existing Phase 21 method)
            val result = sessionRepository.cleanupOldSessions(retentionDays)

            // Update last cleanup timestamp
            settingsRepository.setLastCleanupTimestamp(System.currentTimeMillis())

            Log.i(TAG, "Cleanup complete: ${result.sessionCount} sessions, ${result.messageCount} messages deleted")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
            // WorkManager will retry with exponential backoff
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "CleanupWorker"
    }
}
```

### WorkManager Helper for Scheduling

```kotlin
// Source: Context7 WorkManager API + WebSearch verified patterns + CONTEXT.md decisions
package dev.notyouraverage.smscourier.utils

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.notyouraverage.smscourier.workers.CleanupWorker
import java.util.concurrent.TimeUnit

object WorkManagerHelper {
    private const val CLEANUP_WORK_NAME = "cleanup_old_history"

    fun scheduleCleanup(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)  // User decision from CONTEXT.md
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(
            repeatInterval = 7,
            repeatIntervalTimeUnit = TimeUnit.DAYS,
            flexTimeInterval = 1,  // Can run anytime in last 24 hours of 7-day period
            flexTimeIntervalUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CLEANUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,  // Don't reschedule if already running
            cleanupRequest
        )
    }

    fun cancelCleanup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(CLEANUP_WORK_NAME)
    }

    fun isCleanupScheduled(context: Context): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(CLEANUP_WORK_NAME)
            .get()  // Blocking call, use in background thread or coroutine
        return workInfos.any { !it.state.isFinished }
    }
}
```

### DataStore Settings Extension

```kotlin
// Source: Existing SettingsRepository pattern + Context7 DataStore API
// Add to SettingsRepository.kt

// Read Flows
val autoCleanupEnabled: Flow<Boolean> = dataStore.data
    .map { preferences ->
        preferences[PreferenceKeys.AUTO_CLEANUP_ENABLED]
            ?: SettingsDefaults.AUTO_CLEANUP_ENABLED
    }

val lastCleanupTimestamp: Flow<Long> = dataStore.data
    .map { preferences ->
        preferences[PreferenceKeys.LAST_CLEANUP_TIMESTAMP]
            ?: SettingsDefaults.LAST_CLEANUP_TIMESTAMP
    }

// Write Methods
suspend fun setAutoCleanupEnabled(enabled: Boolean) {
    dataStore.edit { preferences ->
        preferences[PreferenceKeys.AUTO_CLEANUP_ENABLED] = enabled
    }
}

suspend fun setLastCleanupTimestamp(timestampMs: Long) {
    dataStore.edit { preferences ->
        preferences[PreferenceKeys.LAST_CLEANUP_TIMESTAMP] = timestampMs
    }
}
```

### UI Display with Relative Time

```kotlin
// Source: WebSearch verified DateUtils pattern + existing SettingsScreen
// Add helper function
private fun formatLastCleanup(timestampMs: Long): String {
    if (timestampMs == 0L) {
        return "Never"
    }

    return DateUtils.getRelativeTimeSpanString(
        timestampMs,
        System.currentTimeMillis(),
        DateUtils.DAY_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}

// In SettingsScreen
val historyRetentionDays by viewModel.historyRetentionDays.collectAsState()
val autoCleanupEnabled by viewModel.autoCleanupEnabled.collectAsState()
val lastCleanupTimestamp by viewModel.lastCleanupTimestamp.collectAsState()

// Only show toggle if retention is not Forever
if (historyRetentionDays != 0) {
    item {
        SettingsSwitchItem(
            title = "Auto-cleanup",
            subtitle = "Clean up old data every 7 days",
            checked = autoCleanupEnabled,
            onCheckedChange = { viewModel.setAutoCleanupEnabled(it) }
        )
    }

    // Show last cleanup time if toggle is ON
    if (autoCleanupEnabled) {
        item {
            SettingsTextItem(
                title = "Last cleaned",
                value = formatLastCleanup(lastCleanupTimestamp)
            )
        }
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| AlarmManager | WorkManager | 2018 (stable 2019) | Guaranteed execution, respects Doze mode, survives boot, supports constraints |
| JobScheduler | WorkManager (wraps JobScheduler) | 2018 (stable 2019) | Single API for all Android versions, backward compatibility to API 14 |
| Worker (blocking) | CoroutineWorker (suspend) | 2019 (work-runtime-ktx) | Native coroutine support, cleaner async code, matches Room suspend DAOs |
| Custom timestamp formatting | DateUtils.getRelativeTimeSpanString | Platform API since API 3 | Handles all edge cases, localized, battle-tested |
| REPLACE policy | KEEP or CANCEL_AND_REENQUEUE | WorkManager 2.7+ | REPLACE deprecated, new policies more explicit about behavior |

**Deprecated/outdated:**
- AlarmManager for periodic tasks: Doesn't respect Doze mode, requires WAKE_LOCK permission, not guaranteed
- JobScheduler directly: WorkManager wraps JobScheduler and provides backward compatibility
- ExistingPeriodicWorkPolicy.REPLACE: Deprecated in WorkManager 2.8+, use CANCEL_AND_REENQUEUE instead
- Worker for database operations: Use CoroutineWorker to match Room's suspend functions

## Open Questions

1. **Initial cleanup behavior when toggle is enabled (Claude's Discretion)**
   - What we know: User decision from CONTEXT.md says "Let user decide whether to run immediately or wait"
   - What's unclear: Should we show a confirmation dialog asking "Run cleanup now?" when enabling toggle?
   - Recommendation: NO immediate cleanup when enabling toggle - just schedule for next 7-day window. Reason: User has manual "Clean up now" button from Phase 21 if they want immediate cleanup. Auto-cleanup is for maintenance, not on-demand. This follows "silent background operation" principle from CONTEXT.md.

2. **Device idle constraint (Claude's Discretion)**
   - What we know: CONTEXT.md says "Claude's discretion on whether to require device idle"
   - What's unclear: Should cleanup require device to be idle (not in use)?
   - Recommendation: NO idle requirement. Reasons: (a) Cleanup is lightweight (Room DELETE queries), (b) User decision says "no charging required" which suggests they want lenient constraints, (c) Idle requirement can delay cleanup for days on actively-used devices. Battery not low is sufficient protection.

3. **Battery not low constraint (Claude's Discretion)**
   - What we know: CONTEXT.md says "Claude's discretion on whether to require battery not low"
   - What's unclear: Should cleanup require battery level above critical threshold?
   - Recommendation: YES, require battery not low. Reasons: (a) Prevents cleanup from draining last battery %, (b) Still lenient (not "charging required"), (c) Standard practice for background maintenance tasks, (d) WorkManager defines "battery not low" as above 15% - reasonable threshold. This is CONTEXT.md decision "No charging required (cleanup is lightweight)" - battery not low is less strict than charging.

4. **Scheduling on app launch**
   - What we know: Toggle default is ON for new installations
   - What's unclear: When should we schedule the initial cleanup work?
   - Recommendation: Schedule on app launch if toggle is ON and work isn't already scheduled. Location: MainActivity.onCreate or Application.onCreate. Check isCleanupScheduled() first to avoid duplicate scheduling. This ensures cleanup is scheduled even if toggle was ON but work was canceled externally (e.g., user cleared WorkManager data).

## Sources

### Primary (HIGH confidence)

- Context7 library `/androidx/androidx` - WorkManager PeriodicWorkRequest API, CoroutineWorker, Constraints, BackoffPolicy, enqueueUniquePeriodicWork
- Context7 library `/androidx/androidx` - DataStore Preferences longPreferencesKey API
- Existing codebase: ForwardingSessionRepository.cleanupOldSessions (Phase 21) - Cleanup logic already implemented
- Existing codebase: SettingsRepository, PreferenceKeys, SettingsDefaults - DataStore patterns
- Existing codebase: SettingsViewModel, SettingsScreen - Settings UI patterns

### Secondary (MEDIUM confidence)

- [WorkManager in Android: Complete Guide for Periodic & One-Time Background Tasks](https://medium.com/@chetanshingare2991/mastering-workmanager-in-android-the-ultimate-guide-to-reliable-background-tasks-9e62025cda69) - Periodic task setup patterns
- [Task scheduling | Background work | Android Developers](https://developer.android.com/topic/libraries/architecture/workmanager) - Official WorkManager guide
- [Define work requests | Background work | Android Developers](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work) - PeriodicWorkRequest setup
- [WorkManager periodicity | by Pietro Maggi | Android Developers | Medium](https://medium.com/androiddevelopers/workmanager-periodicity-ff35185ff006) - Deep dive into flex intervals and timing
- [Android WorkManager: A Complete Technical Deep Dive | ProAndroidDev](https://proandroiddev.com/android-workmanager-a-complete-technical-deep-dive-f037c768d87b) - WorkManager architecture and best practices (Nov 2025)
- [Using WorkManager with CoroutineWorker in Android | Medium](https://medium.com/@khorassani64/using-workmanager-with-coroutineworker-in-android-3e7c220bc464) - CoroutineWorker patterns
- [Boost Your Android App - Integrating WorkManager with Room Database](https://moldstud.com/articles/p-boost-your-android-app-integrating-workmanager-with-room-database-for-efficient-background-tasks) - WorkManager + Room best practices
- [Threading in CoroutineWorker | Android Developers](https://developer.android.com/develop/background-work/background-tasks/persistent/threading/coroutineworker) - Official CoroutineWorker threading guide
- [Managing work | Background work | Android Developers](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work) - cancelUniqueWork and work management
- [Unique work | Android Developers](https://developer.android.com/topic/libraries/architecture/workmanager/how-to/unique-work) - enqueueUniquePeriodicWork patterns

### Tertiary (LOW confidence)

- [GitHub - AndroidTimeAgo](https://github.com/ardhityawiedhairawan/AndroidTimeAgo) - Example relative time formatting (DateUtils is better, native)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - WorkManager 2.9.1 already in project, official Google library with stable API
- Architecture patterns: HIGH - CoroutineWorker directly calls existing Phase 21 cleanup method, DataStore matches existing patterns
- Pitfalls: HIGH - Based on WorkManager constraints (MIN_PERIODIC_INTERVAL_MILLIS) and existing repository patterns
- UI patterns: HIGH - DataStore Flow collection and toggle patterns already established in SettingsScreen

**Research date:** 2026-02-08
**Valid until:** 60 days (stable APIs, WorkManager 2.9.1 is mature, no major changes expected)
