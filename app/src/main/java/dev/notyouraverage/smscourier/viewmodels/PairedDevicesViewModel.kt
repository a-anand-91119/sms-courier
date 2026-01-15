package dev.notyouraverage.smscourier.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import dev.notyouraverage.smscourier.services.SmsSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
) : ViewModel() {

    companion object {
        private const val MAX_RESEND_ATTEMPTS = 5
        private const val RESEND_COOLDOWN_MS = 60_000L // 1 minute between resends
    }

    // Devices where this phone is the SOURCE (requests forwarding FROM these devices)
    val sourceDevices: StateFlow<List<PairedDevice>> = deviceRepository.getSourceDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Devices where this phone is the TARGET (forwards SMS TO these devices)
    val targetDevices: StateFlow<List<PairedDevice>> = deviceRepository.getTargetDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun canResendPairingRequest(device: PairedDevice): ResendStatus {
        if (device.status != PairingStatus.PENDING_SENT) return ResendStatus.NotPending
        if (device.resendAttemptCount >= MAX_RESEND_ATTEMPTS) return ResendStatus.MaxAttemptsReached
        val lastAttempt = device.lastResendAttemptAt ?: return ResendStatus.CanResend
        val elapsed = System.currentTimeMillis() - lastAttempt
        if (elapsed < RESEND_COOLDOWN_MS) {
            return ResendStatus.Cooldown(remainingMs = RESEND_COOLDOWN_MS - elapsed)
        }
        return ResendStatus.CanResend
    }

    fun resendPairingRequest(device: PairedDevice) {
        viewModelScope.launch {
            deviceRepository.recordResendAttempt(device.phoneNumber, device.role)
            smsSender.sendPairRequest(device.phoneNumber)
        }
    }

    fun deleteDevice(device: PairedDevice) {
        viewModelScope.launch {
            // 1. End any active forwarding sessions for this device
            sessionRepository.endSessionForDevice(device.phoneNumber, "USER")

            // 2. Send role-specific UNPAIR - tell other device to delete the inverse role
            // If we're SOURCE (we request FROM them), they're TARGET (they forward TO us)
            // If we're TARGET (we forward TO them), they're SOURCE (they request FROM us)
            val roleToDeleteOnRemote = when (device.role) {
                dev.notyouraverage.smscourier.data.entities.DeviceRole.SOURCE ->
                    dev.notyouraverage.smscourier.data.entities.DeviceRole.TARGET
                dev.notyouraverage.smscourier.data.entities.DeviceRole.TARGET ->
                    dev.notyouraverage.smscourier.data.entities.DeviceRole.SOURCE
            }
            smsSender.sendUnpair(device.phoneNumber, roleToDeleteOnRemote)

            // 3. Delete only this specific role from local database
            deviceRepository.deleteByPhoneNumberAndRole(device.phoneNumber, device.role)
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
