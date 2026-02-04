# Phase 15: Database Foundation & Migration - Research

**Researched:** 2026-02-04
**Domain:** Room Database Migrations, Schema Management, Migration Testing
**Confidence:** HIGH

## Summary

Phase 15 adds a new ForwardedMessage table with foreign key CASCADE to ForwardingSession, adds soft delete columns to PairedDevice, adds tracking columns to both entities, and performs database migration from version 5 to 6. The standard approach uses Room's manual Migration class with SQL DDL statements, MigrationTestHelper for validated testing, and proper schema export configuration for compile-time validation.

Room 2.8.4 is the latest stable version (Nov 2025) and provides all necessary capabilities including foreign key support, migration testing, and schema validation. The codebase currently uses Room 2.6.1 and should be upgraded to 2.8.4 for improved performance (prepared statement cache) and bug fixes.

The migration requires careful sequencing: create new table first, then alter existing tables, add indexes last. Foreign key CASCADE ensures referential integrity automatically. Soft delete pattern uses timestamp columns (archivedAt) rather than boolean flags for better audit trails. Aggregate statistics columns on PairedDevice enable efficient queries without JOINs.

**Primary recommendation:** Use manual Migration class with comprehensive SQL DDL, add androidx.room:room-testing:2.8.4 dependency, configure schema export via Room Gradle plugin, and write thorough migration tests with MigrationTestHelper.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| androidx.room:room-runtime | 2.8.4 | Database runtime | Official Android persistence library, Jetpack component |
| androidx.room:room-ktx | 2.8.4 | Coroutines + Flow support | First-class Kotlin support with suspend functions |
| androidx.room:room-compiler | 2.8.4 | Code generation (KSP) | Compile-time SQL validation and DAO implementation |
| androidx.room:room-testing | 2.8.4 | Migration testing | Official testing support with MigrationTestHelper |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| androidx.sqlite:sqlite-framework | (transitive) | SQLite driver | Default driver for Android |
| junit | 4.13.2 | Test framework | Already in project for unit tests |
| androidx.test.ext:junit | 1.2.1 | Android test extensions | Already in project for instrumented tests |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Manual Migration | AutoMigration (Room 2.4+) | AutoMigration only works for simple schema changes. Cannot handle: complex data transformations, foreign key additions, custom default values. Manual migration required for this phase. |
| room-testing | Manual database validation | MigrationTestHelper provides schema JSON validation and automatic verification. Manual approach would require parsing exported schemas and writing custom validation - not recommended. |

**Installation:**
```kotlin
// Current version in project: 2.6.1
// Recommended upgrade to: 2.8.4

// gradle/libs.versions.toml
[versions]
room = "2.8.4"  // Update from 2.6.1

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }

// app/build.gradle.kts
dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Add for migration testing
    testImplementation(libs.androidx.room.testing)
}
```

## Architecture Patterns

### Recommended Project Structure
```
app/src/
├── main/java/.../data/
│   ├── SmsCourierDatabase.kt          # @Database annotation, version = 6
│   ├── entities/
│   │   ├── ForwardedMessage.kt        # NEW: Message entity with FK
│   │   ├── PairedDevice.kt            # UPDATE: Add soft delete columns
│   │   └── ForwardingSession.kt       # UPDATE: Add tracking columns
│   ├── dao/
│   │   ├── ForwardedMessageDao.kt     # NEW: Message operations
│   │   ├── PairedDeviceDao.kt         # UPDATE: Archive queries
│   │   └── ForwardingSessionDao.kt    # UPDATE: Statistics queries
│   └── migrations/
│       └── Migration_5_6.kt           # NEW: Migration implementation
├── test/java/.../data/
│   └── migrations/
│       └── Migration_5_6_Test.kt      # NEW: Migration test
└── schemas/
    └── dev.notyouraverage.smscourier.data.SmsCourierDatabase/
        ├── 5.json                      # Exported from current state
        └── 6.json                      # Generated after migration
```

