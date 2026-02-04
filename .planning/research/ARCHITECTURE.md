# Architecture Research: Device History & Bidirectional Visibility Integration

**Domain:** Android Room + Jetpack Compose Architecture Extension
**Researched:** 2026-02-04
**Confidence:** HIGH

## Existing Architecture (v0.0.63)

### Current System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Layer (Jetpack Compose)                │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │  Home    │  │ Paired   │  │Forward   │  │Settings  │    │
│  │ Screen   │  │Devices   │  │Control   │  │ Screen   │    │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘    │
│       │             │              │             │           │
├───────┴─────────────┴──────────────┴─────────────┴───────────┤
│                    ViewModel Layer                            │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │  Home    │  │ Paired   │  │Forward   │  │Settings  │    │
│  │ViewModel │  │Devices   │  │Control   │  │ViewModel │    │
│  │          │  │ViewModel │  │ViewModel │  │          │    │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘    │
│       │             │              │             │           │
├───────┴─────────────┴──────────────┴─────────────┴───────────┤
│                   Repository Layer                            │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────┐   │
│  │ PairedDevice     │  │ ForwardingSession│  │Settings  │   │
│  │ Repository       │  │ Repository       │  │Repository│   │
│  └────────┬─────────┘  └────────┬─────────┘  └────┬─────┘   │
│           │                     │                  │          │
├───────────┴─────────────────────┴──────────────────┴─────────┤
│                    Service Layer                              │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐  ┌──────────────────┐                 │
│  │  MasterService   │  │ SmsCommandHandler│                 │
│  │  (foreground)    │←→│                  │                 │
│  └────────┬─────────┘  └────────┬─────────┘                 │
│           │                     │                             │
├───────────┴─────────────────────┴─────────────────────────────┤
│                    Data Layer (Room v5)                       │
├─────────────────────────────────────────────────────────────┤
│  ┌───────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │ PairedDevice  │  │ForwardingSession │  │  DataStore   │  │
│  │   Entity      │  │    Entity        │  │(Preferences) │  │
│  └───────────────┘  └──────────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Current Component Responsibilities

| Component | Responsibility | Current Implementation |
|-----------|----------------|------------------------|
| **SmsCourierDatabase** | Database singleton, migration management | Room v5, entities: PairedDevice, ForwardingSession |
| **PairedDevice Entity** | Stores device pairing info, roles, security | Composite PK: (phoneNumber, role), password hash, auth key |
| **ForwardingSession Entity** | Tracks forwarding sessions | Auto-increment PK, isActive flag, message count |
| **Repositories** | Expose Flow-based data access | Wraps DAOs, Flow transformation, normalization |
| **ViewModels** | Manage UI state via StateFlow | combine() flows, stateIn() for lifecycle awareness |
| **MasterService** | Orchestrates SMS operations | Foreground service, registers SmsReceiver, session management |
| **SmsCommandHandler** | Business logic for SMSC commands | Handles pairing, forwarding, message interception |

## New Architecture Requirements (v0.0.64)

### Required Additions

The new features require:
1. **ForwardedMessage entity** - Store individual forwarded messages with session relationship
2. **Device history tracking** - Track removed devices with soft delete pattern
3. **Bidirectional visibility calculation** - Compute forwarding direction indicators
4. **Export functionality** - Generate CSV/JSON/TXT from history
5. **Archive management** - Retention policies and auto-cleanup
6. **Session/contact view toggle** - UI state persistence

