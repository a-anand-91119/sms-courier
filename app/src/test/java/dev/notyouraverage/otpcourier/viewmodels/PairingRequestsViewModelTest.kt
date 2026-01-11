package dev.notyouraverage.otpcourier.viewmodels

import app.cash.turbine.test
import dev.notyouraverage.otpcourier.MainCoroutineRule
import dev.notyouraverage.otpcourier.TestFixtures.createTestDevice
import dev.notyouraverage.otpcourier.data.entities.PairedDevice
import dev.notyouraverage.otpcourier.data.entities.PairingStatus
import dev.notyouraverage.otpcourier.repository.PairedDeviceRepository
import dev.notyouraverage.otpcourier.services.SmsSender
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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ExperimentalCoroutinesApi
class PairingRequestsViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @MockK
    private lateinit var deviceRepository: PairedDeviceRepository

    @MockK
    private lateinit var smsSender: SmsSender

    private val pendingRequestsFlow = MutableStateFlow(emptyList<PairedDevice>())

    private lateinit var viewModel: PairingRequestsViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        every { deviceRepository.getPendingRequests() } returns pendingRequestsFlow

        viewModel = PairingRequestsViewModel(deviceRepository, smsSender)
    }

    @Test
    fun `pendingRequests emits repository data`() = runTest {
        val devices = listOf(
            createTestDevice(phoneNumber = "+1111111111", status = PairingStatus.PENDING_RECEIVED),
            createTestDevice(phoneNumber = "+2222222222", status = PairingStatus.PENDING_RECEIVED),
        )

        viewModel.pendingRequests.test {
            awaitItem() // Initial empty list

            pendingRequestsFlow.value = devices

            val result = awaitItem()
            assertEquals(2, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `approvePairing updates password, authKey, status and sends SMS`() = runTest {
        coEvery { deviceRepository.updatePassword(any(), any(), any()) } just runs
        coEvery { deviceRepository.updateAuthKey(any(), any()) } just runs
        coEvery { deviceRepository.updatePairingStatus(any(), any()) } just runs

        viewModel.approvePairing("+1234567890", "testPassword")

        // With UnconfinedTestDispatcher, coroutines complete immediately
        coVerify { deviceRepository.updatePassword("+1234567890", any(), any()) }
        coVerify { deviceRepository.updateAuthKey("+1234567890", any()) }
        coVerify { deviceRepository.updatePairingStatus("+1234567890", PairingStatus.APPROVED) }
        verify { smsSender.sendPairApproved("+1234567890") }
    }

    @Test
    fun `approvePairing completes and clears approvalInProgress`() = runTest {
        coEvery { deviceRepository.updatePassword(any(), any(), any()) } just runs
        coEvery { deviceRepository.updateAuthKey(any(), any()) } just runs
        coEvery { deviceRepository.updatePairingStatus(any(), any()) } just runs

        viewModel.approvePairing("+1234567890", "password")

        // After completion, approvalInProgress should be null
        viewModel.approvalInProgress.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rejectPairing updates status and sends SMS`() = runTest {
        coEvery { deviceRepository.updatePairingStatus(any(), any()) } just runs

        viewModel.rejectPairing("+1234567890")

        // With UnconfinedTestDispatcher, coroutines complete immediately
        coVerify { deviceRepository.updatePairingStatus("+1234567890", PairingStatus.REJECTED) }
        verify { smsSender.sendPairRejected("+1234567890") }
    }

    @Test
    fun `approvalInProgress is initially null`() = runTest {
        viewModel.approvalInProgress.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
