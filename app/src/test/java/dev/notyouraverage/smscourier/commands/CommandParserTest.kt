package dev.notyouraverage.smscourier.commands

import org.junit.Assert.*
import org.junit.Test

class CommandParserTest {

    private val testPhoneNumber = "+1234567890"

    // ==================== Non-SMSC Messages ====================

    @Test
    fun `parse returns null for non-SMSC message`() {
        val result = CommandParser.parse(testPhoneNumber, "Hello World")
        assertNull(result)
    }

    @Test
    fun `parse returns null for empty message`() {
        val result = CommandParser.parse(testPhoneNumber, "")
        assertNull(result)
    }

    @Test
    fun `parse returns null for whitespace only message`() {
        val result = CommandParser.parse(testPhoneNumber, "   ")
        assertNull(result)
    }

    @Test
    fun `parse returns null for message starting with OTP but not SMSC`() {
        val result = CommandParser.parse(testPhoneNumber, "OTP 123456")
        assertNull(result)
    }

    // ==================== PAIR_REQUEST ====================

    @Test
    fun `parse PAIR_REQUEST returns PairRequest`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC PAIR_REQUEST")
        assertTrue(result is ParsedCommand.PairRequest)
        assertEquals(testPhoneNumber, result?.senderPhoneNumber)
    }

    @Test
    fun `parse PAIR_REQUEST case insensitive`() {
        val result = CommandParser.parse(testPhoneNumber, "smsc pair_request")
        assertTrue(result is ParsedCommand.PairRequest)
    }

    @Test
    fun `parse PAIR_REQUEST with mixed case`() {
        val result = CommandParser.parse(testPhoneNumber, "smsc PaIr_ReQuEsT")
        assertTrue(result is ParsedCommand.PairRequest)
    }

    @Test
    fun `parse PAIR_REQUEST with leading whitespace`() {
        val result = CommandParser.parse(testPhoneNumber, "  SMSC PAIR_REQUEST")
        assertTrue(result is ParsedCommand.PairRequest)
    }

    @Test
    fun `parse PAIR_REQUEST with trailing whitespace`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC PAIR_REQUEST  ")
        assertTrue(result is ParsedCommand.PairRequest)
    }

    // ==================== PAIR_APPROVED ====================

    @Test
    fun `parse PAIR_APPROVED returns PairApproved`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC PAIR_APPROVED")
        assertTrue(result is ParsedCommand.PairApproved)
        assertEquals(testPhoneNumber, result?.senderPhoneNumber)
    }

    @Test
    fun `parse PAIR_APPROVED case insensitive`() {
        val result = CommandParser.parse(testPhoneNumber, "smsc pair_approved")
        assertTrue(result is ParsedCommand.PairApproved)
    }

    // ==================== PAIR_REJECTED ====================

    @Test
    fun `parse PAIR_REJECTED returns PairRejected`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC PAIR_REJECTED")
        assertTrue(result is ParsedCommand.PairRejected)
        assertEquals(testPhoneNumber, result?.senderPhoneNumber)
    }

    @Test
    fun `parse PAIR_REJECTED case insensitive`() {
        val result = CommandParser.parse(testPhoneNumber, "smsc pair_rejected")
        assertTrue(result is ParsedCommand.PairRejected)
    }

    // ==================== UNPAIR ====================

    @Test
    fun `parse UNPAIR returns Unpair`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC UNPAIR")
        assertTrue(result is ParsedCommand.Unpair)
        assertEquals(testPhoneNumber, result?.senderPhoneNumber)
    }

    @Test
    fun `parse UNPAIR case insensitive`() {
        val result = CommandParser.parse(testPhoneNumber, "smsc unpair")
        assertTrue(result is ParsedCommand.Unpair)
    }

    // ==================== AUTH_REQUEST ====================

    @Test
    fun `parse AUTH_REQUEST returns AuthRequest`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC AUTH_REQUEST")
        assertTrue(result is ParsedCommand.AuthRequest)
        assertEquals(testPhoneNumber, result?.senderPhoneNumber)
    }

    @Test
    fun `parse AUTH_REQUEST case insensitive`() {
        val result = CommandParser.parse(testPhoneNumber, "smsc auth_request")
        assertTrue(result is ParsedCommand.AuthRequest)
    }

    // ==================== AUTH_CHALLENGE ====================

    @Test
    fun `parse AUTH_CHALLENGE extracts nonce`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC AUTH_CHALLENGE abc123nonce")
        assertTrue(result is ParsedCommand.AuthChallenge)
        assertEquals("abc123nonce", (result as ParsedCommand.AuthChallenge).nonce)
    }

    @Test
    fun `parse AUTH_CHALLENGE with base64 nonce`() {
        val base64Nonce = "dGVzdE5vbmNlMTIz"
        val result = CommandParser.parse(testPhoneNumber, "SMSC AUTH_CHALLENGE $base64Nonce")
        assertTrue(result is ParsedCommand.AuthChallenge)
        assertEquals(base64Nonce, (result as ParsedCommand.AuthChallenge).nonce)
    }

    @Test
    fun `parse AUTH_CHALLENGE case insensitive`() {
        val result = CommandParser.parse(testPhoneNumber, "smsc auth_challenge testNonce")
        assertTrue(result is ParsedCommand.AuthChallenge)
        assertEquals("testNonce", (result as ParsedCommand.AuthChallenge).nonce)
    }

    // ==================== START_FORWARD ====================

    @Test
    fun `parse START_FORWARD with password only`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC START_FORWARD myPassword123")
        assertTrue(result is ParsedCommand.StartForward)
        val cmd = result as ParsedCommand.StartForward
        assertEquals("myPassword123", cmd.password)
        assertNull(cmd.durationMinutes)
    }

    @Test
    fun `parse START_FORWARD with password and duration`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC START_FORWARD myPassword123 60")
        assertTrue(result is ParsedCommand.StartForward)
        val cmd = result as ParsedCommand.StartForward
        assertEquals("myPassword123", cmd.password)
        assertEquals(60, cmd.durationMinutes)
    }

    @Test
    fun `parse START_FORWARD with HMAC response format`() {
        val hmacResponse = "abc123def456ghi789jkl012mno345pqr678stu901vwx234="
        val result = CommandParser.parse(testPhoneNumber, "SMSC START_FORWARD $hmacResponse 30")
        assertTrue(result is ParsedCommand.StartForward)
        val cmd = result as ParsedCommand.StartForward
        assertEquals(hmacResponse, cmd.password)
        assertEquals(30, cmd.durationMinutes)
    }

    @Test
    fun `parse START_FORWARD case insensitive`() {
        val result = CommandParser.parse(testPhoneNumber, "smsc start_forward password123")
        assertTrue(result is ParsedCommand.StartForward)
    }

    // ==================== STOP_FORWARD ====================

    @Test
    fun `parse STOP_FORWARD returns StopForward`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC STOP_FORWARD")
        assertTrue(result is ParsedCommand.StopForward)
        assertEquals(testPhoneNumber, result?.senderPhoneNumber)
    }

    @Test
    fun `parse STOP_FORWARD case insensitive`() {
        val result = CommandParser.parse(testPhoneNumber, "smsc stop_forward")
        assertTrue(result is ParsedCommand.StopForward)
    }

    // ==================== FWD (Forward Data) ====================

    @Test
    fun `parse FWD extracts original sender and message`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC FWD <+9876543210> Your OTP is 123456")
        assertTrue(result is ParsedCommand.ForwardedData)
        val cmd = result as ParsedCommand.ForwardedData
        assertEquals("+9876543210", cmd.originalSender)
        assertEquals("Your OTP is 123456", cmd.message)
    }

    @Test
    fun `parse FWD with short phone number`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC FWD <12345> Short code message")
        assertTrue(result is ParsedCommand.ForwardedData)
        val cmd = result as ParsedCommand.ForwardedData
        assertEquals("12345", cmd.originalSender)
        assertEquals("Short code message", cmd.message)
    }

    @Test
    fun `parse FWD with message containing special characters`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC FWD <+1111111111> Code: 123-456, expires in 5 min!")
        assertTrue(result is ParsedCommand.ForwardedData)
        val cmd = result as ParsedCommand.ForwardedData
        assertEquals("Code: 123-456, expires in 5 min!", cmd.message)
    }

    @Test
    fun `parse FWD case insensitive`() {
        val result = CommandParser.parse(testPhoneNumber, "smsc fwd <+5555555555> Test message")
        assertTrue(result is ParsedCommand.ForwardedData)
    }

    // ==================== FWDE (Forward Data Encrypted) ====================

    @Test
    fun `parse FWDE extracts encrypted content`() {
        val encryptedContent = "SGVsbG8gV29ybGQhIFRoaXMgaXMgZW5jcnlwdGVk"
        val result = CommandParser.parse(testPhoneNumber, "SMSC FWDE $encryptedContent")
        assertTrue(result is ParsedCommand.ForwardedDataEncrypted)
        assertEquals(encryptedContent, (result as ParsedCommand.ForwardedDataEncrypted).encryptedContent)
    }

    @Test
    fun `parse FWDE with long encrypted content`() {
        val longContent = "a".repeat(500)
        val result = CommandParser.parse(testPhoneNumber, "SMSC FWDE $longContent")
        assertTrue(result is ParsedCommand.ForwardedDataEncrypted)
        assertEquals(longContent, (result as ParsedCommand.ForwardedDataEncrypted).encryptedContent)
    }

    @Test
    fun `parse FWDE case insensitive`() {
        val result = CommandParser.parse(testPhoneNumber, "smsc fwde encryptedData123")
        assertTrue(result is ParsedCommand.ForwardedDataEncrypted)
    }

    // ==================== Legacy Commands ====================

    @Test
    fun `parse legacy start command`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC start secretPassword")
        assertTrue(result is ParsedCommand.LegacyStart)
        assertEquals("secretPassword", (result as ParsedCommand.LegacyStart).password)
    }

    @Test
    fun `parse legacy stop command`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC stop secretPassword")
        assertTrue(result is ParsedCommand.LegacyStop)
        assertEquals("secretPassword", (result as ParsedCommand.LegacyStop).password)
    }

    @Test
    fun `parse legacy command case insensitive`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC START password123")
        assertTrue(result is ParsedCommand.LegacyStart)
    }

    // ==================== Unknown Commands ====================

    @Test
    fun `parse unknown SMSC command returns Unknown`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC INVALID_COMMAND")
        assertTrue(result is ParsedCommand.Unknown)
        assertEquals("SMSC INVALID_COMMAND", (result as ParsedCommand.Unknown).rawMessage)
    }

    @Test
    fun `parse malformed SMSC command returns Unknown`() {
        val result = CommandParser.parse(testPhoneNumber, "SMSC")
        assertTrue(result is ParsedCommand.Unknown)
    }

    // ==================== isCommand ====================

    @Test
    fun `isCommand returns true for SMSC prefix`() {
        assertTrue(CommandParser.isCommand("SMSC PAIR_REQUEST"))
    }

    @Test
    fun `isCommand returns false for non-SMSC message`() {
        assertFalse(CommandParser.isCommand("Hello World"))
    }

    @Test
    fun `isCommand is case insensitive`() {
        assertTrue(CommandParser.isCommand("smsc something"))
    }

    @Test
    fun `isCommand handles whitespace`() {
        assertTrue(CommandParser.isCommand("  SMSC PAIR_REQUEST  "))
    }

    @Test
    fun `isCommand returns false for empty string`() {
        assertFalse(CommandParser.isCommand(""))
    }
}
