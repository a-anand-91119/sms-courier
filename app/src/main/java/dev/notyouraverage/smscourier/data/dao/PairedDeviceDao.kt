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
    suspend fun getDeviceByPhoneNumber(phoneNumber: String): PairedDevice?

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

    @Query(
        """
        UPDATE paired_devices
        SET failed_attempts = :attempts, locked_until = :lockedUntil
        WHERE phoneNumber = :phoneNumber
        """,
    )
    suspend fun updateFailedAttempts(phoneNumber: String, attempts: Int, lockedUntil: Long?)

    @Query("UPDATE paired_devices SET pairing_status = :status WHERE phoneNumber = :phoneNumber")
    suspend fun updatePairingStatus(phoneNumber: String, status: PairingStatus)

    @Query(
        """
        UPDATE paired_devices
        SET password_hash = :passwordHash, password_salt = :passwordSalt
        WHERE phoneNumber = :phoneNumber
        """,
    )
    suspend fun updatePassword(phoneNumber: String, passwordHash: String, passwordSalt: String)

    @Query("UPDATE paired_devices SET last_activity_at = :timestamp WHERE phoneNumber = :phoneNumber")
    suspend fun updateLastActivity(phoneNumber: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE paired_devices SET active_encryption_key = :encryptionKey WHERE phoneNumber = :phoneNumber")
    suspend fun updateEncryptionKey(phoneNumber: String, encryptionKey: String?)

    @Query("UPDATE paired_devices SET auth_key = :authKey WHERE phoneNumber = :phoneNumber")
    suspend fun updateAuthKey(phoneNumber: String, authKey: String)

    @Query(
        """
        DELETE FROM paired_devices
        WHERE pairing_status IN ('PENDING_SENT', 'PENDING_RECEIVED')
        AND created_at < :threshold
        """,
    )
    suspend fun deletePendingOlderThan(threshold: Long)
}
