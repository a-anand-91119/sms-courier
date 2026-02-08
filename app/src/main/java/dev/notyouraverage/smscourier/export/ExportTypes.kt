package dev.notyouraverage.smscourier.export

/**
 * Export format options with file extension and MIME type.
 */
enum class ExportFormat(
    val extension: String,
    val mimeType: String,
) {
    CSV(".csv", "text/csv"),
    JSON(".json", "application/json"),
    TXT(".txt", "text/plain"),
}

/**
 * Configuration for an export operation.
 *
 * @param format The output format (CSV, JSON, or TXT)
 * @param includeMetadata Whether to include header with session metadata
 * @param devicePhone Phone number of the device being exported
 * @param deviceRole Role of the device (SOURCE or TARGET)
 */
data class ExportConfig(
    val format: ExportFormat,
    val includeMetadata: Boolean,
    val devicePhone: String,
    val deviceRole: String,
)

/**
 * Complete export data bundle ready for formatting.
 *
 * @param sessions List of sessions with their messages
 * @param exportedAt Timestamp when export was created
 * @param devicePhone Phone number of the device
 * @param deviceRole Role of the device (SOURCE or TARGET)
 */
data class ExportData(
    val sessions: List<SessionExportData>,
    val exportedAt: Long,
    val devicePhone: String,
    val deviceRole: String,
)

/**
 * Session data prepared for export.
 *
 * @param sessionId Unique session identifier
 * @param startedAt Timestamp when session started
 * @param durationMinutes Configured session duration
 * @param messageCount Number of messages in this session
 * @param isActive Whether session is currently active
 * @param messages List of messages in this session
 */
data class SessionExportData(
    val sessionId: Long,
    val startedAt: Long,
    val durationMinutes: Int,
    val messageCount: Int,
    val isActive: Boolean,
    val messages: List<MessageExportData>,
)

/**
 * Message data prepared for export.
 *
 * @param senderNumber Phone number of the message sender
 * @param messageContent The message text content
 * @param timestamp When the message was received
 */
data class MessageExportData(
    val senderNumber: String,
    val messageContent: String,
    val timestamp: Long,
)
