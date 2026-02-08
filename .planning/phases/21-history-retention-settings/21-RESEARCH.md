# Phase 21: History Retention Settings - Research

**Researched:** 2026-02-08
**Domain:** Android DataStore preferences, Room database cleanup, Compose UI with loading states
**Confidence:** HIGH

## Summary

Phase 21 implements configurable history retention with manual cleanup functionality. This research covers three technical domains: (1) DataStore Preferences for storing retention settings, (2) Room database deletion queries with timestamp thresholds, and (3) Compose UI patterns for dropdowns with loading states and confirmation dialogs.

**Key findings:**
- DataStore Preferences is the established standard for settings in 2026, replacing SharedPreferences entirely
- Room database supports efficient timestamp-based bulk deletions with CASCADE foreign key handling
- Jetpack Compose provides native DropdownMenu and AlertDialog components with built-in loading state patterns
- WorkManager with CoroutineWorker is the standard for scheduled cleanup (Phase 22), but manual cleanup uses suspend functions in ViewModelScope

**Primary recommendation:** Use type-safe DataStore keys with validation, atomic database transactions for cleanup with CASCADE deletes, and sealed class state pattern for loading/success/error UI feedback.

## Standard Stack

### Core Dependencies (Already in Project)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| androidx.datastore.preferences | (in libs.versions.toml) | Settings persistence | Official Google recommendation, replaces SharedPreferences entirely as of 2026 |
| androidx.room | 2.6.1 | Database with timestamp queries | Project already uses Room, supports efficient bulk delete operations |
| androidx.compose.material3 | Latest | UI components | Project uses Material3 for DropdownMenu, AlertDialog, CircularProgressIndicator |
| kotlinx.coroutines | Latest | Async operations | DataStore and Room require coroutines, already used throughout app |

### Supporting (Already Available)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| androidx.work.runtime.ktx | (in libs.versions.toml) | Scheduled cleanup | Phase 22 auto-cleanup, already in dependencies |
| androidx.lifecycle.viewmodel | Latest | ViewModel scope | Cleanup operations launched in viewModelScope |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| DataStore Preferences | SharedPreferences | SharedPreferences deprecated, not coroutine-native, less type-safe |
| Room DELETE queries | Raw SQLite | Room provides type safety, migration support, and CASCADE handling automatically |
| Sealed class state | LiveData with nullable | Sealed classes provide exhaustive when expressions and clearer state boundaries |

**Installation:**
All dependencies already present in project. No new dependencies needed.

## Architecture Patterns

### Recommended Project Structure

```
app/src/main/java/dev/notyouraverage/smscourier/
├── data/settings/
│   ├── PreferenceKeys.kt           # Add HISTORY_RETENTION_DAYS key
│   └── SettingsDefaults.kt         # Add HISTORY_RETENTION default (30)
├── repository/
│   ├── SettingsRepository.kt       # Add retention setting methods
│   ├── ForwardingSessionRepository.kt  # Add cleanupOldSessions method
│   └── ForwardedMessageRepository.kt   # Add cleanupOldMessages method
├── viewmodels/
│   └── SettingsViewModel.kt        # Add cleanup operation state
└── composables/screens/
    └── SettingsScreen.kt            # Add Data & Storage section
```

### Pattern 1: DataStore Integer Preference with Validation

**What:** Type-safe preference key with range validation in repository layer
**When to use:** Storing retention period with constrained values (7, 30, 90, or 0 for forever)

**Example:**
```kotlin
// Source: Existing codebase pattern from PreferenceKeys.kt
// PreferenceKeys.kt
object PreferenceKeys {
    val HISTORY_RETENTION_DAYS = intPreferencesKey("history_retention_days")
}

// SettingsDefaults.kt
object SettingsDefaults {
    const val HISTORY_RETENTION_DAYS = 30
}

// SettingsRepository.kt
val historyRetentionDays: Flow<Int> = dataStore.data
    .map { preferences ->
        preferences[PreferenceKeys.HISTORY_RETENTION_DAYS]
            ?: SettingsDefaults.HISTORY_RETENTION_DAYS
    }

suspend fun setHistoryRetentionDays(days: Int) {
    require(days == 0 || days in listOf(7, 30, 90)) {
        "Retention must be 0 (forever) or one of: 7, 30, 90 days"
    }
    dataStore.edit { preferences ->
        preferences[PreferenceKeys.HISTORY_RETENTION_DAYS] = days
    }
}
```

