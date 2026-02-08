package dev.notyouraverage.smscourier.data.migrations

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.notyouraverage.smscourier.data.SmsCourierDatabase
import dev.notyouraverage.smscourier.data.SmsCourierDatabase.Companion.MIGRATION_5_6
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration tests for database version 5 to 6.
 *
 * Tests verify that:
 * - Existing PairedDevice data is preserved
 * - Existing ForwardingSession data is preserved
 * - New columns have correct default values
 * - ForwardingSession.message_count is initialized from messages_forwarded
 * - ForwardingSession.updated_at is initialized from started_at
 * - CASCADE delete removes forwarded_messages when session is deleted
 * - Indexes exist on forwarded_messages table
 */
@RunWith(AndroidJUnit4::class)
class Migration5To6Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SmsCourierDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate5To6_preservesExistingPairedDeviceData() {
        // Create database at version 5
        helper.createDatabase("migration-test-paired-device", 5).apply {
            // Insert a PairedDevice row with known values
            execSQL(
                """
                INSERT INTO paired_devices
                (phoneNumber, device_role, pairing_status, max_forward_duration_minutes,
                 failed_attempts, created_at, last_activity_at, resend_attempt_count)
                VALUES ('+15551234567', 'SOURCE', 'APPROVED', 30, 0, 1000, 2000, 0)
                """,
            )
            close()
        }

        // Run migration to version 6
        helper.runMigrationsAndValidate("migration-test-paired-device", 6, true, MIGRATION_5_6)

