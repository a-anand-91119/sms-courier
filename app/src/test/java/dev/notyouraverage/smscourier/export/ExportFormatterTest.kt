package dev.notyouraverage.smscourier.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExportFormatterTest {

    // Fixed timestamp for deterministic tests: 2026-02-06 14:30:00 UTC
    private val fixedTimestamp = 1770406200000L
    private val fixedTimestampIso: String
        get() {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            return isoFormat.format(Date(fixedTimestamp))
        }
    private val fixedTimestampLocal: String
        get() {
            val localFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            return localFormat.format(Date(fixedTimestamp))
        }

    private fun createTestExportData(
        sessions: List<SessionExportData> = emptyList(),
        exportedAt: Long = fixedTimestamp,
        devicePhone: String = "+1234567890",
        deviceRole: String = "TARGET",
    ) = ExportData(
        sessions = sessions,
        exportedAt = exportedAt,
        devicePhone = devicePhone,
        deviceRole = deviceRole,
    )

    private fun createTestSession(
        sessionId: Long = 1L,
        startedAt: Long = fixedTimestamp,
        durationMinutes: Int = 30,
        messageCount: Int = 0,
        isActive: Boolean = false,
        messages: List<MessageExportData> = emptyList(),
    ) = SessionExportData(
        sessionId = sessionId,
        startedAt = startedAt,
        durationMinutes = durationMinutes,
        messageCount = messageCount,
        isActive = isActive,
        messages = messages,
    )

    private fun createTestMessage(
        senderNumber: String = "+5555555555",
        messageContent: String = "Test message",
        timestamp: Long = fixedTimestamp,
    ) = MessageExportData(
        senderNumber = senderNumber,
        messageContent = messageContent,
        timestamp = timestamp,
    )

    // ==================== CSV Formatter Tests ====================

    @Test
    fun `formatToCsv basic session export with messages`() {
        val message = createTestMessage(
            senderNumber = "+5555555555",
            messageContent = "Your OTP is 123456",
        )
        val session = createTestSession(
            sessionId = 1L,
            messages = listOf(message),
            messageCount = 1,
        )
        val data = createTestExportData(sessions = listOf(session))

        val csv = ExportFormatter.formatToCsv(data, includeMetadata = false)

        assertTrue("Should have header row", csv.startsWith("Session ID,Started At,Sender,Message,Timestamp"))
        assertTrue("Should have data row", csv.contains("1,"))
        assertTrue("Should include sender", csv.contains("+5555555555"))
        assertTrue("Should include message content", csv.contains("Your OTP is 123456"))
    }

    @Test
    fun `formatToCsv quote escaping doubles quotes`() {
        val message = createTestMessage(
            messageContent = "He said \"hello\" to me",
        )
        val session = createTestSession(messages = listOf(message), messageCount = 1)
        val data = createTestExportData(sessions = listOf(session))

        val csv = ExportFormatter.formatToCsv(data, includeMetadata = false)

        // RFC 4180: quotes inside quoted fields are doubled
        assertTrue("Should escape quotes by doubling", csv.contains("\"\"hello\"\""))
        // The whole field should be wrapped in quotes
        assertTrue("Should wrap field in quotes", csv.contains("\"He said \"\"hello\"\" to me\""))
    }

    @Test
    fun `formatToCsv comma handling wraps field in quotes`() {
        val message = createTestMessage(
            messageContent = "Hello, world",
        )
        val session = createTestSession(messages = listOf(message), messageCount = 1)
        val data = createTestExportData(sessions = listOf(session))

        val csv = ExportFormatter.formatToCsv(data, includeMetadata = false)

        // Field containing comma should be wrapped in quotes
        assertTrue("Should wrap comma-containing field", csv.contains("\"Hello, world\""))
    }

    @Test
    fun `formatToCsv newline handling wraps field in quotes`() {
        val message = createTestMessage(
            messageContent = "Line 1\nLine 2",
        )
        val session = createTestSession(messages = listOf(message), messageCount = 1)
        val data = createTestExportData(sessions = listOf(session))

        val csv = ExportFormatter.formatToCsv(data, includeMetadata = false)

        // Field containing newline should be wrapped in quotes
        assertTrue("Should wrap newline-containing field", csv.contains("\"Line 1\nLine 2\""))
    }

    @Test
    fun `formatToCsv carriage return handling wraps field in quotes`() {
        val message = createTestMessage(
            messageContent = "Line 1\r\nLine 2",
        )
        val session = createTestSession(messages = listOf(message), messageCount = 1)
        val data = createTestExportData(sessions = listOf(session))

        val csv = ExportFormatter.formatToCsv(data, includeMetadata = false)

        // Field containing carriage return should be wrapped in quotes
        assertTrue("Should wrap CRLF-containing field", csv.contains("\"Line 1\r\nLine 2\""))
    }

    @Test
    fun `formatToCsv empty messages list produces header only`() {
        val session = createTestSession(messages = emptyList(), messageCount = 0)
        val data = createTestExportData(sessions = listOf(session))

        val csv = ExportFormatter.formatToCsv(data, includeMetadata = false)

        val lines = csv.trim().lines()
        assertEquals("Should have header row only", 1, lines.size)
        assertEquals("Header should match format", "Session ID,Started At,Sender,Message,Timestamp", lines[0])
    }

    @Test
    fun `formatToCsv header row format verification`() {
        val data = createTestExportData(sessions = emptyList())

        val csv = ExportFormatter.formatToCsv(data, includeMetadata = false)

        val headerLine = csv.trim().lines().first()
        assertEquals("Session ID,Started At,Sender,Message,Timestamp", headerLine)
    }

    @Test
    fun `formatToCsv metadata comments are hash prefixed`() {
        val data = createTestExportData(
            devicePhone = "+1234567890",
            deviceRole = "TARGET",
        )

        val csv = ExportFormatter.formatToCsv(data, includeMetadata = true)

        assertTrue("Should have SMS Courier Export comment", csv.contains("# SMS Courier Export"))
        assertTrue("Should have Device comment", csv.contains("# Device: +1234567890 (TARGET)"))
        assertTrue("Should have Exported comment", csv.contains("# Exported:"))
        assertTrue("Should have Sessions comment", csv.contains("# Sessions:"))
    }

    @Test
    fun `formatToCsv include metadata toggle true`() {
        val data = createTestExportData()

        val csvWithMetadata = ExportFormatter.formatToCsv(data, includeMetadata = true)
        val csvWithoutMetadata = ExportFormatter.formatToCsv(data, includeMetadata = false)

        assertTrue("With metadata should have # prefix", csvWithMetadata.startsWith("#"))
        assertFalse("Without metadata should not have # prefix", csvWithoutMetadata.startsWith("#"))
    }

    @Test
    fun `formatToCsv include metadata toggle false`() {
        val data = createTestExportData()

        val csv = ExportFormatter.formatToCsv(data, includeMetadata = false)

        assertFalse("Should not have metadata comments", csv.contains("# SMS Courier Export"))
        assertTrue("Should start with header", csv.startsWith("Session ID,"))
    }

    @Test
    fun `formatToCsv complex escaping with quotes commas and newlines`() {
        val message = createTestMessage(
            messageContent = "He said, \"Hello\nWorld\"",
        )
        val session = createTestSession(messages = listOf(message), messageCount = 1)
        val data = createTestExportData(sessions = listOf(session))

        val csv = ExportFormatter.formatToCsv(data, includeMetadata = false)

        // Should handle all special characters together
        assertTrue("Should handle complex escaping", csv.contains("\"He said, \"\"Hello\nWorld\"\"\""))
    }

    // ==================== JSON Formatter Tests ====================

    @Test
    fun `formatToJson basic session export structure`() {
        val message = createTestMessage(
            senderNumber = "+5555555555",
            messageContent = "Test message",
        )
        val session = createTestSession(
            sessionId = 1L,
            durationMinutes = 30,
            messageCount = 1,
            isActive = false,
            messages = listOf(message),
        )
        val data = createTestExportData(sessions = listOf(session))

        val json = ExportFormatter.formatToJson(data, includeMetadata = false)

        assertTrue("Should have sessions array", json.contains("\"sessions\":"))
        assertTrue("Should have sessionId", json.contains("\"sessionId\": 1"))
        assertTrue("Should have durationMinutes", json.contains("\"durationMinutes\": 30"))
        assertTrue("Should have messageCount", json.contains("\"messageCount\": 1"))
        assertTrue("Should have isActive", json.contains("\"isActive\": false"))
        assertTrue("Should have messages array", json.contains("\"messages\":"))
    }

    @Test
    fun `formatToJson ISO 8601 timestamp format with timezone`() {
        val session = createTestSession(startedAt = fixedTimestamp)
        val data = createTestExportData(sessions = listOf(session))

        val json = ExportFormatter.formatToJson(data, includeMetadata = false)

        // ISO 8601 format should include date, time, and timezone offset
        // Format: YYYY-MM-DDTHH:MM:SS+HH:MM or similar
        assertTrue("Should have startedAt with ISO 8601 format", json.contains("\"startedAt\":"))
        // Check for ISO 8601 pattern (contains T separator and timezone)
        val timestampPattern = Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}([+-]\\d{2}:\\d{2}|Z)")
        assertTrue("Should match ISO 8601 pattern", timestampPattern.containsMatchIn(json))
    }

    @Test
    fun `formatToJson string escaping backslash`() {
        val message = createTestMessage(
            messageContent = "Path: C:\\Users\\test",
        )
        val session = createTestSession(messages = listOf(message), messageCount = 1)
        val data = createTestExportData(sessions = listOf(session))

        val json = ExportFormatter.formatToJson(data, includeMetadata = false)

        // Backslashes should be escaped as \\
        assertTrue("Should escape backslashes", json.contains("C:\\\\Users\\\\test"))
    }

    @Test
    fun `formatToJson string escaping quotes`() {
        val message = createTestMessage(
            messageContent = "He said \"hello\"",
        )
        val session = createTestSession(messages = listOf(message), messageCount = 1)
        val data = createTestExportData(sessions = listOf(session))

        val json = ExportFormatter.formatToJson(data, includeMetadata = false)

        // Quotes should be escaped as \"
        assertTrue("Should escape quotes", json.contains("He said \\\"hello\\\""))
    }

    @Test
    fun `formatToJson string escaping newline`() {
        val message = createTestMessage(
            messageContent = "Line 1\nLine 2",
        )
        val session = createTestSession(messages = listOf(message), messageCount = 1)
        val data = createTestExportData(sessions = listOf(session))

        val json = ExportFormatter.formatToJson(data, includeMetadata = false)

        // Newlines should be escaped as \n
        assertTrue("Should escape newlines", json.contains("Line 1\\nLine 2"))
    }

    @Test
    fun `formatToJson string escaping tab`() {
        val message = createTestMessage(
            messageContent = "Col1\tCol2",
        )
        val session = createTestSession(messages = listOf(message), messageCount = 1)
        val data = createTestExportData(sessions = listOf(session))

        val json = ExportFormatter.formatToJson(data, includeMetadata = false)

        // Tabs should be escaped as \t
        assertTrue("Should escape tabs", json.contains("Col1\\tCol2"))
    }

    @Test
    fun `formatToJson string escaping carriage return`() {
        val message = createTestMessage(
            messageContent = "Line 1\r\nLine 2",
        )
        val session = createTestSession(messages = listOf(message), messageCount = 1)
        val data = createTestExportData(sessions = listOf(session))

        val json = ExportFormatter.formatToJson(data, includeMetadata = false)

        // Carriage returns should be escaped as \r
        assertTrue("Should escape carriage returns", json.contains("\\r\\n"))
    }

    @Test
    fun `formatToJson nested structure sessions contain messages`() {
        val message1 = createTestMessage(senderNumber = "+1111111111")
        val message2 = createTestMessage(senderNumber = "+2222222222")
        val session = createTestSession(
            sessionId = 1L,
            messages = listOf(message1, message2),
            messageCount = 2,
        )
        val data = createTestExportData(sessions = listOf(session))

        val json = ExportFormatter.formatToJson(data, includeMetadata = false)

        // Verify nested structure
        assertTrue("Should have sessions array containing messages", json.contains("\"sessions\":"))
        assertTrue("Should have messages array inside session", json.contains("\"messages\":"))
        assertTrue("Should have first sender", json.contains("+1111111111"))
        assertTrue("Should have second sender", json.contains("+2222222222"))
    }

    @Test
    fun `formatToJson empty messages array`() {
        val session = createTestSession(messages = emptyList(), messageCount = 0)
        val data = createTestExportData(sessions = listOf(session))

        val json = ExportFormatter.formatToJson(data, includeMetadata = false)

        // Empty messages array should be []
        assertTrue("Should have empty messages array", json.contains("\"messages\": ["))
        // The array should close without content
        assertTrue("Should close empty messages array", json.contains("\"messages\": [\n      ]"))
    }

    @Test
    fun `formatToJson metadata object structure`() {
        val data = createTestExportData(
            devicePhone = "+1234567890",
            deviceRole = "TARGET",
        )

        val json = ExportFormatter.formatToJson(data, includeMetadata = true)

        assertTrue("Should have metadata object", json.contains("\"metadata\":"))
        assertTrue("Should have devicePhone in metadata", json.contains("\"devicePhone\": \"+1234567890\""))
        assertTrue("Should have deviceRole in metadata", json.contains("\"deviceRole\": \"TARGET\""))
        assertTrue("Should have exportedAt in metadata", json.contains("\"exportedAt\":"))
        assertTrue("Should have sessionCount in metadata", json.contains("\"sessionCount\":"))
    }

    @Test
    fun `formatToJson include metadata toggle true`() {
        val data = createTestExportData()

        val jsonWithMetadata = ExportFormatter.formatToJson(data, includeMetadata = true)
        val jsonWithoutMetadata = ExportFormatter.formatToJson(data, includeMetadata = false)

        assertTrue("With metadata should have metadata object", jsonWithMetadata.contains("\"metadata\":"))
        assertFalse("Without metadata should not have metadata object", jsonWithoutMetadata.contains("\"metadata\":"))
    }

    @Test
    fun `formatToJson include metadata toggle false`() {
        val data = createTestExportData()

        val json = ExportFormatter.formatToJson(data, includeMetadata = false)

        assertFalse("Should not have metadata", json.contains("\"metadata\":"))
        assertTrue("Should still have sessions", json.contains("\"sessions\":"))
    }

    @Test
    fun `formatToJson multiple sessions with proper comma separation`() {
        val session1 = createTestSession(sessionId = 1L)
        val session2 = createTestSession(sessionId = 2L)
        val data = createTestExportData(sessions = listOf(session1, session2))

        val json = ExportFormatter.formatToJson(data, includeMetadata = false)

        // Both sessions should be present
        assertTrue("Should have first session", json.contains("\"sessionId\": 1"))
        assertTrue("Should have second session", json.contains("\"sessionId\": 2"))
        // Verify valid JSON structure (no parsing errors)
        assertTrue("Should be valid JSON structure", json.contains("},"))
    }

    @Test
    fun `formatToJson message fields complete`() {
        val message = createTestMessage(
            senderNumber = "+5555555555",
            messageContent = "Test content",
            timestamp = fixedTimestamp,
        )
        val session = createTestSession(messages = listOf(message), messageCount = 1)
        val data = createTestExportData(sessions = listOf(session))

        val json = ExportFormatter.formatToJson(data, includeMetadata = false)

        assertTrue("Should have senderNumber", json.contains("\"senderNumber\": \"+5555555555\""))
        assertTrue("Should have messageContent", json.contains("\"messageContent\": \"Test content\""))
        assertTrue("Should have timestamp", json.contains("\"timestamp\":"))
    }

    // ==================== TXT Formatter Tests ====================

    @Test
    fun `formatToTxt chat log style format`() {
        val message = createTestMessage(
            senderNumber = "+5555555555",
            messageContent = "Hello world",
        )
        val session = createTestSession(messages = listOf(message), messageCount = 1)
        val data = createTestExportData(sessions = listOf(session))

        val txt = ExportFormatter.formatToTxt(data, includeMetadata = false)

        // Format should be: [YYYY-MM-DD HH:MM:SS] +phone: message
        assertTrue("Should have timestamp in brackets", txt.contains("["))
        assertTrue("Should have sender phone", txt.contains("+5555555555:"))
        assertTrue("Should have message content", txt.contains("Hello world"))
    }

    @Test
    fun `formatToTxt session header format`() {
        val session = createTestSession(
            sessionId = 5L,
            messageCount = 10,
            isActive = false,
        )
        val data = createTestExportData(sessions = listOf(session))

        val txt = ExportFormatter.formatToTxt(data, includeMetadata = false)

        // Session header format: --- Session X (datetime) - N messages - Status ---
        assertTrue("Should have session header with dashes", txt.contains("---"))
        assertTrue("Should have session ID", txt.contains("Session 5"))
        assertTrue("Should have message count", txt.contains("10 messages"))
        assertTrue("Should have status", txt.contains("Completed"))
    }

    @Test
    fun `formatToTxt message timestamp format`() {
        val message = createTestMessage(timestamp = fixedTimestamp)
        val session = createTestSession(messages = listOf(message), messageCount = 1)
        val data = createTestExportData(sessions = listOf(session))

        val txt = ExportFormatter.formatToTxt(data, includeMetadata = false)

        // Timestamp should be in local datetime format
        val expectedTimestamp = fixedTimestampLocal
        assertTrue("Should have formatted timestamp", txt.contains("[$expectedTimestamp]"))
    }

    @Test
    fun `formatToTxt active session display`() {
        val session = createTestSession(isActive = true)
        val data = createTestExportData(sessions = listOf(session))

        val txt = ExportFormatter.formatToTxt(data, includeMetadata = false)

        assertTrue("Should show Active status", txt.contains("Active"))
        assertFalse("Should not show Completed", txt.contains("Completed"))
    }

    @Test
    fun `formatToTxt completed session display`() {
        val session = createTestSession(isActive = false)
        val data = createTestExportData(sessions = listOf(session))

        val txt = ExportFormatter.formatToTxt(data, includeMetadata = false)

        assertTrue("Should show Completed status", txt.contains("Completed"))
    }

    @Test
    fun `formatToTxt empty messages handling`() {
        val session = createTestSession(messages = emptyList(), messageCount = 0)
        val data = createTestExportData(sessions = listOf(session))

        val txt = ExportFormatter.formatToTxt(data, includeMetadata = false)

        // Should have session header but no message lines
        assertTrue("Should have session header", txt.contains("--- Session"))
        assertTrue("Should show 0 messages", txt.contains("0 messages"))
    }

    @Test
    fun `formatToTxt multi-session formatting`() {
        val session1 = createTestSession(sessionId = 1L, messageCount = 5)
        val session2 = createTestSession(sessionId = 2L, messageCount = 3)
        val data = createTestExportData(sessions = listOf(session1, session2))

        val txt = ExportFormatter.formatToTxt(data, includeMetadata = false)

        // Both sessions should be present
        assertTrue("Should have first session", txt.contains("Session 1"))
        assertTrue("Should have second session", txt.contains("Session 2"))
        // Sessions should be separated
        val session1Index = txt.indexOf("Session 1")
        val session2Index = txt.indexOf("Session 2")
        assertTrue("Session 2 should come after Session 1", session2Index > session1Index)
    }

    @Test
    fun `formatToTxt include metadata header`() {
        val data = createTestExportData(
            devicePhone = "+1234567890",
            deviceRole = "TARGET",
        )

        val txt = ExportFormatter.formatToTxt(data, includeMetadata = true)

        assertTrue("Should have title", txt.contains("SMS Courier Export"))
        assertTrue("Should have separator", txt.contains("=================="))
        assertTrue("Should have device info", txt.contains("Device: +1234567890 (TARGET)"))
        assertTrue("Should have export date", txt.contains("Exported:"))
        assertTrue("Should have sessions count", txt.contains("Sessions:"))
    }

    @Test
    fun `formatToTxt include metadata toggle false`() {
        val data = createTestExportData()

        val txt = ExportFormatter.formatToTxt(data, includeMetadata = false)

        assertFalse("Should not have title header", txt.contains("SMS Courier Export"))
        assertFalse("Should not have separator", txt.contains("=================="))
    }

    @Test
    fun `formatToTxt messages preserve order`() {
        val message1 = createTestMessage(senderNumber = "+1111111111", timestamp = fixedTimestamp)
        val message2 = createTestMessage(senderNumber = "+2222222222", timestamp = fixedTimestamp + 1000)
        val session = createTestSession(messages = listOf(message1, message2), messageCount = 2)
        val data = createTestExportData(sessions = listOf(session))

        val txt = ExportFormatter.formatToTxt(data, includeMetadata = false)

        val msg1Index = txt.indexOf("+1111111111")
        val msg2Index = txt.indexOf("+2222222222")
        assertTrue("Messages should preserve order", msg1Index < msg2Index)
    }

    // ==================== formatToString Tests ====================

    @Test
    fun `formatToString delegates to CSV formatter`() {
        val data = createTestExportData()
        val config = ExportConfig(
            format = ExportFormat.CSV,
            includeMetadata = false,
            devicePhone = "+1234567890",
            deviceRole = "TARGET",
        )

        val result = ExportFormatter.formatToString(data, config)

        assertTrue("Should produce CSV format", result.startsWith("Session ID,"))
    }

    @Test
    fun `formatToString delegates to JSON formatter`() {
        val data = createTestExportData()
        val config = ExportConfig(
            format = ExportFormat.JSON,
            includeMetadata = false,
            devicePhone = "+1234567890",
            deviceRole = "TARGET",
        )

        val result = ExportFormatter.formatToString(data, config)

        assertTrue("Should produce JSON format", result.startsWith("{"))
        assertTrue("Should have sessions array", result.contains("\"sessions\":"))
    }

    @Test
    fun `formatToString delegates to TXT formatter`() {
        val session = createTestSession()
        val data = createTestExportData(sessions = listOf(session))
        val config = ExportConfig(
            format = ExportFormat.TXT,
            includeMetadata = false,
            devicePhone = "+1234567890",
            deviceRole = "TARGET",
        )

        val result = ExportFormatter.formatToString(data, config)

        assertTrue("Should produce TXT format", result.contains("--- Session"))
    }

    @Test
    fun `formatToString passes includeMetadata to formatter`() {
        val data = createTestExportData()
        val configWithMeta = ExportConfig(
            format = ExportFormat.CSV,
            includeMetadata = true,
            devicePhone = "+1234567890",
            deviceRole = "TARGET",
        )
        val configWithoutMeta = ExportConfig(
            format = ExportFormat.CSV,
            includeMetadata = false,
            devicePhone = "+1234567890",
            deviceRole = "TARGET",
        )

        val withMeta = ExportFormatter.formatToString(data, configWithMeta)
        val withoutMeta = ExportFormatter.formatToString(data, configWithoutMeta)

        assertTrue("With metadata should have comments", withMeta.contains("# SMS Courier Export"))
        assertFalse("Without metadata should not have comments", withoutMeta.contains("# SMS Courier Export"))
    }
}
