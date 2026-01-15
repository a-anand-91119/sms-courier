package dev.notyouraverage.smscourier.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.notyouraverage.smscourier.data.dao.ForwardingSessionDao
import dev.notyouraverage.smscourier.data.dao.PairedDeviceDao
import dev.notyouraverage.smscourier.data.entities.ForwardingSession
import dev.notyouraverage.smscourier.data.entities.PairedDevice

@Database(
    entities = [PairedDevice::class, ForwardingSession::class],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SmsCourierDatabase : RoomDatabase() {

    abstract fun pairedDeviceDao(): PairedDeviceDao
    abstract fun forwardingSessionDao(): ForwardingSessionDao

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
                db.execSQL("""
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
                """)

                // Step 2: Copy existing data from old table
                db.execSQL("""
                    INSERT INTO paired_devices_new
                    SELECT * FROM paired_devices
                """)

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

        fun getDatabase(context: Context): SmsCourierDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmsCourierDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