### Pattern 1: Manual Migration with SQL DDL
**What:** Define migration using raw SQL executed via SupportSQLiteDatabase.execSQL()
**When to use:** Complex schema changes requiring foreign keys, indexes, or data transformations
**Example:**
```kotlin
// Source: https://developer.android.com/training/data-storage/room/migrating-db-versions
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create new ForwardedMessage table with foreign key
        db.execSQL("""
            CREATE TABLE forwarded_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                session_id INTEGER NOT NULL,
                sender_number TEXT NOT NULL,
                message_content TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(session_id) REFERENCES forwarding_sessions(id)
                    ON DELETE CASCADE
            )
        """)

        // 2. Create indexes for query performance
        db.execSQL("""
            CREATE INDEX index_forwarded_messages_session_id
            ON forwarded_messages(session_id)
        """)
        db.execSQL("""
            CREATE INDEX index_forwarded_messages_timestamp
            ON forwarded_messages(timestamp)
        """)

        // 3. Add soft delete columns to PairedDevice
        db.execSQL("""
            ALTER TABLE paired_devices
            ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0
        """)
        db.execSQL("""
            ALTER TABLE paired_devices
            ADD COLUMN archived_at INTEGER
        """)
        db.execSQL("""
            ALTER TABLE paired_devices
            ADD COLUMN archival_initiated_by TEXT
        """)

        // 4. Add aggregate statistics to PairedDevice
        db.execSQL("""
            ALTER TABLE paired_devices
            ADD COLUMN total_sessions INTEGER NOT NULL DEFAULT 0
        """)
        db.execSQL("""
            ALTER TABLE paired_devices
            ADD COLUMN total_messages_forwarded INTEGER NOT NULL DEFAULT 0
        """)

        // 5. Add tracking columns to ForwardingSession
        db.execSQL("""
            ALTER TABLE forwarding_sessions
            ADD COLUMN message_count INTEGER NOT NULL DEFAULT 0
        """)
        db.execSQL("""
            ALTER TABLE forwarding_sessions
            ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0
        """)

        // 6. Initialize updated_at with started_at for existing sessions
        db.execSQL("""
            UPDATE forwarding_sessions
            SET updated_at = started_at
        """)

        // 7. Initialize message_count from messages_forwarded for existing sessions
        db.execSQL("""
            UPDATE forwarding_sessions
            SET message_count = messages_forwarded
        """)
    }
}

// In SmsCourierDatabase.kt companion object
Room.databaseBuilder(context, SmsCourierDatabase::class.java, DATABASE_NAME)
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
    .build()
```

### Pattern 2: Foreign Key with CASCADE Delete
**What:** Automatically delete child records when parent is deleted
**When to use:** When child records have no meaning without parent (messages without session)
**Example:**
```kotlin
// Source: https://developer.android.com/reference/android/arch/persistence/room/ForeignKey
@Entity(
    tableName = "forwarded_messages",
    foreignKeys = [
        ForeignKey(
            entity = ForwardingSession::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,  // Delete messages when session deleted
            onUpdate = ForeignKey.CASCADE   // Update if session ID changes (rare)
        )
    ],
    indices = [
        Index(value = ["session_id"]),  // Required for FK performance
        Index(value = ["timestamp"])     // For chronological queries
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
    val timestamp: Long = System.currentTimeMillis()
)
```

### Pattern 3: Soft Delete with Timestamp
**What:** Use nullable timestamp column instead of boolean flag for archival
**When to use:** When you need audit trail of when deletion occurred
**Example:**
```kotlin
// Source: https://jakubpchmiel.com/better-ways-of-using-room/
data class PairedDevice(
    // ... existing fields ...

    // Soft delete fields
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "archived_at")
    val archivedAt: Long? = null,  // NULL = not archived, timestamp = when archived

    @ColumnInfo(name = "archival_initiated_by")
    val archivalInitiatedBy: String? = null  // "LOCAL" or "REMOTE"
)

// DAO queries filter archived records
@Query("SELECT * FROM paired_devices WHERE is_archived = 0 AND device_role = :role")
fun getActiveDevicesByRole(role: DeviceRole): Flow<List<PairedDevice>>

@Query("SELECT * FROM paired_devices WHERE is_archived = 1 ORDER BY archived_at DESC")
fun getArchivedDevices(): Flow<List<PairedDevice>>

// Archive operation
suspend fun archiveDevice(phoneNumber: String, role: DeviceRole, initiatedBy: String) {
    val device = getDeviceByPhoneNumberAndRole(phoneNumber, role)
    device?.let {
        updateDevice(it.copy(
            isArchived = true,
            archivedAt = System.currentTimeMillis(),
            archivalInitiatedBy = initiatedBy
        ))
    }
}
```

