package dev.notyouraverage.smscourier.viewmodels

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.ForwardingSession
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import dev.notyouraverage.smscourier.data.settings.SettingsDefaults
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import dev.notyouraverage.smscourier.repository.SettingsRepository
import dev.notyouraverage.smscourier.services.SmsSender
import dev.notyouraverage.smscourier.services.foreground.MasterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ForwardingControlViewModel(
    private val context: Context,
    private val deviceRepository: PairedDeviceRepository,
    private val sessionRepository: ForwardingSessionRepository,
    private val smsSender: SmsSender,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    companion object {
        private const val TAG = "SMSC:FwdCtrlVM"
    }

    // SESS-01 FIX: Query both SOURCE and TARGET devices for session visibility
    val approvedDevices: StateFlow<List<PairedDevice>> = combine(
        deviceRepository.getDevicesByRoleAndStatus(DeviceRole.SOURCE, PairingStatus.APPROVED),
        deviceRepository.getDevicesByRoleAndStatus(DeviceRole.TARGET, PairingStatus.APPROVED),
    ) { sourceDevices, targetDevices ->
        sourceDevices + targetDevices
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Keep for backward compatibility (deprecated)
    @Deprecated("Use approvedDevices instead which includes both SOURCE and TARGET roles")
    val approvedSourceDevices: StateFlow<List<PairedDevice>> = deviceRepository
        .getDevicesByRoleAndStatus(DeviceRole.SOURCE, PairingStatus.APPROVED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active forwarding sessions
    val activeSessions: StateFlow<List<ForwardingSession>> = sessionRepository.getActiveSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(ForwardingUiState())
    val uiState: StateFlow<ForwardingUiState> = _uiState

    init {
        // Load user's saved default duration from settings
        viewModelScope.launch {
            val savedDuration = settingsRepository.defaultForwardingDurationMinutes.first()
            _uiState.value = _uiState.value.copy(durationMinutes = savedDuration)
        }
    }

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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, confirmStopPhoneNumber = null)

            // SESS-03 FIX: Send STOP_FORWARD SMS to notify remote device
            var smsNotificationFailed = false
            try {
                smsSender.sendStopForward(devicePhoneNumber)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send STOP_FORWARD SMS to $devicePhoneNumber", e)
                smsNotificationFailed = true
            }

            try {
                // Always end local session regardless of SMS result
                sessionRepository.endSessionForDevice(devicePhoneNumber, "USER")
                // SESS-02 FIX: Clear encryption key for both roles (one will be no-op)
                deviceRepository.updateEncryptionKey(devicePhoneNumber, DeviceRole.SOURCE, null)
                deviceRepository.updateEncryptionKey(devicePhoneNumber, DeviceRole.TARGET, null)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (smsNotificationFailed) "Session stopped but remote device wasn't notified" else null,
                )
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

    fun requestStopConfirmation(phoneNumber: String) {
        _uiState.value = _uiState.value.copy(confirmStopPhoneNumber = phoneNumber)
    }

    fun cancelStopConfirmation() {
        _uiState.value = _uiState.value.copy(confirmStopPhoneNumber = null)
    }

    data class ForwardingUiState(
        val password: String = "",
        val durationMinutes: Int = SettingsDefaults.DEFAULT_FORWARDING_DURATION,
        val isLoading: Boolean = false,
        val error: String? = null,
        val success: Boolean = false,
        val confirmStopPhoneNumber: String? = null,
    )

    class Factory(
        private val context: Context,
        private val deviceRepository: PairedDeviceRepository,
        private val sessionRepository: ForwardingSessionRepository,
        private val smsSender: SmsSender,
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ForwardingControlViewModel(
                context,
                deviceRepository,
                sessionRepository,
                smsSender,
                settingsRepository,
            ) as T
        }
    }
}
