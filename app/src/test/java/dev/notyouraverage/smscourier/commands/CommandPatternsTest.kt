package dev.notyouraverage.smscourier.commands

import org.junit.Assert.*
import org.junit.Test

class CommandPatternsTest {

    // ==================== isSmscCommand ====================

    @Test
    fun `isSmscCommand returns true for SMSC prefix`() {
        assertTrue(CommandPatterns.isSmscCommand("SMSC PAIR_REQUEST"))
    }

    @Test
    fun `isSmscCommand returns true for lowercase smsc`() {
        assertTrue(CommandPatterns.isSmscCommand("smsc something"))
    }

    @Test
    fun `isSmscCommand returns true for mixed case`() {
        assertTrue(CommandPatterns.isSmscCommand("SmSc command"))
    }

    @Test
    fun `isSmscCommand returns false for non-SMSC message`() {
        assertFalse(CommandPatterns.isSmscCommand("Hello World"))
    }

    @Test
    fun `isSmscCommand returns false for OTP without C`() {
        assertFalse(CommandPatterns.isSmscCommand("OTP 123456"))
    }

    @Test
    fun `isSmscCommand returns false for empty string`() {
        assertFalse(CommandPatterns.isSmscCommand(""))
    }

    @Test
    fun `isSmscCommand handles leading whitespace`() {
        assertTrue(CommandPatterns.isSmscCommand("  SMSC command"))
    }

    // ==================== PAIR_REQUEST Pattern ====================

    @Test
    fun `PAIR_REQUEST pattern matches exact format`() {
        assertNotNull(CommandPatterns.PAIR_REQUEST.find("SMSC PAIR_REQUEST"))
    }

    @Test
    fun `PAIR_REQUEST pattern matches with trailing whitespace`() {
        assertNotNull(CommandPatterns.PAIR_REQUEST.find("SMSC PAIR_REQUEST  "))
    }

    @Test
    fun `PAIR_REQUEST pattern rejects trailing content`() {
        assertNull(CommandPatterns.PAIR_REQUEST.find("SMSC PAIR_REQUEST extra"))
    }

    @Test
    fun `PAIR_REQUEST pattern is case insensitive`() {
        assertNotNull(CommandPatterns.PAIR_REQUEST.find("smsc pair_request"))
    }

    // ==================== AUTH_CHALLENGE Pattern ====================

    @Test
    fun `AUTH_CHALLENGE pattern captures nonce`() {
        val match = CommandPatterns.AUTH_CHALLENGE.find("SMSC AUTH_CHALLENGE abc123")
        assertNotNull(match)
        assertEquals("abc123", match?.groupValues?.get(1))
    }

    @Test
    fun `AUTH_CHALLENGE pattern captures base64 nonce`() {
        val base64Nonce = "dGVzdE5vbmNlMTIzNDU2Nzg5MA=="
        val match = CommandPatterns.AUTH_CHALLENGE.find("SMSC AUTH_CHALLENGE $base64Nonce")
        assertNotNull(match)
        assertEquals(base64Nonce, match?.groupValues?.get(1))
    }

    @Test
    fun `AUTH_CHALLENGE pattern requires nonce`() {
        assertNull(CommandPatterns.AUTH_CHALLENGE.find("SMSC AUTH_CHALLENGE"))
    }

    // ==================== START_FORWARD Pattern ====================

    @Test
    fun `START_FORWARD pattern captures password`() {
        val match = CommandPatterns.START_FORWARD.find("SMSC START_FORWARD myPassword")
        assertNotNull(match)
        assertEquals("myPassword", match?.groupValues?.get(1))
    }

    @Test
    fun `START_FORWARD pattern captures password and duration`() {
        val match = CommandPatterns.START_FORWARD.find("SMSC START_FORWARD myPassword 60")
        assertNotNull(match)
        assertEquals("myPassword", match?.groupValues?.get(1))
        assertEquals("60", match?.groupValues?.get(2))
    }

    @Test
    fun `START_FORWARD pattern duration is optional`() {
        val match = CommandPatterns.START_FORWARD.find("SMSC START_FORWARD password123")
        assertNotNull(match)
        assertEquals("password123", match?.groupValues?.get(1))
        assertTrue(match?.groupValues?.get(2)?.isEmpty() == true)
    }

    @Test
    fun `START_FORWARD pattern requires password`() {
        assertNull(CommandPatterns.START_FORWARD.find("SMSC START_FORWARD"))
    }

