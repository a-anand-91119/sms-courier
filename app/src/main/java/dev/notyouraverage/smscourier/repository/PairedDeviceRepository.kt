package dev.notyouraverage.smscourier.repository

import dev.notyouraverage.smscourier.data.dao.PairedDeviceDao
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.data.entities.PairingStatus
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

    suspend fun updateEncryptionKey(phoneNumber: String, role: DeviceRole, encryptionKey: String?) = withContext(ioDispatcher) {
        pairedDeviceDao.updateEncryptionKey(normalizePhoneNumber(phoneNumber), role, encryptionKey)
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

    private fun normalizePhoneNumber(phoneNumber: String): String {
        // Basic normalization: remove spaces, dashes, and parentheses
        // In production, use libphonenumber for proper E.164 normalization
        return phoneNumber
            .replace(Regex("[\\s\\-().]"), "")
            .let { if (it.startsWith("+")) it else "+$it" }
    }
}
