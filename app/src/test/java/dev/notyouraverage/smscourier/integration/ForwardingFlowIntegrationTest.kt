package dev.notyouraverage.smscourier.integration

import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupActiveForwardingSession
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupApprovedSourceDevice
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
