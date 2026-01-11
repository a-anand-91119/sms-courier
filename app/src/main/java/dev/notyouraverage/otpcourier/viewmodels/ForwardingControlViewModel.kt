package dev.notyouraverage.otpcourier.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.otpcourier.data.entities.DeviceRole
import dev.notyouraverage.otpcourier.data.entities.ForwardingSession
import dev.notyouraverage.otpcourier.data.entities.PairedDevice
import dev.notyouraverage.otpcourier.data.entities.PairingStatus
import dev.notyouraverage.otpcourier.repository.ForwardingSessionRepository
import dev.notyouraverage.otpcourier.repository.PairedDeviceRepository
import dev.notyouraverage.otpcourier.services.SmsSender
import dev.notyouraverage.otpcourier.services.foreground.MasterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ForwardingControlViewModel(
    private val context: Context,
    private val deviceRepository: PairedDeviceRepository,
    private val sessionRepository: ForwardingSessionRepository,
    private val smsSender: SmsSender,
) : ViewModel() {

    // Approved source devices (devices we can request forwarding from)
    val approvedSourceDevices: StateFlow<List<PairedDevice>> = deviceRepository
        .getDevicesByRoleAndStatus(DeviceRole.SOURCE, PairingStatus.APPROVED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active forwarding sessions
    val activeSessions: StateFlow<List<ForwardingSession>> = sessionRepository.getActiveSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(ForwardingUiState())
    val uiState: StateFlow<ForwardingUiState> = _uiState

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun updateDuration(duration: Int) {
        _uiState.value = _uiState.value.copy(durationMinutes = duration)
    }

    fun startForwarding(targetPhoneNumber: String) {
        val password = _uiState.value.password.trim()
        val duration = _uiState.value.durationMinutes

        if (password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter the password")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Send intent to MasterService to initiate challenge-response auth
                // MasterService will store the password, send AUTH_REQUEST, wait for challenge,
                // compute HMAC response, and send START_FORWARD
                Intent(context, MasterService::class.java).apply {
                    action = MasterService.INITIATE_AUTH_REQUEST
                    putExtra(MasterService.EXTRA_PHONE_NUMBER, targetPhoneNumber)
                    putExtra(MasterService.EXTRA_PASSWORD, password)
                    putExtra(MasterService.EXTRA_DURATION, duration)
                    context.startService(this)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    success = true,
                    password = "",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to initiate forwarding: ${e.message}",
                )
            }
        }
    }

    fun stopForwarding(devicePhoneNumber: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // End the local session
                sessionRepository.endSessionForDevice(devicePhoneNumber, "USER")
                // Clear the stored encryption key
                deviceRepository.updateEncryptionKey(devicePhoneNumber, null)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to stop forwarding: ${e.message}",
                )
            }
        }
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(success = false)
    }

    data class ForwardingUiState(
        val password: String = "",
        val durationMinutes: Int = 30,
        val isLoading: Boolean = false,
        val error: String? = null,
        val success: Boolean = false,
    )

    class Factory(
        private val context: Context,
        private val deviceRepository: PairedDeviceRepository,
        private val sessionRepository: ForwardingSessionRepository,
        private val smsSender: SmsSender,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ForwardingControlViewModel(context, deviceRepository, sessionRepository, smsSender) as T
        }
    }
}
