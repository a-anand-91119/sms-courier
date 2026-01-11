package dev.notyouraverage.otpcourier.viewmodels

import app.cash.turbine.test
import dev.notyouraverage.otpcourier.MainCoroutineRule
import dev.notyouraverage.otpcourier.TestFixtures.createTestDevice
import dev.notyouraverage.otpcourier.data.entities.PairingStatus
import dev.notyouraverage.otpcourier.repository.PairedDeviceRepository
import dev.notyouraverage.otpcourier.services.SmsSender
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AddDeviceViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @MockK
    private lateinit var deviceRepository: PairedDeviceRepository

    @MockK
    private lateinit var smsSender: SmsSender

    private lateinit var viewModel: AddDeviceViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        viewModel = AddDeviceViewModel(deviceRepository, smsSender)
    }

    @Test
    fun `initial state has empty phone number and no error`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.phoneNumber)
            assertNull(state.error)
            assertFalse(state.isLoading)
            assertFalse(state.success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updatePhoneNumber updates state and clears error`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.updatePhoneNumber("+1234567890")

            val state = awaitItem()
            assertEquals("+1234567890", state.phoneNumber)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sendPairingRequest shows error for blank phone number`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.sendPairingRequest()

            val state = awaitItem()
            assertEquals("Please enter a phone number", state.error)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sendPairingRequest shows error for whitespace-only phone number`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.updatePhoneNumber("   ")
            awaitItem() // Phone update

            viewModel.sendPairingRequest()

            val state = awaitItem()
            assertEquals("Please enter a phone number", state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sendPairingRequest shows error for existing approved device`() = runTest {
        val existingDevice = createTestDevice(
            phoneNumber = "+1234567890",
            status = PairingStatus.APPROVED,
        )
        coEvery { deviceRepository.getByPhoneNumber("+1234567890") } returns existingDevice

        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.updatePhoneNumber("+1234567890")
            awaitItem() // Phone update

            viewModel.sendPairingRequest()

            // Skip loading state
            var state = awaitItem()
            if (state.isLoading) {
                state = awaitItem()
            }

            assertTrue(state.error?.contains("already exists") == true)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sendPairingRequest shows error for existing pending device`() = runTest {
        val existingDevice = createTestDevice(
            phoneNumber = "+1234567890",
            status = PairingStatus.PENDING_SENT,
        )
        coEvery { deviceRepository.getByPhoneNumber("+1234567890") } returns existingDevice

        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.updatePhoneNumber("+1234567890")
            awaitItem() // Phone update

            viewModel.sendPairingRequest()

            // Skip loading state
            var state = awaitItem()
            if (state.isLoading) {
                state = awaitItem()
            }

            assertTrue(state.error?.contains("already exists") == true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sendPairingRequest succeeds and inserts device`() = runTest {
        coEvery { deviceRepository.getByPhoneNumber("+1234567890") } returns null
        coEvery { deviceRepository.insert(any()) } just runs

        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.updatePhoneNumber("+1234567890")
            awaitItem() // Phone update

            viewModel.sendPairingRequest()

            // Skip to success state
            var state = awaitItem()
            while (!state.success && state.error == null) {
                state = awaitItem()
            }

            assertTrue(state.success)
            assertEquals("", state.phoneNumber) // Phone number cleared
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { deviceRepository.insert(match { it.phoneNumber == "+1234567890" }) }
        verify { smsSender.sendPairRequest("+1234567890") }
    }

    @Test
    fun `sendPairingRequest completes successfully`() = runTest {
        coEvery { deviceRepository.getByPhoneNumber(any()) } returns null
        coEvery { deviceRepository.insert(any()) } just runs

        viewModel.updatePhoneNumber("+1234567890")
        viewModel.sendPairingRequest()

        // Wait for completion
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.success)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearSuccess resets success flag`() = runTest {
        coEvery { deviceRepository.getByPhoneNumber(any()) } returns null
        coEvery { deviceRepository.insert(any()) } just runs

        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.updatePhoneNumber("+1234567890")
            awaitItem() // Phone update

            viewModel.sendPairingRequest()

            // Skip to success state
            var state = awaitItem()
            while (!state.success && state.error == null) {
                state = awaitItem()
            }

            assertTrue(state.success)

            viewModel.clearSuccess()

            val clearedState = awaitItem()
            assertFalse(clearedState.success)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
