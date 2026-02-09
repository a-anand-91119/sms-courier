package dev.notyouraverage.smscourier.viewmodels

import dev.notyouraverage.smscourier.MainCoroutineRule
import dev.notyouraverage.smscourier.TestFixtures.createTestDevice
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import dev.notyouraverage.smscourier.export.ExportManager
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class DeviceHistoryViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @MockK
    private lateinit var deviceRepository: PairedDeviceRepository

    @MockK
    private lateinit var sessionRepository: ForwardingSessionRepository

    @MockK
    private lateinit var exportManager: ExportManager

    @MockK
    private lateinit var smsSender: SmsSender

    private lateinit var viewModel: DeviceHistoryViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        every { deviceRepository.getActiveDevices() } returns flowOf(emptyList())
        every { deviceRepository.getArchivedDevices() } returns flowOf(emptyList())
        every { sessionRepository.getActiveSessions() } returns flowOf(emptyList())

        viewModel = DeviceHistoryViewModel(
            deviceRepository = deviceRepository,
            sessionRepository = sessionRepository,
            exportManager = exportManager,
            smsSender = smsSender,
        )
    }

    @Test
    fun `unpairDevice ends sessions, sends UNPAIR SMS, and archives device`() = runTest {
        // Device has TARGET role, so inverse role for remote is SOURCE
        val device = createTestDevice(
            phoneNumber = "+1234567890",
            role = DeviceRole.TARGET,
            status = PairingStatus.APPROVED,
        )
        coEvery { sessionRepository.endSessionForDevice(any(), any()) } just runs
        coEvery { deviceRepository.archiveDevice(any(), any(), any()) } just runs

        viewModel.unpairDevice(device)

        // With UnconfinedTestDispatcher, coroutines complete immediately
        coVerify { sessionRepository.endSessionForDevice("+1234567890", "UNPAIR") }
        // TARGET device -> tell remote to delete SOURCE role (inverse)
        verify { smsSender.sendUnpair("+1234567890", DeviceRole.SOURCE) }
        coVerify { deviceRepository.archiveDevice("+1234567890", DeviceRole.TARGET, "USER") }
    }

    @Test
    fun `unpairDevice sends correct inverse role for SOURCE device`() = runTest {
        // Device has SOURCE role, so inverse role for remote is TARGET
        val device = createTestDevice(
            phoneNumber = "+9876543210",
            role = DeviceRole.SOURCE,
            status = PairingStatus.APPROVED,
        )
        coEvery { sessionRepository.endSessionForDevice(any(), any()) } just runs
        coEvery { deviceRepository.archiveDevice(any(), any(), any()) } just runs

        viewModel.unpairDevice(device)

        // SOURCE device -> tell remote to delete TARGET role (inverse)
        verify { smsSender.sendUnpair("+9876543210", DeviceRole.TARGET) }
    }

    @Test
    fun `unpairDevice does not send SMS for non-approved device`() = runTest {
        val device = createTestDevice(
            phoneNumber = "+1234567890",
            role = DeviceRole.TARGET,
            // Not approved
            status = PairingStatus.PENDING_RECEIVED,
        )
        coEvery { sessionRepository.endSessionForDevice(any(), any()) } just runs
        coEvery { deviceRepository.archiveDevice(any(), any(), any()) } just runs

        viewModel.unpairDevice(device)

        // Should NOT send UNPAIR SMS for non-approved device
        verify(exactly = 0) { smsSender.sendUnpair(any(), any()) }
        // But should still archive
        coVerify { deviceRepository.archiveDevice("+1234567890", DeviceRole.TARGET, "USER") }
    }
}
