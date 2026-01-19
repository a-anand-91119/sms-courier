package dev.notyouraverage.smscourier.security

import dev.notyouraverage.smscourier.TestFixtures.createTestDevice
import dev.notyouraverage.smscourier.data.settings.SettingsDefaults
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import dev.notyouraverage.smscourier.repository.SettingsRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
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
class SecurityManagerTest {

    @MockK
    private lateinit var deviceRepository: PairedDeviceRepository

    @MockK
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var securityManager: SecurityManager

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        // Mock settings Flows to return defaults
        every { settingsRepository.maxFailedAttempts } returns flowOf(SettingsDefaults.MAX_FAILED_ATTEMPTS)
        every { settingsRepository.lockoutDurationMinutes } returns flowOf(SettingsDefaults.LOCKOUT_DURATION)
        every { settingsRepository.challengeExpiryMinutes } returns flowOf(SettingsDefaults.CHALLENGE_EXPIRY)

        securityManager = SecurityManager(deviceRepository, settingsRepository)
    }

    // ==================== verifyPassword ====================

    @Test
    fun `verifyPassword returns true for correct password`() {
        val password = "testPassword"
        val hash = SecurityManager.hashPassword(password)
        val device = createTestDevice(passwordHash = hash.hash)

        assertTrue(securityManager.verifyPassword(password, device))
    }

    @Test
    fun `verifyPassword returns false for wrong password`() {
        val hash = SecurityManager.hashPassword("correctPassword")
        val device = createTestDevice(passwordHash = hash.hash)

        assertFalse(securityManager.verifyPassword("wrongPassword", device))
    }

    @Test
    fun `verifyPassword returns false when passwordHash is null`() {
        val device = createTestDevice(passwordHash = null)

        assertFalse(securityManager.verifyPassword("anyPassword", device))
    }

    // ==================== isDeviceLocked ====================

    @Test
    fun `isDeviceLocked returns false when lockedUntil is null`() {
        val device = createTestDevice(lockedUntil = null)

        assertFalse(securityManager.isDeviceLocked(device))
    }

    @Test
    fun `isDeviceLocked returns true when lockedUntil is in future`() {
        val device = createTestDevice(lockedUntil = System.currentTimeMillis() + 60000)

        assertTrue(securityManager.isDeviceLocked(device))
    }

    @Test
    fun `isDeviceLocked returns false when lockedUntil is in past`() {
        val device = createTestDevice(lockedUntil = System.currentTimeMillis() - 60000)

        assertFalse(securityManager.isDeviceLocked(device))
    }

    // ==================== getRemainingLockoutTimeMs ====================

    @Test
    fun `getRemainingLockoutTimeMs returns positive value for locked device`() {
        val lockDuration = 60000L
        val device = createTestDevice(lockedUntil = System.currentTimeMillis() + lockDuration)

        val remaining = securityManager.getRemainingLockoutTimeMs(device)

        assertTrue(remaining > 0)
        assertTrue(remaining <= lockDuration)
    }

    @Test
    fun `getRemainingLockoutTimeMs returns 0 for unlocked device`() {
        val device = createTestDevice(lockedUntil = null)

        assertEquals(0, securityManager.getRemainingLockoutTimeMs(device))
    }

    @Test
    fun `getRemainingLockoutTimeMs returns 0 for expired lockout`() {
        val device = createTestDevice(lockedUntil = System.currentTimeMillis() - 60000)

        assertEquals(0, securityManager.getRemainingLockoutTimeMs(device))
    }

    // ==================== recordFailedAttempt ====================

    @Test
    fun `recordFailedAttempt increments failed attempts`() = runTest {
        val device = createTestDevice(failedAttempts = 2)
        coEvery { deviceRepository.updateFailedAttempts(any(), any(), any(), any()) } just runs

        securityManager.recordFailedAttempt(device)

        coVerify { deviceRepository.updateFailedAttempts(device.phoneNumber, device.role, 3, null) }
    }

    @Test
    fun `recordFailedAttempt locks device after max attempts`() = runTest {
        val device = createTestDevice(failedAttempts = 4) // One more triggers lockout
        coEvery { deviceRepository.updateFailedAttempts(any(), any(), any(), any()) } just runs

        securityManager.recordFailedAttempt(device)

        coVerify {
            deviceRepository.updateFailedAttempts(
                device.phoneNumber,
                device.role,
                5,
                match { it != null && it > System.currentTimeMillis() },
            )
        }
    }

    @Test
    fun `recordFailedAttempt does not lock before max attempts`() = runTest {
        val device = createTestDevice(failedAttempts = 3)
        coEvery { deviceRepository.updateFailedAttempts(any(), any(), any(), any()) } just runs

        securityManager.recordFailedAttempt(device)

        coVerify { deviceRepository.updateFailedAttempts(device.phoneNumber, device.role, 4, null) }
    }

    @Test
    fun `recordFailedAttempt uses configured maxFailedAttempts`() = runTest {
        // Override default with custom value (3 attempts)
        every { settingsRepository.maxFailedAttempts } returns flowOf(3)

        val customSecurityManager = SecurityManager(deviceRepository, settingsRepository)
        // Start observation to apply custom value
        customSecurityManager.startObservingSettings(this)
        // Allow coroutines to process and collect flow values
        advanceUntilIdle()

        val device = createTestDevice(failedAttempts = 2) // One more triggers lockout at 3
        coEvery { deviceRepository.updateFailedAttempts(any(), any(), any(), any()) } just runs

        customSecurityManager.recordFailedAttempt(device)

        coVerify {
            deviceRepository.updateFailedAttempts(
                device.phoneNumber,
                device.role,
                3,
                match { it != null && it > System.currentTimeMillis() },
            )
        }
    }

    // ==================== resetFailedAttempts ====================

    @Test
    fun `resetFailedAttempts clears counter and lockout`() = runTest {
        val device = createTestDevice(failedAttempts = 3, lockedUntil = System.currentTimeMillis() + 60000)
        coEvery { deviceRepository.updateFailedAttempts(any(), any(), any(), any()) } just runs

        securityManager.resetFailedAttempts(device)

        coVerify { deviceRepository.updateFailedAttempts(device.phoneNumber, device.role, 0, null) }
    }

    @Test
    fun `resetFailedAttempts does nothing when already zero`() = runTest {
        val device = createTestDevice(failedAttempts = 0, lockedUntil = null)

        securityManager.resetFailedAttempts(device)

        coVerify(exactly = 0) { deviceRepository.updateFailedAttempts(any(), any(), any(), any()) }
    }

    // ==================== generateChallenge ====================

    @Test
    fun `generateChallenge returns non-empty nonce`() {
        val nonce = securityManager.generateChallenge("+1234567890")

        assertNotNull(nonce)
        assertTrue(nonce.isNotBlank())
    }

    @Test
    fun `generateChallenge stores pending challenge`() {
        val phone = "+1234567890"
        securityManager.generateChallenge(phone)

        assertTrue(securityManager.hasPendingChallenge(phone))
    }

    @Test
    fun `generateChallenge produces different nonces`() {
        val phone1 = "+1111111111"
        val phone2 = "+2222222222"

        val nonce1 = securityManager.generateChallenge(phone1)
        val nonce2 = securityManager.generateChallenge(phone2)

        assertNotEquals(nonce1, nonce2)
    }

    // ==================== hasPendingChallenge ====================

    @Test
    fun `hasPendingChallenge returns false for unknown phone`() {
        assertFalse(securityManager.hasPendingChallenge("+9999999999"))
    }

    @Test
    fun `hasPendingChallenge returns true after generate`() {
        val phone = "+1234567890"
        securityManager.generateChallenge(phone)

        assertTrue(securityManager.hasPendingChallenge(phone))
    }

    // ==================== validateChallengeResponse ====================

    @Test
    fun `validateChallengeResponse returns true for valid response`() {
        val phone = "+1234567890"
        val authKey = SecurityManager.deriveAuthKey("password")

        val nonce = securityManager.generateChallenge(phone)
        val response = SecurityManager.computeHmac(authKey, nonce)

        assertTrue(securityManager.validateChallengeResponse(phone, response, authKey))
    }

    @Test
    fun `validateChallengeResponse returns false for invalid response`() {
        val phone = "+1234567890"
        val authKey = SecurityManager.deriveAuthKey("password")

        securityManager.generateChallenge(phone)

        assertFalse(securityManager.validateChallengeResponse(phone, "wrongResponse", authKey))
    }

    @Test
    fun `validateChallengeResponse returns false when no pending challenge`() {
        val authKey = SecurityManager.deriveAuthKey("password")

        assertFalse(securityManager.validateChallengeResponse("+9999999999", "response", authKey))
    }

    @Test
    fun `validateChallengeResponse removes challenge after successful validation`() {
        val phone = "+1234567890"
        val authKey = SecurityManager.deriveAuthKey("password")

        val nonce = securityManager.generateChallenge(phone)
        val response = SecurityManager.computeHmac(authKey, nonce)

        securityManager.validateChallengeResponse(phone, response, authKey)

        assertFalse(securityManager.hasPendingChallenge(phone))
    }

    @Test
    fun `validateChallengeResponse keeps challenge after failed validation`() {
        val phone = "+1234567890"
        val authKey = SecurityManager.deriveAuthKey("password")

        securityManager.generateChallenge(phone)
        securityManager.validateChallengeResponse(phone, "wrongResponse", authKey)

        // Challenge should still be pending after failed attempt
        assertTrue(securityManager.hasPendingChallenge(phone))
    }

    // ==================== validateChallengeAndAuthenticate ====================

    @Test
    fun `validateChallengeAndAuthenticate returns DeviceNotFound when device not found`() = runTest {
        coEvery { deviceRepository.getApprovedSourceDevice(any()) } returns null

        val result = securityManager.validateChallengeAndAuthenticate("+1234567890", "response")

        assertTrue(result is SecurityManager.AuthenticationResult.DeviceNotFound)
    }

    @Test
    fun `validateChallengeAndAuthenticate returns DeviceLocked when device is locked`() = runTest {
        val device = createTestDevice(lockedUntil = System.currentTimeMillis() + 60000)
        coEvery { deviceRepository.getApprovedSourceDevice(any()) } returns device

        val result = securityManager.validateChallengeAndAuthenticate("+1234567890", "response")

        assertTrue(result is SecurityManager.AuthenticationResult.DeviceLocked)
    }

    @Test
    fun `validateChallengeAndAuthenticate returns AuthKeyMissing when authKey is null`() = runTest {
        val device = createTestDevice(authKey = null)
        coEvery { deviceRepository.getApprovedSourceDevice(any()) } returns device

        val result = securityManager.validateChallengeAndAuthenticate("+1234567890", "response")

        assertTrue(result is SecurityManager.AuthenticationResult.AuthKeyMissing)
    }

    @Test
    fun `validateChallengeAndAuthenticate returns Success for valid response`() = runTest {
        val phone = "+1234567890"
        val password = "testPassword"
        val authKey = SecurityManager.deriveAuthKey(password)
        val device = createTestDevice(phoneNumber = phone, authKey = authKey)

        coEvery { deviceRepository.getApprovedSourceDevice(phone) } returns device
        coEvery { deviceRepository.updateFailedAttempts(any(), any(), any(), any()) } just runs
        coEvery { deviceRepository.updateLastActivity(any(), any()) } just runs

        // Generate challenge and compute response
        val nonce = securityManager.generateChallenge(phone)
        val response = SecurityManager.computeHmac(authKey, nonce)

        val result = securityManager.validateChallengeAndAuthenticate(phone, response)

        assertTrue(result is SecurityManager.AuthenticationResult.Success)
        assertEquals(device, (result as SecurityManager.AuthenticationResult.Success).device)
    }

    @Test
    fun `validateChallengeAndAuthenticate returns InvalidResponse for wrong response`() = runTest {
        val phone = "+1234567890"
        val authKey = SecurityManager.deriveAuthKey("password")
        val device = createTestDevice(phoneNumber = phone, authKey = authKey, failedAttempts = 1)

        coEvery { deviceRepository.getApprovedSourceDevice(phone) } returns device
        coEvery { deviceRepository.updateFailedAttempts(any(), any(), any(), any()) } just runs
        coEvery { deviceRepository.getByPhoneNumberAndRole(phone, device.role) } returns device.copy(failedAttempts = 2)

        securityManager.generateChallenge(phone)
        val result = securityManager.validateChallengeAndAuthenticate(phone, "wrongResponse")

        assertTrue(result is SecurityManager.AuthenticationResult.InvalidResponse)
    }

    @Test
    fun `validateChallengeAndAuthenticate resets failed attempts on success`() = runTest {
        val phone = "+1234567890"
        val password = "testPassword"
        val authKey = SecurityManager.deriveAuthKey(password)
        val device = createTestDevice(phoneNumber = phone, authKey = authKey, failedAttempts = 3)

        coEvery { deviceRepository.getApprovedSourceDevice(phone) } returns device
        coEvery { deviceRepository.updateFailedAttempts(any(), any(), any(), any()) } just runs
        coEvery { deviceRepository.updateLastActivity(any(), any()) } just runs

        val nonce = securityManager.generateChallenge(phone)
        val response = SecurityManager.computeHmac(authKey, nonce)

        securityManager.validateChallengeAndAuthenticate(phone, response)

        coVerify { deviceRepository.updateFailedAttempts(phone, device.role, 0, null) }
    }

    @Test
    fun `validateChallengeAndAuthenticate records failed attempt on failure`() = runTest {
        val phone = "+1234567890"
        val authKey = SecurityManager.deriveAuthKey("password")
        val device = createTestDevice(phoneNumber = phone, authKey = authKey, failedAttempts = 2)

        coEvery { deviceRepository.getApprovedSourceDevice(phone) } returns device
        coEvery { deviceRepository.updateFailedAttempts(any(), any(), any(), any()) } just runs
        coEvery { deviceRepository.getByPhoneNumberAndRole(phone, device.role) } returns device.copy(failedAttempts = 3)

        securityManager.generateChallenge(phone)
        securityManager.validateChallengeAndAuthenticate(phone, "wrongResponse")

        coVerify { deviceRepository.updateFailedAttempts(phone, device.role, 3, null) }
    }
}