        // Verify existing data preserved and new columns have defaults
        helper.runMigrationsAndValidate("migration-test-paired-device", 6, true).apply {
            query("SELECT * FROM paired_devices WHERE phoneNumber = '+15551234567'").use { cursor ->
                assertTrue("PairedDevice row should exist after migration", cursor.moveToFirst())

                // Verify preserved columns
                assertEquals("+15551234567", cursor.getString(cursor.getColumnIndexOrThrow("phoneNumber")))
                assertEquals("SOURCE", cursor.getString(cursor.getColumnIndexOrThrow("device_role")))
                assertEquals("APPROVED", cursor.getString(cursor.getColumnIndexOrThrow("pairing_status")))
                assertEquals(30, cursor.getInt(cursor.getColumnIndexOrThrow("max_forward_duration_minutes")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("failed_attempts")))
                assertEquals(1000L, cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))
                assertEquals(2000L, cursor.getLong(cursor.getColumnIndexOrThrow("last_activity_at")))

                // Verify new soft delete columns have defaults
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("is_archived")))
                assertTrue("archived_at should be NULL", cursor.isNull(cursor.getColumnIndexOrThrow("archived_at")))
                assertTrue("archival_initiated_by should be NULL", cursor.isNull(cursor.getColumnIndexOrThrow("archival_initiated_by")))

                // Verify new aggregate statistics columns have defaults
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("total_sessions")))
                assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("total_messages_forwarded")))
            }
            close()
        }
    }

    @Test
    fun migrate5To6_preservesExistingForwardingSessionData() {
        // Create database at version 5
        helper.createDatabase("migration-test-forwarding-session", 5).apply {
            // Insert a ForwardingSession row with known values
            execSQL(
                """
                INSERT INTO forwarding_sessions
                (device_phone_number, started_at, duration_minutes, expires_at, is_active, messages_forwarded)
                VALUES ('+15551234567', 3000, 30, 4800, 1, 5)
                """,
            )
            close()
        }

        // Run migration to version 6
        helper.runMigrationsAndValidate("migration-test-forwarding-session", 6, true, MIGRATION_5_6)

        // Verify existing data preserved and new columns initialized correctly
        helper.runMigrationsAndValidate("migration-test-forwarding-session", 6, true).apply {
            query("SELECT * FROM forwarding_sessions WHERE device_phone_number = '+15551234567'").use { cursor ->
                assertTrue("ForwardingSession row should exist after migration", cursor.moveToFirst())

                // Verify preserved columns
                assertEquals("+15551234567", cursor.getString(cursor.getColumnIndexOrThrow("device_phone_number")))
                assertEquals(3000L, cursor.getLong(cursor.getColumnIndexOrThrow("started_at")))
                assertEquals(30, cursor.getInt(cursor.getColumnIndexOrThrow("duration_minutes")))
                assertEquals(4800L, cursor.getLong(cursor.getColumnIndexOrThrow("expires_at")))
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("is_active")))
                assertEquals(5, cursor.getInt(cursor.getColumnIndexOrThrow("messages_forwarded")))

                // Verify message_count initialized from messages_forwarded
                assertEquals(5, cursor.getInt(cursor.getColumnIndexOrThrow("message_count")))

                // Verify updated_at initialized from started_at
                assertEquals(3000L, cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")))
            }
            close()
        }
    }

    @Test
    fun migrate5To6_foreignKeyCascadeDeletesMessages() {
        // Create database at version 5
        helper.createDatabase("migration-test-cascade", 5).apply {
            // Insert a ForwardingSession with explicit id
            execSQL(
                """
                INSERT INTO forwarding_sessions
                (id, device_phone_number, started_at, duration_minutes, expires_at, is_active, messages_forwarded)
                VALUES (100, '+15551234567', 1000, 30, 2000, 1, 0)
                """,
            )
            close()
        }

        // Run migration to version 6
        helper.runMigrationsAndValidate("migration-test-cascade", 6, true, MIGRATION_5_6)

        // Test CASCADE behavior
        helper.runMigrationsAndValidate("migration-test-cascade", 6, true).apply {
            // Enable foreign keys
            execSQL("PRAGMA foreign_keys=ON")

            // Insert a forwarded message linked to session 100
            execSQL(
                """
                INSERT INTO forwarded_messages (session_id, sender_number, message_content, timestamp)
                VALUES (100, '+15559876543', 'Test message', 3000)
                """,
            )

            // Verify message exists
            query("SELECT COUNT(*) FROM forwarded_messages WHERE session_id = 100").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }

            // Delete the session
            execSQL("DELETE FROM forwarding_sessions WHERE id = 100")

            // Verify message was CASCADE deleted
            query("SELECT COUNT(*) FROM forwarded_messages WHERE session_id = 100").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Message should be CASCADE deleted when session is deleted", 0, cursor.getInt(0))
            }

            close()
        }
    }

    @Test
    fun migrate5To6_indexesExist() {
        // Create database at version 5
        helper.createDatabase("migration-test-indexes", 5).apply {
            close()
        }

        // Run migration to version 6
        helper.runMigrationsAndValidate("migration-test-indexes", 6, true, MIGRATION_5_6)

        // Verify indexes exist
        helper.runMigrationsAndValidate("migration-test-indexes", 6, true).apply {
            query("SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='forwarded_messages'").use { cursor ->
                val indexNames = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    indexNames.add(cursor.getString(0))
                }

                // Verify session_id index exists
                assertTrue(
                    "Index on session_id should exist",
                    indexNames.any { it.contains("session_id") },
                )

                // Verify timestamp index exists
                assertTrue(
                    "Index on timestamp should exist",
                    indexNames.any { it.contains("timestamp") },
                )
            }
            close()
        }
    }

    @Test
    fun migrate5To6_forwardedMessagesTableExists() {
        // Create database at version 5
        helper.createDatabase("migration-test-table", 5).apply {
            close()
        }

        // Run migration to version 6
        helper.runMigrationsAndValidate("migration-test-table", 6, true, MIGRATION_5_6)

        // Verify forwarded_messages table exists
        helper.runMigrationsAndValidate("migration-test-table", 6, true).apply {
            query("SELECT name FROM sqlite_master WHERE type='table' AND name='forwarded_messages'").use { cursor ->
                assertTrue("forwarded_messages table should exist after migration", cursor.moveToFirst())
                assertEquals("forwarded_messages", cursor.getString(0))
            }
            close()
        }
    }
}
