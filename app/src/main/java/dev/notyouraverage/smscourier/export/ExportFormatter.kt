package dev.notyouraverage.smscourier.export

import dev.notyouraverage.smscourier.utils.DateTimeFormatters

/**
 * Formats export data into CSV, JSON, or TXT string content.
 *
 * - CSV: RFC 4180 compliant with proper escaping
 * - JSON: Nested structure with sessions and messages arrays
 * - TXT: Chat log style for human readability
 */
object ExportFormatter {

    /**
     * Formats export data to string based on config format.
     *
     * @param data The export data to format
     * @param config Export configuration including format and metadata preference
     * @return Formatted string content ready for file writing
     */
    fun formatToString(data: ExportData, config: ExportConfig): String {
        return when (config.format) {
            ExportFormat.CSV -> formatToCsv(data, config.includeMetadata)
            ExportFormat.JSON -> formatToJson(data, config.includeMetadata)
            ExportFormat.TXT -> formatToTxt(data, config.includeMetadata)
        }
    }

    /**
     * Formats data as RFC 4180 compliant CSV.
     *
     * Header row: Session ID, Started At, Sender, Message, Timestamp
     * If includeMetadata: prepends comment lines with device info and export date.
     */
    fun formatToCsv(data: ExportData, includeMetadata: Boolean): String = buildString {
        if (includeMetadata) {
            appendLine("# SMS Courier Export")
            appendLine("# Device: ${data.devicePhone} (${data.deviceRole})")
            appendLine("# Exported: ${DateTimeFormatters.formatExportDateTime(data.exportedAt)}")
            appendLine("# Sessions: ${data.sessions.size}")
            appendLine()
        }

        // Header row
        appendLine("Session ID,Started At,Sender,Message,Timestamp")

        // Data rows
        for (session in data.sessions) {
            val sessionStarted = DateTimeFormatters.formatExportDateTime(session.startedAt)
            for (message in session.messages) {
                val messageTime = DateTimeFormatters.formatExportDateTime(message.timestamp)
                append(session.sessionId)
                append(",")
                append(escapeCsvValue(sessionStarted))
                append(",")
                append(escapeCsvValue(message.senderNumber))
                append(",")
                append(escapeCsvValue(message.messageContent))
                append(",")
                appendLine(escapeCsvValue(messageTime))
            }
        }
    }

    /**
     * Formats data as nested JSON structure.
     *
     * Structure:
     * {
     *   "metadata": { ... },  // only if includeMetadata
     *   "sessions": [{ "sessionId", "startedAt", "messages": [...] }]
     * }
     */
    fun formatToJson(data: ExportData, includeMetadata: Boolean): String = buildString {
        appendLine("{")

        if (includeMetadata) {
            appendLine("  \"metadata\": {")
            appendLine("    \"devicePhone\": ${escapeJsonString(data.devicePhone).wrapQuotes()},")
            appendLine("    \"deviceRole\": ${escapeJsonString(data.deviceRole).wrapQuotes()},")
            appendLine("    \"exportedAt\": ${escapeJsonString(DateTimeFormatters.formatIso8601(data.exportedAt)).wrapQuotes()},")
            appendLine("    \"sessionCount\": ${data.sessions.size}")
            appendLine("  },")
        }

        appendLine("  \"sessions\": [")

        data.sessions.forEachIndexed { sessionIndex, session ->
            appendLine("    {")
            appendLine("      \"sessionId\": ${session.sessionId},")
            appendLine("      \"startedAt\": ${escapeJsonString(DateTimeFormatters.formatIso8601(session.startedAt)).wrapQuotes()},")
            appendLine("      \"durationMinutes\": ${session.durationMinutes},")
            appendLine("      \"messageCount\": ${session.messageCount},")
            appendLine("      \"isActive\": ${session.isActive},")
            appendLine("      \"messages\": [")

            session.messages.forEachIndexed { messageIndex, message ->
                appendLine("        {")
                appendLine("          \"senderNumber\": ${escapeJsonString(message.senderNumber).wrapQuotes()},")
                appendLine("          \"messageContent\": ${escapeJsonString(message.messageContent).wrapQuotes()},")
                appendLine("          \"timestamp\": ${escapeJsonString(DateTimeFormatters.formatIso8601(message.timestamp)).wrapQuotes()}")
                if (messageIndex < session.messages.lastIndex) {
                    appendLine("        },")
                } else {
                    appendLine("        }")
                }
            }

            appendLine("      ]")
            if (sessionIndex < data.sessions.lastIndex) {
                appendLine("    },")
            } else {
                appendLine("    }")
            }
        }

        appendLine("  ]")
        append("}")
    }

    /**
     * Formats data as chat log style plain text.
     *
     * Format: [2026-02-06 10:30:00] +1234567890: Message content
     * Messages grouped by session with header line.
     */
    fun formatToTxt(data: ExportData, includeMetadata: Boolean): String = buildString {
        if (includeMetadata) {
            appendLine("SMS Courier Export")
            appendLine("==================")
            appendLine("Device: ${data.devicePhone} (${data.deviceRole})")
            appendLine("Exported: ${DateTimeFormatters.formatExportDateTime(data.exportedAt)}")
            appendLine("Sessions: ${data.sessions.size}")
            appendLine()
        }

        data.sessions.forEachIndexed { index, session ->
            val sessionStart = DateTimeFormatters.formatExportDateTime(session.startedAt)
            val statusText = if (session.isActive) "Active" else "Completed"
            appendLine("--- Session ${session.sessionId} ($sessionStart) - ${session.messageCount} messages - $statusText ---")
            appendLine()

            for (message in session.messages) {
                val messageTime = DateTimeFormatters.formatExportDateTime(message.timestamp)
                appendLine("[${messageTime}] ${message.senderNumber}: ${message.messageContent}")
            }

            if (index < data.sessions.lastIndex) {
                appendLine()
            }
        }
    }

    /**
     * Escapes a value for RFC 4180 CSV compliance.
     *
     * - Wraps in double quotes if contains comma, quote, or newline
     * - Escapes double quotes by doubling them
     */
    private fun escapeCsvValue(value: String): String {
        val needsQuoting = value.contains(',') ||
            value.contains('"') ||
            value.contains('\n') ||
            value.contains('\r')

        return if (needsQuoting) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * Escapes a string for JSON compliance.
     *
     * Escapes: backslash, double quote, newline, carriage return, tab, backspace, form feed
     * Note: Backslash must be escaped first to avoid double-escaping.
     */
    private fun escapeJsonString(value: String): String {
        return value
            .replace("\\", "\\\\")  // Backslash first!
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")  // Form feed
    }

    /**
     * Wraps a string in double quotes for JSON output.
     */
    private fun String.wrapQuotes(): String = "\"$this\""
}
