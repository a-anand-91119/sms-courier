package dev.notyouraverage.smscourier.integration

import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupActiveForwardingSession
import dev.notyouraverage.smscourier.security.SecurityManager
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
 * Integration tests for the complete forwarding flow.
 *
 * Tests cover:
 * - Authentication challenge generation and validation
 * - Session start via challenge-response
 * - Message forwarding to active sessions
 * - Multi-device forwarding
 * - Session stop and lifecycle
 *
 * Note: These tests use SOURCE devices (they receive forwarded messages)
 * because the TARGET (this device) generates challenges and forwards SMS.
 */
@IntegrationTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ExperimentalCoroutinesApi
class ForwardingFlowIntegrationTest : IntegrationTestBase() {

    // ==================== Authentication Challenge Tests ====================

    @Test
    fun `givenApprovedSourceDevice_whenAuthRequestReceived_thenChallengeSent`() = runTest {
        // Setup: Approved SOURCE device (they want to receive our forwarded messages)
        // Note: handleAuthRequest looks for TARGET role with their phone number
        // Because from TARGET's perspective, the SOURCE device is their target for forwarding
        setupApprovedTargetDevice("+1234567890", "password123")

        // Action: Receive AUTH_REQUEST from source device
        commandHandler.handleAuthRequest("+1234567890")
        waitForAsync()

        // Verify: AUTH_CHALLENGE was sent back
        capturingSmsSender.assertSent("+1234567890", Regex("SMSC AUTH_CHALLENGE .+"))

        // Verify: Challenge is pending
        assertTrue(
            "Should have pending challenge",
            securityManager.hasPendingChallenge("+1234567890"),
        )
    }

    @Test
    fun `givenApprovedSourceDevice_whenAuthRequestReceived_thenNonceIsBase64`() = runTest {
        // Setup
        setupApprovedTargetDevice("+1234567890", "password123")

        // Action
        commandHandler.handleAuthRequest("+1234567890")
        waitForAsync()

        // Extract nonce from "SMSC AUTH_CHALLENGE {nonce}"
        val sent = capturingSmsSender.sentMessages.first()
        val nonce = sent.message.removePrefix("SMSC AUTH_CHALLENGE ")

        // Verify: nonce is valid Base64 (can decode without exception)
        val decoded = try {
            android.util.Base64.decode(nonce, android.util.Base64.NO_WRAP)
            true
        } catch (e: Exception) {
            false
        }
        assertTrue("Nonce should be valid Base64", decoded)
    }

    @Test
    fun `givenNoDevice_whenAuthRequestReceived_thenNoChallengeGenerated`() = runTest {
        // No setup - unknown device

        // Action
        commandHandler.handleAuthRequest("+9999999999")
        waitForAsync()

        // Verify: No AUTH_CHALLENGE message sent
        capturingSmsSender.assertNothingSent()
    }

    // ==================== Session Start Tests ====================

    @Test
    fun `givenApprovedSourceDevice_whenValidHmacResponse_thenSessionCreated`() = runTest {
        // Setup: Approved TARGET device (we forward to them, they are SOURCE role to us)
        val password = "password123"
        setupApprovedTargetDevice("+1234567890", password)

        // Step 1: AUTH_REQUEST - generate challenge
        commandHandler.handleAuthRequest("+1234567890")
        waitForAsync()

        // Extract nonce from AUTH_CHALLENGE message
        val challengeMsg = capturingSmsSender.sentMessages.first()
        val nonce = challengeMsg.message.removePrefix("SMSC AUTH_CHALLENGE ")

        // Compute correct HMAC response
        val authKey = SecurityManager.deriveAuthKey(password)
        val hmacResponse = SecurityManager.computeHmac(authKey, nonce)

        capturingSmsSender.clear()

        // Step 2: START_FORWARD with valid HMAC response
        commandHandler.handleStartForward("+1234567890", hmacResponse, 30)
        waitForAsync()

        // Verify: Session created
        val activeSessions = sessionRepository.getActiveSessionsList()
        assertEquals("Should have 1 active session", 1, activeSessions.size)
        assertEquals("+1234567890", activeSessions[0].devicePhoneNumber)
        assertEquals(30, activeSessions[0].durationMinutes)
        assertTrue("Session should be active", activeSessions[0].isActive)
    }

