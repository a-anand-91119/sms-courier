package dev.notyouraverage.smscourier.integration

import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupApprovedTargetDevice
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupPendingPairingRequest
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupPendingSent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for the complete pairing flow.
 *
 * Tests cover:
 * - Pairing requests from unknown and known devices
 * - Pairing approval with password hashing
 * - Pairing rejection
 * - Unpair commands (role-specific and bidirectional)
 * - Full pairing flow from request to unpair
 *
 * Uses ScenarioBuilders for state setup and CapturingSmsSender for SMS verification.
 */
@IntegrationTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ExperimentalCoroutinesApi
class PairingFlowIntegrationTest : IntegrationTestBase() {

    // ==================== Pairing Request Tests ====================

    @Test
    fun `givenUnknownDevice_whenPairRequestReceived_thenTargetCreatedWithPendingReceived`() = runTest {
        // Given: No existing device
        val phoneNumber = "+1234567890"

        // When: Pair request received
        commandHandler.handlePairRequest(phoneNumber)
        waitForAsync()

        // Then: TARGET device created with PENDING_RECEIVED status
        val device = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET)
        assertNotNull("Device should be created", device)
        assertEquals(PairingStatus.PENDING_RECEIVED, device?.status)
        assertEquals(DeviceRole.TARGET, device?.role)
    }

    @Test
    fun `givenAlreadyPairedDevice_whenPairRequestReceived_thenNoDuplicateCreated`() = runTest {
        // Given: Already approved TARGET device
        val phoneNumber = "+1234567890"
        setupApprovedTargetDevice(phoneNumber, "password123")

        // When: Duplicate pair request received
        commandHandler.handlePairRequest(phoneNumber)
        waitForAsync()

        // Then: Still one device, status unchanged (APPROVED)
        val devices = deviceRepository.getByPhoneNumber(phoneNumber)
        assertEquals("Should have exactly one device", 1, devices.size)
        assertEquals(PairingStatus.APPROVED, devices[0].status)
    }

    @Test
    fun `givenPendingSentDevice_whenPairRequestReceived_thenBothRolesExist`() = runTest {
        // Given: We sent a pair request to them (SOURCE role, PENDING_SENT)
        val phoneNumber = "+1234567890"
        setupPendingSent(phoneNumber)

        // When: They also send pair request to us
        commandHandler.handlePairRequest(phoneNumber)
        waitForAsync()

        // Then: Both SOURCE (PENDING_SENT) and TARGET (PENDING_RECEIVED) exist
        val sourceDevice = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.SOURCE)
        val targetDevice = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET)

        assertNotNull("SOURCE device should exist", sourceDevice)
        assertNotNull("TARGET device should exist", targetDevice)
        assertEquals(PairingStatus.PENDING_SENT, sourceDevice?.status)
        assertEquals(PairingStatus.PENDING_RECEIVED, targetDevice?.status)
    }

    // ==================== Pairing Approval Tests ====================

    @Test
    fun `givenPendingReceivedDevice_whenApproved_thenStatusApprovedAndSmsSent`() = runTest {
        // Given: Pending pairing request
        val phoneNumber = "+1234567890"
        setupPendingPairingRequest(phoneNumber)

        // When: Approve pairing
        commandHandler.approvePairing(phoneNumber, "password123")
        waitForAsync()

        // Then: Status is APPROVED
        val device = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET)
        assertEquals(PairingStatus.APPROVED, device?.status)
        assertNotNull("Password hash should be set", device?.passwordHash)
        assertNotNull("Auth key should be set", device?.authKey)

        // Then: PAIR_APPROVED SMS sent
        capturingSmsSender.assertSent(phoneNumber, Regex("SMSC PAIR_APPROVED"))
    }

    @Test
    fun `givenPendingReceivedDevice_whenApproved_thenPasswordHashedWithBcrypt`() = runTest {
        // Given: Pending pairing request
        val phoneNumber = "+1234567890"
        setupPendingPairingRequest(phoneNumber)

        // When: Approve pairing with password
        val password = "password123"
        commandHandler.approvePairing(phoneNumber, password)
        waitForAsync()

        // Then: Password is hashed with bcrypt format
        val device = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET)
        assertNotNull("Password hash should be set", device?.passwordHash)
        assertTrue(
            "Password hash should be bcrypt format (starts with \$2)",
            device?.passwordHash?.startsWith("\$2") == true,
        )

        // Auth key should be set and not be the raw password
        assertNotNull("Auth key should be set", device?.authKey)
        assertTrue(
            "Auth key should not be raw password",
            device?.authKey != password,
        )
    }

    // ==================== Pairing Rejection Tests ====================

    @Test
    fun `givenPendingReceivedDevice_whenRejected_thenStatusRejectedAndSmsSent`() = runTest {
        // Given: Pending pairing request
        val phoneNumber = "+1234567890"
        setupPendingPairingRequest(phoneNumber)

        // When: Reject pairing
        commandHandler.rejectPairing(phoneNumber)
        waitForAsync()

        // Then: Status is REJECTED
        val device = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET)
        assertEquals(PairingStatus.REJECTED, device?.status)

        // Then: PAIR_REJECTED SMS sent
        capturingSmsSender.assertSent(phoneNumber, Regex("SMSC PAIR_REJECTED"))
    }
}
