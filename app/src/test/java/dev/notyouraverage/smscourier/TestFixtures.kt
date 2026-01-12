package dev.notyouraverage.smscourier

import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.ForwardingSession
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.data.entities.PairingStatus

object TestFixtures {

    fun createTestDevice(
        phoneNumber: String = "+1234567890",
        displayName: String? = null,
        role: DeviceRole = DeviceRole.TARGET,
        status: PairingStatus = PairingStatus.APPROVED,
        passwordHash: String? = null,
        passwordSalt: String? = null,
        activeEncryptionKey: String? = null,
        authKey: String? = null,
        maxForwardDurationMinutes: Int = 30,
        failedAttempts: Int = 0,
        lockedUntil: Long? = null,
        createdAt: Long = System.currentTimeMillis(),
        lastActivityAt: Long = System.currentTimeMillis(),
    ) = PairedDevice(
        phoneNumber = phoneNumber,
        displayName = displayName,
        role = role,
        status = status,
        passwordHash = passwordHash,
        passwordSalt = passwordSalt,
        activeEncryptionKey = activeEncryptionKey,
        authKey = authKey,
        maxForwardDurationMinutes = maxForwardDurationMinutes,
        failedAttempts = failedAttempts,
        lockedUntil = lockedUntil,
        createdAt = createdAt,
        lastActivityAt = lastActivityAt,
    )

    fun createTestSession(
        id: Long = 1L,
        devicePhoneNumber: String = "+1234567890",
        startedAt: Long = System.currentTimeMillis(),
        durationMinutes: Int = 30,
        expiresAt: Long = System.currentTimeMillis() + (30 * 60 * 1000L),
        isActive: Boolean = true,
        stoppedBy: String? = null,
        messagesForwarded: Int = 0,
        encryptionKey: String? = null,
    ) = ForwardingSession(
        id = id,
        devicePhoneNumber = devicePhoneNumber,
        startedAt = startedAt,
        durationMinutes = durationMinutes,
        expiresAt = expiresAt,
        isActive = isActive,
        stoppedBy = stoppedBy,
        messagesForwarded = messagesForwarded,
        encryptionKey = encryptionKey,
    )
}
