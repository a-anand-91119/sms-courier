package dev.notyouraverage.smscourier.commands

import org.junit.Assert.*
import org.junit.Test

class CommandPatternsTest {

    // ==================== isOtpcCommand ====================

    @Test
    fun `isOtpcCommand returns true for OTPC prefix`() {
        assertTrue(CommandPatterns.isOtpcCommand("OTPC PAIR_REQUEST"))
    }

    @Test
    fun `isOtpcCommand returns true for lowercase otpc`() {
        assertTrue(CommandPatterns.isOtpcCommand("otpc something"))
    }

    @Test
    fun `isOtpcCommand returns true for mixed case`() {
        assertTrue(CommandPatterns.isOtpcCommand("OtPc command"))
    }

    @Test
    fun `isOtpcCommand returns false for non-OTPC message`() {
        assertFalse(CommandPatterns.isOtpcCommand("Hello World"))
    }

    @Test
    fun `isOtpcCommand returns false for OTP without C`() {
        assertFalse(CommandPatterns.isOtpcCommand("OTP 123456"))
    }

    @Test
    fun `isOtpcCommand returns false for empty string`() {
        assertFalse(CommandPatterns.isOtpcCommand(""))
    }

    @Test
    fun `isOtpcCommand handles leading whitespace`() {
        assertTrue(CommandPatterns.isOtpcCommand("  OTPC command"))
    }

    // ==================== PAIR_REQUEST Pattern ====================

    @Test
    fun `PAIR_REQUEST pattern matches exact format`() {
        assertNotNull(CommandPatterns.PAIR_REQUEST.find("OTPC PAIR_REQUEST"))
    }

    @Test
    fun `PAIR_REQUEST pattern matches with trailing whitespace`() {
        assertNotNull(CommandPatterns.PAIR_REQUEST.find("OTPC PAIR_REQUEST  "))
    }

    @Test
    fun `PAIR_REQUEST pattern rejects trailing content`() {
        assertNull(CommandPatterns.PAIR_REQUEST.find("OTPC PAIR_REQUEST extra"))
    }

    @Test
    fun `PAIR_REQUEST pattern is case insensitive`() {
        assertNotNull(CommandPatterns.PAIR_REQUEST.find("otpc pair_request"))
    }

    // ==================== AUTH_CHALLENGE Pattern ====================

    @Test
    fun `AUTH_CHALLENGE pattern captures nonce`() {
        val match = CommandPatterns.AUTH_CHALLENGE.find("OTPC AUTH_CHALLENGE abc123")
        assertNotNull(match)
        assertEquals("abc123", match?.groupValues?.get(1))
    }

    @Test
    fun `AUTH_CHALLENGE pattern captures base64 nonce`() {
        val base64Nonce = "dGVzdE5vbmNlMTIzNDU2Nzg5MA=="
        val match = CommandPatterns.AUTH_CHALLENGE.find("OTPC AUTH_CHALLENGE $base64Nonce")
        assertNotNull(match)
        assertEquals(base64Nonce, match?.groupValues?.get(1))
    }

    @Test
    fun `AUTH_CHALLENGE pattern requires nonce`() {
        assertNull(CommandPatterns.AUTH_CHALLENGE.find("OTPC AUTH_CHALLENGE"))
    }

    // ==================== START_FORWARD Pattern ====================

    @Test
    fun `START_FORWARD pattern captures password`() {
        val match = CommandPatterns.START_FORWARD.find("OTPC START_FORWARD myPassword")
        assertNotNull(match)
        assertEquals("myPassword", match?.groupValues?.get(1))
    }

    @Test
    fun `START_FORWARD pattern captures password and duration`() {
        val match = CommandPatterns.START_FORWARD.find("OTPC START_FORWARD myPassword 60")
        assertNotNull(match)
        assertEquals("myPassword", match?.groupValues?.get(1))
        assertEquals("60", match?.groupValues?.get(2))
    }

    @Test
    fun `START_FORWARD pattern duration is optional`() {
        val match = CommandPatterns.START_FORWARD.find("OTPC START_FORWARD password123")
        assertNotNull(match)
        assertEquals("password123", match?.groupValues?.get(1))
        assertTrue(match?.groupValues?.get(2)?.isEmpty() == true)
    }

    @Test
    fun `START_FORWARD pattern requires password`() {
        assertNull(CommandPatterns.START_FORWARD.find("OTPC START_FORWARD"))
    }

