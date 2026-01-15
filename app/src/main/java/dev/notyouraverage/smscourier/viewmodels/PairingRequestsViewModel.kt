package dev.notyouraverage.smscourier.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import dev.notyouraverage.smscourier.security.SecurityManager
import dev.notyouraverage.smscourier.services.SmsSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PairingRequestsViewModel(
    private val deviceRepository: PairedDeviceRepository,
    private val smsSender: SmsSender,
) : ViewModel() {

    val pendingRequests: StateFlow<List<PairedDevice>> = deviceRepository.getPendingRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _approvalInProgress = MutableStateFlow<String?>(null)
    val approvalInProgress: StateFlow<String?> = _approvalInProgress

    fun approvePairing(phoneNumber: String, password: String) {
        viewModelScope.launch {
            _approvalInProgress.value = phoneNumber
            try {
                val passwordHash = SecurityManager.hashPassword(password)
                val authKey = SecurityManager.deriveAuthKey(password)
                // We are TARGET, they are SOURCE
                deviceRepository.updatePassword(phoneNumber, DeviceRole.TARGET, passwordHash.hash, passwordHash.salt)
                deviceRepository.updateAuthKey(phoneNumber, DeviceRole.TARGET, authKey)
                deviceRepository.updatePairingStatus(phoneNumber, DeviceRole.TARGET, PairingStatus.APPROVED)
                smsSender.sendPairApproved(phoneNumber)
            } finally {
                _approvalInProgress.value = null
            }
        }
    }

    fun rejectPairing(phoneNumber: String) {
        viewModelScope.launch {
            // We are TARGET, they are SOURCE
            deviceRepository.updatePairingStatus(phoneNumber, DeviceRole.TARGET, PairingStatus.REJECTED)
            smsSender.sendPairRejected(phoneNumber)
        }
    }

    class Factory(
        private val deviceRepository: PairedDeviceRepository,
        private val smsSender: SmsSender,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PairingRequestsViewModel(deviceRepository, smsSender) as T
        }
    }
}
