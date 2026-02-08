package dev.notyouraverage.smscourier.viewmodels

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dev.notyouraverage.smscourier.data.entities.ForwardingSession
import dev.notyouraverage.smscourier.export.ExportConfig
import dev.notyouraverage.smscourier.export.ExportFormat
import dev.notyouraverage.smscourier.export.ExportFormatter
import dev.notyouraverage.smscourier.export.ExportManager
import dev.notyouraverage.smscourier.export.ExportState
import dev.notyouraverage.smscourier.repository.ForwardedMessageRepository
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class SessionHistoryViewModel(
    private val sessionRepository: ForwardingSessionRepository,
    private val messageRepository: ForwardedMessageRepository,
    private val exportManager: ExportManager,
    val phoneNumber: String,
    val deviceRole: String,
) : ViewModel() {

    // Tab state - per CONTEXT.md: "Always opens to Sessions view by default, no persistence of toggle state"
    private val _selectedTab = MutableStateFlow(Tab.SESSIONS)
    val selectedTab: StateFlow<Tab> = _selectedTab.asStateFlow()

    // Export state for UI feedback
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    // Pending export config (set when user confirms format, cleared after SAF picker result)
    private var pendingExportConfig: ExportConfig? = null

    // Pending session for single-session export
    private var pendingExportSession: ForwardingSession? = null

    // Paged sessions flow - cachedIn survives configuration changes
    val sessions: Flow<PagingData<ForwardingSession>> =
        sessionRepository.getSessionsForDevicePaged(phoneNumber)
            .cachedIn(viewModelScope)

    // Paged contacts flow (distinct senders)
    val contacts: Flow<PagingData<String>> =
        messageRepository.getDistinctSendersForDevice(phoneNumber)
            .cachedIn(viewModelScope)

    // Selected session for bottom sheet
    private val _selectedSession = MutableStateFlow<ForwardingSession?>(null)
    val selectedSession: StateFlow<ForwardingSession?> = _selectedSession.asStateFlow()

    // Contact message counts (loaded on demand)
    private val _contactMessageCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val contactMessageCounts: StateFlow<Map<String, Int>> = _contactMessageCounts.asStateFlow()

    fun selectTab(tab: Tab) {
        _selectedTab.value = tab
    }

    fun selectSession(session: ForwardingSession?) {
        _selectedSession.value = session
    }

    fun loadContactMessageCount(senderNumber: String) {
        if (_contactMessageCounts.value.containsKey(senderNumber)) return
        viewModelScope.launch {
            val count = messageRepository.getMessageCountForSender(phoneNumber, senderNumber)
            _contactMessageCounts.value = _contactMessageCounts.value + (senderNumber to count)
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
            deviceRole = deviceRole,
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
                val data = exportManager.loadDeviceData(phoneNumber, deviceRole)

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
     * Prepares export for a single session.
     * Stores session for SAF picker result.
     */
    fun prepareSingleSessionExport(
        session: ForwardingSession,
        format: ExportFormat,
        includeMetadata: Boolean,
    ): Pair<String, String> {
        pendingExportConfig = ExportConfig(
            format = format,
            includeMetadata = includeMetadata,
            devicePhone = phoneNumber,
            deviceRole = deviceRole,
        )
        pendingExportSession = session
        val filename = exportManager.generateFilename(format)
        return filename to format.mimeType
    }

    /**
     * Executes single-session export after SAF picker returns.
     */
    fun executeSingleSessionExport(uri: Uri, contentResolver: ContentResolver) {
        val config = pendingExportConfig ?: return
        val session = pendingExportSession ?: return
        pendingExportConfig = null
        pendingExportSession = null

        _exportState.value = ExportState.Loading

        viewModelScope.launch {
            try {
                // Load single session data
                val data = exportManager.loadSingleSessionData(session, phoneNumber, deviceRole)

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

    enum class Tab {
        SESSIONS,
        CONTACTS,
    }

    class Factory(
        private val sessionRepository: ForwardingSessionRepository,
        private val messageRepository: ForwardedMessageRepository,
        private val exportManager: ExportManager,
        private val phoneNumber: String,
        private val deviceRole: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SessionHistoryViewModel(
                sessionRepository,
                messageRepository,
                exportManager,
                phoneNumber,
                deviceRole,
            ) as T
        }
    }
}
