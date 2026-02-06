package dev.notyouraverage.smscourier.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PairedDeviceDao {

    @Query("SELECT * FROM paired_devices WHERE device_role = :role")
    fun getDevicesByRole(role: DeviceRole): Flow<List<PairedDevice>>

    @Query("SELECT * FROM paired_devices WHERE phoneNumber = :phoneNumber")
    suspend fun getDeviceByPhoneNumber(phoneNumber: String): List<PairedDevice>

    @Query("SELECT * FROM paired_devices WHERE phoneNumber = :phoneNumber AND device_role = :role")
    suspend fun getDeviceByPhoneNumberAndRole(phoneNumber: String, role: DeviceRole): PairedDevice?

    @Query("SELECT * FROM paired_devices WHERE device_role = :role AND pairing_status = :status")
    fun getDevicesByRoleAndStatus(role: DeviceRole, status: PairingStatus): Flow<List<PairedDevice>>

    @Query(
        """
        SELECT * FROM paired_devices
        WHERE device_role = 'TARGET'
        AND pairing_status = 'APPROVED'
        AND phoneNumber = :phoneNumber
        """,
    )
    suspend fun getApprovedSourceDevice(phoneNumber: String): PairedDevice?

    @Query("SELECT * FROM paired_devices WHERE pairing_status = 'PENDING_RECEIVED'")
    fun getPendingRequests(): Flow<List<PairedDevice>>

    @Query("SELECT * FROM paired_devices WHERE pairing_status = 'APPROVED'")
    fun getApprovedDevices(): Flow<List<PairedDevice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: PairedDevice)

    @Update
    suspend fun updateDevice(device: PairedDevice)

    @Delete
    suspend fun deleteDevice(device: PairedDevice)

    @Query("DELETE FROM paired_devices WHERE phoneNumber = :phoneNumber")
    suspend fun deleteByPhoneNumber(phoneNumber: String)

    @Query("DELETE FROM paired_devices WHERE phoneNumber = :phoneNumber AND device_role = :role")
    suspend fun deleteByPhoneNumberAndRole(phoneNumber: String, role: DeviceRole)

    @Query(
        """
        UPDATE paired_devices
        SET failed_attempts = :attempts, locked_until = :lockedUntil
        WHERE phoneNumber = :phoneNumber AND device_role = :role
        """,
    )
    suspend fun updateFailedAttempts(phoneNumber: String, role: DeviceRole, attempts: Int, lockedUntil: Long?)

    @Query("UPDATE paired_devices SET pairing_status = :status WHERE phoneNumber = :phoneNumber AND device_role = :role")
    suspend fun updatePairingStatus(phoneNumber: String, role: DeviceRole, status: PairingStatus)

    @Query(
        """
        UPDATE paired_devices
        SET password_hash = :passwordHash, password_salt = :passwordSalt
        WHERE phoneNumber = :phoneNumber AND device_role = :role
        """,
    )
    suspend fun updatePassword(phoneNumber: String, role: DeviceRole, passwordHash: String, passwordSalt: String)

    @Query("UPDATE paired_devices SET last_activity_at = :timestamp WHERE phoneNumber = :phoneNumber AND device_role = :role")
    suspend fun updateLastActivity(phoneNumber: String, role: DeviceRole, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE paired_devices SET active_encryption_key = :encryptionKey WHERE phoneNumber = :phoneNumber AND device_role = :role")
    suspend fun updateEncryptionKey(phoneNumber: String, role: DeviceRole, encryptionKey: String?)

    @Query("UPDATE paired_devices SET auth_key = :authKey WHERE phoneNumber = :phoneNumber AND device_role = :role")
    suspend fun updateAuthKey(phoneNumber: String, role: DeviceRole, authKey: String)

    @Query(
        """
        DELETE FROM paired_devices
        WHERE pairing_status IN ('PENDING_SENT', 'PENDING_RECEIVED')
        AND created_at < :threshold
        """,
    )
    suspend fun deletePendingOlderThan(threshold: Long)

    @Query(
        """
        UPDATE paired_devices
        SET resend_attempt_count = :count, last_resend_attempt_at = :timestamp
        WHERE phoneNumber = :phoneNumber AND device_role = :role
        """,
    )
    suspend fun updateResendAttempt(phoneNumber: String, role: DeviceRole, count: Int, timestamp: Long)

    @Query(
        """
        UPDATE paired_devices
        SET total_messages_forwarded = total_messages_forwarded + 1
        WHERE phoneNumber = :phoneNumber AND device_role = :role
        """,
    )
    suspend fun incrementTotalMessagesForwarded(phoneNumber: String, role: DeviceRole)

    @Query(
        """
        UPDATE paired_devices
        SET total_sessions = total_sessions + 1
        WHERE phoneNumber = :phoneNumber AND device_role = :role
        """,
    )
    suspend fun incrementTotalSessions(phoneNumber: String, role: DeviceRole)
}