    // ==================== FORWARD_DATA Pattern ====================

    @Test
    fun `FORWARD_DATA pattern captures sender in angle brackets`() {
        val match = CommandPatterns.FORWARD_DATA.find("SMSC FWD <+1234567890> Hello")
        assertNotNull(match)
        assertEquals("+1234567890", match?.groupValues?.get(1))
    }

    @Test
    fun `FORWARD_DATA pattern captures message`() {
        val match = CommandPatterns.FORWARD_DATA.find("SMSC FWD <+1234567890> Your OTP is 123456")
        assertNotNull(match)
        assertEquals("Your OTP is 123456", match?.groupValues?.get(2))
    }

    @Test
    fun `FORWARD_DATA pattern requires angle brackets`() {
        assertNull(CommandPatterns.FORWARD_DATA.find("SMSC FWD +1234567890 Hello"))
    }

    @Test
    fun `FORWARD_DATA pattern requires message`() {
        assertNull(CommandPatterns.FORWARD_DATA.find("SMSC FWD <+1234567890>"))
    }

    // ==================== FORWARD_DATA_ENCRYPTED Pattern ====================

    @Test
    fun `FORWARD_DATA_ENCRYPTED pattern captures entire encrypted payload`() {
        val encrypted = "SGVsbG8gV29ybGQh"
        val match = CommandPatterns.FORWARD_DATA_ENCRYPTED.find("SMSC FWDE $encrypted")
        assertNotNull(match)
        assertEquals(encrypted, match?.groupValues?.get(1))
    }

    @Test
    fun `FORWARD_DATA_ENCRYPTED pattern captures payload with special chars`() {
        val encrypted = "abc123+/=XYZ"
        val match = CommandPatterns.FORWARD_DATA_ENCRYPTED.find("SMSC FWDE $encrypted")
        assertNotNull(match)
        assertEquals(encrypted, match?.groupValues?.get(1))
    }

    @Test
    fun `FORWARD_DATA_ENCRYPTED pattern requires content`() {
        assertNull(CommandPatterns.FORWARD_DATA_ENCRYPTED.find("SMSC FWDE"))
    }

    // ==================== LEGACY_START_STOP Pattern ====================

    @Test
    fun `LEGACY_START_STOP pattern captures start command`() {
        val match = CommandPatterns.LEGACY_START_STOP.find("SMSC start password123")
        assertNotNull(match)
        assertEquals("start", match?.groupValues?.get(1))
        assertEquals("password123", match?.groupValues?.get(2))
    }

    @Test
    fun `LEGACY_START_STOP pattern captures stop command`() {
        val match = CommandPatterns.LEGACY_START_STOP.find("SMSC stop mySecret")
        assertNotNull(match)
        assertEquals("stop", match?.groupValues?.get(1))
        assertEquals("mySecret", match?.groupValues?.get(2))
    }

    @Test
    fun `LEGACY_START_STOP pattern is case insensitive`() {
        val match = CommandPatterns.LEGACY_START_STOP.find("SMSC START PASSWORD")
        assertNotNull(match)
    }

    // ==================== SMSC_COMMAND Master Pattern ====================

    @Test
    fun `SMSC_COMMAND pattern matches all known commands`() {
        val commands = listOf(
            "SMSC PAIR_REQUEST",
            "SMSC PAIR_APPROVED",
            "SMSC PAIR_REJECTED",
            "SMSC UNPAIR",
            "SMSC AUTH_REQUEST",
            "SMSC AUTH_CHALLENGE nonce",
            "SMSC START_FORWARD pass",
            "SMSC STOP_FORWARD",
            "SMSC FWD <phone> msg",
            "SMSC FWDE encrypted",
            "SMSC start pass",
            "SMSC stop pass",
        )

        commands.forEach { cmd ->
            assertNotNull("Should match: $cmd", CommandPatterns.SMSC_COMMAND.find(cmd))
        }
    }

    @Test
    fun `SMSC_COMMAND pattern does not match SMSC alone`() {
        // SMSC alone without command doesn't match (requires \s+ after SMSC)
        assertNull(CommandPatterns.SMSC_COMMAND.find("SMSC"))
    }

    @Test
    fun `SMSC_COMMAND pattern does not match invalid command types`() {
        // INVALID is not in the list of valid commands
        assertNull(CommandPatterns.SMSC_COMMAND.find("SMSC INVALID"))
    }
}
