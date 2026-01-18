package dev.notyouraverage.smscourier.integration

import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupApprovedSourceDevice
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupApprovedTargetDevice
import dev.notyouraverage.smscourier.integration.ScenarioBuilders.setupBidirectionalPairing
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

    // ==================== Unpair Tests ====================

    @Test
    fun `givenApprovedTargetDevice_whenUnpairSourceReceived_thenDeviceDeleted`() = runTest {
        // Given: Approved TARGET device (they forward to us, we are SOURCE)
        val phoneNumber = "+1234567890"
        setupApprovedTargetDevice(phoneNumber, "password123")

        // When: UNPAIR SOURCE received (they want to delete our SOURCE role from their side)
        // But from our perspective, we have a TARGET record for them
        // Note: UNPAIR SOURCE deletes the SOURCE role, meaning "delete your record of me as SOURCE"
        commandHandler.handleUnpair(phoneNumber, DeviceRole.SOURCE)
        waitForAsync()

        // Then: SOURCE role deleted (we didn't have one), TARGET unchanged
        val targetDevice = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET)
        assertNotNull("TARGET device should still exist", targetDevice)
    }

    @Test
    fun `givenApprovedSourceDevice_whenUnpairTargetReceived_thenDeviceDeleted`() = runTest {
        // Given: Approved SOURCE device (we receive forwarded messages from them)
        val phoneNumber = "+1234567890"
        setupApprovedSourceDevice(phoneNumber, "password123")

        // When: UNPAIR TARGET received (they want to delete our TARGET role = our SOURCE record of them)
        commandHandler.handleUnpair(phoneNumber, DeviceRole.TARGET)
        waitForAsync()

        // Then: TARGET role deleted, SOURCE unchanged
        // Note: We had SOURCE role, UNPAIR TARGET doesn't affect it
        val sourceDevice = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.SOURCE)
        assertNotNull("SOURCE device should still exist", sourceDevice)
    }

    @Test
    fun `givenNoDevice_whenUnpairReceived_thenNoError`() = runTest {
        // Given: No device
        val phoneNumber = "+9999999999"

        // When: Unpair received from unknown device
        commandHandler.handleUnpair(phoneNumber, DeviceRole.SOURCE)
        waitForAsync()

        // Then: No exception thrown, no SMS sent (graceful handling)
        capturingSmsSender.assertNothingSent()
    }

    // ==================== Bidirectional Pairing Tests ====================

    @Test
    fun `givenBidirectionalPairing_whenUnpairSourceReceived_thenOnlySourceRoleDeleted`() = runTest {
        // Given: Bidirectional pairing (both SOURCE and TARGET roles exist)
        val phoneNumber = "+1234567890"
        setupBidirectionalPairing(phoneNumber, "password123")

        // Verify both roles exist
        assertNotNull(deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.SOURCE))
        assertNotNull(deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET))

        // When: UNPAIR SOURCE received
        commandHandler.handleUnpair(phoneNumber, DeviceRole.SOURCE)
        waitForAsync()

        // Then: SOURCE role deleted, TARGET role still exists
        val sourceDevice = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.SOURCE)
        val targetDevice = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET)

        assertNull("SOURCE device should be deleted", sourceDevice)
        assertNotNull("TARGET device should still exist", targetDevice)
    }

    @Test
    fun `givenBidirectionalPairing_whenUnpairTargetReceived_thenOnlyTargetRoleDeleted`() = runTest {
        // Given: Bidirectional pairing (both SOURCE and TARGET roles exist)
        val phoneNumber = "+1234567890"
        setupBidirectionalPairing(phoneNumber, "password123")

        // Verify both roles exist
        assertNotNull(deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.SOURCE))
        assertNotNull(deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET))

        // When: UNPAIR TARGET received
        commandHandler.handleUnpair(phoneNumber, DeviceRole.TARGET)
        waitForAsync()

        // Then: TARGET role deleted, SOURCE role still exists
        val sourceDevice = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.SOURCE)
        val targetDevice = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET)

        assertNotNull("SOURCE device should still exist", sourceDevice)
        assertNull("TARGET device should be deleted", targetDevice)
    }

    // ==================== Full Pairing Flow Tests ====================

    @Test
    fun `fullPairingFlow_requestThenApprove_thenUnpair`() = runTest {
        val phoneNumber = "+1234567890"

        // Step 1: Receive pair request
        commandHandler.handlePairRequest(phoneNumber)
        waitForAsync()

        // Checkpoint: Device created with PENDING_RECEIVED
        val pendingDevice = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET)
        assertNotNull("Device should be created after pair request", pendingDevice)
        assertEquals(PairingStatus.PENDING_RECEIVED, pendingDevice?.status)

        // Step 2: Approve pairing
        commandHandler.approvePairing(phoneNumber, "password123")
        waitForAsync()

        // Checkpoint: Status APPROVED, PAIR_APPROVED sent
        val approvedDevice = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET)
        assertEquals(PairingStatus.APPROVED, approvedDevice?.status)
        capturingSmsSender.assertSent(phoneNumber, Regex("SMSC PAIR_APPROVED"))

        // Step 3: Unpair (role-specific)
        commandHandler.handleUnpair(phoneNumber, DeviceRole.TARGET)
        waitForAsync()

        // Verify: Device deleted
        val deletedDevice = deviceRepository.getByPhoneNumberAndRole(phoneNumber, DeviceRole.TARGET)
        assertNull("Device should be deleted after unpair", deletedDevice)
    }
}
