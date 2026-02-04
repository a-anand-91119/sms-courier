# Stack Research: Device History & Message Storage

**Domain:** Android app enhancement - Message storage, history management, and data export
**Milestone:** v0.0.64 - Device Management & Visibility
**Researched:** 2026-02-04
**Confidence:** HIGH

## Executive Summary

The existing SMS Courier codebase already has Room 2.6.1 and WorkManager 2.9.1 installed. For the v0.0.64 milestone, we need to:
- Upgrade Room to 2.8.4 (current stable) for better migration support
- Upgrade WorkManager to 2.11.1 (current stable) for reliability fixes
- NO new third-party libraries required for CSV/JSON export
- Use existing Material 3 for bottom sheets
- Add Room schema migration from v5 to v6

All new functionality can be achieved with version upgrades and Kotlin standard library.

## Required Stack Changes

### Version Upgrades (Required)

| Library | Current | Recommended | Purpose | Why Upgrade |
|---------|---------|-------------|---------|-------------|
| androidx.room | 2.6.1 | 2.8.4 | Database ORM | Better migration support, performance improvements, prepared statement cache |
| androidx.work | 2.9.1 | 2.11.1 | Background tasks | Network constraint fixes for Android 15+, improved reliability |

### NO New Dependencies Required

The following capabilities are already available or achievable with existing dependencies:

| Capability | How to Achieve | Why No External Library |
|------------|----------------|-------------------------|
| CSV export | Kotlin stdlib string manipulation | Simple format, no complex parsing needed |
| JSON export | Kotlin stdlib string building | Structured data is simple objects, no complex serialization needed |
| Bottom sheet UI | Existing androidx.compose.material3 | ModalBottomSheet already available in Material 3 BOM |
| File export | Android Storage Access Framework | ACTION_CREATE_DOCUMENT intent built into Android |
| History retention settings | Existing DataStore Preferences | Already using androidx.datastore.preferences 1.1.1 |

## Detailed Technology Breakdown

### 1. Room Database Migration (v5 → v6)

**Current Schema (v5):**
- PairedDevice table (composite PK: phoneNumber, device_role)
- ForwardingSession table (FK to PairedDevice via device_phone_number)

**New Schema (v6):**
- Add ForwardedMessage table with FK to ForwardingSession
- Add soft delete columns to PairedDevice (deleted_at, is_archived)

**Implementation Pattern:**

```kotlin
@Database(
    entities = [PairedDevice::class, ForwardingSession::class, ForwardedMessage::class],
    version = 6,
    exportSchema = true
)

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add ForwardedMessage table with foreign key
        db.execSQL("""
            CREATE TABLE forwarded_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                session_id INTEGER NOT NULL,
                sender_phone_number TEXT NOT NULL,
                message_body TEXT NOT NULL,
                received_at INTEGER NOT NULL,
                direction TEXT NOT NULL,
                FOREIGN KEY(session_id) REFERENCES forwarding_sessions(id)
                    ON DELETE CASCADE
            )
        """)

        // Add index on session_id for query performance
        db.execSQL("CREATE INDEX index_forwarded_messages_session_id ON forwarded_messages(session_id)")

        // Add soft delete columns to paired_devices
        db.execSQL("ALTER TABLE paired_devices ADD COLUMN deleted_at INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE paired_devices ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0")
    }
}
```

**Foreign Key Best Practices (from official docs):**
- Index on child table (session_id) is REQUIRED to avoid full table scans
- Use ON DELETE CASCADE to automatically clean up messages when session is deleted
- Room verifies parent table has proper index at compile time
- Test migrations with instrumented tests before deploying