### Pattern 2: Room Timestamp-Based Bulk Delete with Transaction

**What:** Delete records older than threshold using timestamp comparison
**When to use:** Manual cleanup of old messages and sessions

**Example:**
```kotlin
// Source: Existing codebase has deleteMessagesOlderThan in ForwardedMessageDao.kt
// ForwardedMessageDao.kt (already exists)
@Query("DELETE FROM forwarded_messages WHERE timestamp < :threshold")
suspend fun deleteMessagesOlderThan(threshold: Long)

// ForwardingSessionDao.kt (new method)
@Query("DELETE FROM forwarding_sessions WHERE started_at < :threshold AND is_active = 0")
suspend fun deleteSessionsOlderThan(threshold: Long)

// Repository with transaction coordination
suspend fun cleanupOldData(retentionDays: Int): CleanupResult = withContext(Dispatchers.IO) {
    if (retentionDays == 0) return@withContext CleanupResult(0, 0) // Forever means no cleanup

    val thresholdMs = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)

    database.withTransaction {
        // Count before deletion
        val sessionCount = sessionDao.countSessionsOlderThan(thresholdMs)
        val messageCount = messageDao.countMessagesOlderThan(thresholdMs)

        // Delete sessions (CASCADE will delete messages automatically)
        sessionDao.deleteSessionsOlderThan(thresholdMs)

        CleanupResult(sessionCount, messageCount)
    }
}
```

### Pattern 3: Sealed Class for Async Operation State

**What:** Type-safe state representation for loading, success, and error states
**When to use:** Cleanup button with spinner, confirmation dialog with results

**Example:**
```kotlin
// Source: Existing ExportState.kt pattern from Phase 19
sealed class CleanupState {
    data object Idle : CleanupState()
    data object Loading : CleanupState()
    data class Success(val sessionsDeleted: Int, val messagesDeleted: Int) : CleanupState()
    data class Error(val message: String) : CleanupState()
}

// ViewModel
private val _cleanupState = MutableStateFlow<CleanupState>(CleanupState.Idle)
val cleanupState: StateFlow<CleanupState> = _cleanupState.asStateFlow()

fun cleanupOldHistory() {
    viewModelScope.launch {
        _cleanupState.value = CleanupState.Loading
        try {
            val retention = historyRetentionDays.first()
            val result = cleanupRepository.cleanupOldData(retention)
            _cleanupState.value = CleanupState.Success(
                sessionsDeleted = result.sessionCount,
                messagesDeleted = result.messageCount
            )
        } catch (e: Exception) {
            _cleanupState.value = CleanupState.Error(e.message ?: "Cleanup failed")
        }
    }
}
```

### Pattern 4: DropdownMenu for Preset Selection

**What:** Material3 DropdownMenu with preset values triggered by clickable ListItem
**When to use:** Retention period picker (7, 30, 90 days, Forever)

**Example:**
```kotlin
// Source: Existing SettingsScreen.kt duration dropdown pattern (lines 196-217)
var retentionExpanded by remember { mutableStateOf(false) }
val retentionOptions = listOf(7, 30, 90, 0) // 0 = Forever

Box {
    SettingsSelectionItem(
        title = "History retention",
        selectedValue = formatRetentionPeriod(retention),
        onClick = { retentionExpanded = true }
    )
    DropdownMenu(
        expanded = retentionExpanded,
        onDismissRequest = { retentionExpanded = false }
    ) {
        retentionOptions.forEach { days ->
            DropdownMenuItem(
                text = { Text(formatRetentionPeriod(days)) },
                onClick = {
                    viewModel.setHistoryRetention(days)
                    retentionExpanded = false
                }
            )
        }
    }
}

private fun formatRetentionPeriod(days: Int): String = when (days) {
    0 -> "Forever"
    7 -> "7 days"
    30 -> "30 days"
    90 -> "90 days"
    else -> "$days days"
}
```