### Pattern 4: Aggregate Statistics Columns
**What:** Denormalized count columns on parent entity for efficient queries
**When to use:** When COUNT(*) queries would be expensive and data is read frequently
**Example:**
```kotlin
data class PairedDevice(
    // ... existing fields ...

    // Aggregate statistics (denormalized for performance)
    @ColumnInfo(name = "total_sessions")
    val totalSessions: Int = 0,

    @ColumnInfo(name = "total_messages_forwarded")
    val totalMessagesForwarded: Int = 0
)

// Update statistics when session ends
suspend fun updateDeviceStatistics(phoneNumber: String, role: DeviceRole, sessionMessageCount: Int) {
    db.execSQL("""
        UPDATE paired_devices
        SET total_sessions = total_sessions + 1,
            total_messages_forwarded = total_messages_forwarded + ?
        WHERE phoneNumber = ? AND device_role = ?
    """, arrayOf(sessionMessageCount, phoneNumber, role.name))
}
```

### Pattern 5: Migration Testing with MigrationTestHelper
**What:** Validate migration preserves data and schema matches exported JSON
**When to use:** Every migration must be tested before release
**Example:**
```kotlin
// Source: https://developer.android.com/training/data-storage/room/migrating-db-versions
@RunWith(AndroidJUnit4::class)
class Migration_5_6_Test {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SmsCourierDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate5To6_preservesExistingData() {
        // 1. Create database at version 5 with test data
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL("""
                INSERT INTO paired_devices
                (phoneNumber, device_role, pairing_status, created_at, last_activity_at)
                VALUES ('+15551234567', 'SOURCE', 'APPROVED', 1000, 2000)
            """)
            execSQL("""
                INSERT INTO forwarding_sessions
                (device_phone_number, started_at, duration_minutes, expires_at,
                 is_active, messages_forwarded)
                VALUES ('+15551234567', 3000, 30, 4000, 1, 5)
            """)
            close()
        }

        // 2. Run migration and validate schema
        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // 3. Verify data preserved
        db.query("SELECT * FROM paired_devices").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("+15551234567", cursor.getString(cursor.getColumnIndex("phoneNumber")))

            // New columns have default values
            assertEquals(0, cursor.getInt(cursor.getColumnIndex("is_archived")))
            assertNull(cursor.getString(cursor.getColumnIndex("archived_at")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndex("total_sessions")))
        }

        db.query("SELECT * FROM forwarding_sessions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(5, cursor.getInt(cursor.getColumnIndex("message_count")))
            assertEquals(3000L, cursor.getLong(cursor.getColumnIndex("updated_at")))
        }

        // 4. Verify new table exists
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='forwarded_messages'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }

        // 5. Verify indexes exist
        db.query("SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='forwarded_messages'").use { cursor ->
            val indexes = mutableListOf<String>()
            while (cursor.moveToNext()) {
                indexes.add(cursor.getString(0))
            }
            assertTrue(indexes.any { it.contains("session_id") })
            assertTrue(indexes.any { it.contains("timestamp") })
        }

        db.close()
    }

    @Test
    fun migrate5To6_foreignKeyCascadeWorks() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL("""
                INSERT INTO forwarding_sessions
                (id, device_phone_number, started_at, duration_minutes, expires_at, is_active)
                VALUES (100, '+15551234567', 1000, 30, 2000, 1)
            """)
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Insert message linked to session
        db.execSQL("""
            INSERT INTO forwarded_messages
            (session_id, sender_number, message_content, timestamp)
            VALUES (100, '+15559876543', 'Test message', 3000)
        """)

        // Verify message exists
        db.query("SELECT COUNT(*) FROM forwarded_messages WHERE session_id = 100").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }

        // Delete session
        db.execSQL("DELETE FROM forwarding_sessions WHERE id = 100")

        // Verify message was CASCADE deleted
        db.query("SELECT COUNT(*) FROM forwarded_messages WHERE session_id = 100").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }

        db.close()
    }
}
```

