package dev.notyouraverage.smscourier.integration

import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupActiveForwardingSession
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupApprovedTargetDevice
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupPendingPairingRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Infrastructure validation tests.
 *
 * These tests prove the integration test infrastructure works correctly:
 * - IntegrationTestBase setup (database, repositories, commandHandler)
 * - CapturingSmsSender captures outgoing messages
 * - ScenarioBuilders create valid test states
 *
 * This is NOT meant to exhaustively test flows (that's Phase 11).
 * This validates the infrastructure is ready for Phase 11.
 */
@IntegrationTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ExperimentalCoroutinesApi
class InfrastructureValidationTest : IntegrationTestBase() {

    @Test
    fun `infrastructure validates database and repositories work`() = runTest {
        // Setup: Create paired device via scenario builder
        val device = setupApprovedTargetDevice("+1234567890", "testPassword")

        // Verify: Device retrievable from repository
        val retrieved = deviceRepository.getByPhoneNumberAndRole("+1234567890", DeviceRole.TARGET)

        assertNotNull("Device should be retrievable", retrieved)
        assertEquals(PairingStatus.APPROVED, retrieved?.status)
        assertNotNull("Password hash should be set", retrieved?.passwordHash)
        assertNotNull("Auth key should be set", retrieved?.authKey)
    }

    @Test
    fun `infrastructure validates session repository works`() = runTest {
        // Setup: Create active forwarding session
        val (device, session) = setupActiveForwardingSession(
            phoneNumber = "+1234567890",
            durationMinutes = 30,
        )

        // Verify: Session retrievable and active
        val activeSessions = sessionRepository.getActiveSessionsList()
        assertEquals(1, activeSessions.size)
        assertEquals("+1234567890", activeSessions[0].devicePhoneNumber)
        assertTrue("Session should be active", activeSessions[0].isActive)
    }

    @Test
    fun `infrastructure validates capturing sender records messages`() = runTest {
        // Setup: Active session
        val (device, session) = setupActiveForwardingSession("+1234567890")

        // Action: Process incoming SMS through commandHandler
        commandHandler.handleIncomingSms("+5555555555", "Test OTP: 123456")

        // Wait for any async operations
        waitForAsync()

        // Verify: CapturingSmsSender captured the forwarded message
        assertEquals(1, capturingSmsSender.sentMessages.size)
        val sent = capturingSmsSender.sentMessages[0]
        assertEquals("+1234567890", sent.phoneNumber)
        assertTrue("Should be forward command", sent.message.startsWith("SMSC FWD"))
    }

    @Test
    fun `infrastructure validates command handler processes commands`() = runTest {
        // Setup: Pending pairing request
        val device = setupPendingPairingRequest("+1234567890")

        // Action: Approve pairing (sends PAIR_APPROVED)
        commandHandler.approvePairing("+1234567890", "testPassword123")

        // Wait for any async operations
        waitForAsync()

        // Verify: Status updated and SMS sent
        val updated = deviceRepository.getByPhoneNumberAndRole("+1234567890", DeviceRole.TARGET)
        assertEquals(PairingStatus.APPROVED, updated?.status)

        capturingSmsSender.assertMessageCount(1)
        val sent = capturingSmsSender.sentMessages[0]
        assertEquals("+1234567890", sent.phoneNumber)
        assertEquals("SMSC PAIR_APPROVED", sent.message)
    }

    @Test
    fun `infrastructure validates security manager integration`() = runTest {
        // Setup: Approved device ready for auth challenge
        val device = setupApprovedTargetDevice("+1234567890", "testPassword123")

        // Action: Generate challenge
        val nonce = securityManager.generateChallenge("+1234567890")

        // Verify: Challenge pending
        assertTrue("Should have pending challenge", securityManager.hasPendingChallenge("+1234567890"))
        assertFalse("Device should not be locked", securityManager.isDeviceLocked(device))
    }

    @Test
    fun `capturing sender findByPrefix filters correctly`() = runTest {
        // Setup: Send various command types
        capturingSmsSender.sendPairRequest("+1111111111")
        capturingSmsSender.sendPairApproved("+2222222222")
        capturingSmsSender.sendPairRejected("+3333333333")

        // Verify: Filter by prefix works
        val pairRequests = capturingSmsSender.findByPrefix("SMSC PAIR_REQUEST")
        val pairApproved = capturingSmsSender.findByPrefix("SMSC PAIR_APPROVED")
        val pairRejected = capturingSmsSender.findByPrefix("SMSC PAIR_REJECTED")

        assertEquals(1, pairRequests.size)
        assertEquals(1, pairApproved.size)
        assertEquals(1, pairRejected.size)
        assertEquals("+1111111111", pairRequests[0].phoneNumber)
    }

    @Test
    fun `capturing sender clear resets state`() = runTest {
        // Setup: Send some messages
        capturingSmsSender.sendPairRequest("+1234567890")
        assertEquals(1, capturingSmsSender.sentMessages.size)

        // Action: Clear
        capturingSmsSender.clear()

        // Verify: Empty
        capturingSmsSender.assertNothingSent()
    }
}