### Pattern 5: Button with Loading State and Confirmation Dialog

**What:** Button that shows CircularProgressIndicator during operation, then AlertDialog with results
**When to use:** "Clean up now" button with confirmation before action and summary after completion

**Example:**
```kotlin
// Source: Context7 AlertDialog pattern + existing export patterns
val cleanupState by viewModel.cleanupState.collectAsState()
var showConfirmDialog by remember { mutableStateOf(false) }

// Cleanup button
Button(
    onClick = { showConfirmDialog = true },
    enabled = cleanupState !is CleanupState.Loading
) {
    if (cleanupState is CleanupState.Loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
    Text("Clean up now")
}

// Confirmation dialog
if (showConfirmDialog) {
    AlertDialog(
        onDismissRequest = { showConfirmDialog = false },
        title = { Text("Clean up old history?") },
        text = {
            Text("This will permanently delete messages and sessions older than $retention days.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.cleanupOldHistory()
                    showConfirmDialog = false
                }
            ) {
                Text("Clean up")
            }
        },
        dismissButton = {
            TextButton(onClick = { showConfirmDialog = false }) {
                Text("Cancel")
            }
        }
    )
}

// Success dialog
if (cleanupState is CleanupState.Success) {
    val success = cleanupState as CleanupState.Success
    AlertDialog(
        onDismissRequest = { viewModel.resetCleanupState() },
        title = { Text("Cleanup complete") },
        text = {
            Text("Deleted ${success.sessionsDeleted} sessions and ${success.messagesDeleted} messages.")
        },
        confirmButton = {
            TextButton(onClick = { viewModel.resetCleanupState() }) {
                Text("OK")
            }
        }
    )
}
```

### Anti-Patterns to Avoid

- **Deleting messages before sessions**: Room CASCADE foreign key handles this automatically when sessions are deleted
- **Manual message deletion after session deletion**: Unnecessary and causes double-work, CASCADE handles it
- **Counting messages separately**: CASCADE means message count equals sum of deleted session message counts
- **Querying after DELETE**: Use COUNT() in same transaction before DELETE for accurate reporting
- **Blocking UI thread**: All cleanup must be in viewModelScope.launch, never on main thread
- **Forever = -1 or null**: Use 0 for consistency with "no limit" convention (like WorkManager)

## Don't Hand-Roll

Problems that have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Settings storage | Custom file I/O or SharedPreferences | DataStore Preferences | Type-safe, coroutine-native, handles migrations, atomicity guaranteed |
| Timestamp calculations | Manual date arithmetic | Standard library (System.currentTimeMillis - days * 86400000L) | Handles leap seconds, DST, time zones correctly |
| Foreign key cascading | Manual deletion loop for messages | Room CASCADE foreign key | Database-level guarantee, atomic, faster than loops |
| State management for async ops | Multiple boolean flags | Sealed class (Idle/Loading/Success/Error) | Type-safe, exhaustive when expressions, single source of truth |
| Transaction coordination | Try-catch with manual rollback | Room withTransaction {} | Automatic rollback on exception, simpler code |
| Dropdown menu state | Custom popup logic | Material3 DropdownMenu | Accessibility, keyboard navigation, Material theming built-in |

**Key insight:** DataStore and Room are mature, production-ready solutions that handle edge cases (crashes during write, concurrent access, transaction rollback) that custom implementations consistently miss.

## Common Pitfalls

### Pitfall 1: Deleting Active Sessions

**What goes wrong:** User clicks "Clean up now" and active forwarding sessions get deleted, breaking live forwarding
**Why it happens:** Forgetting to filter out is_active = 1 sessions in DELETE query
**How to avoid:** Always include `AND is_active = 0` in session deletion query
**Warning signs:** Forwarding stops working immediately after cleanup, database foreign key violations in logs