### Pattern 6: Schema Export Configuration
**What:** Configure Room to export schema JSON files for validation
**When to use:** Always enable for production databases
**Example:**
```kotlin
// app/build.gradle.kts
// Source: https://medium.com/@vontonnie/create-room-schema-export-directory-7066d427eae8

plugins {
    id("androidx.room")  // Room Gradle Plugin (Room 2.6.0+)
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    // ... other config ...

    sourceSets {
        // Make schemas available to tests
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
}

// SmsCourierDatabase.kt
@Database(
    entities = [PairedDevice::class, ForwardingSession::class, ForwardedMessage::class],
    version = 6,
    exportSchema = true  // Must be true for testing and AutoMigration
)
@TypeConverters(Converters::class)
abstract class SmsCourierDatabase : RoomDatabase() {
    // ...
}
```

### Anti-Patterns to Avoid
- **Don't use constants for SQL in migrations:** Room cannot validate SQL at compile time if stored in constants. Always inline the full SQL statement in execSQL() calls.
- **Don't skip migration testing:** Migrations can fail in production if not tested. Always write MigrationTestHelper tests before releasing schema changes.
- **Don't use OnConflictStrategy.REPLACE with CASCADE:** REPLACE deletes the old row first, triggering CASCADE deletes. Use @Upsert annotation or INSERT OR IGNORE instead.
- **Don't forget indexes on foreign key columns:** Without indexes, SQLite performs full table scans when parent tables are modified. Room will warn but won't fail compilation.
- **Don't initialize default values only in migration:** If a column has a default value in migration but not in entity @ColumnInfo, fresh installs will have different schema than migrated databases. Always specify defaults in both places.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Migration validation | Custom schema comparison | MigrationTestHelper | Parses exported JSON schemas, validates structure and constraints, handles edge cases (nullable columns, indexes, foreign keys). Custom validation would miss subtle schema differences. |
| Migration execution | Custom SQLite wrapper | Room Migration class | Integrates with Room's version management, provides SupportSQLiteDatabase with proper error handling, ensures migrations run in correct order, supports fallback strategies. |
| Pagination for large message lists | Custom offset/limit queries | Paging 3 with Room PagingSource | Room automatically generates PagingSource implementation (Room 2.3.0+), handles memory efficiently, supports incremental loading, integrates with Compose LazyColumn. Required by phase design decisions. |
| Foreign key constraint validation | Manual referential integrity checks | SQLite FOREIGN KEY with CASCADE | SQLite enforces constraints atomically, prevents orphaned records, CASCADE automatically maintains consistency. Manual checks are error-prone and can't prevent race conditions. |
| Aggregate statistics updates | Computed queries with JOIN | Denormalized columns with UPDATE | JOIN queries are expensive for frequently displayed data (home screen). Denormalized columns provide O(1) reads at cost of O(1) writes. Justified by read-heavy usage pattern. |

**Key insight:** Room provides compile-time validation and runtime safety for migrations. Hand-rolling migration logic loses these guarantees and introduces risk of data corruption. Always use Room's Migration class and MigrationTestHelper.

## Common Pitfalls

### Pitfall 1: Missing Schema Export Configuration
**What goes wrong:** MigrationTestHelper tests fail with "Cannot find exported schema" or AutoMigration cannot generate migration code
**Why it happens:** exportSchema defaults to true but schemaDirectory is not configured, causing Room to skip schema export
**How to avoid:**
1. Add Room Gradle plugin to app/build.gradle.kts
2. Configure `room { schemaDirectory("$projectDir/schemas") }`
3. Verify schemas directory is NOT in .gitignore
4. Add schemas directory to androidTest assets in sourceSets
**Warning signs:** MigrationTestHelper constructor fails immediately, or Room compiler warnings about missing schema location

