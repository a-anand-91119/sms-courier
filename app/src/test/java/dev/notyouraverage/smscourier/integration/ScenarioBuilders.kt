package dev.notyouraverage.smscourier.integration

import dev.notyouraverage.smscourier.TestFixtures.createTestDevice
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.ForwardingSession
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import dev.notyouraverage.smscourier.security.SecurityManager
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Pre-built test scenarios for integration tests.
 *
 * These extension functions simplify setup of complex test states
 * by handling password hashing, auth key derivation, and database insertion.
 *
 * Usage:
 * ```
 * class MyIntegrationTest : IntegrationTestBase() {
 *     @Test
 *     fun `test with approved device`() = runTest {
 *         val device = setupApprovedSourceDevice("+1234567890", "password123")
 *         // device is now in database with bcrypt hash and auth key
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
object ScenarioBuilders {

    /**
     * Creates an approved SOURCE device (they receive our forwarded messages).
     * - PairingStatus.APPROVED
     * - Password hash and auth key derived from password
     * - Inserted into database
     *
     * Use when testing scenarios where this device is the TARGET that forwards TO SOURCE.
     */
    suspend fun IntegrationTestBase.setupApprovedSourceDevice(
        phoneNumber: String = "+1234567890",
        password: String = "testPassword123",
    ): PairedDevice {
        val passwordHash = SecurityManager.hashPassword(password)
        val authKey = SecurityManager.deriveAuthKey(password)

        val device = createTestDevice(
            phoneNumber = phoneNumber,
            role = DeviceRole.SOURCE,
            status = PairingStatus.APPROVED,
            passwordHash = passwordHash.hash,
            passwordSalt = passwordHash.salt,
            authKey = authKey,
        )
        deviceRepository.insert(device)
        return device
    }

    /**
     * Creates an approved TARGET device (we receive forwarded messages from them).
     * - PairingStatus.APPROVED
     * - Password hash and auth key derived from password
     * - activeEncryptionKey set for decrypting incoming FWDE messages
     * - Inserted into database
     *
     * Use when testing scenarios where this device is the SOURCE that receives FROM TARGET.
     */
    suspend fun IntegrationTestBase.setupApprovedTargetDevice(
        phoneNumber: String = "+1234567890",
        password: String = "testPassword123",
    ): PairedDevice {
        val passwordHash = SecurityManager.hashPassword(password)
        val authKey = SecurityManager.deriveAuthKey(password)

        val device = createTestDevice(
            phoneNumber = phoneNumber,
            role = DeviceRole.TARGET,
            status = PairingStatus.APPROVED,
            passwordHash = passwordHash.hash,
            passwordSalt = passwordHash.salt,
            authKey = authKey,
            // For decrypting FWDE messages
            activeEncryptionKey = authKey,
        )
        deviceRepository.insert(device)
        return device
    }

    /**
     * Creates an approved SOURCE device with an active forwarding session.
     * Returns both the device and the session.
     *
     * Use when testing forwarding logic where messages should be forwarded to SOURCE.
     */
    suspend fun IntegrationTestBase.setupActiveForwardingSession(
        phoneNumber: String = "+1234567890",
        durationMinutes: Int = 30,
        password: String = "testPassword123",
    ): Pair<PairedDevice, ForwardingSession> {
        val device = setupApprovedSourceDevice(phoneNumber, password)
        val encryptionKey = SecurityManager.deriveAuthKey(password)
        val sessionId = sessionRepository.startSession(phoneNumber, durationMinutes, encryptionKey)
        val session = sessionRepository.getSessionById(sessionId)!!
        return device to session
    }

    /**
     * Creates a pending pairing request scenario (we received PAIR_REQUEST from them).
     * - Device role: TARGET (they want us to forward to them)
     * - Status: PENDING_RECEIVED
     * - No password yet (set during approval)
     *
     * Use when testing pairing approval/rejection flows.
     */
    suspend fun IntegrationTestBase.setupPendingPairingRequest(
        phoneNumber: String = "+1234567890",
    ): PairedDevice {
        val device = createTestDevice(
            phoneNumber = phoneNumber,
            role = DeviceRole.TARGET,
            status = PairingStatus.PENDING_RECEIVED,
        )
        deviceRepository.insert(device)
        return device
    }

    /**
     * Creates a pending pairing sent scenario (we sent PAIR_REQUEST to them).
     * - Device role: SOURCE (we want to receive forwarded messages from them)
     * - Status: PENDING_SENT
     *
     * Use when testing scenarios where we're waiting for PAIR_APPROVED/PAIR_REJECTED.
     */
    suspend fun IntegrationTestBase.setupPendingSent(
        phoneNumber: String = "+1234567890",
    ): PairedDevice {
        val device = createTestDevice(
            phoneNumber = phoneNumber,
            role = DeviceRole.SOURCE,
            status = PairingStatus.PENDING_SENT,
        )
        deviceRepository.insert(device)
        return device
    }

    /**
     * Creates a locked device scenario (too many failed auth attempts).
     * - Device is approved but locked
     * - lockedUntil set to future time
     *
     * Use when testing lockout behavior for failed authentication.
     */
    suspend fun IntegrationTestBase.setupLockedDevice(
        phoneNumber: String = "+1234567890",
        password: String = "testPassword123",
        // 5 minutes default
        lockDurationMs: Long = 5 * 60 * 1000,
    ): PairedDevice {
        val passwordHash = SecurityManager.hashPassword(password)
        val authKey = SecurityManager.deriveAuthKey(password)

        val device = createTestDevice(
            phoneNumber = phoneNumber,
            role = DeviceRole.TARGET,
            status = PairingStatus.APPROVED,
            passwordHash = passwordHash.hash,
            passwordSalt = passwordHash.salt,
            authKey = authKey,
            failedAttempts = SecurityManager.MAX_FAILED_ATTEMPTS,
            lockedUntil = System.currentTimeMillis() + lockDurationMs,
        )
        deviceRepository.insert(device)
        return device
    }

    /**
     * Creates a bidirectional pairing scenario (both SOURCE and TARGET roles exist).
     * Useful for testing scenarios where both devices forward to each other.
     *
     * Returns: Pair<SOURCE device, TARGET device>
     */
    suspend fun IntegrationTestBase.setupBidirectionalPairing(
        phoneNumber: String = "+1234567890",
        password: String = "testPassword123",
    ): Pair<PairedDevice, PairedDevice> {
        val sourceDevice = setupApprovedSourceDevice(phoneNumber, password)
        val targetDevice = setupApprovedTargetDevice(phoneNumber, password)
        return sourceDevice to targetDevice
    }
}