**Sources:**
- [Room Foreign Key Reference](https://developer.android.com/reference/android/arch/persistence/room/ForeignKey)
- [Room Migration Guide](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [Room 2.8.4 Release Notes](https://developer.android.com/jetpack/androidx/releases/room)

### 2. Data Export Implementation

**Strategy:** Use Kotlin standard library only, no external CSV/JSON libraries.

**Why no external libraries?**
- CSV format is trivial: comma-separated values with escaped quotes
- JSON structure is simple: array of message objects
- External libraries add dependency overhead for minimal value
- Kotlin stdlib provides all needed string manipulation

**CSV Export Pattern:**

```kotlin
fun exportToCsv(messages: List<ForwardedMessage>): String {
    val header = "Timestamp,Sender,Direction,Message\n"
    val rows = messages.joinToString("\n") { msg ->
        val escapedMessage = msg.messageBody.replace("\"", "\"\"")
        "${msg.receivedAt},${msg.senderPhoneNumber},${msg.direction},\"$escapedMessage\""
    }
    return header + rows
}
```

**JSON Export Pattern:**

```kotlin
fun exportToJson(messages: List<ForwardedMessage>): String {
    val jsonObjects = messages.joinToString(",\n  ") { msg ->
        """
        {
          "timestamp": ${msg.receivedAt},
          "sender": "${msg.senderPhoneNumber}",
          "direction": "${msg.direction}",
          "message": "${msg.messageBody.replace("\"", "\\\"")}"
        }
        """.trimIndent()
    }
    return "[\n  $jsonObjects\n]"
}
```

**Plain Text Export Pattern:**

```kotlin
fun exportToText(messages: List<ForwardedMessage>): String {
    return messages.joinToString("\n\n") { msg ->
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(msg.receivedAt))
        val direction = if (msg.direction == "RECEIVED") "↓" else "↑"
        "$timestamp $direction ${msg.senderPhoneNumber}\n${msg.messageBody}"
    }
}
```

**File Write with Storage Access Framework:**

```kotlin
// In ViewModel or Activity
val createDocumentLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.CreateDocument("text/csv")
) { uri ->
    uri?.let { contentResolver.openOutputStream(it)?.use { output ->
        output.write(csvContent.toByteArray())
    }}
}

// Trigger export
createDocumentLauncher.launch("sms_history.csv")
```

**Sources:**
- [Android Storage Use Cases](https://developer.android.com/training/data-storage/use-cases)
- [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files)
- [Scoped Storage Guide](https://source.android.com/docs/core/storage/scoped)

### 3. WorkManager Periodic Cleanup

**Configuration:**

```kotlin
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.11.1")
}
```

**Cleanup Worker Pattern:**

```kotlin
class HistoryCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val retentionDays = inputData.getInt("RETENTION_DAYS", 30)
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)

        // Delete old messages
        database.forwardedMessageDao().deleteOlderThan(cutoffTime)

        return Result.success()
    }
}
```

**Scheduling Configuration:**

```kotlin
val constraints = Constraints.Builder()
    .setRequiresDeviceIdle(true)        // Run when device inactive
    .setRequiresCharging(true)          // Minimize battery impact
    .setRequiresStorageNotLow(true)     // Ensure adequate storage
    .build()

val cleanupWork = PeriodicWorkRequestBuilder<HistoryCleanupWorker>(
    repeatInterval = 1, TimeUnit.DAYS,   // Daily cleanup
    flexInterval = 2, TimeUnit.HOURS     // 2-hour window for optimization
)
    .setConstraints(constraints)
    .setInputData(workDataOf("RETENTION_DAYS" to retentionDays))
    .build()

WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork(
        "history_cleanup",
        ExistingPeriodicWorkPolicy.KEEP,
        cleanupWork
    )
```

**Key Constraints:**
- Minimum periodic interval: 15 minutes (we use 1 day)
- Use flexInterval for system optimization (allows 2-hour scheduling window)
- DeviceIdle prevents UI impact
- RequiresCharging minimizes battery drain

**Sources:**
- [WorkManager 2.11.1 Release Notes](https://developer.android.com/jetpack/androidx/releases/work)
- [WorkManager Periodic Tasks](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- [WorkManager Periodicity Deep Dive](https://medium.com/androiddevelopers/workmanager-periodicity-ff35185ff006)

### 4. UI Components (No Changes Needed)

**Bottom Sheet for Message Details:**

Existing Material 3 BOM already includes ModalBottomSheet:

```kotlin
// Already available from existing dependencies
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

@Composable
fun MessageDetailsSheet(
    message: ForwardedMessage,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        // Message details content
    }
}
```

**Sources:**
- [Material 3 Bottom Sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets)
- [ModalBottomSheet Compose Documentation](https://composables.com/material3/modalbottomsheet)

### 5. Settings Persistence (No Changes Needed)

History retention settings use existing DataStore pattern:

```kotlin
// In PreferenceKeys.kt
val HISTORY_RETENTION_DAYS = intPreferencesKey("history_retention_days")
val HISTORY_VIEW_MODE = stringPreferencesKey("history_view_mode") // "session" or "contact"

// In SettingsRepository.kt
val historyRetentionDays: Flow<Int> = dataStore.data.map { prefs ->
    prefs[PreferenceKeys.HISTORY_RETENTION_DAYS] ?: 30
}

suspend fun setHistoryRetentionDays(days: Int) {
    require(days in 1..365) { "Retention must be 1-365 days" }
    dataStore.edit { it[PreferenceKeys.HISTORY_RETENTION_DAYS] = days }
}
```

## Installation

### Update libs.versions.toml

```toml
[versions]
room = "2.8.4"  # upgrade from 2.6.1
workRuntimeKtx = "2.11.1"  # upgrade from 2.9.1

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workRuntimeKtx" }
```

### No Changes to build.gradle.kts

The dependency declarations remain the same, only versions change in `libs.versions.toml`:

```kotlin
dependencies {
    // Room - version upgraded via libs.versions.toml
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager - version upgraded via libs.versions.toml
    implementation(libs.androidx.work.runtime.ktx)

    // Existing dependencies (no changes)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
}
```

## What NOT to Add

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| kotlinx-serialization-json | Overkill for simple JSON export | Kotlin stdlib string building |
| kotlinx-serialization-csv | External dependency for trivial format | Manual CSV generation (5 lines) |
| Apache Commons CSV | Large dependency, Java-focused | Kotlin stdlib joinToString |
| Gson / Moshi | Reflection overhead, unnecessary | Direct string construction |
| OpenCSV | Heavy library for simple use case | Kotlin stdlib |

**Rationale:** The export data is simple (flat message objects), formats are trivial (CSV is comma-separated, JSON is basic objects), and the stdlib provides all needed string manipulation. Adding external libraries increases APK size, dependency management overhead, and potential security surface area for zero functional benefit.

## Version Compatibility

| Package | Version | Compatible With | Notes |
|---------|---------|-----------------|-------|
| androidx.room:* | 2.8.4 | Kotlin 2.0+, KSP 1.9.0+ | Requires AGP 8.4+ for Room Gradle Plugin |
| androidx.work:* | 2.11.1 | compileSdk 33+ | Fixed network constraints on Android 15+ |
| androidx.datastore:* | 1.1.1 | Kotlin coroutines 1.7+ | Already compatible |
| Kotlin | 1.9.0 | All dependencies | Room 2.8 prefers Kotlin 2.0 but compatible with 1.9 |

**Important Notes:**
- Room 2.8.4 minSDK increased from API 21 to API 23 (project targets API 34, no issue)
- WorkManager 2.11.1 requires compileSdk 33+ (project uses 35, no issue)
- Room 2.8.x has prepared statement cache for better performance
- WorkManager 2.11.1 fixes critical network constraint bug on Android 15

## Migration Checklist

Before implementing:

- [ ] Upgrade Room from 2.6.1 to 2.8.4 in libs.versions.toml
- [ ] Upgrade WorkManager from 2.9.1 to 2.11.1 in libs.versions.toml
- [ ] Create MIGRATION_5_6 for new ForwardedMessage table
- [ ] Add ForwardedMessage entity with @ForeignKey annotation
- [ ] Create index on ForwardedMessage.session_id
- [ ] Add soft delete columns to PairedDevice
- [ ] Update database version from 5 to 6
- [ ] Write migration instrumented test
- [ ] Add export functions using Kotlin stdlib only
- [ ] Implement HistoryCleanupWorker
- [ ] Add history retention preferences to DataStore
- [ ] Use existing ModalBottomSheet from Material 3

## Sources

**High Confidence (Official Documentation):**
- [Room 2.8.4 Release Notes](https://developer.android.com/jetpack/androidx/releases/room) - Version verification
- [WorkManager 2.11.1 Release Notes](https://developer.android.com/jetpack/androidx/releases/work) - Version verification
- [Room Migration Guide](https://developer.android.com/training/data-storage/room/migrating-db-versions) - Migration patterns
- [Room Foreign Keys](https://developer.android.com/reference/android/arch/persistence/room/ForeignKey) - FK best practices
- [WorkManager Periodic Tasks](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work) - Minimum intervals, constraints
- [Android Storage Use Cases](https://developer.android.com/training/data-storage/use-cases) - Export recommendations
- [Material 3 Bottom Sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets) - UI components

**Medium Confidence (Community Sources - Cross-Verified):**
- [Room Migrations Best Practices](https://proandroiddev.com/why-room-crashes-when-you-change-your-database-and-how-to-fix-it-ca8e3538bf57) - Migration patterns
- [WorkManager Periodicity](https://medium.com/androiddevelopers/workmanager-periodicity-ff35185ff006) - Flex intervals
- [Room Foreign Key Patterns](https://medium.com/@vontonnie/connecting-room-tables-using-foreign-keys-c19450361603) - Implementation examples

---
*Stack research for: SMS Courier v0.0.64 - Device History & Message Storage*
*Researched: 2026-02-04*
*Confidence: HIGH - All recommendations verified with official Android documentation*
