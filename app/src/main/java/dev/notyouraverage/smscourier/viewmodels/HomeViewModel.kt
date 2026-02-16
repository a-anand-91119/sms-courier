package dev.notyouraverage.smscourier.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.smscourier.data.Direction
import dev.notyouraverage.smscourier.data.DirectionalStatus
import dev.notyouraverage.smscourier.data.SessionWithDirection
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.ForwardingSession
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    deviceRepository: PairedDeviceRepository,
    sessionRepository: ForwardingSessionRepository,
) : ViewModel() {

    val homeState: StateFlow<HomeState> = combine(
        deviceRepository.getApprovedDevices(),
        deviceRepository.getPendingRequests(),
        sessionRepository.getActiveSessions(),
    ) { approved, pending, sessions ->
        HomeState(
            approvedDevicesCount = approved.size,
            pendingRequestsCount = pending.size,
            activeSessionsCount = sessions.size,
            approvedDevices = approved,
            pendingDevices = pending,
            activeSessions = sessions,
            directionalStatus = calculateDirectionalStatus(approved, sessions),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeState(),
    )

    /**
     * Calculate directional status from approved devices and active sessions.
     *
     * Logic:
     * - Group sessions by devicePhoneNumber
     * - For each phone with active sessions, check if device has SOURCE and/or TARGET role
     * - BIDIRECTIONAL requires active sessions for BOTH roles with same phone number
     * - RECEIVING_FROM: only SOURCE role has active session (this device receives)
     * - FORWARDING_TO: only TARGET role has active session (this device forwards)
     */
    private fun calculateDirectionalStatus(
        devices: List<PairedDevice>,
        sessions: List<ForwardingSession>,
    ): DirectionalStatus {
        if (sessions.isEmpty()) {
            return DirectionalStatus()
        }

        // Group devices by phone number for lookup
        val devicesByPhone = devices.groupBy { it.phoneNumber }

        // Group sessions by phone number to detect bidirectional
        val sessionsByPhone = sessions.groupBy { it.devicePhoneNumber }

        val sessionsWithDirection = mutableListOf<SessionWithDirection>()
        var forwardingToCount = 0
        var receivingFromCount = 0
        var bidirectionalCount = 0

        sessionsByPhone.forEach { (phone, phoneSessions) ->
            val phoneDevices = devicesByPhone[phone] ?: emptyList()
            val sourceDevice = phoneDevices.find { it.role == DeviceRole.SOURCE }
            val targetDevice = phoneDevices.find { it.role == DeviceRole.TARGET }

            // Determine direction based on which roles exist
            // For bidirectional: both SOURCE and TARGET must exist for this phone
            when {
                sourceDevice != null && targetDevice != null -> {
                    // Both roles exist with sessions for this phone = bidirectional
                    bidirectionalCount += phoneSessions.size
                    phoneSessions.forEach { session ->
                        sessionsWithDirection.add(
                            SessionWithDirection(
                                session = session,
                                // Use SOURCE device as primary for bidirectional
                                device = sourceDevice,
                                direction = Direction.BIDIRECTIONAL,
                            ),
                        )
                    }
                }
                sourceDevice != null -> {
                    // Only SOURCE role: this device RECEIVES messages
                    receivingFromCount += phoneSessions.size
                    phoneSessions.forEach { session ->
                        sessionsWithDirection.add(
                            SessionWithDirection(
                                session = session,
                                device = sourceDevice,
                                direction = Direction.RECEIVING_FROM,
                            ),
                        )
                    }
                }
                targetDevice != null -> {
                    // Only TARGET role: this device FORWARDS messages
                    forwardingToCount += phoneSessions.size
                    phoneSessions.forEach { session ->
                        sessionsWithDirection.add(
                            SessionWithDirection(
                                session = session,
                                device = targetDevice,
                                direction = Direction.FORWARDING_TO,
                            ),
                        )
                    }
                }
            }
        }

        return DirectionalStatus(
            forwardingToCount = forwardingToCount,
            receivingFromCount = receivingFromCount,
            bidirectionalCount = bidirectionalCount,
            activeSessions = sessionsWithDirection,
        )
    }

    data class HomeState(
        val approvedDevicesCount: Int = 0,
        val pendingRequestsCount: Int = 0,
        val activeSessionsCount: Int = 0,
        val approvedDevices: List<PairedDevice> = emptyList(),
        val pendingDevices: List<PairedDevice> = emptyList(),
        val activeSessions: List<ForwardingSession> = emptyList(),
        val directionalStatus: DirectionalStatus = DirectionalStatus(),
    )

    class Factory(
        private val deviceRepository: PairedDeviceRepository,
        private val sessionRepository: ForwardingSessionRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(deviceRepository, sessionRepository) as T
        }
    }
}
