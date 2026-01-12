package dev.notyouraverage.smscourier.commands

object CommandParser {

    fun parse(senderPhoneNumber: String, messageBody: String): ParsedCommand? {
        val trimmedMessage = messageBody.trim()

        // Check if it's an OTPC command at all
        if (!CommandPatterns.isOtpcCommand(trimmedMessage)) {
            return null
        }

        // Try each pattern in order of specificity

        // Pairing commands
        CommandPatterns.PAIR_REQUEST.find(trimmedMessage)?.let {
            return ParsedCommand.PairRequest(senderPhoneNumber)
        }

        CommandPatterns.PAIR_APPROVED.find(trimmedMessage)?.let {
            return ParsedCommand.PairApproved(senderPhoneNumber)
        }

        CommandPatterns.PAIR_REJECTED.find(trimmedMessage)?.let {
            return ParsedCommand.PairRejected(senderPhoneNumber)
        }

        CommandPatterns.UNPAIR.find(trimmedMessage)?.let {
            return ParsedCommand.Unpair(senderPhoneNumber)
        }

        // Authentication commands (challenge-response)
        CommandPatterns.AUTH_REQUEST.find(trimmedMessage)?.let {
            return ParsedCommand.AuthRequest(senderPhoneNumber)
        }

        CommandPatterns.AUTH_CHALLENGE.find(trimmedMessage)?.let { matchResult ->
            val nonce = matchResult.groupValues[1]
            return ParsedCommand.AuthChallenge(
                senderPhoneNumber = senderPhoneNumber,
                nonce = nonce,
            )
        }

        // Forwarding commands
        CommandPatterns.START_FORWARD.find(trimmedMessage)?.let { matchResult ->
            val password = matchResult.groupValues[1]
            val durationStr = matchResult.groupValues.getOrNull(2)
            val duration = durationStr?.takeIf { it.isNotEmpty() }?.toIntOrNull()
            return ParsedCommand.StartForward(
                senderPhoneNumber = senderPhoneNumber,
                password = password,
                durationMinutes = duration,
            )
        }

        CommandPatterns.STOP_FORWARD.find(trimmedMessage)?.let {
            return ParsedCommand.StopForward(senderPhoneNumber)
        }

        // Encrypted forward data (received on Source phone from Target)
        CommandPatterns.FORWARD_DATA_ENCRYPTED.find(trimmedMessage)?.let { matchResult ->
            val encryptedContent = matchResult.groupValues[1]
            return ParsedCommand.ForwardedDataEncrypted(
                senderPhoneNumber = senderPhoneNumber,
                encryptedContent = encryptedContent,
            )
        }

        // Forward data - unencrypted (received on Source phone from Target)
        CommandPatterns.FORWARD_DATA.find(trimmedMessage)?.let { matchResult ->
            val originalSender = matchResult.groupValues[1]
            val message = matchResult.groupValues[2]
            return ParsedCommand.ForwardedData(
                senderPhoneNumber = senderPhoneNumber,
                originalSender = originalSender,
                message = message,
            )
        }

        // Legacy commands (backward compatibility)
        CommandPatterns.LEGACY_START_STOP.find(trimmedMessage)?.let { matchResult ->
            val command = matchResult.groupValues[1].lowercase()
            val password = matchResult.groupValues[2]
            return when (command) {
                "start" -> ParsedCommand.LegacyStart(senderPhoneNumber, password)
                "stop" -> ParsedCommand.LegacyStop(senderPhoneNumber, password)
                else -> ParsedCommand.Unknown(senderPhoneNumber, trimmedMessage)
            }
        }

        // If it started with OTPC but didn't match any pattern
        return ParsedCommand.Unknown(senderPhoneNumber, trimmedMessage)
    }

    fun isCommand(messageBody: String): Boolean {
        return CommandPatterns.isOtpcCommand(messageBody.trim())
    }
}