### Pitfall 2: Foreign Key Index Performance
**What goes wrong:** App becomes slow when deleting sessions or updating parent records with many child messages
**Why it happens:** Without index on child table foreign key column, SQLite performs full table scan when CASCADE delete/update occurs
**How to avoid:**
1. Always create index on foreign key columns: `Index(value = ["session_id"])`
2. Check Room build warnings - it will warn about missing indexes
3. Test migration with large dataset (1000+ messages) to verify performance
**Warning signs:** Room compiler warning "Expected an index on column(s) session_id", slow deletion in production

### Pitfall 3: Migration Data Loss from DEFAULT Inconsistency
**What goes wrong:** Fresh installs have different default values than migrated databases, causing queries to behave differently
**Why it happens:** Default value specified in migration SQL but not in entity @ColumnInfo(defaultValue = "...")
**How to avoid:**
1. When adding column with DEFAULT in migration, also add `@ColumnInfo(defaultValue = "...")` in entity
2. Keep defaults synchronized between SQL and Kotlin
3. Test both migration path and fresh install in tests
**Warning signs:** Different behavior between fresh installs and updates, inconsistent query results

### Pitfall 4: CASCADE Delete with REPLACE Conflict Strategy
**What goes wrong:** Using `@Insert(onConflict = OnConflictStrategy.REPLACE)` triggers CASCADE delete, wiping out child records unexpectedly
**Why it happens:** REPLACE in SQLite is DELETE + INSERT, and DELETE fires ON DELETE CASCADE
**How to avoid:**
1. Use `@Upsert` annotation (Room 2.5.0+) instead of REPLACE
2. Or use INSERT OR IGNORE and separate UPDATE
3. Document this behavior in DAO comments
**Warning signs:** Messages disappearing when session is "updated", unexpected CASCADE deletes in logs

### Pitfall 5: Not Testing Migration Chain
**What goes wrong:** Migration 5→6 works, but 4→5→6 fails because intermediate state is incompatible
**Why it happens:** Only testing latest migration, not full upgrade path from all previous versions
**How to avoid:**
1. Test migration from every historical version to latest
2. Use runMigrationsAndValidate with all migrations: `MIGRATION_4_5, MIGRATION_5_6`
3. Test on device with actual version 4 database before releasing
**Warning signs:** App crashes on update for users on older versions, migration succeeds in tests but fails in production

### Pitfall 6: Updated_At Without Trigger
**What goes wrong:** updated_at column exists but never changes from initial value
**Why it happens:** No mechanism to automatically update timestamp on record modification
**How to avoid:**
1. Option A: Update manually in DAO update methods
2. Option B: Create SQLite trigger in migration (more complex, not recommended for Android)
3. For this phase: manual update is sufficient since updates are infrequent
**Warning signs:** updated_at column always equals started_at, timestamps don't reflect latest activity

## Code Examples

Verified patterns from official sources:

