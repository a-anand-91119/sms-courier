package dev.notyouraverage.otpcourier.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.otpcourier.data.entities.PairedDevice
import dev.notyouraverage.otpcourier.data.entities.PairingStatus
import dev.notyouraverage.otpcourier.repository.PairedDeviceRepository
import dev.notyouraverage.otpcourier.security.SecurityManager
import dev.notyouraverage.otpcourier.services.SmsSender
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
                deviceRepository.updatePassword(phoneNumber, passwordHash.hash, passwordHash.salt)
                deviceRepository.updateAuthKey(phoneNumber, authKey)
                deviceRepository.updatePairingStatus(phoneNumber, PairingStatus.APPROVED)
                smsSender.sendPairApproved(phoneNumber)
            } finally {
                _approvalInProgress.value = null
            }
        }
    }

    fun rejectPairing(phoneNumber: String) {
        viewModelScope.launch {
            deviceRepository.updatePairingStatus(phoneNumber, PairingStatus.REJECTED)
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
