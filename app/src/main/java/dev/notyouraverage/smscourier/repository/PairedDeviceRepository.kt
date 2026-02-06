package dev.notyouraverage.smscourier.repository

import android.util.Log
import dev.notyouraverage.smscourier.data.dao.PairedDeviceDao
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import dev.notyouraverage.smscourier.security.KeystoreEncryptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PairedDeviceRepository(
    private val pairedDeviceDao: PairedDeviceDao,
) {
    private val ioDispatcher = Dispatchers.IO

    companion object {
        private const val TAG = "SMSC:DeviceRepository"
    }

    fun getSourceDevices(): Flow<List<PairedDevice>> =
        pairedDeviceDao.getDevicesByRole(DeviceRole.SOURCE)

    fun getTargetDevices(): Flow<List<PairedDevice>> =
        pairedDeviceDao.getDevicesByRole(DeviceRole.TARGET)

    fun getActiveDevices(): Flow<List<PairedDevice>> =
        pairedDeviceDao.getActiveDevices()

    fun getArchivedDevices(): Flow<List<PairedDevice>> =
        pairedDeviceDao.getArchivedDevices()

    fun getPendingRequests(): Flow<List<PairedDevice>> =
        pairedDeviceDao.getPendingRequests()

    fun getApprovedDevices(): Flow<List<PairedDevice>> =
        pairedDeviceDao.getApprovedDevices()

    fun getDevicesByRoleAndStatus(role: DeviceRole, status: PairingStatus): Flow<List<PairedDevice>> =
        pairedDeviceDao.getDevicesByRoleAndStatus(role, status)

    suspend fun getByPhoneNumber(phoneNumber: String): List<PairedDevice> = withContext(ioDispatcher) {
        pairedDeviceDao.getDeviceByPhoneNumber(normalizePhoneNumber(phoneNumber))
    }

    suspend fun getByPhoneNumberAndRole(phoneNumber: String, role: DeviceRole): PairedDevice? = withContext(ioDispatcher) {
        pairedDeviceDao.getDeviceByPhoneNumberAndRole(normalizePhoneNumber(phoneNumber), role)
    }

    suspend fun getApprovedSourceDevice(phoneNumber: String): PairedDevice? = withContext(ioDispatcher) {
        pairedDeviceDao.getApprovedSourceDevice(normalizePhoneNumber(phoneNumber))
    }

    suspend fun insert(device: PairedDevice) = withContext(ioDispatcher) {
        pairedDeviceDao.insertDevice(
            device.copy(phoneNumber = normalizePhoneNumber(device.phoneNumber)),
        )
    }

    suspend fun update(device: PairedDevice) = withContext(ioDispatcher) {
        pairedDeviceDao.updateDevice(device)
    }

    suspend fun delete(device: PairedDevice) = withContext(ioDispatcher) {
        pairedDeviceDao.deleteDevice(device)
    }

    suspend fun deleteByPhoneNumber(phoneNumber: String) = withContext(ioDispatcher) {
        pairedDeviceDao.deleteByPhoneNumber(normalizePhoneNumber(phoneNumber))
    }

    suspend fun deleteByPhoneNumberAndRole(phoneNumber: String, role: DeviceRole) = withContext(ioDispatcher) {
        pairedDeviceDao.deleteByPhoneNumberAndRole(normalizePhoneNumber(phoneNumber), role)
    }

    suspend fun archiveDevice(phoneNumber: String, role: DeviceRole, initiatedBy: String) = withContext(ioDispatcher) {
        pairedDeviceDao.archiveDevice(
            normalizePhoneNumber(phoneNumber),
            role,
            System.currentTimeMillis(),
            initiatedBy,
        )
    }

    suspend fun updatePairingStatus(phoneNumber: String, role: DeviceRole, status: PairingStatus) = withContext(ioDispatcher) {
        pairedDeviceDao.updatePairingStatus(normalizePhoneNumber(phoneNumber), role, status)
    }

    suspend fun updatePassword(phoneNumber: String, role: DeviceRole, passwordHash: String, passwordSalt: String) =
        withContext(ioDispatcher) {
            pairedDeviceDao.updatePassword(normalizePhoneNumber(phoneNumber), role, passwordHash, passwordSalt)
        }

    suspend fun updateFailedAttempts(phoneNumber: String, role: DeviceRole, attempts: Int, lockedUntil: Long?) =
        withContext(ioDispatcher) {
            pairedDeviceDao.updateFailedAttempts(normalizePhoneNumber(phoneNumber), role, attempts, lockedUntil)
        }

    suspend fun updateLastActivity(phoneNumber: String, role: DeviceRole) = withContext(ioDispatcher) {
        pairedDeviceDao.updateLastActivity(normalizePhoneNumber(phoneNumber), role)
    }

    /**
     * Stores the encryption key after encrypting it with Android Keystore.
     * The key is encrypted at rest in the database.
     */
    suspend fun updateEncryptionKey(phoneNumber: String, role: DeviceRole, encryptionKey: String?) = withContext(ioDispatcher) {
        val encryptedKey = encryptionKey?.let { key ->
            KeystoreEncryptionManager.encrypt(key).also { encrypted ->
                if (encrypted == null) {
                    Log.e(TAG, "Failed to encrypt encryption key for $phoneNumber")
                }
            }
        }
        pairedDeviceDao.updateEncryptionKey(normalizePhoneNumber(phoneNumber), role, encryptedKey)
    }

    /**
     * Retrieves and decrypts the encryption key for a device.
     * Returns null if no key is stored or decryption fails.
     */
    suspend fun getDecryptedEncryptionKey(phoneNumber: String, role: DeviceRole): String? = withContext(ioDispatcher) {
        val device = pairedDeviceDao.getDeviceByPhoneNumberAndRole(normalizePhoneNumber(phoneNumber), role)
        val encryptedKey = device?.activeEncryptionKey ?: return@withContext null
        KeystoreEncryptionManager.decrypt(encryptedKey).also { decrypted ->
            if (decrypted == null) {
                Log.e(TAG, "Failed to decrypt encryption key for $phoneNumber")
            }
        }
    }

    suspend fun updateAuthKey(phoneNumber: String, role: DeviceRole, authKey: String) = withContext(ioDispatcher) {
        pairedDeviceDao.updateAuthKey(normalizePhoneNumber(phoneNumber), role, authKey)
    }

    suspend fun deletePendingOlderThan(threshold: Long) = withContext(ioDispatcher) {
        pairedDeviceDao.deletePendingOlderThan(threshold)
    }

    suspend fun recordResendAttempt(phoneNumber: String, role: DeviceRole) = withContext(ioDispatcher) {
        val device = getByPhoneNumberAndRole(phoneNumber, role) ?: return@withContext
        pairedDeviceDao.updateResendAttempt(
            normalizePhoneNumber(phoneNumber),
            role,
            device.resendAttemptCount + 1,
            System.currentTimeMillis(),
        )
    }

    suspend fun incrementTotalSessions(phoneNumber: String, role: DeviceRole) = withContext(ioDispatcher) {
        pairedDeviceDao.incrementTotalSessions(normalizePhoneNumber(phoneNumber), role)
    }

    suspend fun incrementTotalMessagesForwarded(phoneNumber: String, role: DeviceRole) = withContext(ioDispatcher) {
        pairedDeviceDao.incrementTotalMessagesForwarded(normalizePhoneNumber(phoneNumber), role)
    }

    private fun normalizePhoneNumber(phoneNumber: String): String {
        // Basic normalization: remove spaces, dashes, and parentheses
        // In production, use libphonenumber for proper E.164 normalization
        return phoneNumber
            .replace(Regex("[\\s\\-().]"), "")
            .let { if (it.startsWith("+")) it else "+$it" }
    }
}
