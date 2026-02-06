package dev.notyouraverage.smscourier.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import dev.notyouraverage.smscourier.data.settings.SettingsDefaults
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import dev.notyouraverage.smscourier.repository.SettingsRepository
import dev.notyouraverage.smscourier.services.SmsSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ResendStatus {
    data object CanResend : ResendStatus()
    data object NotPending : ResendStatus()
    data object MaxAttemptsReached : ResendStatus()
    data class Cooldown(val remainingMs: Long) : ResendStatus()
}

class PairedDevicesViewModel(
    private val deviceRepository: PairedDeviceRepository,
    private val sessionRepository: ForwardingSessionRepository,
    private val smsSender: SmsSender,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    // Settings-based pairing limits (reactive via StateFlow)
    private val maxResendAttempts: StateFlow<Int> = settingsRepository.maxPairingResendAttempts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDefaults.MAX_PAIRING_RESEND_ATTEMPTS)

    private val resendCooldownMinutes: StateFlow<Int> = settingsRepository.pairingResendCooldownMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDefaults.PAIRING_RESEND_COOLDOWN)

    private val resendCooldownMs: Long get() = resendCooldownMinutes.value * 60 * 1000L

    // Devices where this phone is the SOURCE (requests forwarding FROM these devices)
    val sourceDevices: StateFlow<List<PairedDevice>> = deviceRepository.getSourceDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Devices where this phone is the TARGET (forwards SMS TO these devices)
    val targetDevices: StateFlow<List<PairedDevice>> = deviceRepository.getTargetDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _resendingDevice = MutableStateFlow<String?>(null)
    val resendingDevice: StateFlow<String?> = _resendingDevice.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun canResendPairingRequest(device: PairedDevice): ResendStatus {
        if (device.status != PairingStatus.PENDING_SENT) return ResendStatus.NotPending
        if (device.resendAttemptCount >= maxResendAttempts.value) return ResendStatus.MaxAttemptsReached
        val lastAttempt = device.lastResendAttemptAt ?: return ResendStatus.CanResend
        val elapsed = System.currentTimeMillis() - lastAttempt
        val cooldownMs = resendCooldownMs
        if (elapsed < cooldownMs) {
            return ResendStatus.Cooldown(remainingMs = cooldownMs - elapsed)
        }
        return ResendStatus.CanResend
    }

    fun resendPairingRequest(device: PairedDevice) {
        viewModelScope.launch {
            _resendingDevice.value = device.phoneNumber
            try {
                deviceRepository.recordResendAttempt(device.phoneNumber, device.role)
                smsSender.sendPairRequest(device.phoneNumber)
                _snackbarMessage.value = "Pairing request sent to ${device.phoneNumber}"
            } finally {
                _resendingDevice.value = null
            }
        }
    }

    fun deleteDevice(device: PairedDevice) {
        viewModelScope.launch {
            // 1. End any active forwarding sessions for this device
            sessionRepository.endSessionForDevice(device.phoneNumber, "USER")

            // 2. Only send UNPAIR SMS if pairing was actually established (APPROVED status)
            // Skip for: REJECTED (they already know), PENDING_SENT (they don't have us),
            //           PENDING_RECEIVED (pairing never completed)
            if (device.status == PairingStatus.APPROVED) {
                // Send role-specific UNPAIR - tell other device to delete the inverse role
                // If we're SOURCE (we request FROM them), they're TARGET (they forward TO us)
                // If we're TARGET (we forward TO them), they're SOURCE (they request FROM us)
                val roleToDeleteOnRemote = when (device.role) {
                    dev.notyouraverage.smscourier.data.entities.DeviceRole.SOURCE ->
                        dev.notyouraverage.smscourier.data.entities.DeviceRole.TARGET
                    dev.notyouraverage.smscourier.data.entities.DeviceRole.TARGET ->
                        dev.notyouraverage.smscourier.data.entities.DeviceRole.SOURCE
                }
                smsSender.sendUnpair(device.phoneNumber, roleToDeleteOnRemote)
            }

            // 3. Archive only this specific role in local database (soft delete)
            deviceRepository.archiveDevice(device.phoneNumber, device.role, "LOCAL")
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
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PairedDevicesViewModel(
                deviceRepository,
                sessionRepository,
                smsSender,
                settingsRepository,
            ) as T
        }
    }
}
