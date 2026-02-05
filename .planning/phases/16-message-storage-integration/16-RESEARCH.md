# Phase 16: Message Storage Integration - Research

**Researched:** 2026-02-05
**Domain:** Android Room Database - Transaction Management & Real-time Message Storage
**Confidence:** HIGH

## Summary

Phase 16 implements data persistence for forwarded SMS messages during active forwarding sessions. The research focused on Room 2.6.1 transaction patterns, synchronous write strategies, error handling approaches, and background reconciliation for eventual consistency.

**Key findings:**
1. Room 2.6.1 suspend functions automatically handle threading via Dispatchers.IO - no manual `withContext` needed in DAOs
2. Room's `@Transaction` annotation provides automatic rollback on exceptions with ACID guarantees
3. Foreign key CASCADE delete is already configured in ForwardedMessage entity (Phase 15)
4. Synchronous blocking writes are appropriate for critical data consistency in foreground services
5. Background reconciliation for failed statistics updates can use in-memory queue with WorkManager fallback

**Primary recommendation:** Use Room's `@Transaction` annotation for atomic multi-table operations (message insert + counter updates), catch SQLExceptions gracefully without crashing MasterService, and implement in-memory reconciliation queue for non-critical statistics failures.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Room | 2.6.1 | SQLite persistence layer | Official Android Jetpack library, type-safe queries, migration support |
| Room KTX | 2.6.1 | Coroutines integration | Provides suspend functions and Flow support for reactive queries |
| Kotlin Coroutines | 1.9.0 | Async/threading | Standard for async Android development, integrated with Room |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| WorkManager | (future) | Background task execution | Phase 21 cleanup tasks and background reconciliation retry |
| DataStore | (existing) | Settings persistence | Already in use for app settings, not needed for this phase |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Room transactions | Manual BEGIN/COMMIT | Room provides better error handling, automatic rollback, and type safety |
| Synchronous writes | Full async with callbacks | Synchronous guarantees data persistence before service crash (critical for SMS) |
| In-memory queue | Immediate WorkManager | In-memory queue is faster for temporary failures; WorkManager for persistent retry |

**Current versions in project:**
- Room: 2.6.1 (retained for Kotlin 1.9.0 compatibility per Phase 15 decision SCHEMA-01)
- Already configured: Foreign key CASCADE on ForwardedMessage → ForwardingSession relationship

## Architecture Patterns

### Recommended Project Structure
```
app/src/main/java/dev/notyouraverage/smscourier/
├── data/
│   ├── dao/                    # Already exists - ForwardedMessageDao
│   ├── entities/               # Already exists - ForwardedMessage
│   └── SmsCourierDatabase.kt   # Already exists - v6 schema
├── repository/
│   ├── ForwardingSessionRepository.kt  # Already exists - add transaction methods
│   └── ForwardedMessageRepository.kt   # NEW - encapsulate message storage logic
├── handlers/
│   └── SmsCommandHandler.kt    # MODIFY - integrate message storage
└── services/
    └── foreground/MasterService.kt  # MODIFY - crash recovery logic
```

### Pattern 1: @Transaction for Atomic Multi-Table Updates
**What:** Use Room's `@Transaction` annotation to ensure all database operations succeed or fail together
**When to use:** When inserting ForwardedMessage + updating session/device counters simultaneously
**Example:**
```kotlin
// Source: https://github.com/androidx/androidx Room documentation
@Dao
interface ForwardedMessageDao {
    @Transaction
    suspend fun insertMessageAndUpdateCounters(
        message: ForwardedMessage,
        sessionId: Long,
        devicePhone: String,
        deviceRole: DeviceRole
    ) {
        // All operations succeed or all rollback
        insertMessage(message)
        sessionDao.incrementMessageCount(sessionId)
        deviceDao.incrementTotalMessagesForwarded(devicePhone, deviceRole)
    }

    @Insert
    suspend fun insertMessage(message: ForwardedMessage): Long
}
```

### Pattern 2: Synchronous Blocking Writes for Critical Data
**What:** Use `runBlocking` in foreground service for critical message storage that must complete before service death
**When to use:** When data loss is unacceptable (SMS forwarding is critical path)
**Example:**
```kotlin
// In MasterService.handleForwardSms (TARGET device)
fun handleForwardSms(originalSender: String, content: String, sessionId: Long) {
    // Block until database write completes - guarantees persistence
    runBlocking(Dispatchers.IO) {
        try {
            messageRepository.storeForwardedMessage(
                sessionId = sessionId,
                senderNumber = originalSender,
                messageContent = content
            )
            Log.d(TAG, "Message stored successfully")
        } catch (e: SQLException) {
            // Log but continue - message still forwarded to SOURCE
            Log.e(TAG, "Failed to store message: ${e.message}")
        }
    }
}
```