### Complete Migration 5→6 Implementation
```kotlin
// SmsCourierDatabase.kt
// Source: Codebase analysis + https://developer.android.com/training/data-storage/room/migrating-db-versions
private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Step 1: Create ForwardedMessage table with foreign key CASCADE
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS forwarded_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                session_id INTEGER NOT NULL,
                sender_number TEXT NOT NULL,
                message_content TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(session_id) REFERENCES forwarding_sessions(id)
                    ON DELETE CASCADE
            )
        """)

        // Step 2: Create indexes for ForwardedMessage
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_forwarded_messages_session_id
            ON forwarded_messages(session_id)
        """)
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_forwarded_messages_timestamp
            ON forwarded_messages(timestamp)
        """)

        // Step 3: Add soft delete columns to PairedDevice
        db.execSQL("ALTER TABLE paired_devices ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE paired_devices ADD COLUMN archived_at INTEGER")
        db.execSQL("ALTER TABLE paired_devices ADD COLUMN archival_initiated_by TEXT")

        // Step 4: Add aggregate statistics to PairedDevice
        db.execSQL("ALTER TABLE paired_devices ADD COLUMN total_sessions INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE paired_devices ADD COLUMN total_messages_forwarded INTEGER NOT NULL DEFAULT 0")

        // Step 5: Add tracking columns to ForwardingSession
        db.execSQL("ALTER TABLE forwarding_sessions ADD COLUMN message_count INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE forwarding_sessions ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")

        // Step 6: Initialize new columns from existing data
        db.execSQL("UPDATE forwarding_sessions SET updated_at = started_at WHERE updated_at = 0")
        db.execSQL("UPDATE forwarding_sessions SET message_count = messages_forwarded WHERE message_count = 0")
    }
}

@Database(
    entities = [PairedDevice::class, ForwardingSession::class, ForwardedMessage::class],
    version = 6,  // Increment from 5
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SmsCourierDatabase : RoomDatabase() {
    abstract fun pairedDeviceDao(): PairedDeviceDao
    abstract fun forwardingSessionDao(): ForwardingSessionDao
    abstract fun forwardedMessageDao(): ForwardedMessageDao  // NEW

    companion object {
        private const val DATABASE_NAME = "sms_courier_database"

        fun getDatabase(context: Context): SmsCourierDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SmsCourierDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6  // Add new migration
                )
                .build()
        }
    }
}
```

### ForwardedMessage Entity with Foreign Key
```kotlin
// entities/ForwardedMessage.kt
package dev.notyouraverage.smscourier.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "forwarded_messages",
    foreignKeys = [
        ForeignKey(
            entity = ForwardingSession::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
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
    val timestamp: Long = System.currentTimeMillis()
)
```

### Updated PairedDevice Entity
```kotlin
// entities/PairedDevice.kt - Add new fields to existing entity
data class PairedDevice(
    // ... existing fields ...

    // Soft delete columns (DATA-02)
    @ColumnInfo(name = "is_archived", defaultValue = "0")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "archived_at")
    val archivedAt: Long? = null,

    @ColumnInfo(name = "archival_initiated_by")
    val archivalInitiatedBy: String? = null,  // "LOCAL" or "REMOTE"

    // Aggregate statistics (DATA-04)
    @ColumnInfo(name = "total_sessions", defaultValue = "0")
    val totalSessions: Int = 0,

    @ColumnInfo(name = "total_messages_forwarded", defaultValue = "0")
    val totalMessagesForwarded: Int = 0
)
```

### Updated ForwardingSession Entity
```kotlin
// entities/ForwardingSession.kt - Add new fields to existing entity
data class ForwardingSession(
    // ... existing fields ...

    // Message count tracking (DATA-03)
    @ColumnInfo(name = "message_count", defaultValue = "0")
    val messageCount: Int = 0,

    // Updated timestamp (DATA-03)
    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis()
)
```