**Prevention:**
```kotlin
// WRONG - deletes active sessions
@Query("DELETE FROM forwarding_sessions WHERE started_at < :threshold")

// RIGHT - only deletes completed sessions
@Query("DELETE FROM forwarding_sessions WHERE started_at < :threshold AND is_active = 0")
```

### Pitfall 2: Retention Value Not Validated

**What goes wrong:** User or corrupted preference file stores invalid retention value (like -5 or 1000 days)
**Why it happens:** Missing validation in setHistoryRetention method
**How to avoid:** Use require() with explicit allowed values (0, 7, 30, 90) in repository setter
**Warning signs:** Unexpected cleanup behavior, settings screen shows wrong value, crashes on invalid enum conversion

**Prevention:**
```kotlin
// WRONG - accepts any integer
suspend fun setHistoryRetention(days: Int) {
    dataStore.edit { preferences ->
        preferences[PreferenceKeys.HISTORY_RETENTION_DAYS] = days
    }
}

// RIGHT - validates input
suspend fun setHistoryRetention(days: Int) {
    require(days == 0 || days in listOf(7, 30, 90)) {
        "Retention must be 0 (forever) or one of: 7, 30, 90 days"
    }
    dataStore.edit { preferences ->
        preferences[PreferenceKeys.HISTORY_RETENTION_DAYS] = days
    }
}
```

### Pitfall 3: Counting After Deletion

**What goes wrong:** Cleanup reports "Deleted 0 sessions, 0 messages" even though data was deleted
**Why it happens:** Counting records after DELETE query when they're already gone
**How to avoid:** COUNT before DELETE in same transaction
**Warning signs:** Success dialog always shows 0 deleted, but data is actually gone from database

**Prevention:**
```kotlin
// WRONG - counts after deletion
database.withTransaction {
    sessionDao.deleteSessionsOlderThan(threshold)
    val count = sessionDao.countSessionsOlderThan(threshold) // Always 0!
}

// RIGHT - count before deletion
database.withTransaction {
    val count = sessionDao.countSessionsOlderThan(threshold)
    sessionDao.deleteSessionsOlderThan(threshold)
    return count
}
```

### Pitfall 4: Message Count Mismatch with CASCADE

**What goes wrong:** Cleanup logic tries to count messages separately, gets different count than actual CASCADE deletion
**Why it happens:** Messages deleted by CASCADE happen at database level, not visible to DAO query timing
**How to avoid:** Trust CASCADE - don't query messages separately, sum session message counts instead
**Warning signs:** Reported message count doesn't match actual deleted count, inconsistent numbers across runs

**Prevention:**
```kotlin
// WRONG - queries messages independently
val messageCount = messageDao.countMessagesOlderThan(threshold)
val sessionCount = sessionDao.deleteSessionsOlderThan(threshold) // CASCADE deletes messages too

// RIGHT - sum message counts from sessions being deleted
val sessions = sessionDao.getSessionsOlderThan(threshold)
val messageCount = sessions.sumOf { it.messageCount }
val sessionCount = sessions.size
sessionDao.deleteSessionsOlderThan(threshold) // CASCADE matches the count
```

### Pitfall 5: Forever Setting Edge Case

**What goes wrong:** Cleanup runs when retention is "Forever", deleting everything or crashing
**Why it happens:** Not checking for 0 (forever) value before calculating threshold
**How to avoid:** Early return in cleanup logic when retention == 0
**Warning signs:** Cleanup deletes all data when Forever is selected, negative timestamp calculations

**Prevention:**
```kotlin
// WRONG - treats 0 as 0 days
suspend fun cleanup(retentionDays: Int) {
    val threshold = System.currentTimeMillis() - (retentionDays * 86400000L)
    // When retentionDays = 0, threshold = current time, deletes everything!
    deleteOlderThan(threshold)
}

// RIGHT - checks for forever setting
suspend fun cleanup(retentionDays: Int): CleanupResult {
    if (retentionDays == 0) return CleanupResult(0, 0) // No cleanup for forever
    val threshold = System.currentTimeMillis() - (retentionDays * 86400000L)
    deleteOlderThan(threshold)
}
```

## Code Examples