### Pattern 3: Role-Specific Storage Logic
**What:** Implement different storage behavior based on DeviceRole (TARGET stores messages, SOURCE doesn't)
**When to use:** When handling forwarded message receipt
**Example:**
```kotlin
// In SmsCommandHandler
suspend fun handleIncomingSms(originalSender: String, messageBody: String) {
    val activeSessions = sessionRepository.getActiveSessionsList()

    for (session in activeSessions) {
        // TARGET device: Store message before forwarding
        messageRepository.storeForwardedMessage(
            sessionId = session.id,
            senderNumber = originalSender,
            messageContent = messageBody,
            destinationNumber = session.devicePhoneNumber
        )

        // Then forward to SOURCE
        smsSender.sendForwardedSms(session.devicePhoneNumber, originalSender, messageBody)
    }
}

// On SOURCE device (receives FWD command)
suspend fun handleForwardedData(senderPhone: String, originalSender: String, message: String) {
    // SOURCE: Show notification ONLY - no database storage
    notificationManager.showForwardedMessageNotification(originalSender, message, senderPhone)
}
```

### Pattern 4: Crash Recovery on Service Start
**What:** Find and mark incomplete sessions as STOPPED when MasterService restarts
**When to use:** In MasterService.onCreate or onStartCommand during service initialization
**Example:**
```kotlin
// In MasterService.startSelf()
private suspend fun recoverFromCrash() {
    val now = System.currentTimeMillis()
    val activeSessions = sessionRepository.getActiveSessionsList()

    activeSessions.forEach { session ->
        if (session.expiresAt > now) {
            // Session still valid - resume normal operation
            resumeSession(session)
        } else {
            // Session expired during crash - mark as crashed
            sessionRepository.endSession(session.id, "CRASH")
            Log.w(TAG, "Marked session ${session.id} as crashed during recovery")
        }
    }
}
```

### Pattern 5: In-Memory Reconciliation Queue for Statistics
**What:** Queue failed statistics updates in memory, retry on next message or session event
**When to use:** For non-critical counters (totalMessagesForwarded) that can be eventually consistent
**Example:**
```kotlin
class StatisticsReconciliationQueue {
    private val pendingUpdates = mutableListOf<StatUpdate>()

    data class StatUpdate(
        val devicePhone: String,
        val deviceRole: DeviceRole,
        val increment: Int,
        val timestamp: Long
    )

    suspend fun queueUpdate(devicePhone: String, deviceRole: DeviceRole) {
        pendingUpdates.add(StatUpdate(devicePhone, deviceRole, 1, System.currentTimeMillis()))
    }

    suspend fun reconcile() {
        val iterator = pendingUpdates.iterator()
        while (iterator.hasNext()) {
            val update = iterator.next()
            try {
                deviceRepository.incrementTotalMessagesForwarded(
                    update.devicePhone,
                    update.deviceRole,
                    update.increment
                )
                iterator.remove() // Success - remove from queue
            } catch (e: SQLException) {
                Log.w(TAG, "Reconciliation failed for ${update.devicePhone}, will retry")
            }
        }
    }
}
```

### Anti-Patterns to Avoid
- **Don't use `withContext(Dispatchers.IO)` in DAOs:** Room suspend functions already switch to IO dispatcher automatically - wrapping adds unnecessary overhead and can cause threading issues
- **Don't validate foreign keys before insert:** Trust Room's foreign key constraints and catch `SQLiteConstraintException` - pre-validation adds race conditions and duplicate logic
- **Don't create emergency sessions:** If session doesn't exist when storing message, drop the message silently - creating sessions breaks authentication flow and security model
- **Don't use `updatedAt` for per-message tracking:** Only update `updatedAt` on START/STOP events - updating per message creates unnecessary write amplification

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Database transactions | Manual BEGIN TRANSACTION/COMMIT | Room `@Transaction` annotation | Handles nested transactions, automatic rollback, works with suspend functions |
| Background retry logic | Custom retry loops | WorkManager with backoff policy | Handles process death, configurable retry strategies, battery optimization |
| Timestamp generation | Custom timestamp fields | `defaultValue = System.currentTimeMillis()` in entity | Automatically set by Room, no repository logic needed |
| Foreign key cascade | Manual deletion code | `@ForeignKey(onDelete = CASCADE)` | Automatic by SQLite, no orphaned records possible |
| Thread switching | Manual thread creation | Room suspend + Dispatchers.IO | Room manages thread pools, prevents thread exhaustion |

**Key insight:** Room 2.6+ handles most database complexity automatically. Custom solutions bypass Room's optimizations (query batching, connection pooling, WAL mode) and introduce bugs. The existing codebase already uses Room correctly - extend patterns, don't replace them.

## Common Pitfalls

### Pitfall 1: Coroutine Context Switching Deadlock
**What goes wrong:** Using `withContext(Dispatchers.IO)` inside a Room transaction causes deadlock because Room's transaction requires consistent thread
**Why it happens:** Room 2.x transactions use thread-local variables to track transaction state - switching coroutine context loses this state
**How to avoid:** Never use `withContext` inside `@Transaction` methods - Room already handles threading
**Warning signs:** Tests hang indefinitely, database locks never release, logcat shows "transaction already committed" errors

**Example (WRONG):**
```kotlin
@Transaction
suspend fun dangerousMethod() {
    insertMessage(msg)
    withContext(Dispatchers.IO) { // DEADLOCK - Room transaction lost
        updateCounters()
    }
}
```

**Example (CORRECT):**
```kotlin
@Transaction
suspend fun safeMethod() {
    insertMessage(msg)
    updateCounters() // Room handles threading automatically
}
```

### Pitfall 2: Catching Generic Exception Hides Constraint Violations
**What goes wrong:** Catching `Exception` instead of specific SQL exceptions masks foreign key violations and constraint errors
**Why it happens:** Generic exception handling is convenient but loses critical error information
**How to avoid:** Catch `SQLException` and `SQLiteConstraintException` specifically, log the constraint violation details
**Warning signs:** Silent data loss, orphaned records, "session not found" errors in logs without clear cause

**Example (WRONG):**
```kotlin
try {
    dao.insertMessage(message)
} catch (e: Exception) {
    // Swallows constraint violations silently
    Log.e(TAG, "Insert failed")
}
```

**Example (CORRECT):**
```kotlin
try {
    dao.insertMessage(message)
} catch (e: SQLiteConstraintException) {
    Log.e(TAG, "Constraint violation: ${e.message} - session may have been deleted")
    // Don't retry - constraint violations are permanent
} catch (e: SQLException) {
    Log.e(TAG, "Database error: ${e.message}")
    // Queue for reconciliation
}
```

### Pitfall 3: Timestamp Precision Loss
**What goes wrong:** Using seconds instead of milliseconds loses precision for rapid message forwarding, causes incorrect ordering
**Why it happens:** `System.currentTimeMillis()` returns milliseconds but developer divides by 1000
**How to avoid:** Always store `System.currentTimeMillis()` directly as Long - Room uses BIGINT which handles milliseconds perfectly
**Warning signs:** Messages with identical timestamps, incorrect sort order in UI, "delivered before sent" anomalies

### Pitfall 4: Blocking Main Thread with runBlocking
**What goes wrong:** Using `runBlocking` in UI code or Activity/ViewModel freezes app, causes ANR (Application Not Responding)
**Why it happens:** Confusion between "synchronous database writes" (correct in Service) vs "blocking main thread" (incorrect in UI)
**How to avoid:** Use `runBlocking` ONLY in foreground Service for critical persistence, NEVER in Activity/ViewModel/Composable
**Warning signs:** UI freezes during message forwarding, ANR dialogs, Choreographer frame skips in logcat

**Correct usage:**
- ✅ MasterService.handleForwardSms - runBlocking on service worker thread is OK
- ❌ ViewModel.sendMessage - use viewModelScope.launch instead
- ❌ Composable onClick - use rememberCoroutineScope().launch instead

### Pitfall 5: Not Handling Service Process Death
**What goes wrong:** Active sessions remain "ACTIVE" in database after service crash, preventing new sessions
**Why it happens:** Android can kill background service without calling onDestroy
**How to avoid:** On service start, check for orphaned ACTIVE sessions and mark them STOPPED with crash timestamp
**Warning signs:** "Session already active" errors after app force-stop, users can't start new forwarding after crash

## Code Examples

Verified patterns from official sources:

### Example 1: Transaction with Multiple DAOs
```kotlin
// Source: https://github.com/androidx/androidx Room documentation
class ForwardedMessageRepository(
    private val messageDao: ForwardedMessageDao,
    private val sessionDao: ForwardingSessionDao,
    private val deviceDao: PairedDeviceDao,
    private val database: SmsCourierDatabase
) {
    // Use database.withTransaction for cross-DAO transactions
    suspend fun storeMessageWithCounters(
        sessionId: Long,
        senderNumber: String,
        messageContent: String,
        devicePhone: String,
        deviceRole: DeviceRole
    ): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            // All operations atomic - rollback on any failure
            val messageId = messageDao.insertMessage(
                ForwardedMessage(
                    sessionId = sessionId,
                    senderNumber = senderNumber,
                    messageContent = messageContent
                )
            )

            sessionDao.incrementMessageCount(sessionId)
            deviceDao.incrementTotalMessagesForwarded(devicePhone, deviceRole)

            messageId
        }
    }
}
```

### Example 2: Graceful Error Handling
```kotlin
// Based on Android Room error handling patterns
suspend fun storeForwardedMessage(
    sessionId: Long,
    senderNumber: String,
    messageContent: String
): Result<Long> = withContext(Dispatchers.IO) {
    try {
        val messageId = messageDao.insertMessage(
            ForwardedMessage(
                sessionId = sessionId,
                senderNumber = senderNumber,
                messageContent = messageContent
            )
        )
        Result.success(messageId)
    } catch (e: SQLiteConstraintException) {
        // Foreign key violation - session was deleted
        Log.w(TAG, "Session $sessionId not found, dropping message")
        Result.failure(e)
    } catch (e: SQLException) {
        // Generic database error - could be transient
        Log.e(TAG, "Database error storing message: ${e.message}")
        Result.failure(e)
    }
}
```

### Example 3: Foreign Key CASCADE Verification
```kotlin
// Source: Existing codebase - ForwardedMessage.kt
@Entity(
    tableName = "forwarded_messages",
    foreignKeys = [
        ForeignKey(
            entity = ForwardingSession::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE  // ✅ Already configured in Phase 15
        )
    ],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["timestamp"])
    ]
)
data class ForwardedMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    @ColumnInfo(name = "sender_number")
    val senderNumber: String,

    @ColumnInfo(name = "message_content")
    val messageContent: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()  // ✅ Millisecond precision
)
```

### Example 4: Crash Recovery Pattern
```kotlin
// In MasterService.startSelf()
private suspend fun recoverActiveSessions() {
    val now = System.currentTimeMillis()
    val activeSessions = sessionRepository.getActiveSessionsList()

    if (activeSessions.isEmpty()) {
        Log.d(TAG, "No active sessions to recover")
        return
    }

    Log.i(TAG, "Found ${activeSessions.size} active sessions, checking for crash recovery")

    activeSessions.forEach { session ->
        when {
            session.expiresAt < now -> {
                // Expired during downtime
                sessionRepository.endSession(session.id, "EXPIRED")
                Log.w(TAG, "Session ${session.id} expired during service downtime")
            }
            session.startedAt + (5 * 60 * 1000) < now -> {
                // Started >5 minutes ago but still active - likely crash
                sessionRepository.endSession(session.id, "CRASH")
                Log.w(TAG, "Session ${session.id} likely crashed - marking stopped")
            }
            else -> {
                // Recently started, valid session - resume
                resumeSession(session)
                Log.i(TAG, "Resuming valid session ${session.id}")
            }
        }
    }
}
```

### Example 5: DAO Methods for Counter Updates
```kotlin
// Add to ForwardingSessionDao.kt
@Query("UPDATE forwarding_sessions SET message_count = message_count + 1 WHERE id = :sessionId")
suspend fun incrementMessageCount(sessionId: Long)

// Add to PairedDeviceDao.kt
@Query("""
    UPDATE paired_devices
    SET total_messages_forwarded = total_messages_forwarded + :increment
    WHERE phoneNumber = :phoneNumber AND device_role = :role
""")
suspend fun incrementTotalMessagesForwarded(
    phoneNumber: String,
    role: DeviceRole,
    increment: Int = 1
)
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual transaction with BEGIN/COMMIT | `@Transaction` annotation | Room 2.0 (2018) | Automatic rollback, better error handling |
| Callbacks for async queries | Suspend functions | Room 2.1 (2019) | Cleaner code, structured concurrency |
| LiveData for reactive queries | Flow | Room 2.2 (2020) | More flexible, composable streams |
| Manual thread switching with Executors | Dispatchers.IO | Room 2.1+ (2019) | Automatic thread management |
| Manual foreign key validation | Database-level constraints | Always recommended | Race-condition free |

**Deprecated/outdated:**
- `beginTransaction()` / `endTransaction()` / `setTransactionSuccessful()`: Deprecated in Room 2.0+, use `@Transaction` or `database.withTransaction { }` instead
- `AsyncTask` for database operations: Deprecated Android 11+, use suspend functions
- `LiveData` for new code: Not deprecated but Flow is more flexible for complex transformations

**Current best practice (2026):** Room 2.6.1 with Kotlin Coroutines, suspend DAO functions, `@Transaction` for atomicity, and Flow for reactive queries. The project is already using this stack correctly.

## Open Questions

Things that couldn't be fully resolved:

1. **Background reconciliation persistence across app restarts**
   - What we know: In-memory queue works for active service, WorkManager handles persistent retry
   - What's unclear: Whether to persist reconciliation queue to DataStore or trust WorkManager for next app launch
   - Recommendation: Start with in-memory queue, add DataStore persistence in Phase 21 (Retention Settings) if needed

2. **Transaction isolation level configuration**
   - What we know: Room uses SQLite defaults (SERIALIZABLE for transactions)
   - What's unclear: Whether to explicitly configure IMMEDIATE vs DEFERRED transactions for this use case
   - Recommendation: Use Room defaults - they're battle-tested for Android, no evidence of issues in existing codebase

3. **destinationNumber field in ForwardedMessage**
   - What we know: Context says TARGET stores "destinationNumber (which SOURCE received it)" for multiple SOURCE support
   - What's unclear: Current ForwardedMessage entity doesn't have destinationNumber field
   - Recommendation: Add `destinationNumber: String` field to ForwardedMessage entity in Phase 16 (BREAKING CHANGE - requires migration to schema v7)

## Sources

### Primary (HIGH confidence)
- [AndroidX Room Documentation](https://github.com/androidx/androidx) - Transaction APIs, foreign keys, error handling patterns
- [Room 🔗 Coroutines](https://medium.com/androiddevelopers/room-coroutines-422b786dc4c5) - Official Android Developers guide on Room + Coroutines
- [Threading models in Coroutines and Android SQLite API](https://medium.com/androiddevelopers/threading-models-in-coroutines-and-android-sqlite-api-6cab11f7eb90) - Official guide on threading with Room
- [Write asynchronous DAO queries](https://developer.android.com/training/data-storage/room/async-queries) - Android Developer official documentation

### Secondary (MEDIUM confidence)
- [Kotlin Coroutines and Room - Simplifying Database Operations](https://moldstud.com/articles/p-kotlin-coroutines-and-room-simplifying-database-operations-in-android-development) - Community guide verified against official docs
- [Offline-First Android: Room, DataStore, WorkManager patterns](https://medium.com/@vishalpvijayan4/offline-first-android-build-resilient-apps-for-spotty-networks-with-room-datastore-workmanager-4a23144e8ea2) - Background reconciliation patterns
- [Android Data Sync Approaches](https://medium.com/@shivayogih25/android-data-sync-approaches-offline-first-remote-first-hybrid-done-right-c4d065920164) - Eventual consistency strategies
- [Room Exception Handling](https://www.jndiary.com/post/room-exception-handling) - SQLite exception patterns

### Tertiary (LOW confidence)
- [Timestamps with Android Room](https://medium.com/@stephenja/timestamps-with-android-room-f3fd57b48250) - Timestamp best practices (2018, may be dated)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Room 2.6.1 is current version in project, extensively documented
- Architecture: HIGH - Patterns verified in androidx source code and official Android documentation
- Pitfalls: MEDIUM - Based on community reports and testing experiences, not all officially documented

**Research date:** 2026-02-05
**Valid until:** 90 days (Room is stable library, slow-changing)

**Key decision points for planner:**
1. ✅ Use existing Room 2.6.1 - no version upgrade needed
2. ✅ Foreign key CASCADE already configured in Phase 15
3. ⚠️ Need to add `destinationNumber` field to ForwardedMessage (schema migration v6 → v7)
4. ✅ Use `@Transaction` for atomic message insert + counter updates
5. ✅ Synchronous blocking writes in MasterService are correct approach for critical data
6. ✅ In-memory reconciliation queue sufficient for Phase 16 scope