    // ==================== FORWARD_DATA Pattern ====================

    @Test
    fun `FORWARD_DATA pattern captures sender in angle brackets`() {
        val match = CommandPatterns.FORWARD_DATA.find("OTPC FWD <+1234567890> Hello")
        assertNotNull(match)
        assertEquals("+1234567890", match?.groupValues?.get(1))
    }

    @Test
    fun `FORWARD_DATA pattern captures message`() {
        val match = CommandPatterns.FORWARD_DATA.find("OTPC FWD <+1234567890> Your OTP is 123456")
        assertNotNull(match)
        assertEquals("Your OTP is 123456", match?.groupValues?.get(2))
    }

    @Test
    fun `FORWARD_DATA pattern requires angle brackets`() {
        assertNull(CommandPatterns.FORWARD_DATA.find("OTPC FWD +1234567890 Hello"))
    }

    @Test
    fun `FORWARD_DATA pattern requires message`() {
        assertNull(CommandPatterns.FORWARD_DATA.find("OTPC FWD <+1234567890>"))
    }

    // ==================== FORWARD_DATA_ENCRYPTED Pattern ====================

    @Test
    fun `FORWARD_DATA_ENCRYPTED pattern captures entire encrypted payload`() {
        val encrypted = "SGVsbG8gV29ybGQh"
        val match = CommandPatterns.FORWARD_DATA_ENCRYPTED.find("OTPC FWDE $encrypted")
        assertNotNull(match)
        assertEquals(encrypted, match?.groupValues?.get(1))
    }

    @Test
    fun `FORWARD_DATA_ENCRYPTED pattern captures payload with special chars`() {
        val encrypted = "abc123+/=XYZ"
        val match = CommandPatterns.FORWARD_DATA_ENCRYPTED.find("OTPC FWDE $encrypted")
        assertNotNull(match)
        assertEquals(encrypted, match?.groupValues?.get(1))
    }

    @Test
    fun `FORWARD_DATA_ENCRYPTED pattern requires content`() {
        assertNull(CommandPatterns.FORWARD_DATA_ENCRYPTED.find("OTPC FWDE"))
    }

    // ==================== LEGACY_START_STOP Pattern ====================

    @Test
    fun `LEGACY_START_STOP pattern captures start command`() {
        val match = CommandPatterns.LEGACY_START_STOP.find("OTPC start password123")
        assertNotNull(match)
        assertEquals("start", match?.groupValues?.get(1))
        assertEquals("password123", match?.groupValues?.get(2))
    }

    @Test
    fun `LEGACY_START_STOP pattern captures stop command`() {
        val match = CommandPatterns.LEGACY_START_STOP.find("OTPC stop mySecret")
        assertNotNull(match)
        assertEquals("stop", match?.groupValues?.get(1))
        assertEquals("mySecret", match?.groupValues?.get(2))
    }

    @Test
    fun `LEGACY_START_STOP pattern is case insensitive`() {
        val match = CommandPatterns.LEGACY_START_STOP.find("OTPC START PASSWORD")
        assertNotNull(match)
    }

    // ==================== OTPC_COMMAND Master Pattern ====================

    @Test
    fun `OTPC_COMMAND pattern matches all known commands`() {
        val commands = listOf(
            "OTPC PAIR_REQUEST",
            "OTPC PAIR_APPROVED",
            "OTPC PAIR_REJECTED",
            "OTPC UNPAIR",
            "OTPC AUTH_REQUEST",
            "OTPC AUTH_CHALLENGE nonce",
            "OTPC START_FORWARD pass",
            "OTPC STOP_FORWARD",
            "OTPC FWD <phone> msg",
            "OTPC FWDE encrypted",
            "OTPC start pass",
            "OTPC stop pass",
        )

        commands.forEach { cmd ->
            assertNotNull("Should match: $cmd", CommandPatterns.OTPC_COMMAND.find(cmd))
        }
    }

    @Test
    fun `OTPC_COMMAND pattern does not match OTPC alone`() {
        // OTPC alone without command doesn't match (requires \s+ after OTPC)
        assertNull(CommandPatterns.OTPC_COMMAND.find("OTPC"))
    }

    @Test
    fun `OTPC_COMMAND pattern does not match invalid command types`() {
        // INVALID is not in the list of valid commands
        assertNull(CommandPatterns.OTPC_COMMAND.find("OTPC INVALID"))
    }
}
