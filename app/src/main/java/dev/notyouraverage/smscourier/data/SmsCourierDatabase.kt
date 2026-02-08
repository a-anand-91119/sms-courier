package dev.notyouraverage.smscourier.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.notyouraverage.smscourier.data.dao.ForwardedMessageDao
import dev.notyouraverage.smscourier.data.dao.ForwardingSessionDao
import dev.notyouraverage.smscourier.data.dao.PairedDeviceDao
import dev.notyouraverage.smscourier.data.entities.ForwardedMessage
import dev.notyouraverage.smscourier.data.entities.ForwardingSession
import dev.notyouraverage.smscourier.data.entities.PairedDevice

@Database(
    entities = [PairedDevice::class, ForwardingSession::class, ForwardedMessage::class],
    version = 8,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SmsCourierDatabase : RoomDatabase() {

    abstract fun pairedDeviceDao(): PairedDeviceDao
    abstract fun forwardingSessionDao(): ForwardingSessionDao
    abstract fun forwardedMessageDao(): ForwardedMessageDao

    companion object {
        private const val DATABASE_NAME = "sms_courier_database"

        @Volatile
        private var INSTANCE: SmsCourierDatabase? = null

        // Migration from version 1 to 2: Add encryption_key to forwarding_sessions and active_encryption_key to paired_devices
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add encryption_key column to forwarding_sessions table
                db.execSQL("ALTER TABLE forwarding_sessions ADD COLUMN encryption_key TEXT DEFAULT NULL")
                // Add active_encryption_key column to paired_devices table
                db.execSQL("ALTER TABLE paired_devices ADD COLUMN active_encryption_key TEXT DEFAULT NULL")
            }
        }

        // Migration from version 2 to 3: Add auth_key for challenge-response authentication
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE paired_devices ADD COLUMN auth_key TEXT DEFAULT NULL")
            }
        }

        // Migration from version 3 to 4: Change to composite primary key (phoneNumber, role)
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Step 1: Create new table with composite primary key
                db.execSQL(
                    """
                    CREATE TABLE paired_devices_new (
                        phoneNumber TEXT NOT NULL,
                        displayName TEXT,
                        device_role TEXT NOT NULL,
                        pairing_status TEXT NOT NULL,
                        password_hash TEXT,
                        password_salt TEXT,
                        active_encryption_key TEXT,
                        auth_key TEXT,
                        max_forward_duration_minutes INTEGER NOT NULL DEFAULT 30,
                        failed_attempts INTEGER NOT NULL DEFAULT 0,
                        locked_until INTEGER,
                        created_at INTEGER NOT NULL,
                        last_activity_at INTEGER NOT NULL,
                        PRIMARY KEY (phoneNumber, device_role)
                    )
                """,
                )

                // Step 2: Copy existing data from old table
                db.execSQL(
                    """
                    INSERT INTO paired_devices_new
                    SELECT * FROM paired_devices
                """,
                )

                // Step 3: Drop old table
                db.execSQL("DROP TABLE paired_devices")

                // Step 4: Rename new table to original name
                db.execSQL("ALTER TABLE paired_devices_new RENAME TO paired_devices")
            }
        }

        // Migration from version 4 to 5: Add rate limiting fields for resend pairing request
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE paired_devices ADD COLUMN resend_attempt_count INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE paired_devices ADD COLUMN last_resend_attempt_at INTEGER")
            }
        }

        // Migration from version 5 to 6: Add ForwardedMessage table, soft delete, and tracking columns
        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Step 1: Create ForwardedMessage table with foreign key CASCADE
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS forwarded_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        session_id INTEGER NOT NULL,
                        sender_number TEXT NOT NULL,
                        message_content TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY(session_id) REFERENCES forwarding_sessions(id)
                            ON DELETE CASCADE
                    )
                """,
                )

                // Step 2: Create indexes for query performance
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_forwarded_messages_session_id
                    ON forwarded_messages(session_id)
                """,
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_forwarded_messages_timestamp
                    ON forwarded_messages(timestamp)
                """,
                )

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

        // Migration from version 6 to 7: Add destination_number to ForwardedMessage
        internal val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add destination_number column with default empty string
                db.execSQL(
                    "ALTER TABLE forwarded_messages ADD COLUMN destination_number TEXT NOT NULL DEFAULT ''",
                )
                // Create index for destination_number queries
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_forwarded_messages_destination_number " +
                        "ON forwarded_messages(destination_number)",
                )
            }
        }

        // Migration from version 7 to 8: Remove orphaned FK from forwarding_sessions
        // The FK to paired_devices was invalid (phoneNumber is not unique in composite PK)
        internal val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite doesn't support DROP FOREIGN KEY, must recreate table
                // Step 1: Create new table without the FK
                db.execSQL(
                    """
                    CREATE TABLE forwarding_sessions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        device_phone_number TEXT NOT NULL,
                        started_at INTEGER NOT NULL,
                        duration_minutes INTEGER NOT NULL,
                        expires_at INTEGER NOT NULL,
                        is_active INTEGER NOT NULL DEFAULT 1,
                        stopped_by TEXT,
                        messages_forwarded INTEGER NOT NULL DEFAULT 0,
                        encryption_key TEXT,
                        message_count INTEGER NOT NULL DEFAULT 0,
                        updated_at INTEGER NOT NULL DEFAULT 0
                    )
                """,
                )

                // Step 2: Copy data from old table
                db.execSQL(
                    """
                    INSERT INTO forwarding_sessions_new (
                        id, device_phone_number, started_at, duration_minutes,
                        expires_at, is_active, stopped_by, messages_forwarded,
                        encryption_key, message_count, updated_at
                    )
                    SELECT id, device_phone_number, started_at, duration_minutes,
                           expires_at, is_active, stopped_by, messages_forwarded,
                           encryption_key, message_count, updated_at
                    FROM forwarding_sessions
                """,
                )

                // Step 3: Drop old table
                db.execSQL("DROP TABLE forwarding_sessions")

                // Step 4: Rename new table
                db.execSQL("ALTER TABLE forwarding_sessions_new RENAME TO forwarding_sessions")

                // Step 5: Recreate index
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_forwarding_sessions_device_phone_number
                    ON forwarding_sessions(device_phone_number)
                """,
                )
            }
        }

        fun getDatabase(context: Context): SmsCourierDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmsCourierDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
