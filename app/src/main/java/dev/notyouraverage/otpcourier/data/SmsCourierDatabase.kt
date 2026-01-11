package dev.notyouraverage.otpcourier.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.notyouraverage.otpcourier.data.dao.ForwardingSessionDao
import dev.notyouraverage.otpcourier.data.dao.PairedDeviceDao
import dev.notyouraverage.otpcourier.data.entities.ForwardingSession
import dev.notyouraverage.otpcourier.data.entities.PairedDevice

@Database(
    entities = [PairedDevice::class, ForwardingSession::class],
    version = 3,
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

        fun getDatabase(context: Context): SmsCourierDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmsCourierDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
