package dev.notyouraverage.smscourier.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import dev.notyouraverage.smscourier.services.SmsSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PairedDevicesViewModel(
    private val deviceRepository: PairedDeviceRepository,
    private val sessionRepository: ForwardingSessionRepository,
    private val smsSender: SmsSender,
) : ViewModel() {

    // Devices where this phone is the SOURCE (requests forwarding FROM these devices)
    val sourceDevices: StateFlow<List<PairedDevice>> = deviceRepository.getSourceDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Devices where this phone is the TARGET (forwards SMS TO these devices)
    val targetDevices: StateFlow<List<PairedDevice>> = deviceRepository.getTargetDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun deleteDevice(device: PairedDevice) {
        viewModelScope.launch {
            // 1. End any active forwarding sessions for this device
            sessionRepository.endSessionForDevice(device.phoneNumber, "USER")
            // 2. Send UNPAIR SMS to notify the other device
            smsSender.sendUnpair(device.phoneNumber)
            // 3. Delete from local database
            deviceRepository.delete(device)
        }
    }

    fun cancelPendingRequest(phoneNumber: String) {
        viewModelScope.launch {
            deviceRepository.deleteByPhoneNumber(phoneNumber)
        }
    }

    class Factory(
        private val deviceRepository: PairedDeviceRepository,
        private val sessionRepository: ForwardingSessionRepository,
        private val smsSender: SmsSender,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PairedDevicesViewModel(deviceRepository, sessionRepository, smsSender) as T
        }
    }
}
