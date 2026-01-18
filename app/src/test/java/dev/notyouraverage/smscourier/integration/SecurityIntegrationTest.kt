package dev.notyouraverage.smscourier.integration

import dev.notyouraverage.smscourier.TestFixtures.createTestDevice
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupActiveForwardingSession
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupApprovedSourceDevice
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupApprovedTargetDevice
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupLockedDevice
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupPendingPairingRequest
import dev.notyouraverage.smscourier.security.SecurityManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Security and error handling integration tests.
 *
 * Tests cover:
 * - Failed authentication tracking
 * - Device lockout mechanism
 * - Unknown device handling
 * - Idempotency of commands
 * - Invalid state transitions
 * - Rapid command handling
 */
@IntegrationTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ExperimentalCoroutinesApi
class SecurityIntegrationTest : IntegrationTestBase() {

    // ==================== Failed Authentication Tests ====================

    @Test
    fun `given approved device, when invalid HMAC response, then failed attempt incremented`() = runTest {
        // Setup: Create approved TARGET device (we are TARGET, they are SOURCE)
        val device = setupApprovedTargetDevice("+1234567890", "password123")

        // Step 1: Handle AUTH_REQUEST to create a pending challenge
        commandHandler.handleAuthRequest("+1234567890")
        waitForAsync()
        capturingSmsSender.clear()

        // Step 2: Send START_FORWARD with wrong HMAC response
        commandHandler.handleStartForward("+1234567890", "wrong-hmac", 30)
        waitForAsync()

        // Verify: Failed attempt recorded
        val updatedDevice = deviceRepository.getByPhoneNumberAndRole("+1234567890", DeviceRole.TARGET)
        assertNotNull("Device should exist", updatedDevice)
        assertTrue("Failed attempts should be > 0", updatedDevice!!.failedAttempts > 0)

        // Verify: No session created
        val sessions = sessionRepository.getActiveSessionsList()
        assertTrue("No session should be created", sessions.isEmpty())
    }

    @Test
    fun `given approved device, when multiple failed attempts, then count accumulates`() = runTest {
        // Setup: Create approved TARGET device
        setupApprovedTargetDevice("+1234567890", "password123")

        // Perform 3 failed authentication attempts
        repeat(3) {
            commandHandler.handleAuthRequest("+1234567890")
            waitForAsync()
            capturingSmsSender.clear()
            commandHandler.handleStartForward("+1234567890", "wrong-hmac", 30)
            waitForAsync()
        }

        // Verify: Failed attempts accumulated
        val device = deviceRepository.getByPhoneNumberAndRole("+1234567890", DeviceRole.TARGET)
        assertNotNull("Device should exist", device)
        assertEquals("Should have 3 failed attempts", 3, device!!.failedAttempts)
    }

    // ==================== Lockout Tests ====================

    @Test
    fun `given locked device, when AUTH_REQUEST received, then lockout respected`() = runTest {
        // Setup: Create locked device (TARGET role - they forward TO us)
        val device = setupLockedDevice("+1234567890", "password123", lockDurationMs = 5 * 60 * 1000)

        // Verify device is initially locked
        assertTrue("Device should be locked", securityManager.isDeviceLocked(device))

        // Action: Handle AUTH_REQUEST from this locked device
        commandHandler.handleAuthRequest("+1234567890")
        waitForAsync()

        // Verify: No AUTH_CHALLENGE sent because device is locked
        capturingSmsSender.assertNothingSent()
    }

    @Test
    fun `given locked device, when START_FORWARD received, then no session created`() = runTest {
        // Setup: Create locked device with a pending challenge
        val device = setupLockedDevice("+1234567890", "password123", lockDurationMs = 5 * 60 * 1000)

        // Generate a challenge manually (to simulate having one)
        securityManager.generateChallenge("+1234567890")

        // Action: Try to start forwarding from locked device
        commandHandler.handleStartForward("+1234567890", "any-response", 30)
        waitForAsync()

        // Verify: No session created
        val sessions = sessionRepository.getActiveSessionsList()
        assertTrue("No session should be created for locked device", sessions.isEmpty())
    }