    @Test
    fun `givenApprovedSourceDevice_whenInvalidHmacResponse_thenNoSessionCreated`() = runTest {
        // Setup
        val password = "password123"
        setupApprovedTargetDevice("+1234567890", password)

        // Step 1: AUTH_REQUEST
        commandHandler.handleAuthRequest("+1234567890")
        waitForAsync()
        capturingSmsSender.clear()

        // Step 2: START_FORWARD with INVALID response
        commandHandler.handleStartForward("+1234567890", "invalid-response", 30)
        waitForAsync()

        // Verify: No session created
        val activeSessions = sessionRepository.getActiveSessionsList()
        assertTrue("Should have no active sessions", activeSessions.isEmpty())

        // Verify: Failed attempt tracked
        val device = deviceRepository.getByPhoneNumberAndRole("+1234567890", DeviceRole.TARGET)
        assertNotNull(device)
        assertEquals("Failed attempts should be recorded", 1, device!!.failedAttempts)
    }

    @Test
    fun `givenNoChallenge_whenStartForwardReceived_thenNoSessionCreated`() = runTest {
        // Setup: Approved device but NO AUTH_REQUEST step (skip challenge generation)
        setupApprovedTargetDevice("+1234567890", "password123")

        // Action: START_FORWARD without prior AUTH_REQUEST
        commandHandler.handleStartForward("+1234567890", "any-response", 30)
        waitForAsync()

        // Verify: No session created (challenge required)
        val activeSessions = sessionRepository.getActiveSessionsList()
        assertTrue("Should have no active sessions", activeSessions.isEmpty())
    }

    // ==================== Message Forwarding Tests ====================

    @Test
    fun `givenActiveSession_whenIncomingSmsReceived_thenMessageForwarded`() = runTest {
        // Setup: Active forwarding session
        setupActiveForwardingSession("+1234567890", 30)

        // Action: Receive incoming SMS from third party
        commandHandler.handleIncomingSms("+5555555555", "Your OTP is 123456")
        waitForAsync()

        // Verify: Message forwarded to session device
        assertEquals("Should have 1 forwarded message", 1, capturingSmsSender.sentMessages.size)
        val sent = capturingSmsSender.sentMessages[0]
        assertEquals("+1234567890", sent.phoneNumber)
        assertTrue(
            "Should be forward command (FWD or FWDE)",
            sent.message.startsWith("SMSC FWD") || sent.message.startsWith("SMSC FWDE"),
        )
    }

    @Test
    fun `givenActiveSession_whenMultipleMessagesReceived_thenMessageCountIncremented`() = runTest {
        // Setup: Active forwarding session
        val (_, session) = setupActiveForwardingSession("+1234567890", 30)

        // Action: Receive multiple SMS messages
        commandHandler.handleIncomingSms("+5555555555", "Message 1")
        waitForAsync()
        commandHandler.handleIncomingSms("+6666666666", "Message 2")
        waitForAsync()

        // Verify: Both messages forwarded
        assertEquals("Should have 2 forwarded messages", 2, capturingSmsSender.sentMessages.size)

        // Verify: Session message count incremented
        val updatedSession = sessionRepository.getSessionById(session.id)
        assertNotNull(updatedSession)
        assertEquals("Session should have 2 messages forwarded", 2, updatedSession!!.messagesForwarded)
    }

    @Test
    fun `givenNoActiveSession_whenIncomingSmsReceived_thenNoForwarding`() = runTest {
        // Setup: Approved device but no active session
        setupApprovedTargetDevice("+1234567890", "password123")

        // Action: Receive incoming SMS
        commandHandler.handleIncomingSms("+5555555555", "Test message")
        waitForAsync()

        // Verify: No message forwarded
        capturingSmsSender.assertNothingSent()
    }

    @Test
    fun `givenMultipleActiveSessions_whenIncomingSmsReceived_thenForwardedToAll`() = runTest {
        // Setup: Two active forwarding sessions
        setupActiveForwardingSession("+1111111111", 30)
        setupActiveForwardingSession("+2222222222", 30)

        // Action: Receive incoming SMS
        commandHandler.handleIncomingSms("+5555555555", "Broadcast message")
        waitForAsync()

        // Verify: Message forwarded to both devices
        assertEquals("Should forward to both sessions", 2, capturingSmsSender.sentMessages.size)
        val recipients = capturingSmsSender.sentMessages.map { it.phoneNumber }
        assertTrue("Should include first device", recipients.contains("+1111111111"))
        assertTrue("Should include second device", recipients.contains("+2222222222"))
    }

