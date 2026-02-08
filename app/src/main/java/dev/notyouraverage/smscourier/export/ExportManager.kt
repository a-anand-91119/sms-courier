package dev.notyouraverage.smscourier.export

import dev.notyouraverage.smscourier.data.entities.ForwardedMessage
import dev.notyouraverage.smscourier.data.entities.ForwardingSession
import dev.notyouraverage.smscourier.repository.ForwardedMessageRepository
import dev.notyouraverage.smscourier.repository.ForwardingSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages export operations including data loading and filename generation.
 * Per CONTEXT.md: Supports single-session and per-device bulk export.
 *
 * This class does NOT write to files. Writing happens in the UI layer via
 * ContentResolver after SAF picker returns a Uri. This class only handles
 * data loading and formatting orchestration.
 */
class ExportManager(
    private val sessionRepository: ForwardingSessionRepository,
    private val messageRepository: ForwardedMessageRepository,
) {
    /**
     * Generates the default filename for export.
     * Format per CONTEXT.md: "smscourier_export_YYYYMMDD_HHMMSS.{ext}"
     */
    fun generateFilename(format: ExportFormat): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val timestamp = dateFormat.format(Date())
        return "smscourier_export_$timestamp${format.extension}"
    }

    /**
     * Gets the MIME type for the given export format.
     * Used by SAF for document creation.
     */
    fun getMimeType(format: ExportFormat): String = format.mimeType

    /**
     * Loads export data for a single session.
     *
     * @param session The session to export
     * @param devicePhone Phone number of the device
     * @param deviceRole Role string (SOURCE or TARGET)
     * @return ExportData ready for formatting
     */
    suspend fun loadSingleSessionData(
        session: ForwardingSession,
        devicePhone: String,
        deviceRole: String,
    ): ExportData = withContext(Dispatchers.IO) {
        val messages = messageRepository.getMessagesForSessionList(session.id)

        ExportData(
            sessions = listOf(
                SessionExportData(
                    sessionId = session.id,
                    startedAt = session.startedAt,
                    durationMinutes = session.durationMinutes,
                    messageCount = session.messageCount,
                    isActive = session.isActive,
                    messages = messages.map { it.toExportData() },
                ),
            ),
            exportedAt = System.currentTimeMillis(),
            devicePhone = devicePhone,
            deviceRole = deviceRole,
        )
    }

    /**
     * Loads export data for all sessions of a device.
     *
     * @param phoneNumber Phone number of the device
     * @param deviceRole Role string (SOURCE or TARGET)
     * @return ExportData ready for formatting
     */
    suspend fun loadDeviceData(
        phoneNumber: String,
        deviceRole: String,
    ): ExportData = withContext(Dispatchers.IO) {
        val sessions = sessionRepository.getSessionsForDeviceList(phoneNumber)
        val sessionsWithMessages = sessions.map { session ->
            val messages = messageRepository.getMessagesForSessionList(session.id)
            SessionExportData(
                sessionId = session.id,
                startedAt = session.startedAt,
                durationMinutes = session.durationMinutes,
                messageCount = session.messageCount,
                isActive = session.isActive,
                messages = messages.map { it.toExportData() },
            )
        }

        ExportData(
            sessions = sessionsWithMessages,
            exportedAt = System.currentTimeMillis(),
            devicePhone = phoneNumber,
            deviceRole = deviceRole,
        )
    }

    /**
     * Extension to convert ForwardedMessage entity to MessageExportData.
     */
    private fun ForwardedMessage.toExportData() = MessageExportData(
        senderNumber = senderNumber,
        messageContent = messageContent,
        timestamp = timestamp,
    )
}
