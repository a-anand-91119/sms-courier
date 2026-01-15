package dev.notyouraverage.smscourier.handlers

import dev.notyouraverage.smscourier.TestFixtures.createTestDevice
import dev.notyouraverage.smscourier.TestFixtures.createTestSession
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import dev.notyouraverage.smscourier.notifications.PairingNotificationManager
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import dev.notyouraverage.smscourier.security.SecurityManager
import dev.notyouraverage.smscourier.services.SmsSender
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ExperimentalCoroutinesApi
class SmsCommandHandlerTest {

    @MockK
    private lateinit var deviceRepository: PairedDeviceRepository

    @MockK
    private lateinit var sessionRepository: ForwardingSessionRepository

    @MockK
    private lateinit var smsSender: SmsSender

    @MockK
    private lateinit var notificationManager: PairingNotificationManager

    @MockK
    private lateinit var securityManager: SecurityManager

    private lateinit var handler: SmsCommandHandler
    private val forwardingStateChanges = mutableListOf<SmsCommandHandler.ForwardingState>()

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        forwardingStateChanges.clear()
        handler = SmsCommandHandler(
            deviceRepository = deviceRepository,
            sessionRepository = sessionRepository,
            smsSender = smsSender,
            notificationManager = notificationManager,
            securityManager = securityManager,
            onForwardingStateChanged = { forwardingStateChanges.add(it) },
        )
    }

    // ==================== handlePairRequest ====================

    @Test
    fun `handlePairRequest creates new device for unknown sender`() = runTest {
        val phone = "+1234567890"
        coEvery { deviceRepository.getByPhoneNumberAndRole(phone, DeviceRole.TARGET) } returns null
        coEvery { deviceRepository.getByPhoneNumberAndRole(phone, DeviceRole.SOURCE) } returns null
        coEvery { deviceRepository.insert(any()) } just runs

        handler.handlePairRequest(phone)

        coVerify {
            deviceRepository.insert(
                match {
                    it.phoneNumber == phone &&
                        it.status == PairingStatus.PENDING_RECEIVED &&
                        it.role == DeviceRole.TARGET
                },
            )
        }
        verify { notificationManager.showPairingRequestNotification(phone) }
    }

    @Test
    fun `handlePairRequest ignores already approved device`() = runTest {
        val device = createTestDevice(status = PairingStatus.APPROVED, role = DeviceRole.TARGET)
        coEvery { deviceRepository.getByPhoneNumberAndRole(any(), DeviceRole.TARGET) } returns device

        handler.handlePairRequest(device.phoneNumber)

        coVerify(exactly = 0) { deviceRepository.insert(any()) }
        verify(exactly = 0) { notificationManager.showPairingRequestNotification(any()) }
    }

    @Test
    fun `handlePairRequest allows re-request from rejected device`() = runTest {
        val device = createTestDevice(status = PairingStatus.REJECTED, role = DeviceRole.TARGET)
        coEvery { deviceRepository.getByPhoneNumberAndRole(any(), DeviceRole.TARGET) } returns device
        coEvery { deviceRepository.updatePairingStatus(any(), any(), any()) } just runs

        handler.handlePairRequest(device.phoneNumber)

        coVerify { deviceRepository.updatePairingStatus(device.phoneNumber, DeviceRole.TARGET, PairingStatus.PENDING_RECEIVED) }
        verify { notificationManager.showPairingRequestNotification(device.phoneNumber) }
    }

    @Test
    fun `handlePairRequest refreshes notification for pending device`() = runTest {
        val device = createTestDevice(status = PairingStatus.PENDING_RECEIVED, role = DeviceRole.TARGET)
        coEvery { deviceRepository.getByPhoneNumberAndRole(any(), DeviceRole.TARGET) } returns device

        handler.handlePairRequest(device.phoneNumber)

        verify { notificationManager.showPairingRequestNotification(device.phoneNumber) }
        coVerify(exactly = 0) { deviceRepository.insert(any()) }
    }

    // ==================== handlePairApproved ====================

    @Test
    fun `handlePairApproved updates status to APPROVED`() = runTest {
        val device = createTestDevice(status = PairingStatus.PENDING_SENT, role = DeviceRole.SOURCE)
        coEvery { deviceRepository.getByPhoneNumberAndRole(any(), DeviceRole.SOURCE) } returns device
        coEvery { deviceRepository.updatePairingStatus(any(), any(), any()) } just runs
        coEvery { deviceRepository.updateLastActivity(any(), any()) } just runs

        handler.handlePairApproved(device.phoneNumber)

        coVerify { deviceRepository.updatePairingStatus(device.phoneNumber, DeviceRole.SOURCE, PairingStatus.APPROVED) }
        verify { notificationManager.showPairingResponseNotification(device.phoneNumber, approved = true) }
    }

    @Test
    fun `handlePairApproved ignores unknown device`() = runTest {
        coEvery { deviceRepository.getByPhoneNumberAndRole(any(), DeviceRole.SOURCE) } returns null

        handler.handlePairApproved("+1234567890")

        coVerify(exactly = 0) { deviceRepository.updatePairingStatus(any(), any(), any()) }
    }

    @Test
    fun `handlePairApproved ignores device not in PENDING_SENT status`() = runTest {
        val device = createTestDevice(status = PairingStatus.APPROVED, role = DeviceRole.SOURCE)
        coEvery { deviceRepository.getByPhoneNumberAndRole(any(), DeviceRole.SOURCE) } returns device

        handler.handlePairApproved(device.phoneNumber)

        coVerify(exactly = 0) { deviceRepository.updatePairingStatus(any(), any(), any()) }
    }

    // ==================== handlePairRejected ====================

    @Test
    fun `handlePairRejected updates status to REJECTED`() = runTest {
        val device = createTestDevice(status = PairingStatus.PENDING_SENT, role = DeviceRole.SOURCE)
        coEvery { deviceRepository.getByPhoneNumberAndRole(any(), DeviceRole.SOURCE) } returns device
        coEvery { deviceRepository.updatePairingStatus(any(), any(), any()) } just runs
        coEvery { deviceRepository.updateLastActivity(any(), any()) } just runs

        handler.handlePairRejected(device.phoneNumber)

        coVerify { deviceRepository.updatePairingStatus(device.phoneNumber, DeviceRole.SOURCE, PairingStatus.REJECTED) }
        verify { notificationManager.showPairingResponseNotification(device.phoneNumber, approved = false) }
    }

    // ==================== handleUnpair ====================

    @Test
    fun `handleUnpair ends sessions and deletes device`() = runTest {
        val device = createTestDevice()
        coEvery { deviceRepository.getByPhoneNumber(any()) } returns listOf(device)
        coEvery { sessionRepository.endSessionForDevice(any(), any()) } just runs
        coEvery { deviceRepository.deleteByPhoneNumber(any()) } just runs

        handler.handleUnpair(device.phoneNumber)

        coVerify { sessionRepository.endSessionForDevice(device.phoneNumber, "UNPAIR") }
        coVerify { deviceRepository.deleteByPhoneNumber(device.phoneNumber) }
    }

    @Test
    fun `handleUnpair does nothing for unknown device`() = runTest {
        coEvery { deviceRepository.getByPhoneNumber(any()) } returns emptyList()

        handler.handleUnpair("+1234567890")

        coVerify(exactly = 0) { deviceRepository.deleteByPhoneNumber(any()) }
    }

    // ==================== handleAuthRequest ====================

    @Test
    fun `handleAuthRequest generates challenge for approved device`() = runTest {
        val device = createTestDevice(status = PairingStatus.APPROVED, role = DeviceRole.TARGET)
        coEvery { deviceRepository.getByPhoneNumberAndRole(any(), DeviceRole.TARGET) } returns device
        every { securityManager.isDeviceLocked(device) } returns false
        every { securityManager.generateChallenge(any()) } returns "testNonce123"

        handler.handleAuthRequest(device.phoneNumber)

        verify { securityManager.generateChallenge(device.phoneNumber) }
        verify { smsSender.sendAuthChallenge(device.phoneNumber, "testNonce123") }
    }

    @Test
    fun `handleAuthRequest ignores unapproved device`() = runTest {
        val device = createTestDevice(status = PairingStatus.PENDING_RECEIVED, role = DeviceRole.TARGET)
        coEvery { deviceRepository.getByPhoneNumberAndRole(any(), DeviceRole.TARGET) } returns device

        handler.handleAuthRequest(device.phoneNumber)

        verify(exactly = 0) { securityManager.generateChallenge(any()) }
    }

    @Test
    fun `handleAuthRequest ignores locked device`() = runTest {
        val device = createTestDevice(status = PairingStatus.APPROVED, role = DeviceRole.TARGET)
        coEvery { deviceRepository.getByPhoneNumberAndRole(any(), DeviceRole.TARGET) } returns device
        every { securityManager.isDeviceLocked(device) } returns true

        handler.handleAuthRequest(device.phoneNumber)

        verify(exactly = 0) { securityManager.generateChallenge(any()) }
    }

    @Test
    fun `handleAuthRequest ignores unknown device`() = runTest {
        coEvery { deviceRepository.getByPhoneNumberAndRole(any(), DeviceRole.TARGET) } returns null

        handler.handleAuthRequest("+1234567890")

        verify(exactly = 0) { securityManager.generateChallenge(any()) }
    }

    // ==================== handleStartForward ====================

    @Test
    fun `handleStartForward uses challenge auth when pending challenge exists`() = runTest {
        val device = createTestDevice()
        every { securityManager.hasPendingChallenge(any()) } returns true
        coEvery { securityManager.validateChallengeAndAuthenticate(any(), any()) } returns
            SecurityManager.AuthenticationResult.Success(device)
        coEvery { sessionRepository.getActiveSessionForDevice(any()) } returns null
        coEvery { sessionRepository.startSession(any(), any(), any()) } returns 1L

        handler.handleStartForward(device.phoneNumber, "hmacResponse", 30)

        coVerify { securityManager.validateChallengeAndAuthenticate(device.phoneNumber, "hmacResponse") }
    }

    @Test
    fun `handleStartForward starts new session on success`() = runTest {
        val testAuthKey = "testAuthKey123"
        val device = createTestDevice(maxForwardDurationMinutes = 60, authKey = testAuthKey)
        every { securityManager.hasPendingChallenge(any()) } returns true
        coEvery { securityManager.validateChallengeAndAuthenticate(any(), any()) } returns
            SecurityManager.AuthenticationResult.Success(device)
        coEvery { sessionRepository.getActiveSessionForDevice(any()) } returns null
        coEvery { sessionRepository.startSession(any(), any(), any()) } returns 1L

        handler.handleStartForward(device.phoneNumber, "response", 30)

        coVerify { sessionRepository.startSession(device.phoneNumber, 30, testAuthKey) }
        assertEquals(1, forwardingStateChanges.size)
        assertTrue(forwardingStateChanges[0] is SmsCommandHandler.ForwardingState.Started)
    }

    @Test
    fun `handleStartForward extends existing session`() = runTest {
        val device = createTestDevice(maxForwardDurationMinutes = 60)
        val session = createTestSession(id = 5L)
        every { securityManager.hasPendingChallenge(any()) } returns true
        coEvery { securityManager.validateChallengeAndAuthenticate(any(), any()) } returns
            SecurityManager.AuthenticationResult.Success(device)
        coEvery { sessionRepository.getActiveSessionForDevice(any()) } returns session
        coEvery { sessionRepository.updateSessionDuration(any(), any()) } just runs

        handler.handleStartForward(device.phoneNumber, "response", 45)

        coVerify { sessionRepository.updateSessionDuration(5L, 45) }
        assertTrue(forwardingStateChanges[0] is SmsCommandHandler.ForwardingState.Started)
    }

    @Test
    fun `handleStartForward respects maxForwardDurationMinutes`() = runTest {
        val testAuthKey = "testAuthKey456"
        val device = createTestDevice(maxForwardDurationMinutes = 30, authKey = testAuthKey)
        every { securityManager.hasPendingChallenge(any()) } returns true
        coEvery { securityManager.validateChallengeAndAuthenticate(any(), any()) } returns
            SecurityManager.AuthenticationResult.Success(device)
        coEvery { sessionRepository.getActiveSessionForDevice(any()) } returns null
        coEvery { sessionRepository.startSession(any(), any(), any()) } returns 1L

        // Request 60 minutes but device max is 30
        handler.handleStartForward(device.phoneNumber, "response", 60)

        // Should be capped to 30
        coVerify { sessionRepository.startSession(device.phoneNumber, 30, testAuthKey) }
    }

    @Test
    fun `handleStartForward does nothing for DeviceNotFound`() = runTest {
        every { securityManager.hasPendingChallenge(any()) } returns true
        coEvery { securityManager.validateChallengeAndAuthenticate(any(), any()) } returns
            SecurityManager.AuthenticationResult.DeviceNotFound

        handler.handleStartForward("+1234567890", "response", 30)

        coVerify(exactly = 0) { sessionRepository.startSession(any(), any(), any()) }
        assertTrue(forwardingStateChanges.isEmpty())
    }

    @Test
    fun `handleStartForward does nothing for DeviceLocked`() = runTest {
        every { securityManager.hasPendingChallenge(any()) } returns true
        coEvery { securityManager.validateChallengeAndAuthenticate(any(), any()) } returns
            SecurityManager.AuthenticationResult.DeviceLocked(remainingMs = 60000)

        handler.handleStartForward("+1234567890", "response", 30)

        coVerify(exactly = 0) { sessionRepository.startSession(any(), any(), any()) }
        assertTrue(forwardingStateChanges.isEmpty())
    }

    // ==================== handleStopForward ====================

    @Test
    fun `handleStopForward ends active session`() = runTest {
        val session = createTestSession(id = 5L)
        coEvery { sessionRepository.getActiveSessionForDevice(any()) } returns session
        coEvery { sessionRepository.endSession(any(), any()) } just runs

        handler.handleStopForward("+1234567890")

        coVerify { sessionRepository.endSession(5L, "REMOTE") }
        assertEquals(1, forwardingStateChanges.size)
        assertTrue(forwardingStateChanges[0] is SmsCommandHandler.ForwardingState.Stopped)
    }

    @Test
    fun `handleStopForward does nothing when no active session`() = runTest {
        coEvery { sessionRepository.getActiveSessionForDevice(any()) } returns null

        handler.handleStopForward("+1234567890")

        coVerify(exactly = 0) { sessionRepository.endSession(any(), any()) }
        assertTrue(forwardingStateChanges.isEmpty())
    }

    // ==================== handleForwardedDataEncrypted ====================

    @Test
    fun `handleForwardedDataEncrypted shows notification on decryption failure`() = runTest {
        val device = createTestDevice(activeEncryptionKey = null, role = DeviceRole.SOURCE)
        coEvery { deviceRepository.getByPhoneNumberAndRole(any(), DeviceRole.SOURCE) } returns device

        handler.handleForwardedDataEncrypted(device.phoneNumber, "encryptedContent")

        verify { notificationManager.showForwardedMessageNotification("Unknown", any(), device.phoneNumber) }
    }

    @Test
    fun `handleForwardedDataEncrypted handles unknown device`() = runTest {
        coEvery { deviceRepository.getByPhoneNumberAndRole(any(), DeviceRole.SOURCE) } returns null

        handler.handleForwardedDataEncrypted("+1234567890", "encryptedContent")

        verify(exactly = 0) { notificationManager.showForwardedMessageNotification(any(), any(), any()) }
    }

    // ==================== handleIncomingSms ====================

    @Test
    fun `handleIncomingSms forwards to all active sessions`() = runTest {
        val sessions = listOf(
            createTestSession(devicePhoneNumber = "+1111111111"),
            createTestSession(devicePhoneNumber = "+2222222222"),
        )
        coEvery { sessionRepository.getActiveSessionsList() } returns sessions
        coEvery { sessionRepository.recordForwardedMessage(any()) } just runs

        handler.handleIncomingSms("+5555555555", "Test OTP: 123456")

        verify { smsSender.sendForwardedSms("+1111111111", "+5555555555", "Test OTP: 123456") }
        verify { smsSender.sendForwardedSms("+2222222222", "+5555555555", "Test OTP: 123456") }
    }

    @Test
    fun `handleIncomingSms does nothing when no active sessions`() = runTest {
        coEvery { sessionRepository.getActiveSessionsList() } returns emptyList()

        handler.handleIncomingSms("+5555555555", "Test message")

        verify(exactly = 0) { smsSender.sendForwardedSms(any(), any(), any()) }
    }

    @Test
    fun `handleIncomingSms records forwarded message`() = runTest {
        val session = createTestSession(id = 5L)
        coEvery { sessionRepository.getActiveSessionsList() } returns listOf(session)
        coEvery { sessionRepository.recordForwardedMessage(any()) } just runs

        handler.handleIncomingSms("+5555555555", "Test")

        coVerify { sessionRepository.recordForwardedMessage(5L) }
    }

    // ==================== initiatePairing ====================

    @Test
    fun `initiatePairing creates device and sends SMS`() = runTest {
        val phone = "+1234567890"
        coEvery { deviceRepository.getByPhoneNumberAndRole(phone, DeviceRole.SOURCE) } returns null
        coEvery { deviceRepository.getByPhoneNumberAndRole(phone, DeviceRole.TARGET) } returns null
        coEvery { deviceRepository.insert(any()) } just runs

        handler.initiatePairing(phone)

        coVerify {
            deviceRepository.insert(
                match {
                    it.phoneNumber == phone &&
                        it.role == DeviceRole.SOURCE &&
                        it.status == PairingStatus.PENDING_SENT
                },
            )
        }
        verify { smsSender.sendPairRequest(phone) }
    }

    @Test
    fun `initiatePairing does nothing if device already exists`() = runTest {
        val device = createTestDevice(role = DeviceRole.SOURCE)
        coEvery { deviceRepository.getByPhoneNumberAndRole(any(), DeviceRole.SOURCE) } returns device

        handler.initiatePairing(device.phoneNumber)

        coVerify(exactly = 0) { deviceRepository.insert(any()) }
        verify(exactly = 0) { smsSender.sendPairRequest(any()) }
    }

    // ==================== approvePairing ====================

    @Test
    fun `approvePairing hashes password and sends SMS`() = runTest {
        val phone = "+1234567890"
        val password = "testPassword"
        coEvery { deviceRepository.updatePassword(any(), any(), any(), any()) } just runs
        coEvery { deviceRepository.updateAuthKey(any(), any(), any()) } just runs
        coEvery { deviceRepository.updatePairingStatus(any(), any(), any()) } just runs

        handler.approvePairing(phone, password)

        coVerify { deviceRepository.updatePassword(phone, DeviceRole.TARGET, any(), any()) }
        coVerify { deviceRepository.updateAuthKey(phone, DeviceRole.TARGET, any()) }
        coVerify { deviceRepository.updatePairingStatus(phone, DeviceRole.TARGET, PairingStatus.APPROVED) }
        verify { smsSender.sendPairApproved(phone) }
    }

    // ==================== rejectPairing ====================

    @Test
    fun `rejectPairing updates status and sends SMS`() = runTest {
        val phone = "+1234567890"
        coEvery { deviceRepository.updatePairingStatus(any(), any(), any()) } just runs

        handler.rejectPairing(phone)

        coVerify { deviceRepository.updatePairingStatus(phone, DeviceRole.TARGET, PairingStatus.REJECTED) }
        verify { smsSender.sendPairRejected(phone) }
    }
}