    // ==================== Session Stop Tests ====================

    @Test
    fun `givenActiveSession_whenStopForwardReceived_thenSessionEnded`() = runTest {
        // Setup: Active forwarding session
        val (_, session) = setupActiveForwardingSession("+1234567890", 30)

        // Verify: Session is active before stop
        val beforeStop = sessionRepository.getSessionById(session.id)
        assertTrue("Session should be active before stop", beforeStop!!.isActive)

        // Action: Receive STOP_FORWARD
        commandHandler.handleStopForward("+1234567890")
        waitForAsync()

        // Verify: Session ended
        val afterStop = sessionRepository.getSessionById(session.id)
        assertNotNull(afterStop)
        assertFalse("Session should be inactive after stop", afterStop!!.isActive)
        assertEquals("Session should be stopped by REMOTE", "REMOTE", afterStop.stoppedBy)
    }

    @Test
    fun `givenNoActiveSession_whenStopForwardReceived_thenNoError`() = runTest {
        // Setup: Approved device but no active session
        setupApprovedTargetDevice("+1234567890", "password123")

        // Action: Receive STOP_FORWARD (should not throw)
        commandHandler.handleStopForward("+1234567890")
        waitForAsync()

        // Verify: No error, graceful handling (nothing to verify other than no exception)
        assertTrue("Should handle gracefully", true)
    }

    // ==================== Full Forwarding Flow Tests ====================

    @Test
    fun `fullForwardingFlow_authThenForwardThenStop`() = runTest {
        // Setup: Approved TARGET device
        val password = "password123"
        setupApprovedTargetDevice("+1234567890", password)

        // Step 1: AUTH_REQUEST
        commandHandler.handleAuthRequest("+1234567890")
        waitForAsync()

        // Extract nonce and compute HMAC
        val challengeMsg = capturingSmsSender.sentMessages.first()
        val nonce = challengeMsg.message.removePrefix("SMSC AUTH_CHALLENGE ")
        val authKey = SecurityManager.deriveAuthKey(password)
        val hmacResponse = SecurityManager.computeHmac(authKey, nonce)

        capturingSmsSender.clear()

        // Step 2: START_FORWARD with valid response
        commandHandler.handleStartForward("+1234567890", hmacResponse, 30)
        waitForAsync()

        // Checkpoint: Session active
        val activeSessions = sessionRepository.getActiveSessionsList()
        assertEquals("Should have 1 active session", 1, activeSessions.size)
        val session = activeSessions[0]
        assertTrue("Session should be active", session.isActive)

        // Step 3: Receive incoming SMS
        commandHandler.handleIncomingSms("+5555555555", "Test OTP: 789012")
        waitForAsync()

        // Checkpoint: Message forwarded, count incremented
        assertEquals("Should have 1 forwarded message", 1, capturingSmsSender.sentMessages.size)
        val updatedSession = sessionRepository.getSessionById(session.id)
        assertEquals("Message count should be 1", 1, updatedSession!!.messagesForwarded)

        // Step 4: STOP_FORWARD
        commandHandler.handleStopForward("+1234567890")
        waitForAsync()

        // Verify: Session ended, stoppedBy set
        val finalSession = sessionRepository.getSessionById(session.id)
        assertNotNull(finalSession)
        assertFalse("Session should be inactive", finalSession!!.isActive)
        assertEquals("Should be stopped by REMOTE", "REMOTE", finalSession.stoppedBy)
    }

    // ==================== Helper for TARGET device setup ====================

    /**
     * Creates an approved TARGET device.
     *
     * In the forwarding flow:
     * - This device is the TARGET (forwards messages)
     * - The remote device is the SOURCE (receives forwarded messages)
     * - handleAuthRequest looks for TARGET role for the sender
     */
    private suspend fun setupApprovedTargetDevice(
        phoneNumber: String,
        password: String,
    ) {
        val passwordHash = SecurityManager.hashPassword(password)
        val authKey = SecurityManager.deriveAuthKey(password)

        val device = dev.notyouraverage.smscourier.TestFixtures.createTestDevice(
            phoneNumber = phoneNumber,
            role = DeviceRole.TARGET,
            status = dev.notyouraverage.smscourier.data.entities.PairingStatus.APPROVED,
            passwordHash = passwordHash.hash,
            passwordSalt = passwordHash.salt,
            authKey = authKey,
        )
        deviceRepository.insert(device)
    }
}
