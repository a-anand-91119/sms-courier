package dev.notyouraverage.smscourier.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArchiveManagementViewModel(
    private val phoneNumber: String,
    private val role: DeviceRole,
    private val deviceRepository: PairedDeviceRepository,
) : ViewModel() {

    private val _device = MutableStateFlow<PairedDevice?>(null)
    val device: StateFlow<PairedDevice?> = _device.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadDevice()
    }

    private fun loadDevice() {
        viewModelScope.launch {
            _isLoading.value = true
            _device.value = deviceRepository.getByPhoneNumberAndRole(phoneNumber, role)
            _isLoading.value = false
        }
    }

    /**
     * Returns human-readable removal reason.
     * Per ARCH-02: Shows "You initiated" or "Other side initiated"
     */
    fun getRemovalReason(): String {
        val device = _device.value ?: return "Unknown"
        return when (device.archivalInitiatedBy) {
            "LOCAL" -> "You initiated the removal"
            "REMOTE" -> "The other device initiated the removal"
            else -> "Removal reason unknown"
        }
    }

    fun deleteAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            _device.value?.let { device ->
                deviceRepository.delete(device)
            }
            onComplete()
        }
    }

    class Factory(
        private val phoneNumber: String,
        private val role: String,
        private val deviceRepository: PairedDeviceRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ArchiveManagementViewModel(
                phoneNumber = phoneNumber,
                role = DeviceRole.valueOf(role),
                deviceRepository = deviceRepository,
            ) as T
        }
    }
}
