package dev.notyouraverage.smscourier.viewmodels

import android.content.Context
import app.cash.turbine.test
import dev.notyouraverage.smscourier.MainCoroutineRule
import dev.notyouraverage.smscourier.TestFixtures.createTestDevice
import dev.notyouraverage.smscourier.TestFixtures.createTestSession
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.ForwardingSession
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import dev.notyouraverage.smscourier.data.settings.SettingsDefaults
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import dev.notyouraverage.smscourier.repository.SettingsRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import dev.notyouraverage.smscourier.UATTest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ExperimentalCoroutinesApi
class ForwardingControlViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var deviceRepository: PairedDeviceRepository

    @MockK
    private lateinit var sessionRepository: ForwardingSessionRepository

    @MockK
    private lateinit var smsSender: SmsSender

    @MockK
    private lateinit var settingsRepository: SettingsRepository

    private val approvedSourceDevicesFlow = MutableStateFlow(emptyList<PairedDevice>())
    private val activeSessionsFlow = MutableStateFlow(emptyList<ForwardingSession>())

    private lateinit var viewModel: ForwardingControlViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        every { deviceRepository.getDevicesByRoleAndStatus(DeviceRole.SOURCE, PairingStatus.APPROVED) } returns approvedSourceDevicesFlow
        every { sessionRepository.getActiveSessions() } returns activeSessionsFlow
        every { settingsRepository.defaultForwardingDurationMinutes } returns flowOf(SettingsDefaults.DEFAULT_FORWARDING_DURATION)

        viewModel = ForwardingControlViewModel(context, deviceRepository, sessionRepository, smsSender, settingsRepository)
    }

    @Test
    fun `initial state has default values`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.password)
            assertEquals(SettingsDefaults.DEFAULT_FORWARDING_DURATION, state.durationMinutes)
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertFalse(state.success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `duration is loaded from settings on init`() = runTest {
        val customDuration = 15
        every { settingsRepository.defaultForwardingDurationMinutes } returns flowOf(customDuration)

        val customViewModel = ForwardingControlViewModel(
            context,
            deviceRepository,
            sessionRepository,
            smsSender,
            settingsRepository,
        )

        customViewModel.uiState.test {
            // Skip initial default state if present
            var state = awaitItem()
            // If we got the initial state, wait for the loaded state
            if (state.durationMinutes != customDuration) {
                state = awaitItem()
            }
            assertEquals(customDuration, state.durationMinutes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updatePassword updates state and clears error`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.updatePassword("myPassword")

            val state = awaitItem()
            assertEquals("myPassword", state.password)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateDuration updates state`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.updateDuration(60)

            val state = awaitItem()
            assertEquals(60, state.durationMinutes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startForwarding shows error for blank password`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.startForwarding("+1234567890")

            val state = awaitItem()
            assertEquals("Please enter the password", state.error)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startForwarding shows error for whitespace-only password`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.updatePassword("   ")
            awaitItem() // Password update

            viewModel.startForwarding("+1234567890")

            val state = awaitItem()
            assertEquals("Please enter the password", state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startForwarding sends intent to MasterService`() = runTest {
        viewModel.updatePassword("testPassword")
        viewModel.startForwarding("+1234567890")

        // Wait for completion
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.success)
            assertEquals("", state.password) // Password cleared
            cancelAndIgnoreRemainingEvents()
        }

        // Context.startService should have been called
        verify { context.startService(any()) }
    }

    @Test
    fun `startForwarding completes successfully`() = runTest {
        viewModel.updatePassword("testPassword")
        viewModel.startForwarding("+1234567890")

        // Wait for completion
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.success)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stopForwarding ends session and clears encryption key`() = runTest {
        coEvery { sessionRepository.endSessionForDevice(any(), any()) } just runs
        coEvery { deviceRepository.updateEncryptionKey(any(), any(), any()) } just runs

        viewModel.stopForwarding("+1234567890")

        // With UnconfinedTestDispatcher, coroutines complete immediately
        coVerify { sessionRepository.endSessionForDevice("+1234567890", "USER") }
        coVerify { deviceRepository.updateEncryptionKey("+1234567890", DeviceRole.SOURCE, null) }
    }

    @Test
    fun `clearSuccess resets success flag`() = runTest {
        viewModel.updatePassword("testPassword")
        viewModel.startForwarding("+1234567890")

        // Verify success state
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.success)

            viewModel.clearSuccess()

            val clearedState = awaitItem()
            assertFalse(clearedState.success)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `approvedSourceDevices emits repository data`() = runTest {
        val devices = listOf(
            createTestDevice(phoneNumber = "+1111111111", role = DeviceRole.SOURCE, status = PairingStatus.APPROVED),
            createTestDevice(phoneNumber = "+2222222222", role = DeviceRole.SOURCE, status = PairingStatus.APPROVED),
        )

        viewModel.approvedSourceDevices.test {
            awaitItem() // Initial empty list

            approvedSourceDevicesFlow.value = devices

            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `activeSessions emits repository data`() = runTest {
        val sessions = listOf(
            createTestSession(id = 1L, devicePhoneNumber = "+1111111111"),
            createTestSession(id = 2L, devicePhoneNumber = "+2222222222"),
        )

        viewModel.activeSessions.test {
            awaitItem() // Initial empty list

            activeSessionsFlow.value = sessions

            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Category(UATTest::class)
    @Test
    fun `UAT SESS-03 - stopForwarding sends STOP_FORWARD SMS to other device`() = runTest {
        val devicePhoneNumber = "+5559876543"
        coEvery { sessionRepository.endSessionForDevice(any(), any()) } just runs
        coEvery { deviceRepository.updateEncryptionKey(any(), any(), any()) } just runs

        viewModel.stopForwarding(devicePhoneNumber)

        // SESS-03 BUG: When stopping forwarding, the other device should be notified
        // via STOP_FORWARD SMS so they know the session has ended.
        // Currently, the code only ends the local session and clears the encryption key,
        // but does NOT send any SMS notification.
        //
        // After fix in Phase 26:
        // - stopForwarding will call smsSender.sendStopForward(devicePhoneNumber)
        //
        // THIS TEST WILL FAIL until Phase 26 adds the SMS notification.
        verify { smsSender.sendStopForward(devicePhoneNumber) }
    }
}
