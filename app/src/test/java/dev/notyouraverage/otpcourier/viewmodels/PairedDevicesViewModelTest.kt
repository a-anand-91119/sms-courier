package dev.notyouraverage.otpcourier.viewmodels

import app.cash.turbine.test
import dev.notyouraverage.otpcourier.MainCoroutineRule
import dev.notyouraverage.otpcourier.TestFixtures.createTestDevice
import dev.notyouraverage.otpcourier.data.entities.DeviceRole
import dev.notyouraverage.otpcourier.data.entities.PairedDevice
import dev.notyouraverage.otpcourier.repository.ForwardingSessionRepository
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

@ExperimentalCoroutinesApi
class PairedDevicesViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @MockK
    private lateinit var deviceRepository: PairedDeviceRepository

    @MockK
    private lateinit var sessionRepository: ForwardingSessionRepository

    @MockK
    private lateinit var smsSender: SmsSender

    private val sourceDevicesFlow = MutableStateFlow(emptyList<PairedDevice>())
    private val targetDevicesFlow = MutableStateFlow(emptyList<PairedDevice>())

    private lateinit var viewModel: PairedDevicesViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        every { deviceRepository.getSourceDevices() } returns sourceDevicesFlow
        every { deviceRepository.getTargetDevices() } returns targetDevicesFlow

        viewModel = PairedDevicesViewModel(deviceRepository, sessionRepository, smsSender)
    }

    @Test
    fun `sourceDevices emits repository data`() = runTest {
        val devices = listOf(
            createTestDevice(phoneNumber = "+1111111111", role = DeviceRole.SOURCE),
            createTestDevice(phoneNumber = "+2222222222", role = DeviceRole.SOURCE),
        )

        viewModel.sourceDevices.test {
            awaitItem() // Initial empty list

            sourceDevicesFlow.value = devices

            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("+1111111111", result[0].phoneNumber)
            assertEquals("+2222222222", result[1].phoneNumber)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `targetDevices emits repository data`() = runTest {
        val devices = listOf(
            createTestDevice(phoneNumber = "+3333333333", role = DeviceRole.TARGET),
            createTestDevice(phoneNumber = "+4444444444", role = DeviceRole.TARGET),
        )

        viewModel.targetDevices.test {
            awaitItem() // Initial empty list

            targetDevicesFlow.value = devices

            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("+3333333333", result[0].phoneNumber)
            assertEquals("+4444444444", result[1].phoneNumber)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteDevice ends sessions, sends UNPAIR, and deletes from DB`() = runTest {
        val device = createTestDevice(phoneNumber = "+1234567890")
        coEvery { sessionRepository.endSessionForDevice(any(), any()) } just runs
        coEvery { deviceRepository.delete(any()) } just runs

        viewModel.deleteDevice(device)

        // With UnconfinedTestDispatcher, coroutines complete immediately
        coVerify { sessionRepository.endSessionForDevice("+1234567890", "USER") }
        verify { smsSender.sendUnpair("+1234567890") }
        coVerify { deviceRepository.delete(device) }
    }

    @Test
    fun `cancelPendingRequest deletes device by phone number`() = runTest {
        coEvery { deviceRepository.deleteByPhoneNumber(any()) } just runs

        viewModel.cancelPendingRequest("+1234567890")

        // With UnconfinedTestDispatcher, coroutines complete immediately
        coVerify { deviceRepository.deleteByPhoneNumber("+1234567890") }
    }

    @Test
    fun `isLoading initially false`() = runTest {
        viewModel.isLoading.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
