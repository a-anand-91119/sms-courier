package dev.notyouraverage.smscourier.viewmodels

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.export.ExportConfig
import dev.notyouraverage.smscourier.export.ExportFormat
import dev.notyouraverage.smscourier.export.ExportFormatter
import dev.notyouraverage.smscourier.export.ExportManager
import dev.notyouraverage.smscourier.export.ExportState
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

class DeviceHistoryViewModel(
    private val deviceRepository: PairedDeviceRepository,
    private val sessionRepository: ForwardingSessionRepository,
    private val exportManager: ExportManager,
) : ViewModel() {

    // Active devices combined with session status
    val activeDevices: StateFlow<List<DeviceWithActiveSession>> = combine(
        deviceRepository.getActiveDevices(),
        sessionRepository.getActiveSessions(),
    ) { devices, sessions ->
        val activePhoneNumbers = sessions.map { it.devicePhoneNumber }.toSet()
        devices.map { device ->
            DeviceWithActiveSession(
                device = device,
                hasActiveSession = device.phoneNumber in activePhoneNumbers,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    // Archived/removed devices
    val removedDevices: StateFlow<List<PairedDevice>> = deviceRepository.getArchivedDevices()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
        )

    // Loading state (true until first data arrives)
    val isLoading: StateFlow<Boolean> = combine(
        activeDevices,
        removedDevices,
    ) { _, _ ->
        false // Once we have any data, we're not loading
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        true,
    )

    // Export state for UI feedback
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    // Pending export configuration
    private var pendingExportConfig: ExportConfig? = null
    private var pendingExportDevice: PairedDevice? = null

    /**
     * Called when user selects format in bottom sheet.
     * Stores config for SAF picker result and returns filename + MIME type.
     */
    fun prepareExport(
        device: PairedDevice,
        format: ExportFormat,
        includeMetadata: Boolean,
    ): Pair<String, String> {
        pendingExportConfig = ExportConfig(
            format = format,
            includeMetadata = includeMetadata,
            devicePhone = device.phoneNumber,
            deviceRole = device.role.name,
        )
        pendingExportDevice = device
        val filename = exportManager.generateFilename(format)
        return filename to format.mimeType
    }

    /**
     * Called after SAF picker returns a Uri.
     * Performs the actual export to the selected file.
     */
    fun executeExport(uri: Uri, contentResolver: ContentResolver) {
        val config = pendingExportConfig ?: return
        val device = pendingExportDevice ?: return
        pendingExportConfig = null
        pendingExportDevice = null

        _exportState.value = ExportState.Loading

        viewModelScope.launch {
            try {
                // Load data
                val data = exportManager.loadDeviceData(device.phoneNumber, device.role.name)

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

    /**
     * Unpairs a device by archiving it.
     * The device will move from Active Devices to Removed Devices section.
     */
    fun unpairDevice(device: PairedDevice) {
        viewModelScope.launch {
            deviceRepository.archiveDevice(
                phoneNumber = device.phoneNumber,
                role = device.role,
                initiatedBy = "USER",
            )
        }
    }

    data class DeviceWithActiveSession(
        val device: PairedDevice,
        val hasActiveSession: Boolean,
    )

    class Factory(
        private val deviceRepository: PairedDeviceRepository,
        private val sessionRepository: ForwardingSessionRepository,
        private val exportManager: ExportManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DeviceHistoryViewModel(deviceRepository, sessionRepository, exportManager) as T
        }
    }
}
