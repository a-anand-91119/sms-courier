package dev.notyouraverage.smscourier.viewmodels

import dev.notyouraverage.smscourier.MainCoroutineRule
import dev.notyouraverage.smscourier.TestFixtures.createTestDevice
import dev.notyouraverage.smscourier.UATTest
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import dev.notyouraverage.smscourier.export.ExportManager
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import dev.notyouraverage.smscourier.services.SmsSender
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
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
import org.junit.experimental.categories.Category

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

    @Category(UATTest::class)
    @Test
    fun `UAT DEVH-02 - archiveDevice moves active device to archived state without unpairing`() = runTest {
        // DEVH-02: There is no explicit "Archive" action on a paired device. Users must
        // unpair first, which sends UNPAIR SMS to the remote device. This test verifies
        // that an archiveDevice method should exist to archive a device locally WITHOUT
        // unpairing (preserving the pairing relationship, just hiding the device).

        val device = createTestDevice(
            phoneNumber = "+5551234567",
            role = DeviceRole.TARGET,
            status = PairingStatus.APPROVED,
            isArchived = false,
        )
        coEvery { deviceRepository.archiveDevice(any(), any(), any()) } just runs

        // This method call will throw NotImplementedError because archiveDevice
        // doesn't exist on DeviceHistoryViewModel yet
        viewModel.archiveDevice(device)

        // DEVH-02: Should call repository archiveDevice method WITHOUT sending UNPAIR SMS
        coVerify {
            deviceRepository.archiveDevice(
                phoneNumber = "+5551234567",
                role = DeviceRole.TARGET,
                initiatedBy = "USER",
            )
        }

        // Should NOT send UNPAIR SMS (archive preserves pairing relationship)
        verify(exactly = 0) { smsSender.sendUnpair(any(), any()) }

        // After fix in Phase 27: DeviceHistoryViewModel will have an archiveDevice method
        // that archives the device without sending UNPAIR SMS to the remote device.
    }

    @Test
    fun `archiveDevice ends active session before archiving`() = runTest {
        val device = createTestDevice(
            phoneNumber = "+1112223333",
            role = DeviceRole.TARGET,
            status = PairingStatus.APPROVED,
        )
        coEvery { sessionRepository.endSessionForDevice(any(), any()) } just runs
        coEvery { deviceRepository.archiveDevice(any(), any(), any()) } just runs

        viewModel.archiveDevice(device)

        coVerifyOrder {
            sessionRepository.endSessionForDevice("+1112223333", "ARCHIVE")
            deviceRepository.archiveDevice(any(), any(), any())
        }
    }

    @Test
    fun `archiveDevice does NOT send UNPAIR SMS for APPROVED status`() = runTest {
        val device = createTestDevice(
            phoneNumber = "+2223334444",
            role = DeviceRole.TARGET,
            status = PairingStatus.APPROVED,
        )
        coEvery { sessionRepository.endSessionForDevice(any(), any()) } just runs
        coEvery { deviceRepository.archiveDevice(any(), any(), any()) } just runs

        viewModel.archiveDevice(device)

        coVerify(exactly = 0) { smsSender.sendUnpair(any(), any()) }
    }

    @Test
    fun `archiveDevice does NOT send UNPAIR SMS for PENDING_RECEIVED status`() = runTest {
        val device = createTestDevice(
            phoneNumber = "+3334445555",
            role = DeviceRole.SOURCE,
            status = PairingStatus.PENDING_RECEIVED,
        )
        coEvery { sessionRepository.endSessionForDevice(any(), any()) } just runs
        coEvery { deviceRepository.archiveDevice(any(), any(), any()) } just runs

        viewModel.archiveDevice(device)

        coVerify(exactly = 0) { smsSender.sendUnpair(any(), any()) }
    }

    @Test
    fun `archiveDevice does NOT send UNPAIR SMS for REJECTED status`() = runTest {
        val device = createTestDevice(
            phoneNumber = "+4445556666",
            role = DeviceRole.TARGET,
            status = PairingStatus.REJECTED,
        )
        coEvery { sessionRepository.endSessionForDevice(any(), any()) } just runs
        coEvery { deviceRepository.archiveDevice(any(), any(), any()) } just runs

        viewModel.archiveDevice(device)

        coVerify(exactly = 0) { smsSender.sendUnpair(any(), any()) }
    }
}
