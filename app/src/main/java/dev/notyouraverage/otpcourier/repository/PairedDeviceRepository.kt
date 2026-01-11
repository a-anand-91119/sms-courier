package dev.notyouraverage.otpcourier.repository

import dev.notyouraverage.otpcourier.data.dao.PairedDeviceDao
import dev.notyouraverage.otpcourier.data.entities.DeviceRole
import dev.notyouraverage.otpcourier.data.entities.PairedDevice
import dev.notyouraverage.otpcourier.data.entities.PairingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PairedDeviceRepository(
    private val pairedDeviceDao: PairedDeviceDao,
) {
    private val ioDispatcher = Dispatchers.IO

    fun getSourceDevices(): Flow<List<PairedDevice>> =
        pairedDeviceDao.getDevicesByRole(DeviceRole.SOURCE)

    fun getTargetDevices(): Flow<List<PairedDevice>> =
        pairedDeviceDao.getDevicesByRole(DeviceRole.TARGET)

    fun getPendingRequests(): Flow<List<PairedDevice>> =
        pairedDeviceDao.getPendingRequests()

    fun getApprovedDevices(): Flow<List<PairedDevice>> =
        pairedDeviceDao.getApprovedDevices()

    fun getDevicesByRoleAndStatus(role: DeviceRole, status: PairingStatus): Flow<List<PairedDevice>> =
        pairedDeviceDao.getDevicesByRoleAndStatus(role, status)

    suspend fun getByPhoneNumber(phoneNumber: String): PairedDevice? = withContext(ioDispatcher) {
        pairedDeviceDao.getDeviceByPhoneNumber(normalizePhoneNumber(phoneNumber))
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

    suspend fun updatePairingStatus(phoneNumber: String, status: PairingStatus) = withContext(ioDispatcher) {
        pairedDeviceDao.updatePairingStatus(normalizePhoneNumber(phoneNumber), status)
    }

    suspend fun updatePassword(phoneNumber: String, passwordHash: String, passwordSalt: String) =
        withContext(ioDispatcher) {
            pairedDeviceDao.updatePassword(normalizePhoneNumber(phoneNumber), passwordHash, passwordSalt)
        }

    suspend fun updateFailedAttempts(phoneNumber: String, attempts: Int, lockedUntil: Long?) =
        withContext(ioDispatcher) {
            pairedDeviceDao.updateFailedAttempts(normalizePhoneNumber(phoneNumber), attempts, lockedUntil)
        }

    suspend fun updateLastActivity(phoneNumber: String) = withContext(ioDispatcher) {
        pairedDeviceDao.updateLastActivity(normalizePhoneNumber(phoneNumber))
    }

    suspend fun updateEncryptionKey(phoneNumber: String, encryptionKey: String?) = withContext(ioDispatcher) {
        pairedDeviceDao.updateEncryptionKey(normalizePhoneNumber(phoneNumber), encryptionKey)
    }

    suspend fun updateAuthKey(phoneNumber: String, authKey: String) = withContext(ioDispatcher) {
        pairedDeviceDao.updateAuthKey(normalizePhoneNumber(phoneNumber), authKey)
    }

    suspend fun deletePendingOlderThan(threshold: Long) = withContext(ioDispatcher) {
        pairedDeviceDao.deletePendingOlderThan(threshold)
    }

    private fun normalizePhoneNumber(phoneNumber: String): String {
        // Basic normalization: remove spaces, dashes, and parentheses
        // In production, use libphonenumber for proper E.164 normalization
        return phoneNumber
            .replace(Regex("[\\s\\-().]"), "")
            .let { if (it.startsWith("+")) it else "+$it" }
    }
}
