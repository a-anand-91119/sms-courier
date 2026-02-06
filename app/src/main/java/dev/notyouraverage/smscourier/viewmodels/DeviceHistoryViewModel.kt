package dev.notyouraverage.smscourier.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class DeviceHistoryViewModel(
    private val deviceRepository: PairedDeviceRepository,
    private val sessionRepository: ForwardingSessionRepository,
) : ViewModel() {

    // Active devices combined with session status
    val activeDevices: StateFlow<List<DeviceWithActiveSession>> = combine(
        deviceRepository.getActiveDevices(),
        sessionRepository.getActiveSessions(),
    ) { devices, sessions ->
        val activePhoneNumbers = sessions.map { it.devicePhoneNumber }.toSet()
        devices.map { device ->
            DeviceWithActiveSession(
                device = device,
                hasActiveSession = device.phoneNumber in activePhoneNumbers,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    // Archived/removed devices
    val removedDevices: StateFlow<List<PairedDevice>> = deviceRepository.getArchivedDevices()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
        )

    // Loading state (true until first data arrives)
    val isLoading: StateFlow<Boolean> = combine(
        activeDevices,
        removedDevices,
    ) { _, _ ->
        false // Once we have any data, we're not loading
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        true,
    )

    data class DeviceWithActiveSession(
        val device: PairedDevice,
        val hasActiveSession: Boolean,
    )

    class Factory(
        private val deviceRepository: PairedDeviceRepository,
        private val sessionRepository: ForwardingSessionRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DeviceHistoryViewModel(deviceRepository, sessionRepository) as T
        }
    }
}