### Enhanced System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Layer (Jetpack Compose)                │
├─────────────────────────────────────────────────────────────┤
│  Existing Screens         │  NEW Screens                     │
│  ┌──────────┐            │  ┌──────────┐  ┌──────────┐      │
│  │  Home    │ (enhanced) │  │ Device   │  │ Session  │      │
│  │ Screen   │←───────────┼→ │ History  │  │ History  │      │
│  └──────────┘            │  └──────────┘  └──────────┘      │
│  ┌──────────┐            │  ┌──────────┐                    │
│  │ Paired   │ (enhanced) │  │ Archive  │                    │
│  │ Devices  │←───────────┼→ │ Mgmt     │                    │
│  └──────────┘            │  └──────────┘                    │
├──────────────────────────┴──────────────────────────────────┤
│                    ViewModel Layer                            │
├─────────────────────────────────────────────────────────────┤
│  Enhanced ViewModels      │  NEW ViewModels                  │
│  ┌──────────┐            │  ┌──────────┐  ┌──────────┐      │
│  │  Home    │ (+bidir    │  │ Device   │  │ Session  │      │
│  │ViewModel │  status)   │  │ History  │  │ History  │      │
│  └──────────┘            │  │ViewModel │  │ViewModel │      │
│                          │  └──────────┘  └──────────┘      │
│                          │  ┌──────────┐                    │
│                          │  │ Archive  │                    │
│                          │  │ViewModel │                    │
│                          │  └──────────┘                    │
├──────────────────────────┴──────────────────────────────────┤
│                   Repository Layer                            │
├─────────────────────────────────────────────────────────────┤
│  Existing Repositories    │  NEW Repository                  │
│  ┌──────────────────┐    │  ┌────────────────┐              │
│  │ PairedDevice     │    │  │ ForwardedMessage│              │
│  │ Repository       │    │  │ Repository      │              │
│  │ (enhanced)       │    │  │                 │              │
│  └──────────────────┘    │  └────────────────┘              │
│  ┌──────────────────┐    │                                   │
│  │ ForwardingSession│    │                                   │
│  │ Repository       │    │                                   │
│  │ (enhanced)       │    │                                   │
│  └──────────────────┘    │                                   │
├──────────────────────────┴──────────────────────────────────┤
│                    Service Layer                              │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐  ┌──────────────────┐  NEW            │
│  │ SmsCommandHandler│  │  MasterService   │  ┌──────────┐   │
│  │ (enhanced to     │  │                  │  │ History  │   │
│  │  store messages) │  │                  │  │ Cleanup  │   │
│  └──────────────────┘  └──────────────────┘  │ Worker   │   │
│                                               └──────────┘   │
├─────────────────────────────────────────────────────────────┤
│                    Data Layer (Room v6)                       │
├─────────────────────────────────────────────────────────────┤
│  Enhanced Entities        │  NEW Entity                      │
│  ┌───────────────┐        │  ┌────────────────┐             │
│  │ PairedDevice  │        │  │ ForwardedMessage│             │
│  │ (+isArchived, │        │  │ (FK: sessionId) │             │
│  │  archivedAt)  │        │  └────────────────┘             │
│  └───────────────┘        │                                  │
│  ┌───────────────┐        │                                  │
│  │ForwardingSession       │                                  │
│  │ (unchanged)   │        │                                  │
│  └───────────────┘        │                                  │
└─────────────────────────────────────────────────────────────┘
```

## Database Schema Migration (v5 → v6)

### Migration Strategy

Room database version 5 → 6 requires:

**MIGRATION_5_6:**
```kotlin
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Add ForwardedMessage table with foreign key to forwarding_sessions
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS forwarded_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                session_id INTEGER NOT NULL,
                original_sender TEXT NOT NULL,
                message_body TEXT NOT NULL,
                received_at INTEGER NOT NULL,
                was_encrypted INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(session_id) REFERENCES forwarding_sessions(id)
                    ON DELETE CASCADE
            )
        """)

        // 2. Create index for efficient session lookup
        db.execSQL("""
            CREATE INDEX index_forwarded_messages_session_id
            ON forwarded_messages(session_id)
        """)

        // 3. Add soft delete columns to paired_devices
        db.execSQL("""
            ALTER TABLE paired_devices
            ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0
        """)

        db.execSQL("""
            ALTER TABLE paired_devices
            ADD COLUMN archived_at INTEGER
        """)
    }
}
```

### Migration Best Practices Applied

1. **Schema export enabled** - `exportSchema = true` in `@Database` annotation generates JSON for version tracking
2. **Manual migration chosen** - Complex schema changes (foreign keys, indexes) require manual SQL
3. **Incremental version bump** - Version 5 → 6 (single increment)
4. **ON DELETE CASCADE** - Deleting sessions automatically removes associated messages
5. **Index creation** - Optimizes session-to-messages queries

**Sources:**
- [Migrate your Room database | Android Developers](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [Understanding migrations with Room | Medium](https://medium.com/androiddevelopers/understanding-migrations-with-room-f01e04b07929)

## New Entity: ForwardedMessage

### Entity Definition

```kotlin
@Entity(
    tableName = "forwarded_messages",
    foreignKeys = [
        ForeignKey(
            entity = ForwardingSession::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("session_id")]
)
data class ForwardedMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    @ColumnInfo(name = "original_sender")
    val originalSender: String,

    @ColumnInfo(name = "message_body")
    val messageBody: String,

    @ColumnInfo(name = "received_at")
    val receivedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "was_encrypted")
    val wasEncrypted: Boolean = false
)
```

### Relationship Pattern: One-to-Many

**Pattern:** ForwardingSession (parent) → ForwardedMessage (children)

- **Foreign key constraint** ensures referential integrity
- **ON DELETE CASCADE** automatically removes messages when session deleted
- **Index on session_id** optimizes queries for session history
- **@Relation annotation NOT needed** for basic foreign key (only for embedded queries)

**Sources:**
- [Choose relationship types between objects | Android Developers](https://developer.android.com/training/data-storage/room/relationships)
- [Database relations with Room | Medium](https://medium.com/androiddevelopers/database-relations-with-room-544ab95e4542)

## Soft Delete Pattern: Device Archiving

### Implementation Strategy

Instead of hard deleting paired devices, implement **soft delete as lifecycle state**:

```kotlin
// Enhanced PairedDevice entity
@Entity(tableName = "paired_devices", ...)
data class PairedDevice(
    // ... existing fields ...

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "archived_at")
    val archivedAt: Long? = null
)
```

### Repository Query Patterns

```kotlin
// PairedDeviceDao enhancements
@Dao
interface PairedDeviceDao {
    // Active devices (existing queries enhanced)
    @Query("SELECT * FROM paired_devices WHERE device_role = :role AND is_archived = 0")
    fun getActiveDevicesByRole(role: DeviceRole): Flow<List<PairedDevice>>

    // Archived devices (NEW)
    @Query("SELECT * FROM paired_devices WHERE is_archived = 1 ORDER BY archived_at DESC")
    fun getArchivedDevices(): Flow<List<PairedDevice>>

    // All devices including archived (NEW)
    @Query("SELECT * FROM paired_devices ORDER BY is_archived ASC, last_activity_at DESC")
    fun getAllDevicesIncludingArchived(): Flow<List<PairedDevice>>

    // Archive operation (NEW)
    @Query("UPDATE paired_devices SET is_archived = 1, archived_at = :archivedAt WHERE phoneNumber = :phoneNumber AND device_role = :role")
    suspend fun archiveDevice(phoneNumber: String, role: DeviceRole, archivedAt: Long)

    // Restore from archive (NEW)
    @Query("UPDATE paired_devices SET is_archived = 0, archived_at = NULL WHERE phoneNumber = :phoneNumber AND device_role = :role")
    suspend fun restoreDevice(phoneNumber: String, role: DeviceRole)

    // Cleanup old archives (NEW)
    @Query("DELETE FROM paired_devices WHERE is_archived = 1 AND archived_at < :threshold")
    suspend fun deleteArchivedOlderThan(threshold: Long)
}
```

### Archiving vs Deletion Decision Tree

```
Device deletion requested
    ↓
Is pairing APPROVED?
    ├─ YES → Archive device (soft delete)
    │         - Preserve forwarding history
    │         - Keep sessions + messages
    │         - Send UNPAIR to remote
    │
    └─ NO → Hard delete
            - PENDING_SENT: No history to preserve
            - PENDING_RECEIVED: Never completed pairing
            - REJECTED: Already concluded
```

**Rationale:** Soft delete as **explicit lifecycle state** (archived) is clearer than hidden boolean flag.

**Sources:**
- [Avoiding the soft delete anti-pattern | Cultured Systems](https://www.cultured.systems/2024/04/24/Soft-delete/)

## Integration Points

### Summary of Files to Modify

| File | Change Type | Why |
|------|-------------|-----|
| `handlers/SmsCommandHandler.kt` | Enhance | Store messages on forward/receive |
| `viewmodels/PairedDevicesViewModel.kt` | Enhance | Archive instead of hard delete |
| `viewmodels/HomeViewModel.kt` | Enhance | Calculate bidirectional status |
| `data/dao/PairedDeviceDao.kt` | Enhance | Add archive queries |
| `repository/PairedDeviceRepository.kt` | Enhance | Add archive methods |

### Integration Point 1: Message Storage in SmsCommandHandler

**File to modify:** `handlers/SmsCommandHandler.kt`

**Change:** Add `ForwardedMessageRepository` parameter, call `insertMessage()` after forwarding

**Location:** `handleIncomingSms()` method (line ~355-368)

**After forwarding each SMS:**
```kotlin
forwardedMessageRepository.insertMessage(
    ForwardedMessage(
        sessionId = session.id,
        originalSender = originalSender,
        messageBody = messageBody,
        wasEncrypted = session.encryptionKey != null
    )
)
```

### Integration Point 2: Message Storage on Receive (SOURCE device)

**File to modify:** `handlers/SmsCommandHandler.kt`

**Change:** Store received forwarded messages

**Locations:**
- `handleForwardedDataEncrypted()` (line ~310-342)
- `handleForwardedData()` (unencrypted version)

**After showing notification:**
```kotlin
val activeSession = sessionRepository.getActiveSessionForDevice(senderPhone)
if (activeSession != null) {
    forwardedMessageRepository.insertMessage(
        ForwardedMessage(
            sessionId = activeSession.id,
            originalSender = originalSender,
            messageBody = message,
            wasEncrypted = true
        )
    )
}
```

### Integration Point 3: Archive on Device Deletion

**File to modify:** `viewmodels/PairedDevicesViewModel.kt`

**Change:** Archive APPROVED devices instead of hard delete

**Location:** `deleteDevice()` method (line ~89-113)

**Replace hard delete with:**
```kotlin
if (device.status == PairingStatus.APPROVED) {
    // Send UNPAIR SMS
    smsSender.sendUnpair(device.phoneNumber, roleToDeleteOnRemote)

    // CHANGED: Archive instead of hard delete (preserves history)
    deviceRepository.archiveDevice(device.phoneNumber, device.role)
} else {
    // Hard delete for non-approved pairings (no history to preserve)
    deviceRepository.deleteByPhoneNumberAndRole(device.phoneNumber, device.role)
}
```

### Integration Point 4: Bidirectional Status Calculation

**File to modify:** `viewmodels/HomeViewModel.kt`

**Change:** Calculate bidirectional status from devices and sessions

**Location:** `homeState` StateFlow declaration

**Add helper function:**
```kotlin
private fun calculateDeviceStatuses(
    devices: List<PairedDevice>,
    sessions: List<ForwardingSession>
): Map<String, BidirectionalStatus> {
    val devicesByPhone = devices.groupBy { it.phoneNumber }
    val activeSessionPhones = sessions.map { it.devicePhoneNumber }.toSet()

    return devicesByPhone.mapValues { (phone, deviceRoles) ->
        val hasSource = deviceRoles.any { it.role == DeviceRole.SOURCE }
        val hasTarget = deviceRoles.any { it.role == DeviceRole.TARGET }
        val isForwardingActive = activeSessionPhones.contains(phone)

        when {
            hasSource && hasTarget && isForwardingActive ->
                BidirectionalStatus.BIDIRECTIONAL_ACTIVE  // ⇅
            hasSource && hasTarget ->
                BidirectionalStatus.BIDIRECTIONAL_INACTIVE // ↕
            hasSource && isForwardingActive ->
                BidirectionalStatus.RECEIVING              // ↓
            hasSource ->
                BidirectionalStatus.CAN_RECEIVE            // ⇩
            hasTarget && isForwardingActive ->
                BidirectionalStatus.SENDING                // ↑
            hasTarget ->
                BidirectionalStatus.CAN_SEND               // ⇧
            else ->
                BidirectionalStatus.NONE                   // −
        }
    }
}
```

**Pattern:** Reactive calculation using `combine()` Flow operator

**Sources:**
- [State and Jetpack Compose | Android Developers](https://developer.android.com/develop/ui/compose/state)
- [Jetpack Compose with ViewModel and Flow | Medium](https://medium.com/@android-world/jetpack-compose-with-viewmodel-and-flow-a-comprehensive-guide-ce3b079a44d1)

## New Components Summary

| Component | Type | File Location | Purpose |
|-----------|------|---------------|---------|
| ForwardedMessage | Entity | `data/entities/ForwardedMessage.kt` | Store individual messages |
| ForwardedMessageDao | DAO | `data/dao/ForwardedMessageDao.kt` | Database operations for messages |
| ForwardedMessageRepository | Repository | `repository/ForwardedMessageRepository.kt` | Expose Flow-based message access, export |
| DeviceHistoryViewModel | ViewModel | `viewmodels/DeviceHistoryViewModel.kt` | Manage device history screen state |
| SessionHistoryViewModel | ViewModel | `viewmodels/SessionHistoryViewModel.kt` | Manage session history screen state |
| ArchiveManagementViewModel | ViewModel | `viewmodels/ArchiveManagementViewModel.kt` | Manage archive settings and cleanup |
| HistoryCleanupWorker | Worker | `workers/HistoryCleanupWorker.kt` | Periodic background cleanup |
| DeviceHistoryScreen | Composable | `composables/screens/DeviceHistoryScreen.kt` | UI for device history |
| SessionHistoryScreen | Composable | `composables/screens/SessionHistoryScreen.kt` | UI for session history |
| ArchiveManagementScreen | Composable | `composables/screens/ArchiveManagementScreen.kt` | UI for archive management |
| BidirectionalStatus | Enum | `data/entities/BidirectionalStatus.kt` | Forwarding direction indicators |
| ExportFormat | Enum | `data/enums/ExportFormat.kt` | CSV, JSON, TXT formats |

## Export Pattern

### File Generation and Sharing

Export functionality uses Android's FileProvider + share intent pattern:

1. **Generate content** in repository (withContext(Dispatchers.IO))
2. **Write to cache directory** (app-private, temporary)
3. **Create URI** using FileProvider
4. **Launch share intent** with ACTION_SEND

**FileProvider setup required:**
- Add provider to `AndroidManifest.xml`
- Create `res/xml/file_paths.xml` with cache-path

**Export formats:**
- **CSV**: Headers + rows, for spreadsheet import
- **JSON**: Structured data, for programmatic access
- **TXT**: Human-readable, for viewing/printing

**Sources:**
- [Send simple data to other apps | Android Developers](https://developer.android.com/training/sharing/send)
- [Sharing a file | Android Developers](https://developer.android.com/training/secure-file-sharing/share-file)

## Data Flow Changes

### Message Forwarding Flow (Enhanced)

```
Incoming SMS to TARGET device
    ↓
SmsReceiver intercepts
    ↓
MasterService.onReceive()
    ↓
SmsCommandHandler.handleIncomingSms()
    ↓
For each active session:
    ├─ Send forwarded SMS to SOURCE
    ├─ sessionRepository.recordForwardedMessage(session.id)
    └─ NEW: forwardedMessageRepository.insertMessage() [TARGET side]
    ↓
SMS sent to SOURCE device
    ↓
SmsReceiver on SOURCE
    ↓
SmsCommandHandler.handleForwardedDataEncrypted()
    ↓
    ├─ Decrypt message
    ├─ Show notification
    └─ NEW: forwardedMessageRepository.insertMessage() [SOURCE side]
```

### Device Deletion Flow (Enhanced)

```
User taps "Delete Device"
    ↓
PairedDevicesViewModel.deleteDevice()
    ↓
Is status APPROVED?
    ├─ YES → Archive flow
    │   ├─ End active sessions
    │   ├─ Send UNPAIR SMS
    │   └─ deviceRepository.archiveDevice()
    │       └─ UPDATE paired_devices SET is_archived = 1
    │           (sessions + messages preserved)
    │
    └─ NO → Hard delete flow
        ├─ End active sessions
        └─ deviceRepository.deleteByPhoneNumberAndRole()
            └─ DELETE FROM paired_devices
                (cascades to sessions → messages via FK)
```

### History Cleanup Flow

```
Daily at midnight (WorkManager)
    ↓
HistoryCleanupWorker.doWork()
    ↓
1. Get retention setting (e.g., 90 days)
2. Calculate threshold timestamp
    ↓
3. DELETE FROM paired_devices
   WHERE is_archived = 1 AND archived_at < threshold
    ↓
4. DELETE FROM forwarded_messages
   WHERE received_at < threshold
    ↓
Result.success()
```

## Recommended Build Order

### Phase Structure Recommendation

Based on dependencies and integration complexity:

**Phase 1: Data Layer Foundation**
- Migration 5 → 6 (ForwardedMessage table, soft delete columns)
- ForwardedMessage entity
- ForwardedMessageDao
- ForwardedMessageRepository
- Enhanced PairedDeviceDao (archive queries)
- Enhanced PairedDeviceRepository (archive methods)
- **Rationale:** Everything depends on data layer

**Phase 2: Message Storage Integration**
- Update SmsCommandHandler (message storage on forward)
- Update SmsCommandHandler (message storage on receive)
- Enhanced PairedDevicesViewModel (archive on delete)
- **Rationale:** Starts populating database for testing UI

**Phase 3: History UI - Device Level**
- DeviceHistoryViewModel
- DeviceHistoryScreen
- Archive indicator on PairedDevicesScreen
- Navigation integration
- **Rationale:** Users need device-level view before drilling into sessions

**Phase 4: History UI - Session Level**
- SessionHistoryViewModel
- SessionHistoryScreen
- Message list with session breakdown
- Navigation from device history
- **Rationale:** Detail view after master view

**Phase 5: Bidirectional Visibility**
- Enhanced HomeViewModel (status calculation)
- BidirectionalStatus enum
- Update HomeScreen (indicators)
- Update PairedDevicesScreen (directional arrows)
- **Rationale:** Independent of history, can be done in parallel

**Phase 6: Export Functionality**
- Export format generation (CSV, JSON, TXT)
- FileProvider setup
- Share intent integration
- Export UI in history screens
- **Rationale:** Requires data layer but independent of cleanup

**Phase 7: Archive Management**
- ArchiveManagementViewModel
- ArchiveManagementScreen
- Settings integration (retention days)
- Manual cleanup trigger
- **Rationale:** Management UI after core features

**Phase 8: Auto-Cleanup**
- HistoryCleanupWorker
- WorkManager scheduling
- Settings integration (auto-cleanup toggle)
- MainApplication initialization
- **Rationale:** Automation last after manual flows tested

## Anti-Patterns to Avoid

### Anti-Pattern 1: Eager Loading All Messages

**What people do:** Load all messages for all sessions on app startup
**Why it's wrong:** Causes memory pressure, slow UI, unnecessary database queries
**Do this instead:** Use pagination with `PagingSource` for large message lists, load messages only when session detail screen opened

### Anti-Pattern 2: Blocking UI Thread for Export

**What people do:** Generate CSV/JSON synchronously when user taps export
**Why it's wrong:** ANR (Application Not Responding) on large datasets
**Do this instead:** Use `withContext(Dispatchers.IO)` for file generation, show progress indicator

### Anti-Pattern 3: No Foreign Key Constraints

**What people do:** Manual cleanup of messages when session deleted
**Why it's wrong:** Easy to miss cleanup, orphaned records accumulate
**Do this instead:** Use `ForeignKey.CASCADE` to auto-delete messages with sessions

### Anti-Pattern 4: Hard Delete Everything

**What people do:** Delete paired devices immediately, lose all history
**Why it's wrong:** Users can't review history after removing device
**Do this instead:** Soft delete (archive) for APPROVED pairings, hard delete only for PENDING/REJECTED

### Anti-Pattern 5: Recalculating Status on Every Recomposition

**What people do:** Calculate bidirectional status in Composable
**Why it's wrong:** Recalculates on every recomposition, inefficient
**Do this instead:** Calculate in ViewModel using `combine()`, cache in StateFlow

## Sources

### Official Android Documentation
- [Migrate your Room database | Android Developers](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [Choose relationship types between objects | Android Developers](https://developer.android.com/training/data-storage/room/relationships)
- [Define work requests | Android Developers](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- [Send simple data to other apps | Android Developers](https://developer.android.com/training/sharing/send)
- [State and Jetpack Compose | Android Developers](https://developer.android.com/develop/ui/compose/state)

### Technical Articles
- [Understanding migrations with Room | Medium](https://medium.com/androiddevelopers/understanding-migrations-with-room-f01e04b07929)
- [Database relations with Room | Medium](https://medium.com/androiddevelopers/database-relations-with-room-544ab95e4542)
- [Jetpack Compose with ViewModel and Flow | Medium](https://medium.com/@android-world/jetpack-compose-with-viewmodel-and-flow-a-comprehensive-guide-ce3b079a44d1)
- [Avoiding the soft delete anti-pattern | Cultured Systems](https://www.cultured.systems/2024/04/24/Soft-delete/)

---
*Architecture research for: SMS Courier v0.0.64 Device History & Bidirectional Visibility*
*Researched: 2026-02-04*
*Confidence: HIGH - All findings verified against official Android documentation and existing codebase patterns*
