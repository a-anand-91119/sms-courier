package dev.notyouraverage.smscourier.viewmodels

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.export.ExportConfig
import dev.notyouraverage.smscourier.export.ExportFormat
import dev.notyouraverage.smscourier.export.ExportFormatter
import dev.notyouraverage.smscourier.export.ExportManager
import dev.notyouraverage.smscourier.export.ExportState
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class ArchiveManagementViewModel(
    private val phoneNumber: String,
    private val role: DeviceRole,
    private val deviceRepository: PairedDeviceRepository,
    private val exportManager: ExportManager,
) : ViewModel() {

    private val _device = MutableStateFlow<PairedDevice?>(null)
    val device: StateFlow<PairedDevice?> = _device.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    // Pending export config
    private var pendingExportConfig: ExportConfig? = null

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

    /**
     * Called when user selects format in bottom sheet.
     * Stores config for SAF picker result and returns filename + MIME type.
     */
    fun prepareExport(format: ExportFormat, includeMetadata: Boolean): Pair<String, String> {
        pendingExportConfig = ExportConfig(
            format = format,
            includeMetadata = includeMetadata,
            devicePhone = phoneNumber,
            deviceRole = role.name,
        )
        val filename = exportManager.generateFilename(format)
        return filename to format.mimeType
    }

    /**
     * Called after SAF picker returns a Uri.
     * Performs the actual export to the selected file.
     */
    fun executeExport(uri: Uri, contentResolver: ContentResolver) {
        val config = pendingExportConfig ?: return
        pendingExportConfig = null

        _exportState.value = ExportState.Loading

        viewModelScope.launch {
            try {
                // Load data
                val data = exportManager.loadDeviceData(phoneNumber, role.name)

                // Format content
                val content = ExportFormatter.formatToString(data, config)

                // Write to file
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray(Charsets.UTF_8))
                } ?: throw IOException("Failed to open output stream")

                val filename = exportManager.generateFilename(config.format)
                _exportState.value = ExportState.Success(filename)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Export failed")
            }
        }
    }

    /**
     * Clears export state after snackbar is dismissed.
     */
    fun clearExportState() {
        _exportState.value = ExportState.Idle
    }

    class Factory(
        private val phoneNumber: String,
        private val role: String,
        private val deviceRepository: PairedDeviceRepository,
        private val exportManager: ExportManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ArchiveManagementViewModel(
                phoneNumber = phoneNumber,
                role = DeviceRole.valueOf(role),
                deviceRepository = deviceRepository,
                exportManager = exportManager,
            ) as T
        }
    }
}