    @Test
    fun `given device at max failed attempts, when another failed attempt, then locked out`() = runTest {
        // Setup: Create approved device with MAX - 1 failed attempts
        val passwordHash = SecurityManager.hashPassword("password123")
        val authKey = SecurityManager.deriveAuthKey("password123")

        val device = createTestDevice(
            phoneNumber = "+1234567890",
            role = DeviceRole.TARGET,
            status = PairingStatus.APPROVED,
            passwordHash = passwordHash.hash,
            passwordSalt = passwordHash.salt,
            authKey = authKey,
            failedAttempts = SecurityManager.MAX_FAILED_ATTEMPTS - 1,
        )
        deviceRepository.insert(device)

        // Step 1: Generate challenge
        commandHandler.handleAuthRequest("+1234567890")
        waitForAsync()
        capturingSmsSender.clear()

        // Step 2: Fail authentication one more time
        commandHandler.handleStartForward("+1234567890", "wrong-response", 30)
        waitForAsync()

        // Verify: Device is now locked
        val updatedDevice = deviceRepository.getByPhoneNumberAndRole("+1234567890", DeviceRole.TARGET)
        assertNotNull("Device should exist", updatedDevice)
        assertEquals("Should have MAX failed attempts", SecurityManager.MAX_FAILED_ATTEMPTS, updatedDevice!!.failedAttempts)
        assertNotNull("lockedUntil should be set", updatedDevice.lockedUntil)
        assertTrue("lockedUntil should be in future", updatedDevice.lockedUntil!! > System.currentTimeMillis())
    }

    // ==================== Unknown Device Tests ====================

    @Test
    fun `given unknown device, when START_FORWARD received, then no session created`() = runTest {
        // No setup - unknown device

        // Create a challenge for unknown device (even though it doesn't exist)
        securityManager.generateChallenge("+9999999999")

        // Action: Try to start forwarding
        commandHandler.handleStartForward("+9999999999", "any-response", 30)
        waitForAsync()

        // Verify: No session created, no error
        val sessions = sessionRepository.getActiveSessionsList()
        assertTrue("No session should be created", sessions.isEmpty())
    }

    @Test
    fun `given unknown device, when STOP_FORWARD received, then no error`() = runTest {
        // No setup - unknown device

        // Action: Try to stop forwarding from unknown device
        // This should not throw any exception
        commandHandler.handleStopForward("+9999999999")
        waitForAsync()

        // Verify: Graceful handling (no crash, no session to stop)
        val sessions = sessionRepository.getActiveSessionsList()
        assertTrue("No sessions exist", sessions.isEmpty())
    }

    @Test
    fun `given unknown device, when UNPAIR received, then no error`() = runTest {
        // No setup - unknown device

        // Action: Try to unpair unknown device
        commandHandler.handleUnpair("+9999999999", DeviceRole.SOURCE)
        waitForAsync()

        // Verify: No SMS sent (no confirmation to unknown device)
        capturingSmsSender.assertNothingSent()

        // Verify: No exception was thrown (test completes successfully)
    }

    // ==================== Idempotency Tests ====================

    @Test
    fun `given approved device, when duplicate PAIR_REQUEST received, then idempotent`() = runTest {
        // Setup: Create approved TARGET device (they are SOURCE, we forward to them)
        val phoneNumber = "+1234567890"
        setupApprovedTargetDevice(phoneNumber, "password123")

        // Action: Handle PAIR_REQUEST twice (simulating duplicate command)
        commandHandler.handlePairRequest(phoneNumber)
        waitForAsync()
        commandHandler.handlePairRequest(phoneNumber)
        waitForAsync()

        // Verify: Still one TARGET device with APPROVED status
        val device = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET)
        assertNotNull("Device should exist", device)
        assertEquals("Status should still be APPROVED", PairingStatus.APPROVED, device!!.status)

