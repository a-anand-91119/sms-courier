package dev.notyouraverage.smscourier.repository

import android.content.Context
import app.cash.turbine.test
import dev.notyouraverage.smscourier.MainCoroutineRule
import dev.notyouraverage.smscourier.data.settings.AppTheme
import dev.notyouraverage.smscourier.data.settings.SettingsDefaults
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsRepositoryTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setup() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        settingsRepository = SettingsRepository(context)
        // Clear any persisted data from previous tests
        settingsRepository.clearAllSettings()
    }

    // ===== Default Value Tests =====

    @Test
    fun `notificationPersistence returns default when not set`() = runTest {
        settingsRepository.notificationPersistence.test {
            assertEquals(SettingsDefaults.NOTIFICATION_PERSISTENCE, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `defaultForwardingDurationMinutes returns default when not set`() = runTest {
        settingsRepository.defaultForwardingDurationMinutes.test {
            assertEquals(SettingsDefaults.DEFAULT_FORWARDING_DURATION, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `theme returns SYSTEM when not set`() = runTest {
        settingsRepository.theme.test {
            assertEquals(AppTheme.SYSTEM, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lockoutDurationMinutes returns default when not set`() = runTest {
        settingsRepository.lockoutDurationMinutes.test {
            assertEquals(SettingsDefaults.LOCKOUT_DURATION, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `maxFailedAttempts returns default when not set`() = runTest {
        settingsRepository.maxFailedAttempts.test {
            assertEquals(SettingsDefaults.MAX_FAILED_ATTEMPTS, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `challengeExpiryMinutes returns default when not set`() = runTest {
        settingsRepository.challengeExpiryMinutes.test {
            assertEquals(SettingsDefaults.CHALLENGE_EXPIRY, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `maxPairingResendAttempts returns default when not set`() = runTest {
        settingsRepository.maxPairingResendAttempts.test {
            assertEquals(SettingsDefaults.MAX_PAIRING_RESEND_ATTEMPTS, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pairingResendCooldownMinutes returns default when not set`() = runTest {
        settingsRepository.pairingResendCooldownMinutes.test {
            assertEquals(SettingsDefaults.PAIRING_RESEND_COOLDOWN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `authRequestTimeoutMinutes returns default when not set`() = runTest {
        settingsRepository.authRequestTimeoutMinutes.test {
            assertEquals(SettingsDefaults.AUTH_REQUEST_TIMEOUT, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== Write-Read Round Trip Tests =====

    @Test
    fun `setNotificationPersistence persists true value`() = runTest {
        settingsRepository.notificationPersistence.test {
            awaitItem() // Default false

            settingsRepository.setNotificationPersistence(true)

            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setDefaultForwardingDuration persists value`() = runTest {
        settingsRepository.defaultForwardingDurationMinutes.test {
            awaitItem() // Default

            settingsRepository.setDefaultForwardingDuration(30)

            assertEquals(30, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setTheme persists DARK theme`() = runTest {
        settingsRepository.theme.test {
            awaitItem() // Default SYSTEM

            settingsRepository.setTheme(AppTheme.DARK)

            assertEquals(AppTheme.DARK, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setTheme persists LIGHT theme`() = runTest {
        settingsRepository.theme.test {
            awaitItem() // Default SYSTEM

            settingsRepository.setTheme(AppTheme.LIGHT)

            assertEquals(AppTheme.LIGHT, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setLockoutDuration persists value`() = runTest {
        settingsRepository.lockoutDurationMinutes.test {
            awaitItem() // Default

            settingsRepository.setLockoutDuration(30)

            assertEquals(30, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setMaxFailedAttempts persists value`() = runTest {
        settingsRepository.maxFailedAttempts.test {
            awaitItem() // Default

            settingsRepository.setMaxFailedAttempts(3)

            assertEquals(3, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setChallengeExpiry persists value`() = runTest {
        settingsRepository.challengeExpiryMinutes.test {
            awaitItem() // Default

            settingsRepository.setChallengeExpiry(5)

            assertEquals(5, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setMaxPairingResendAttempts persists value`() = runTest {
        settingsRepository.maxPairingResendAttempts.test {
            awaitItem() // Default

            settingsRepository.setMaxPairingResendAttempts(3)

            assertEquals(3, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setPairingResendCooldown persists value`() = runTest {
        settingsRepository.pairingResendCooldownMinutes.test {
            awaitItem() // Default

            settingsRepository.setPairingResendCooldown(5)

            assertEquals(5, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setAuthRequestTimeout persists value`() = runTest {
        settingsRepository.authRequestTimeoutMinutes.test {
            awaitItem() // Default

            settingsRepository.setAuthRequestTimeout(15)

            assertEquals(15, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== Validation Rejection Tests =====

    @Test
    fun `setDefaultForwardingDuration rejects zero`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { settingsRepository.setDefaultForwardingDuration(0) }
        }
    }

    @Test
    fun `setDefaultForwardingDuration rejects values over 60`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { settingsRepository.setDefaultForwardingDuration(61) }
        }
    }

    @Test
    fun `setDefaultForwardingDuration accepts boundary value 1`() = runTest {
        settingsRepository.setDefaultForwardingDuration(1)
        settingsRepository.defaultForwardingDurationMinutes.test {
            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setDefaultForwardingDuration accepts boundary value 60`() = runTest {
        settingsRepository.setDefaultForwardingDuration(60)
        settingsRepository.defaultForwardingDurationMinutes.test {
            assertEquals(60, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setLockoutDuration rejects zero`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { settingsRepository.setLockoutDuration(0) }
        }
    }

    @Test
    fun `setLockoutDuration rejects values over 60`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { settingsRepository.setLockoutDuration(61) }
        }
    }

    @Test
    fun `setMaxFailedAttempts rejects zero`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { settingsRepository.setMaxFailedAttempts(0) }
        }
    }

    @Test
    fun `setMaxFailedAttempts rejects values over 10`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { settingsRepository.setMaxFailedAttempts(11) }
        }
    }

    @Test
    fun `setMaxFailedAttempts accepts boundary value 1`() = runTest {
        settingsRepository.setMaxFailedAttempts(1)
        settingsRepository.maxFailedAttempts.test {
            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setMaxFailedAttempts accepts boundary value 10`() = runTest {
        settingsRepository.setMaxFailedAttempts(10)
        settingsRepository.maxFailedAttempts.test {
            assertEquals(10, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setChallengeExpiry rejects zero`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { settingsRepository.setChallengeExpiry(0) }
        }
    }

    @Test
    fun `setChallengeExpiry rejects values over 10`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { settingsRepository.setChallengeExpiry(11) }
        }
    }

    @Test
    fun `setMaxPairingResendAttempts rejects zero`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { settingsRepository.setMaxPairingResendAttempts(0) }
        }
    }

    @Test
    fun `setMaxPairingResendAttempts rejects values over 10`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { settingsRepository.setMaxPairingResendAttempts(11) }
        }
    }

    @Test
    fun `setPairingResendCooldown rejects zero`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { settingsRepository.setPairingResendCooldown(0) }
        }
    }

    @Test
    fun `setPairingResendCooldown rejects values over 10`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { settingsRepository.setPairingResendCooldown(11) }
        }
    }

    @Test
    fun `setAuthRequestTimeout rejects zero`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { settingsRepository.setAuthRequestTimeout(0) }
        }
    }

    @Test
    fun `setAuthRequestTimeout rejects values over 30`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { settingsRepository.setAuthRequestTimeout(31) }
        }
    }

    @Test
    fun `setAuthRequestTimeout accepts boundary value 30`() = runTest {
        settingsRepository.setAuthRequestTimeout(30)
        settingsRepository.authRequestTimeoutMinutes.test {
            assertEquals(30, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===== Edge Case Tests =====

    @Test
    fun `setting same value twice does not cause issues`() = runTest {
        settingsRepository.notificationPersistence.test {
            awaitItem() // Default

            settingsRepository.setNotificationPersistence(true)
            assertTrue(awaitItem())

            // Setting same value again
            settingsRepository.setNotificationPersistence(true)
            // Flow may or may not emit for same value depending on DataStore behavior
            // Just verify no exception thrown
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple settings can be set independently`() = runTest {
        settingsRepository.setNotificationPersistence(true)
        settingsRepository.setDefaultForwardingDuration(45)
        settingsRepository.setTheme(AppTheme.DARK)

        settingsRepository.notificationPersistence.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        settingsRepository.defaultForwardingDurationMinutes.test {
            assertEquals(45, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        settingsRepository.theme.test {
            assertEquals(AppTheme.DARK, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `settings can toggle between values`() = runTest {
        settingsRepository.notificationPersistence.test {
            assertFalse(awaitItem()) // Default

            settingsRepository.setNotificationPersistence(true)
            assertTrue(awaitItem())

            settingsRepository.setNotificationPersistence(false)
            assertFalse(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
