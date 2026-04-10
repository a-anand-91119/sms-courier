package dev.notyouraverage.smscourier.viewmodels

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.notyouraverage.smscourier.data.entities.DeviceRole
import dev.notyouraverage.smscourier.data.entities.PairedDevice
import dev.notyouraverage.smscourier.data.entities.PairingStatus
import dev.notyouraverage.smscourier.export.ExportConfig
import dev.notyouraverage.smscourier.export.ExportFormat
import dev.notyouraverage.smscourier.export.ExportFormatter
import dev.notyouraverage.smscourier.export.ExportManager
import dev.notyouraverage.smscourier.export.ExportState
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import dev.notyouraverage.smscourier.repository.PairedDeviceRepository
import dev.notyouraverage.smscourier.services.SmsSender
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
    private val smsSender: SmsSender,
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
     * Checks if a device has an active session.
     */
    suspend fun hasActiveSession(device: PairedDevice): Boolean {
        return sessionRepository.getActiveSessionForDevice(device.phoneNumber) != null
    }

    /**
     * Unpairs a device by ending any active sessions, notifying the remote device, and archiving it.
     * The device will move from Active Devices to Removed Devices section.
     */
    fun unpairDevice(device: PairedDevice) {
        viewModelScope.launch {
            // End any active sessions for this device
            sessionRepository.endSessionForDevice(device.phoneNumber, "UNPAIR")

            // Only send UNPAIR SMS if pairing was actually established (APPROVED status)
            if (device.status == PairingStatus.APPROVED) {
                // Send role-specific UNPAIR - tell other device to delete the inverse role
                val roleToDeleteOnRemote = when (device.role) {
                    DeviceRole.SOURCE -> DeviceRole.TARGET
                    DeviceRole.TARGET -> DeviceRole.SOURCE
                }
                smsSender.sendUnpair(device.phoneNumber, roleToDeleteOnRemote)
            }

            // Archive the device
            deviceRepository.archiveDevice(
                phoneNumber = device.phoneNumber,
                role = device.role,
                initiatedBy = "USER",
            )
        }
    }

    /**
     * Archives a device locally without sending an UNPAIR SMS to the remote device.
     * - Ends any active session for the device (local only, no SMS).
     * - Archives the device so it appears in the Removed Devices section.
     * - The remote device is NOT notified; the pairing remains valid on their side.
     *
     * Use [unpairDevice] if you want to notify the other device.
     */
    fun archiveDevice(device: PairedDevice) {
        viewModelScope.launch {
            // End any active session locally (idempotent -- no-op if none)
            sessionRepository.endSessionForDevice(device.phoneNumber, "ARCHIVE")

            // Archive the device. initiatedBy = "USER" matches the existing unpairDevice()
            // convention AND the Phase 24 UAT DEVH-02 test's locked assertion.
            deviceRepository.archiveDevice(
                phoneNumber = device.phoneNumber,
                role = device.role,
                initiatedBy = "USER",
            )
        }
    }

    /**
     * Permanently deletes a device and all its history.
     * This removes all sessions (messages cascade delete) and the device record.
     */
    fun permanentlyDeleteDevice(device: PairedDevice) {
        viewModelScope.launch {
            // Delete all sessions (messages cascade via FK)
            sessionRepository.deleteAllForDevice(device.phoneNumber)
            // Delete the device record
            deviceRepository.delete(device)
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
        private val smsSender: SmsSender,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DeviceHistoryViewModel(deviceRepository, sessionRepository, exportManager, smsSender) as T
        }
    }
}
