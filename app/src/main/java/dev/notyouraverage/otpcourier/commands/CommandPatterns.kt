package dev.notyouraverage.otpcourier.commands

object CommandPatterns {
    // Master pattern to detect any OTPC command
    val OTPC_COMMAND = Regex(
        """OTPC\s+(PAIR_REQUEST|PAIR_APPROVED|PAIR_REJECTED|UNPAIR|AUTH_REQUEST|""" +
            """AUTH_CHALLENGE|START_FORWARD|STOP_FORWARD|FWD|FWDE|start|stop)(?:\s+(.*))?""",
        RegexOption.IGNORE_CASE,
    )

    // Individual command patterns for extraction
    val PAIR_REQUEST = Regex(
        """OTPC\s+PAIR_REQUEST\s*$""",
        RegexOption.IGNORE_CASE,
    )

    // AUTH_REQUEST - Source requests authentication challenge from Target
    val AUTH_REQUEST = Regex(
        """OTPC\s+AUTH_REQUEST\s*$""",
        RegexOption.IGNORE_CASE,
    )

    // AUTH_CHALLENGE <nonce> - Target sends challenge to Source
    // Group 1: nonce (Base64 encoded)
    val AUTH_CHALLENGE = Regex(
        """OTPC\s+AUTH_CHALLENGE\s+(\S+)\s*$""",
        RegexOption.IGNORE_CASE,
    )

    val PAIR_APPROVED = Regex(
        """OTPC\s+PAIR_APPROVED\s*$""",
        RegexOption.IGNORE_CASE,
    )

    val PAIR_REJECTED = Regex(
        """OTPC\s+PAIR_REJECTED\s*$""",
        RegexOption.IGNORE_CASE,
    )

    val UNPAIR = Regex(
        """OTPC\s+UNPAIR\s*$""",
        RegexOption.IGNORE_CASE,
    )

    // START_FORWARD <response> [duration_minutes]
    // Group 1: HMAC response (challenge-response auth) or password (legacy)
    // Group 2: optional duration
    val START_FORWARD = Regex(
        """OTPC\s+START_FORWARD\s+(\S+)(?:\s+(\d+))?\s*$""",
        RegexOption.IGNORE_CASE,
    )

    val STOP_FORWARD = Regex(
        """OTPC\s+STOP_FORWARD\s*$""",
        RegexOption.IGNORE_CASE,
    )

    // FWD <+sender_phone> message
    // Group 1: original sender (inside angle brackets), Group 2: message
    val FORWARD_DATA = Regex(
        """OTPC\s+FWD\s+<([^>]+)>\s+(.+)$""",
        RegexOption.IGNORE_CASE,
    )

    // FWDE <encrypted_base64>
    // Group 1: encrypted content (Base64)
    val FORWARD_DATA_ENCRYPTED = Regex(
        """OTPC\s+FWDE\s+(.+)$""",
        RegexOption.IGNORE_CASE,
    )

    // Legacy commands for backward compatibility
    // OTPC start <password> or OTPC stop <password>
    // Group 1: start/stop, Group 2: password
    val LEGACY_START_STOP = Regex(
        """OTPC\s+(start|stop)\s+(\S+)""",
        RegexOption.IGNORE_CASE,
    )

    fun isOtpcCommand(message: String): Boolean {
        return message.trim().startsWith("OTPC", ignoreCase = true)
    }
}