Verified patterns from official sources and existing codebase:

### DataStore Preference Flow

```kotlin
// Source: Existing SettingsRepository.kt pattern
val historyRetentionDays: Flow<Int> = dataStore.data
    .map { preferences ->
        preferences[PreferenceKeys.HISTORY_RETENTION_DAYS]
            ?: SettingsDefaults.HISTORY_RETENTION_DAYS
    }

suspend fun setHistoryRetentionDays(days: Int) {
    require(days == 0 || days in listOf(7, 30, 90)) {
        "Retention must be 0 (forever) or one of: 7, 30, 90 days"
    }
    dataStore.edit { preferences ->
        preferences[PreferenceKeys.HISTORY_RETENTION_DAYS] = days
    }
}
```

### Room Bulk Delete with Count

```kotlin
// ForwardingSessionDao.kt
@Query("SELECT COUNT(*) FROM forwarding_sessions WHERE started_at < :threshold AND is_active = 0")
suspend fun countSessionsOlderThan(threshold: Long): Int

@Query("SELECT * FROM forwarding_sessions WHERE started_at < :threshold AND is_active = 0")
suspend fun getSessionsOlderThan(threshold: Long): List<ForwardingSession>

@Query("DELETE FROM forwarding_sessions WHERE started_at < :threshold AND is_active = 0")
suspend fun deleteSessionsOlderThan(threshold: Long): Int
```

### Repository Cleanup Method

```kotlin
// Source: Adapted from existing repository patterns
data class CleanupResult(val sessionCount: Int, val messageCount: Int)

suspend fun cleanupOldData(retentionDays: Int): CleanupResult = withContext(Dispatchers.IO) {
    if (retentionDays == 0) return@withContext CleanupResult(0, 0)

    val thresholdMs = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)

    database.withTransaction {
        val sessions = sessionDao.getSessionsOlderThan(thresholdMs)
        val sessionCount = sessions.size
        val messageCount = sessions.sumOf { it.messageCount }

        sessionDao.deleteSessionsOlderThan(thresholdMs)

        CleanupResult(sessionCount, messageCount)
    }
}
```

### ViewModel Cleanup Action