### ForwardedMessageDao
```kotlin
// dao/ForwardedMessageDao.kt
package dev.notyouraverage.smscourier.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.notyouraverage.smscourier.data.entities.ForwardedMessage

@Dao
interface ForwardedMessageDao {

    @Insert
    suspend fun insertMessage(message: ForwardedMessage): Long

    @Query("SELECT * FROM forwarded_messages WHERE session_id = :sessionId ORDER BY timestamp DESC")
    fun getMessagesForSession(sessionId: Long): PagingSource<Int, ForwardedMessage>

    @Query("SELECT COUNT(*) FROM forwarded_messages WHERE session_id = :sessionId")
    suspend fun getMessageCountForSession(sessionId: Long): Int

    @Query("DELETE FROM forwarded_messages WHERE session_id = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)

    // Note: CASCADE delete will handle deletion automatically when session is deleted
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Schema export via KSP arg | Room Gradle Plugin | Room 2.6.0 (Sept 2023) | Cleaner configuration, better IDE support |
| Manual PagingSource implementation | Room auto-generates PagingSource | Room 2.3.0 (Oct 2020) | Zero boilerplate for paginated queries |
| OnConflictStrategy.REPLACE | @Upsert annotation | Room 2.5.0 (Aug 2022) | Avoids CASCADE delete issues with REPLACE |
| Manual migration only | AutoMigration for simple changes | Room 2.4.0 (April 2022) | Reduces boilerplate for schema additions |
| room-compiler as annotationProcessor | KSP (Kotlin Symbol Processing) | Room 2.5.0 (Aug 2022) | Faster builds, better Kotlin integration |

**Deprecated/outdated:**
- **annotationProcessor for room-compiler:** Use KSP instead (faster, Kotlin-native)
- **kapt for room-compiler:** Deprecated in favor of KSP
- **RoomSchemaArgProvider with kapt:** Use Room Gradle Plugin instead
- **Room 2.6.1 (project current):** Upgrade to 2.8.4 for prepared statement cache and bug fixes

## Open Questions

Things that couldn't be fully resolved:

1. **Should updated_at use SQLite trigger or manual updates?**
   - What we know: Triggers automatically update timestamp, but Room doesn't natively support trigger definitions
   - What's unclear: Whether trigger overhead justifies complexity for infrequent updates
   - Recommendation: Use manual updates in DAO for simplicity. If updated_at becomes critical, add trigger in future migration

2. **Performance impact of CASCADE delete with 10,000+ messages**
   - What we know: Indexes on foreign key columns prevent full table scans
   - What's unclear: Real-world performance with large message counts in production
   - Recommendation: Implement as designed with indexes, monitor performance in production, add pagination for message deletion if needed

3. **Room 2.8.4 compatibility with existing test infrastructure**
   - What we know: Project uses Room 2.6.1, upgrading to 2.8.4 recommended
   - What's unclear: Whether existing Robolectric tests (SmsReceiverTest, etc.) require changes
   - Recommendation: Upgrade Room version, run full test suite to verify compatibility, address any failures

## Sources

### Primary (HIGH confidence)
- [Migrate your Room database | Android Developers](https://developer.android.com/training/data-storage/room/migrating-db-versions) - Official migration guide
- [ForeignKey | API reference | Android Developers](https://developer.android.com/reference/android/arch/persistence/room/ForeignKey) - Foreign key constants and behavior
- [Room | Jetpack | Android Developers](https://developer.android.com/jetpack/androidx/releases/room) - Latest version 2.8.4 release notes
- Codebase analysis: SmsCourierDatabase.kt (current migration patterns), entities, DAOs

### Secondary (MEDIUM confidence)
- [Testing Room migrations | Medium](https://medium.com/androiddevelopers/testing-room-migrations-be93cdb0d975) - MigrationTestHelper patterns (verified with official docs)
- [Create ROOM Schema Export Directory | Medium](https://medium.com/@vontonnie/create-room-schema-export-directory-7066d427eae8) - Schema configuration (verified with official Room plugin docs)
- [Connecting Room Tables Using Foreign Keys | Medium](https://medium.com/@vontonnie/connecting-room-tables-using-foreign-keys-c19450361603) - Foreign key examples (verified with official API reference)
- [Better ways of using Room Database](https://jakubpchmiel.com/better-ways-of-using-room/) - Soft delete patterns
- [SQLite Triggers (+ Android Room) | ProAndroidDev](https://proandroiddev.com/sqlite-triggers-android-room-2e7120bb3e3a) - Trigger implementation patterns
- [Pagination in Android Room Database using Paging 3](https://genicsblog.com/gouravkhunger/pagination-in-android-room-database-using-the-paging-3-library) - PagingSource integration

### Tertiary (LOW confidence)
- [The Hidden Dangers of Room Database Performance | ProAndroidDev](https://proandroiddev.com/the-hidden-dangers-of-room-database-performance-and-how-to-fix-them-ac93830885bd) - Index performance discussion (Sep 2025, recent but single source)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Official AndroidX releases, version 2.8.4 confirmed from developer.android.com
- Architecture: HIGH - Manual migration and MigrationTestHelper verified from official Android documentation
- Pitfalls: MEDIUM - Combination of official docs and community experience, verified patterns

**Research date:** 2026-02-04
**Valid until:** 2026-03-04 (30 days - Room is stable Jetpack component)
