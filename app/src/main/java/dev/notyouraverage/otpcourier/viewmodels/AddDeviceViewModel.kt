package dev.notyouraverage.otpcourier.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.otpcourier.data.entities.DeviceRole
import dev.notyouraverage.otpcourier.data.entities.PairedDevice
import dev.notyouraverage.otpcourier.data.entities.PairingStatus
import dev.notyouraverage.otpcourier.repository.PairedDeviceRepository
import dev.notyouraverage.otpcourier.services.SmsSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddDeviceViewModel(
    private val deviceRepository: PairedDeviceRepository,
    private val smsSender: SmsSender,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDeviceUiState())
    val uiState: StateFlow<AddDeviceUiState> = _uiState

    fun updatePhoneNumber(phoneNumber: String) {
        _uiState.value = _uiState.value.copy(
            phoneNumber = phoneNumber,
            error = null,
        )
    }

    fun sendPairingRequest() {
        val phoneNumber = _uiState.value.phoneNumber.trim()

        if (phoneNumber.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter a phone number")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Check if device already exists
                val existing = deviceRepository.getByPhoneNumber(phoneNumber)
                if (existing != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Device already exists with status: ${existing.status}",
                    )
                    return@launch
                }

                // Create new device entry (we are source, they are target)
                deviceRepository.insert(
                    PairedDevice(
                        phoneNumber = phoneNumber,
                        role = DeviceRole.SOURCE,
                        status = PairingStatus.PENDING_SENT,
                        createdAt = System.currentTimeMillis(),
                        lastActivityAt = System.currentTimeMillis(),
                    ),
                )

                // Send pairing request SMS
                smsSender.sendPairRequest(phoneNumber)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    success = true,
                    phoneNumber = "",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to send pairing request: ${e.message}",
                )
            }
        }
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(success = false)
    }

    data class AddDeviceUiState(
        val phoneNumber: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val success: Boolean = false,
    )

    class Factory(
        private val deviceRepository: PairedDeviceRepository,
        private val smsSender: SmsSender,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddDeviceViewModel(deviceRepository, smsSender) as T
        }
    }
}