```kotlin
// Source: Adapted from existing SettingsViewModel.kt patterns
sealed class CleanupState {
    data object Idle : CleanupState()
    data object Loading : CleanupState()
    data class Success(val sessionsDeleted: Int, val messagesDeleted: Int) : CleanupState()
    data class Error(val message: String) : CleanupState()
}

private val _cleanupState = MutableStateFlow<CleanupState>(CleanupState.Idle)
val cleanupState: StateFlow<CleanupState> = _cleanupState.asStateFlow()

fun cleanupOldHistory() {
    viewModelScope.launch {
        _cleanupState.value = CleanupState.Loading
        try {
            val retention = historyRetentionDays.first()
            val result = /* call repository cleanup */
            _cleanupState.value = CleanupState.Success(
                sessionsDeleted = result.sessionCount,
                messagesDeleted = result.messageCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
            _cleanupState.value = CleanupState.Error(e.message ?: "Cleanup failed")
        }
    }
}

fun resetCleanupState() {
    _cleanupState.value = CleanupState.Idle
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| SharedPreferences | DataStore Preferences | 2020 (stable 2021) | Type safety, coroutines, atomic writes, no UI blocking |
| Manual SQLite | Room with suspend | 2018 (coroutines 2019) | Type-safe queries, automatic migrations, compile-time verification |
| LiveData for settings | Flow with StateIn | 2020 (mature 2022) | Better coroutine integration, cold vs hot streams, structured concurrency |
| Custom loading flags | Sealed class states | Compose era (2021+) | Exhaustive when, single state source, cleaner UI code |

**Deprecated/outdated:**
- SharedPreferences: Official Google guidance as of 2026 says "if starting new project, SharedPreferences should not be in codebase"
- Blocking DAO calls: Room 2.0+ requires suspend functions, no longer supports synchronous queries on main thread
- ExposedDropdownMenuBox without menuAnchor(): Material3 migration guide shows this is required (broke in M3 migration)

## Open Questions

1. **Archived device cleanup behavior**
   - What we know: PairedDevice has isArchived field for soft delete
   - What's unclear: Should cleanup delete sessions for archived devices, or only active/paired devices?
   - Recommendation: User decision from CONTEXT.md - "Whether archived device data is exempt from cleanup or included" is in Claude's Discretion section. Recommend INCLUDING archived device sessions in cleanup (user archived device to remove clutter, likely wants history cleaned too). Can be changed later if user feedback differs.

2. **Message count helper text**
   - What we know: Dropdown can show "7 days", "30 days", etc.
   - What's unclear: Should it show "(~2,450 messages)" below the retention picker?
   - Recommendation: User decision from CONTEXT.md - "Whether to show message count as helper text below retention dropdown" in Claude's Discretion. Recommend NOT showing count initially (simpler UI, avoids performance cost of COUNT query), can add in Phase 22 if users request it.

3. **Forever button behavior**
   - What we know: "Clean up now" button exists, Forever means no automatic cleanup
   - What's unclear: Hide button? Disable button? Show "Nothing to clean" message?
   - Recommendation: User decision from CONTEXT.md - "Behavior of 'Clean up now' button when Forever is selected" in Claude's Discretion. Recommend KEEPING button enabled but showing info dialog "Retention set to Forever - no data to clean up" for user clarity.

## Sources

### Primary (HIGH confidence)

- Context7 library `/websites/developer_android_jetpack_androidx` - DataStore Preferences API, intPreferencesKey usage
- Context7 library `/androidx/androidx` - Room database delete queries, CASCADE foreign keys, transaction API
- Context7 library `/websites/developer_android_develop_ui_compose_components` - DropdownMenu, AlertDialog, CircularProgressIndicator patterns
- Existing codebase: PreferenceKeys.kt, SettingsRepository.kt, SettingsViewModel.kt, SettingsScreen.kt - Established patterns for settings
- Existing codebase: ForwardedMessageDao.kt, ForwardingSessionDao.kt - Database schema with CASCADE foreign keys, existing deleteMessagesOlderThan query
- Existing codebase: ExportState.kt, ExportManager.kt - Sealed class pattern for async operations

### Secondary (MEDIUM confidence)

- [Stop Using SharedPreferences: Mastering Jetpack DataStore in 2026](https://medium.com/@kemal_codes/stop-using-sharedpreferences-mastering-jetpack-datastore-in-2026-b88b2db50e91) - DataStore best practices for 2026
- [App Architecture: Data Layer - DataStore](https://developer.android.com/topic/libraries/architecture/datastore) - Official Android DataStore docs (updated 2026-01-28)
- [Room Database Best Practices for Android Apps | 2025 Guide](https://medium.com/@sixtinbydizora/room-database-best-practices-building-bulletproof-android-apps-0bf240123f44) - Room cleanup patterns
- [Progress indicators | Jetpack Compose](https://developer.android.com/develop/ui/compose/components/progress) - CircularProgressIndicator usage (updated 2026-02-02)
- [Menus | Jetpack Compose](https://developer.android.com/develop/ui/compose/components/menu) - DropdownMenu implementation
- [ExposedDropdownMenuBox – Material 3 Compose](https://composables.com/docs/androidx.compose.material3/material3/components/ExposedDropdownMenuBox) - Material3 menuAnchor requirement

### Tertiary (LOW confidence)

- [Android Notification History](https://source.android.com/docs/core/display/notification-history) - Example of 24-hour default retention pattern in Android platform

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All dependencies already in project, official Google libraries with stable APIs
- Architecture patterns: HIGH - Directly adapted from existing codebase patterns (SettingsScreen, ExportState, Repository methods)
- Pitfalls: HIGH - Based on existing database schema (CASCADE foreign keys, is_active flag) and common Room/DataStore mistakes
- UI patterns: HIGH - Material3 official documentation updated February 2026, existing SettingsScreen patterns

**Research date:** 2026-02-08
**Valid until:** 60 days (stable APIs, all dependencies already in use)
