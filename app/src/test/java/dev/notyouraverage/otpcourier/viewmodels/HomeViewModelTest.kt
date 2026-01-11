package dev.notyouraverage.otpcourier.viewmodels

import app.cash.turbine.test
import dev.notyouraverage.otpcourier.MainCoroutineRule
import dev.notyouraverage.otpcourier.TestFixtures.createTestDevice
import dev.notyouraverage.otpcourier.TestFixtures.createTestSession
import dev.notyouraverage.otpcourier.data.entities.PairingStatus
import dev.notyouraverage.otpcourier.repository.ForwardingSessionRepository
import dev.notyouraverage.otpcourier.repository.PairedDeviceRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class HomeViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @MockK
    private lateinit var deviceRepository: PairedDeviceRepository

    @MockK
    private lateinit var sessionRepository: ForwardingSessionRepository

    private val approvedDevicesFlow = MutableStateFlow(emptyList<dev.notyouraverage.otpcourier.data.entities.PairedDevice>())
    private val pendingRequestsFlow = MutableStateFlow(emptyList<dev.notyouraverage.otpcourier.data.entities.PairedDevice>())
    private val activeSessionsFlow = MutableStateFlow(emptyList<dev.notyouraverage.otpcourier.data.entities.ForwardingSession>())

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        every { deviceRepository.getApprovedDevices() } returns approvedDevicesFlow
        every { deviceRepository.getPendingRequests() } returns pendingRequestsFlow
        every { sessionRepository.getActiveSessions() } returns activeSessionsFlow

        viewModel = HomeViewModel(deviceRepository, sessionRepository)
    }

    @Test
    fun `homeState has zero counts initially`() = runTest {
        viewModel.homeState.test {
            val state = awaitItem()
            assertEquals(0, state.approvedDevicesCount)
            assertEquals(0, state.pendingRequestsCount)
            assertEquals(0, state.activeSessionsCount)
            assertTrue(state.activeSessions.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `homeState updates when approved devices change`() = runTest {
        val devices = listOf(
            createTestDevice(phoneNumber = "+1111111111", status = PairingStatus.APPROVED),
            createTestDevice(phoneNumber = "+2222222222", status = PairingStatus.APPROVED),
        )

        viewModel.homeState.test {
            // Initial state
            awaitItem()

            // Update approved devices
            approvedDevicesFlow.value = devices

            val state = awaitItem()
            assertEquals(2, state.approvedDevicesCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `homeState updates when pending requests change`() = runTest {
        val pendingDevices = listOf(
            createTestDevice(phoneNumber = "+1111111111", status = PairingStatus.PENDING_RECEIVED),
            createTestDevice(phoneNumber = "+2222222222", status = PairingStatus.PENDING_RECEIVED),
            createTestDevice(phoneNumber = "+3333333333", status = PairingStatus.PENDING_SENT),
        )

        viewModel.homeState.test {
            // Initial state
            awaitItem()

            // Update pending requests
            pendingRequestsFlow.value = pendingDevices

            val state = awaitItem()
            assertEquals(3, state.pendingRequestsCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `homeState updates when active sessions change`() = runTest {
        val sessions = listOf(
            createTestSession(id = 1L, devicePhoneNumber = "+1111111111"),
            createTestSession(id = 2L, devicePhoneNumber = "+2222222222"),
        )

        viewModel.homeState.test {
            // Initial state
            awaitItem()

            // Update active sessions
            activeSessionsFlow.value = sessions

            val state = awaitItem()
            assertEquals(2, state.activeSessionsCount)
            assertEquals(2, state.activeSessions.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `homeState combines counts from all three flows`() = runTest {
        val approved = listOf(createTestDevice(phoneNumber = "+1111111111", status = PairingStatus.APPROVED))
        val pending = listOf(
            createTestDevice(phoneNumber = "+2222222222", status = PairingStatus.PENDING_RECEIVED),
            createTestDevice(phoneNumber = "+3333333333", status = PairingStatus.PENDING_SENT),
        )
        val sessions = listOf(
            createTestSession(id = 1L),
            createTestSession(id = 2L),
            createTestSession(id = 3L),
        )

        viewModel.homeState.test {
            // Initial state
            awaitItem()

            // Update all flows
            approvedDevicesFlow.value = approved
            pendingRequestsFlow.value = pending
            activeSessionsFlow.value = sessions

            // Skip intermediate states and get final combined state
            var state = awaitItem()
            // Keep awaiting until we get the fully combined state
            while (state.approvedDevicesCount != 1 || state.pendingRequestsCount != 2 || state.activeSessionsCount != 3) {
                state = awaitItem()
            }

            assertEquals(1, state.approvedDevicesCount)
            assertEquals(2, state.pendingRequestsCount)
            assertEquals(3, state.activeSessionsCount)
            assertEquals(3, state.activeSessions.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