        // Verify: No duplicate entries (check SOURCE role doesn't exist)
        val allDevices = deviceRepository.getByPhoneNumber(phoneNumber)
        assertEquals("Should have only one device entry", 1, allDevices.size)
    }

    @Test
    fun `given active session, when duplicate STOP_FORWARD received, then idempotent`() = runTest {
        // Setup: Create active forwarding session
        val phoneNumber = "+1234567890"
        setupActiveForwardingSession(phoneNumber, 30)

        // Verify session is active
        val initialSessions = sessionRepository.getActiveSessionsList()
        assertEquals("Should have one active session", 1, initialSessions.size)

        // Action: Stop forwarding twice
        commandHandler.handleStopForward(phoneNumber)
        waitForAsync()
        commandHandler.handleStopForward(phoneNumber)
        waitForAsync()

        // Verify: Session ended once, no error on second call
        val finalSessions = sessionRepository.getActiveSessionsList()
        assertTrue("All sessions should be ended", finalSessions.isEmpty())
    }

    @Test
    fun `given pending received, when duplicate approval received, then idempotent`() = runTest {
        // Setup: Create pending pairing request
        val phoneNumber = "+1234567890"
        setupPendingPairingRequest(phoneNumber)

        // Action: Approve pairing twice
        commandHandler.approvePairing(phoneNumber, "password123")
        waitForAsync()
        capturingSmsSender.clear()

        commandHandler.approvePairing(phoneNumber, "password123")
        waitForAsync()

        // Verify: Device approved, second approval is idempotent
        val device = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET)
        assertNotNull("Device should exist", device)
        assertEquals("Status should be APPROVED", PairingStatus.APPROVED, device!!.status)
    }

    // ==================== Invalid State Transition Tests ====================

    @Test
    fun `given no device, when approval attempted, then no error`() = runTest {
        // No setup (no pending device)

        // Action: Try to approve non-existent device
        commandHandler.approvePairing("+9999999999", "password")
        waitForAsync()

        // Verify: No exception thrown, graceful handling
        // PAIR_APPROVED will still be sent (current behavior)
        // but no device created
        val device = deviceRepository.getByPhoneNumberAndRole("+9999999999", DeviceRole.TARGET)
        // Device may or may not be created depending on implementation
        // The key is no exception was thrown
    }

    @Test
    fun `given approved device, when approval attempted, then status unchanged`() = runTest {
        // Setup: Create approved device
        val phoneNumber = "+1234567890"
        val originalPassword = "originalPassword"
        setupApprovedTargetDevice(phoneNumber, originalPassword)

        // Capture original auth key for comparison
        val originalDevice = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET)
        val originalAuthKey = originalDevice?.authKey

        // Action: Try to approve again with different password
        commandHandler.approvePairing(phoneNumber, "newPassword")
        waitForAsync()

        // Verify: Status still APPROVED
        val device = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET)
        assertNotNull("Device should exist", device)
        assertEquals("Status should still be APPROVED", PairingStatus.APPROVED, device!!.status)
    }

    @Test
    fun `given rejected device, when PAIR_REQUEST received, then can re-request`() = runTest {
        // Setup: Create rejected device
        val phoneNumber = "+1234567890"
        val device = createTestDevice(
            phoneNumber = phoneNumber,
            role = DeviceRole.TARGET,
            status = PairingStatus.REJECTED,
        )
        deviceRepository.insert(device)

        // Action: Send new PAIR_REQUEST
        commandHandler.handlePairRequest(phoneNumber)
        waitForAsync()

        // Verify: Status changed to PENDING_RECEIVED (re-request allowed)
        val updatedDevice = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET)
        assertNotNull("Device should exist", updatedDevice)
        assertEquals(
            "Status should be PENDING_RECEIVED",
            PairingStatus.PENDING_RECEIVED,
            updatedDevice!!.status,
        )
    }

    // ==================== Rapid Command Tests ====================

    @Test
    fun `given approved device, when rapid AUTH_REQUESTS, then last challenge valid`() = runTest {
        // Setup: Create approved TARGET device
        val phoneNumber = "+1234567890"
        val password = "password123"
        setupApprovedTargetDevice(phoneNumber, password)

        // Action: Send 3 AUTH_REQUESTS rapidly (without waiting between)
        commandHandler.handleAuthRequest(phoneNumber)
        commandHandler.handleAuthRequest(phoneNumber)
        commandHandler.handleAuthRequest(phoneNumber)
        waitForAsync()

        // Extract nonce from last AUTH_CHALLENGE
        val challenges = capturingSmsSender.findByPrefix("SMSC AUTH_CHALLENGE")
        assertTrue("Should have sent AUTH_CHALLENGE(s)", challenges.isNotEmpty())

        val lastChallenge = challenges.last().message
        val nonce = lastChallenge.removePrefix("SMSC AUTH_CHALLENGE ")

        // Compute correct HMAC with the last nonce
        val authKey = SecurityManager.deriveAuthKey(password)
        val correctResponse = SecurityManager.computeHmac(authKey, nonce)

        capturingSmsSender.clear()

        // Action: Send START_FORWARD with correct HMAC
        commandHandler.handleStartForward(phoneNumber, correctResponse, 30)
        waitForAsync()

        // Verify: Session created (last challenge is valid)
        val sessions = sessionRepository.getActiveSessionsList()
        assertEquals("Session should be created", 1, sessions.size)
        assertEquals("Session for correct phone", phoneNumber, sessions[0].devicePhoneNumber)
    }
}
