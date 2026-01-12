package dev.notyouraverage.smscourier.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.smscourier.data.entities.ForwardingSession
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
            activeSessions = sessions,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeState(),
    )

    data class HomeState(
        val approvedDevicesCount: Int = 0,
        val pendingRequestsCount: Int = 0,
        val activeSessionsCount: Int = 0,
        val activeSessions: List<ForwardingSession> = emptyList(),
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
